(ns pingpong.server
  (:require [clojure.java.io :as io]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [taoensso.sente :as sente]
            [org.httpkit.server :refer [run-server]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [pingpong.websocket :refer [ring-ajax-get-or-ws-handshake ring-ajax-post ch-chsk]]
            [pingpong.utils :as utils]))



(defroutes main-routes
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req))
  (route/not-found "Page not found"))


;; Send state to other player.
(add-watch utils/last-changed-uid
           nil
           (fn [_ _ _ p1-uid]
            (let [games @utils/follow-games
                  p1 (get games p1-uid)
                  p2-uid (:opp-uid p1)]
              (when p2-uid
                (let [p2 (get games p2-uid)
                      p2-state (:state p2)]
                  ;; Server waits both players before sending new states.
                  (when p2-state
                    (let [p1-callback (:callback p1)
                          p2-callback (:callback p2)
                          [new-state-p1
                           new-state-p2] (utils/states-to-players (:state p1) p2-state)]
                      (p1-callback new-state-p1)
                      (p2-callback new-state-p2))
                    ;; Reset states.
                    (swap! utils/follow-games assoc-in [p1-uid :state] nil)
                    (swap! utils/follow-games assoc-in [p2-uid :state] nil)))))))


;;; Events --->
; checks for event id
(defmulti event :id)


(defmethod event :default [{:keys [event]}]
  (prn "Default server" event))

(defmethod event :chsk/ws-ping [ev-msg]
  nil)

;; Put new client to game.
(defmethod event :chsk/uidport-open [{:keys [uid]}]
  (utils/uid-to-game! uid)
  (prn "Client added to game" uid))

;; Remove offline client from game.
(defmethod event :chsk/uidport-close [{:keys [uid]}]
  (utils/remove-uid-from-game! uid)
  (prn "Client removed from game" uid))

;; States from players.
(defmethod event :pingpong/state [{:keys [uid ?data ?reply-fn]}]
  (swap! utils/follow-games assoc-in [uid :state] ?data)
  (swap! utils/follow-games assoc-in [uid :callback] ?reply-fn)
  (reset! utils/last-changed-uid uid))


;;; Router --->
(defonce router_ (atom nil))

;; Stop router if we aware of any router stopper callback function.
(defn stop-router! []
   (when-let [stop-f @router_] (stop-f)))

;; Stop and start router while storing the router stop-function in
;; router_ atom.
(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-chsk-router! ch-chsk event)))

(start-router!)


(defn -main [& args]
  (let [port 8090
        handler (if true
                    (wrap-reload (wrap-defaults #'main-routes site-defaults))
                    (wrap-defaults main-routes site-defaults))]
    (run-server handler {:port port})
    (prn "Serveri pyörii" port)))
