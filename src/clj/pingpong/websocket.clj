(ns pingpong.websocket
  (:require [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [pingpong.utils :as utils]))


(def running-uid (atom 0))

(defn uid-to-client! [ring-req]
  (swap! running-uid inc))


;;; Sente channels --->
(let [{:keys [ch-recv send-fn connected-uids ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket-server! (get-sch-adapter) 
                                         {:user-id-fn uid-to-client!
                                          :csrf-token-fn nil})]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv)         ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn)         ; ChannelSocket's send API fn
  (def connected-uids                connected-uids)) ; Watchable, read-only atom


;; Send state to other player.
(add-watch utils/last-changed-uid
           nil
           (fn [_ _ _ p1-uid]
            (let [games @utils/follow-games
                  p1 (get games p1-uid)
                  p2-uid (:opp-uid p1)]
              (when p2-uid
                (let [p2 (get games p2-uid)]
                  ;; Server waits both players before sending new states.
                  (when (:state p2)
                    (let [p1-callback (:callback p1)
                          p2-callback (:callback p2)
                          [new-state-p1
                           new-state-p2] (utils/states-to-players! p1 p2)]
                      (p1-callback new-state-p1)
                      (p2-callback new-state-p2))
                    ;; Reset states.
                    (swap! utils/follow-games assoc-in [p1-uid :state] nil)
                    (swap! utils/follow-games assoc-in [p2-uid :state] nil)))))))

(defn uid-to-game! [client-uid]
  ;; Try find opponent
  (loop [games (keys @utils/follow-games)]
    (if (empty? games)
      ;; No other players or all games are full.
      (do (swap! utils/follow-games assoc client-uid utils/empty-game)
          (prn "Client waiting opponent" client-uid))
      (let [player-uid (first games)
            {:keys [opp-uid]} (get @utils/follow-games player-uid)]
        (if (or opp-uid (= player-uid client-uid))
          (recur (rest games))
          ;; Opponent found.
          (let [game-p1 (assoc utils/empty-game :opp-uid player-uid)]
            (swap! utils/follow-games assoc client-uid game-p1)
            (swap! utils/follow-games assoc-in [player-uid :opp-uid] client-uid)
            (prn "Clients start a game" client-uid player-uid)
            (chsk-send! player-uid [:pingpong/game-on])
            (chsk-send! client-uid [:pingpong/game-on])))))))

(defn remove-uid-from-game! [client-uid]
  (let [opp-uid (get-in @utils/follow-games [client-uid :opp-uid])]
    (when opp-uid
      (chsk-send! opp-uid [:pingpong/game-off])
      (swap! utils/follow-games assoc-in [opp-uid :opp-uid] nil)
      (prn "Clients removed from game" client-uid opp-uid)
      (uid-to-game! opp-uid)))
  (swap! utils/follow-games dissoc client-uid)
  (prn "Client removed from queue" client-uid))


;;; Events --->
; checks for event id
(defmulti event :id)


(defmethod event :default [{:keys [event]}]
  (prn "Default server" event))

(defmethod event :chsk/ws-ping [ev-msg]
  nil)

;; Put new client to game.
(defmethod event :chsk/uidport-open [{:keys [uid]}]
  (uid-to-game! uid))

;; Remove offline client from game.
(defmethod event :chsk/uidport-close [{:keys [uid]}]
  (remove-uid-from-game! uid))

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
