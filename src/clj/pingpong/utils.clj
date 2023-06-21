(ns pingpong.utils
  (:require [pingpong.websocket :refer [chsk-send!]]))


(def follow-games (atom {}))
(def last-changed-uid (atom nil))

(def empty-game {:opp-uid nil
                 :state nil
                 :callback nil})


;; Reverse x-axis because both players see's themselves on right.
(defn reverse-x [v]
  [(- (first v)) (second v)])

(defn reverse-x-ball-state [ball-s]
  [(reverse-x (first ball-s))
   (reverse-x (second ball-s))
   (nth ball-s 2)])

(defn calc-avg [a b] 
  (/ (+ a b) 2))

(defn new-state [ball-state [_ _ _ opp-bat opp-bat-dir _ _] p-score opp-score]
  (concat ball-state [opp-bat opp-bat-dir] [p-score opp-score]))

(defn calc-ball-state [p1-ball-s p2-ball-s]
  [(mapv calc-avg (first p1-ball-s) (reverse-x (first p2-ball-s)))
   (second p1-ball-s)
   (calc-avg (nth p1-ball-s 2) (nth p2-ball-s 2))])

(defn balls-and-scores-to-players [p1-state p2-state]  
  (let [p1-score (max (nth p1-state 5) (nth p2-state 6))
        p2-score (max (nth p1-state 6) (nth p2-state 5))
        ball-state (calc-ball-state (take 3 p1-state) (take 3 p2-state))
        ball-speed (nth ball-state 2)]
    (if (or (< (nth p1-state 5) p1-score) (< (nth p2-state 5) p2-score))
      (let [p1-ball-state [[0 0] [(dec (* 2 (rand-int 2))) 0] ball-speed]
            p2-ball-state (reverse-x-ball-state p1-ball-state)]
        [p1-ball-state p2-ball-state p1-score p2-score])
      [ball-state (reverse-x-ball-state ball-state) p1-score p2-score])))

(defn states-to-players [p1-state p2-state]
  (let [[ball-state-p1 
         ball-state-p2
         p1-score
         p2-score] (balls-and-scores-to-players p1-state p2-state)
        new-state-p1 (new-state ball-state-p1 p2-state p1-score p2-score)
        new-state-p2 (new-state ball-state-p2 p1-state p2-score p1-score)]
    [new-state-p1 new-state-p2]))

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
  (swap! follow-games dissoc client-uid)
  (let [opp-uid (get-in @follow-games [client-uid :opp-uid])]
    (when opp-uid
      (chsk-send! opp-uid [:pingpong/game-off])
      (swap! follow-games dissoc opp-uid)
      (uid-to-game! opp-uid))))
