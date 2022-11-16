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

(defn calc-avg [value-1 value-2]
  (int (/ (+ value-1 value-2) 2)))

(defn sign [value]
  (if (< value 0) 1 -1))

; (defn ball-state-to-p1 [s1 s2]
;   (let [p2-ball (reverse-x (first s2))
;         p2-ball-dir (reverse-x (second s2))
;         ball-avg (mapv calc-avg (first s1) p2-ball)
;         p1-ball-dir-sign (sign (first (second s1)))
;         p2-ball-dir-sign (sign (first p2-ball-dir))
;         ball-dir-avg (if (not= p1-ball-dir-sign p2-ball-dir-sign)
;                          p2-ball-dir
;                          (mapv calc-avg (second s1) p2-ball-dir))
;         ball-speed-avg (/ (+ (nth s1 2) (nth s2 2)) 2)]
;     [ball-avg
;      ball-dir-avg
;      ball-speed-avg]))

(defn ball-state-to-p1 [state]
  [(first state) (second state) (nth state 2)])

(defn reverse-x-ball [ball-state]
  [(reverse-x (first ball-state))
   (reverse-x (second ball-state))
   (last ball-state)])

(defn new-state [ball-state [_ _ _ opp-bat opp-bat-dir]]
  (concat ball-state [opp-bat opp-bat-dir]))

;; Add uid to game.
(defn uid-to-game! [client-uid]
  ;; Try find opponent
  (loop [games (keys @follow-games)]
    (if (empty? games)
      ;; No other players or all games are full.
      (swap! follow-games assoc client-uid empty-game)
      (let [player-uid (first games)
            {:keys [opp-uid]} (get @follow-games player-uid)]
        (if (or opp-uid (= player-uid client-uid))
          (recur (rest games))
          ;; Opponent found.
          (let [game-p1 (assoc empty-game :opp-uid player-uid)
                game-p2 (assoc empty-game :opp-uid client-uid)]
            (swap! follow-games assoc player-uid game-p2)
            (swap! follow-games assoc client-uid game-p1)
            (chsk-send! player-uid [:pingpong/game-on])
            (chsk-send! client-uid [:pingpong/game-on])))))))

(defn remove-uid-from-game! [client-uid]
  (let [opp-uid (get-in @follow-games [client-uid :opp-uid])]
    (when opp-uid
      (chsk-send! opp-uid [:pingpong/game-off])
      (uid-to-game! opp-uid))
  (swap! follow-games dissoc client-uid)))
