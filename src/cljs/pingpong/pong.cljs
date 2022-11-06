(ns pingpong.pong
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [pingpong.utils :refer [check-reset calc-bat-dir calc-new-ball]]
            [pingpong.client :refer [send-state-to-server! server-state]]
            [pingpong.constants :as c]))


(defn setup []
  (q/frame-rate 60)
  (-> @server-state
      (assoc :player-bat (- (/ c/bat-height 2)))
      (assoc :player-bat-dir 0)))

(defn key-pressed [state event]
  (case (:key event)
    :m (-> state
           (assoc :down-pressed? true)
           (assoc :last-pressed :down))
    :k (-> state
           (assoc :up-pressed? true)
           (assoc :last-pressed :up))
    state))

(defn key-released [state event]
  (case (:key event)
    :m (assoc state :down-pressed? false)
    :k (assoc state :up-pressed? false)
    state))


(defn update-state [s]
  (let [ss (-> @server-state
               (assoc :player-bat (:player-bat s))
               (assoc :player-bat-dir (:player-bat-dir s)))

        new-player-bat-dir (calc-bat-dir s)
        new-player-bat (+ (:player-bat s) (* c/bat-speed new-player-bat-dir))

        new-opp-bat-dir (:opponent-bat-dir ss)
        new-opp-bat (:opponent-bat ss)

        [new-ball
         new-ball-dir
         new-ball-speed] (if (:state-used? ss)
                           (calc-new-ball s)
                           (do
                             (swap! server-state assoc :state-used? true)
                             (calc-new-ball ss)))

        [final-ball
         final-ball-dir
         final-ball-speed
         p-score-inc
         opp-score-inc] (check-reset new-ball new-ball-dir new-ball-speed)

        p-score (+ (:player-score ss) p-score-inc)
        opp-score (+ (:opponent-score ss) opp-score-inc)

        next-state (-> s
                     (assoc :game-on? (:game-on? ss))
                     (assoc :ball final-ball)
                     (assoc :ball-dir final-ball-dir)
                     (assoc :ball-speed final-ball-speed)
                     (assoc :player-bat new-player-bat)
                     (assoc :player-bat-dir new-player-bat-dir)
                     (assoc :opponent-bat new-opp-bat)
                     (assoc :opponent-bat-dir new-opp-bat-dir)
                     (assoc :player-score p-score)
                     (assoc :opponent-score opp-score))]

    (send-state-to-server! next-state)
    (prn "local" (:ball s) "server" (:ball ss))
    ; (send-score-to-server! [p-score opp-score])
    next-state))

(defn draw-keys []
  (let [bottom (/ (q/height) 2)
        k-height (* bottom 0.7)
        m-height (* bottom 0.85)]
  (q/text-size 30)
  (q/text "K" 0 k-height)
  (q/text "M" -2 m-height)))

(defn draw-scores [{:keys [player-score opponent-score]}]
  (let [p-width (- (/ (q/width) 2) 50)
        opp-width (- 50 (/ (q/width) 2))
        p-opp-height (- 50 (/ (q/height) 2))]
  (q/text-size 25)
  (q/text-num player-score p-width p-opp-height)
  (q/text-num opponent-score opp-width p-opp-height)))

(defn draw-bats [{:keys [player-bat opponent-bat host?]}]
  (q/rect (- (/ (q/width) 2)) opponent-bat c/bat-width c/bat-height)
  (q/rect (- (/ (q/width) 2) c/bat-width) player-bat c/bat-width c/bat-height))

(defn debug [state]
  (q/text-size 25)
  (q/text-num (:ball state) 0 50))

(defn draw-state [{:as state :keys [ball game-on?]}]
  (q/background c/background-color)
  (q/fill 255)
  (q/translate (/ (q/width) 2) (/ (q/height) 2))
  (draw-keys)
;;  (debug state)
  ;; Draw ball only when game is on!
  (when game-on?
    (draw-scores state)
    (q/ellipse (first ball) (second ball) c/ball-diameter c/ball-diameter))
  (draw-bats state))

(defn run-game! ^:export []
  (q/defsketch pingpong
    :title "Play pong!"
    :size c/size
    :setup setup
    :key-pressed key-pressed
    :key-released key-released
    :update update-state
    :draw draw-state
    :middleware [m/fun-mode]))

(run-game!)
