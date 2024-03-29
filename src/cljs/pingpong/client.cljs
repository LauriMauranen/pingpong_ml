(ns pingpong.client
  (:require [taoensso.sente :as sente :refer [cb-success?]]
            [pingpong.constants :as c]
            [pingpong.utils :refer [make-game-list]]
            [pingpong.state :refer [app-state]]))


(goog-define WS_PORT "undefined")
(def ws-conf (if (identical? WS_PORT "undefined") {} {:port WS_PORT} ))


;;; Sente channels --->
(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client! "/chsk" nil ws-conf)]

  (def chsk       chsk)
  (def ch-chsk    ch-recv)  ;; ChannelSocket's receive channel
  (def chsk-send! send-fn)  ;; ChannelSocket's send API fn
  (def chsk-state state))   ;; Watchable, read-only atom


;; Here we store server state.
(def server-state (atom {:ball [0 0]
                         :ball-dir [(dec (* (rand-int 2) 2)) 0]
                         :ball-speed c/ball-start-speed
                         :opponent-bat (- (/ c/bat-height 2))
                         :opponent-bat-dir 0
                         :player-score 0
                         :opponent-score 0
                         :game-on? false
                         :state-used? true
                         :can-score-inc? true}))


;; Send state to server.
(defn send-state-to-server!
  [{:keys [ball ball-dir ball-speed player-bat
           player-bat-dir player-score opponent-score]}]
  (chsk-send!
    [:pingpong/state [ball
                      ball-dir
                      ball-speed
                      player-bat
                      player-bat-dir
                      player-score
                      opponent-score]]
    c/timeout ;; Timeout
    (fn [reply]
      (when (cb-success? reply)
        (let [new-state (zipmap [:ball
                                 :ball-dir
                                 :ball-speed
                                 :opponent-bat
                                 :opponent-bat-dir
                                 :player-score
                                 :opponent-score
                                 :can-score-inc?] reply)
              new-state (assoc new-state :state-used? false)]
          ; (prn new-state)
          (swap! server-state into new-state))))))

(defn ask-games-data! []
  (chsk-send!
    [:pingpong/games]
    c/timeout ;; Timeout
    (fn [reply]
      (when (cb-success? reply)
        (swap! app-state assoc :games (make-game-list reply))))))

(defn send-username-to-server! [username]
  (chsk-send! [:pingpong/username username] c/timeout))


;;; Event handler --->
(defmulti event :id)

(defmethod event :default [{:keys [event]}]
  ; (prn "Default client" event)
  )

(defmethod event :chsk/recv [{:as ev-msg :keys [?data]}]
  (let [id (first ?data)
        data (second ?data)]
    ; (prn "Receive" id data)
    (case id
      :pingpong/game-on (do (swap! server-state assoc :game-on? true)
                            (swap! server-state assoc :player-score 0)
                            (swap! server-state assoc :opponent-score 0))
      :pingpong/game-off (swap! server-state assoc :game-on? false)
      nil)))


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
