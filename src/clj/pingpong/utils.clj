(ns pingpong.utils
  (:require [pingpong.websocket :refer [chsk-send!]]))


(def follow-games (atom {}))
(def last-changed-uid (atom nil))

(def empty-game {:opp-uid nil
                 :p-score 0
                 :opp-score 0
                 :state nil
                 :callback nil})


;; Reverse x-axis because both players see's themselves on right.
(defn reverse-x [v]
  [(- (first v)) (second v)])


(defn state-for-client
  [[ball ball-dir ball-speed bat bat-dir]]
  [(reverse-x ball)
   (reverse-x ball-dir)
   ball-speed
   bat
   bat-dir])


;; Add uid to game.
(defn uid-to-game! [client-uid]
  ;; Try find opponent
  (loop [games (keys @follow-games)]
    (if (empty? games)
      ;; No other players or all games are full.
      (swap! follow-games assoc client-uid empty-game)
      (let [player-uid (first games)
            {:keys [opp-uid]} (get @follow-games player-uid)]
        (if opp-uid
          (recur (rest games))
          ;; Opponent found.
          (let [game (assoc empty-game :opp-uid player-uid)]
            (swap! follow-games assoc-in [player-uid :opp-uid] client-uid)
            (swap! follow-games assoc client-uid game)
            (chsk-send! player-uid [:pingpong/game-on? true])
            (chsk-send! client-uid [:pingpong/game-on? true])))))))


(defn remove-uid-from-game! [client-uid]
  (let [opp-uid (get-in @follow-games [client-uid :opp-uid])]
    (when opp-uid
      (swap! follow-games assoc opp-uid empty-game)
      (chsk-send! opp-uid [:pingpong/game-on? false]))
  (swap! follow-games dissoc client-uid)))
