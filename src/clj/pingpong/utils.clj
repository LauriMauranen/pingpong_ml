(ns pingpong.utils)


(def follow-games (atom {}))
(def last-changed-uid (atom nil))

(def empty-game {:p-uid nil
                 :opp-uid nil
                 :p-name nil
                 ; :opp-name nil
                 :p-score 0
                 :opp-score 0
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
  (vec (concat ball-state [opp-bat opp-bat-dir] [p-score opp-score])))

(defn calc-ball-state [p1-ball-s p2-ball-s]
  [(mapv calc-avg (first p1-ball-s) (reverse-x (first p2-ball-s)))
   (second p1-ball-s)
   (calc-avg (nth p1-ball-s 2) (nth p2-ball-s 2))])

(defn balls-and-scores-to-players! [p1 p2]
  (let [p1-state (:state p1)
        p2-state (:state p2)
        p1-score (nth p1-state 5)
        p2-score (nth p1-state 6)
        ball-speed (nth p1-state 2)]
    (if (or (< (:opp-score p2) p1-score) (< (:p-score p2) p2-score))
      (let [p1-ball-state [[0 0] [(dec (* 2 (rand-int 2))) 0] ball-speed]
            p1-uid (:opp-uid p2)
            p2-uid (:opp-uid p1)]
        (swap! follow-games assoc-in [p1-uid :p-score] p1-score)
        (swap! follow-games assoc-in [p1-uid :opp-score] p2-score)
        (swap! follow-games assoc-in [p2-uid :p-score] p2-score)
        (swap! follow-games assoc-in [p2-uid :opp-score] p1-score)
        [p1-ball-state (reverse-x-ball-state p1-ball-state) p1-score p2-score true])
      (let [ball-state (calc-ball-state (take 3 p1-state) (take 3 p2-state))]
        [ball-state (reverse-x-ball-state ball-state) p1-score p2-score false]))))

(defn states-to-players! [p1 p2]
  (let [[ball-state-p1
         ball-state-p2
         p1-score
         p2-score
         can-score-inc?] (balls-and-scores-to-players! p1 p2)
        new-state-p1 (new-state ball-state-p1 (:state p2) p1-score p2-score)
        new-state-p2 (new-state ball-state-p2 (:state p1) p2-score p1-score)]
    (mapv #(conj % can-score-inc?) [new-state-p1 new-state-p2])))
