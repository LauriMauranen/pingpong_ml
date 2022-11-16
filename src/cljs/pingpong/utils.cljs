(ns pingpong.utils
  (:require [quil.core :as q :include-macros true]
            [pingpong.constants :as c]))


; (def server-local-offset (atom {:n 0
;                                 :x-avg 0
;                                 :y-avg 0}))


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

(defn round [v]
  (let [f #(/ (.round js/Math (* (+ % 0.0001) 1000)) 1000)]
    (f v)))

(defn round-v [v]
  (mapv round v))

(defn check-reset [ball ball-dir ball-speed]
  (let [p-score? (< (first ball) (- (/ (first c/size) 2)))
        opp-score? (> (first ball) (/ (first c/size) 2))
        rand-dir [(dec (* 2 (rand-int 2))) 0]]
    (if p-score?
      [[0 0] rand-dir c/ball-start-speed 1 0]
      (if opp-score?
        [[0 0] rand-dir c/ball-start-speed 0 1]
        [(round-v ball) (round-v ball-dir) (round ball-speed) 0 0]))))

(defn calc-ball [ball ball-dir ball-speed]
  (mapv + ball (map #(* ball-speed %) ball-dir)))

(defn calc-new-ball [{:as state :keys [ball ball-speed ball-dir]}]
  [(calc-ball ball ball-dir ball-speed)
   (calc-new-ball-dir state)
   (+ ball-speed c/speed-inc)])

(defn calc-avg [value-1 value-2 n]
  (round (/ (+ value-1 value-2) n)))

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
   {:as server-state :keys [ball ball-dir ball-speed]}]
  (let [new-ball (mapv calc-local-server (:ball state) ball)
        new-ball-dir (mapv calc-local-server (:ball-dir state) ball-dir)
        new-ball-speed (calc-local-server (:ball-speed state) ball-speed)
        full-state (-> server-state
                       (assoc :player-bat player-bat)
                       (assoc :player-bat-dir player-bat-dir))]
  [(calc-ball new-ball new-ball-dir (* new-ball-speed c/server-lag-offset))
   (calc-new-ball-dir full-state)
   (+ new-ball-speed c/speed-inc)]))

; (defn calc-server-offset [state server-state new-ball]
;   (when (not (:state-used? server-state))
;     (let [[local-ball _ _] (calc-new-ball state)
;           diff-x (abs (- (first local-ball) (first new-ball)))
;           diff-y (abs (- (second local-ball) (second new-ball)))]
;       (when (not= 0 diff-x)
;         (let [{:keys [n]} (swap! server-local-offset update :n inc)]
;           (swap! server-local-offset update :x-avg calc-avg diff-x n)
;           (swap! server-local-offset update :y-avg calc-avg diff-y n)))
;     ; (prn "diff x" diff-x)))
;     ; (prn "diff y" diff-y)
;     ; (prn "avg y" (:y-avg @server-local-offset))
;     (prn "avg x" (:x-avg @server-local-offset)))))

(defn calc-bat-server [state {:keys [opponent-bat opponent-bat-dir]}]
  (if (= opponent-bat-dir 0)
    (:opponent-bat state)
    (let [opp-bat (calc-weighted-avg (:opponent-bat state) opponent-bat)]
      (+ opp-bat (* c/bat-speed opponent-bat-dir)))))
