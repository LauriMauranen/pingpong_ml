(ns pingpong.utils
  (:require [pingpong.websocket :refer [chsk-send!]]))


(def follow-games (atom []))
(def last-changed-uid (atom nil))


;; Helper function to pick number for new uid.
(defn smallest-new-num! []
  (let [games @follow-games
        ;; These numbers are already in use.
        nums (sort (map #(Integer/parseInt (str( last %))) (keys games)))
        len (count nums)]
    (loop [try-num 1
           index 0]
      (if (and (< index len) 
               (>= try-num (nth nums index)))
        (recur (inc try-num) (inc index))
        try-num))))


(defn make-p1-state
  [[ball ball-dir ball-speed p1-bat p1-bat-dir p1-score p2-score]
   [_ _ _ p2-bat p2-bat-dir _ _]]
  [ball
   ball-dir
   ball-speed
   p1-bat
   p1-bat-dir
   p2-bat
   p2-bat-dir
   p1-score
   p2-score])


;; Reverse x-axis because both players see's themselves on right.
(defn reverse-x [v]
  [(- (first v)) (second v)])


(defn make-p2-state
  [[ball ball-dir ball-speed p1-bat p1-bat-dir p1-score p2-score]
   [_ _ _ p2-bat p2-bat-dir _ _]]
  [(reverse-x ball)
   (reverse-x ball-dir)
   ball-speed
   p2-bat
   p2-bat-dir
   p1-bat
   p1-bat-dir
   p2-score
   p1-score])


;; Gives uid to every client.
(defn uid-to-client! [ring-req]
  (format "user-%d" (smallest-new-num!)))


;; Add uid to game.
(defn uid-to-game! [client-uid]
  ;; Try find opponent
  (loop [games @follow-games
         idx 0]
    (if (empty? games)
      ;; No other players or all games are full.
      (swap! follow-games conj {:player-1 client-uid :player-2 nil})
      (let [game (first games)
            player-1 (:player-1 game)
            player-2 (:player-2 game)]
        (if (or (not (or player-1 player-2)) (and player-1 player-2))
          (recur (rest games) (inc idx))
          ;; Opponent found.
          (if player-1
            (swap! follow-games assoc-in [idx :player-2] client-uid)
            (swap! follow-games assoc-in [idx :player-1] client-uid)))))))
