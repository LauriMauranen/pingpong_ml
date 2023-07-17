(ns pingpong.utils
  (:require [quil.core :as q :include-macros true]
            [pingpong.constants :as c]))


(defn check-bat-player [{:keys [ball ball-dir player-bat ball-speed]}]
  (let [ball-radius (/ c/ball-diameter 2)
        ball-edge (+ (first ball) ball-radius)
        bat-edge (- (/ (first c/size) 2) c/bat-width)]
    (and (< ball-edge bat-edge)
         (< bat-edge (+ ball-edge (* 2 ball-speed (first ball-dir))))
         (and (> (second ball) (- player-bat ball-radius))
              (< (second ball) (+ player-bat c/bat-height ball-radius))))))

(defn check-bat-opponent [{:keys [ball ball-dir opponent-bat ball-speed]}]
  (let [ball-radius (/ c/ball-diameter 2)
        ball-edge (- (first ball) ball-radius)
        bat-edge (- c/bat-width (/ (first c/size) 2))]
    (and (< bat-edge ball-edge)
         (< (+ ball-edge (* 2 ball-speed (first ball-dir))) bat-edge)
         ;; web-extra added to help non-host player!
         (and (> (second ball) (- opponent-bat ball-radius))
              (< (second ball) (+ opponent-bat c/bat-height ball-radius))))))

(defn check-roof-floor [{:keys [ball ball-dir ball-speed]}]
  (let [ball-roof-edge (- (second ball) (/ c/ball-diameter 2))
        ball-floor-edge (+ (second ball) (/ c/ball-diameter 2))
        roof-edge (- (/ (second c/size) 2))
        floor-edge (/ (second c/size) 2)]
    (or (and (> ball-roof-edge roof-edge)
             (< (+ ball-roof-edge (* ball-speed (second ball-dir))) roof-edge))
        (and (< ball-floor-edge floor-edge)
             (> (+ ball-floor-edge (* ball-speed (second ball-dir))) floor-edge)))))

(defn rotate [v angle]  ;; Counter-clockwise with positive angle.
  [(- (* (first v) (q/cos angle)) (* (second v) (q/sin angle)))
   (+ (* (first v) (q/sin angle)) (* (second v) (q/cos angle)))])

(defn reverse-x [v]
  [(- (first v)) (second v)])

(defn player-hit-bat [ball-dir bat-dir]
  (let [angle (- q/PI (q/acos (second ball-dir)))
        turned-dir (reverse-x ball-dir)]
    (case bat-dir
      0 turned-dir
      1 (rotate turned-dir (/ angle 2))
      -1 (rotate turned-dir (- (/ angle 2))))))

(defn opponent-hit-bat [ball-dir bat-dir]
  (let [angle (- q/PI (q/acos (second ball-dir)))
        turned-dir (reverse-x ball-dir)]
    (case bat-dir
      0 turned-dir
      -1 (rotate turned-dir (/ angle 2))
      1 (rotate turned-dir (- (/ angle 2))))))

(defn hit-rf [ball-dir]
  [(first ball-dir) (- (second ball-dir))])

(defn calc-bat-dir [{:keys [up-pressed? down-pressed? last-pressed]}]
  (if (and up-pressed? (= last-pressed :up))
    (- 1)
    (if (and down-pressed? (= last-pressed :down))
      1
      0)))

(defn calc-new-ball-dir
  [{:as state :keys [player-bat-dir opponent-bat-dir ball-dir]}]
  (cond
    (check-bat-player state)
      (player-hit-bat ball-dir player-bat-dir)

    (check-bat-opponent state)
      (opponent-hit-bat ball-dir opponent-bat-dir)

    (check-roof-floor state)
      (hit-rf ball-dir)

    :else ball-dir))

(defn round [value]
  (let [f #(/ (.round js/Math (* (+ % 0.0001) 1000)) 1000)]
    (f value)))

(defn round-v [v]
  (mapv round v))

(defn check-reset [ball ball-dir ball-speed can-score-inc?]
  (let [p-score? (and can-score-inc? (< (first ball) (- (/ (first c/size) 2))))
        opp-score? (and can-score-inc? (> (first ball) (/ (first c/size) 2)))
        ; if new ball server does that
        ; rand-dir [(dec (* 2 (rand-int 2))) 0]]
        new-ball (round-v ball)
        new-ball-dir (round-v ball-dir)
        new-ball-speed (round ball-speed)
        p-score-inc (if p-score? 1 0)
        opp-score-inc (if opp-score? 1 0)]
    [new-ball new-ball-dir new-ball-speed p-score-inc opp-score-inc]))

(defn calc-ball [ball ball-dir ball-speed]
  (mapv + ball (map #(* ball-speed %) ball-dir)))

(defn calc-new-ball [{:as state :keys [ball ball-speed ball-dir]}]
  [(calc-ball ball ball-dir ball-speed)
   (calc-new-ball-dir state)
   (+ ball-speed c/speed-inc)])

(defn calc-avg [value-1 value-2]
  (round (/ (+ value-1 value-2) 2)))

(defn calc-weighted-avg [value-1 value-2]
  (let [v1 (* value-1 c/wa-weight-local)
        v2 (* value-2 c/wa-weight-server)]
    (round (/ (+ v1 v2) c/wa-div))))

(defn use-local [value-1 value-2]
  value-1)

(defn calc-local-server [local server]
  (let [func calc-weighted-avg]
    (func local server)))

(defn calc-new-ball-server
  [{:as state :keys [player-bat player-bat-dir]}
   {:as server-state :keys [ball ball-dir ball-speed can-score-inc?]}]
  (let [new-ball (if can-score-inc?
                   ball
                   (mapv calc-local-server (:ball state) ball))
        new-ball-dir (if can-score-inc?
                       ball-dir
                       (mapv calc-local-server (:ball-dir state) ball-dir))
        new-ball-speed (if can-score-inc?
                         ball-speed
                         (calc-local-server (:ball-speed state) ball-speed))
        full-state (-> server-state
                       (assoc :player-bat player-bat)
                       (assoc :player-bat-dir player-bat-dir))]
  [(calc-ball new-ball new-ball-dir (* new-ball-speed c/server-lag-offset))
   (calc-new-ball-dir full-state)
   (+ new-ball-speed c/speed-inc)]))

(defn calc-bat-server [state {:keys [opponent-bat opponent-bat-dir]}]
  ; (if (= opponent-bat-dir 0)
  ;   (:opponent-bat state)
    (let [opp-bat (calc-avg (:opponent-bat state) opponent-bat)]
      (+ opp-bat (* c/bat-speed opponent-bat-dir))))

(defn player-in-list [games-list player-uid]
  (loop [games games-list]
    (if (empty? games)
      false
      (if (= player-uid (:p2-uid (first games)))
        true
        (recur (rest games))))))

(defn make-game-list [all-games]
  (loop [games all-games
         game-list []]
    (if (empty? games)
      game-list
      (let [game (first games)
            new-game-list (if (player-in-list game-list (:p-uid game))
                              game-list
                              (conj game-list {:p1-name (:p-name game)
                                               :p2-name (:opp-name game)
                                               :p1-score (:p-score game)
                                               :p2-score (:opp-score game)
                                               :p2-uid (:opp-uid game)}))]
        (recur (rest games) new-game-list)))))
