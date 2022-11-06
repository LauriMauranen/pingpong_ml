(ns pingpong.client
  (:require [taoensso.sente :as sente :refer [cb-success?]]
            [pingpong.constants :as c]))


;;; Sente channels --->
(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client! "/chsk" nil {:type :auto
                                                      :host "localhost"
                                                      :port 8090})]

  (def chsk       chsk)
  (def ch-chsk    ch-recv)  ;; ChannelSocket's receive channel
  (def chsk-send! send-fn)  ;; ChannelSocket's send API fn
  (def chsk-state state))   ;; Watchable, read-only atom


;; Here we store server state.
(def server-state (atom {:ball [0 0]
                         :ball-dir [(dec (* 2 (rand-int 2))) 0]
                         :ball-speed c/ball-start-speed
                         :opponent-bat (- (/ c/bat-height 2))
                         :opponent-bat-dir 0
                         :player-score 0
                         :opponent-score 0
                         :game-on? false
                         :state-used? false}))


;; Send state to server.
(defn send-state-to-server!
  [{:keys [ball ball-dir ball-speed
           player-bat player-bat-dir]}]
  (chsk-send!
    [:pingpong/state [ball
                      ball-dir
                      ball-speed
                      player-bat
                      player-bat-dir]]
    100 ;; Timeout
    (fn [reply]
      (when (cb-success? reply)
        (let [new-state (zipmap [:ball
                                 :ball-dir
                                 :ball-speed
                                 :opponent-bat
                                 :opponent-bat-dir] reply)
              new-state (assoc new-state :state-used? false)]
          (swap! server-state into new-state))))))



;;; Event handler --->
(defmulti event :id)

(defmethod event :default [{:keys [event]}]
  (prn "Default client" event))

(defmethod event :chsk/recv [{:as ev-msg :keys [?data]}]
  (prn "Receive" ?data)
  (case (first ?data)
    :pingpong/game-on? (swap! server-state assoc :game-on? (second ?data))
    nil))


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
