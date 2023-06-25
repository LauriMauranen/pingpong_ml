(ns pingpong.pong
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [pingpong.utils :refer [check-reset calc-bat-dir calc-new-ball
                                    calc-new-ball-server calc-avg calc-bat-server]]
            [pingpong.client :refer [send-state-to-server! server-state]]
            [pingpong.constants :as c]))


(defn setup []
  (q/frame-rate c/frame-rate)
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
  (let [ss @server-state
        state-used? (:state-used? ss)

        new-player-bat-dir (calc-bat-dir s)
        new-player-bat (+ (:player-bat s) (* c/bat-speed new-player-bat-dir))

        new-opp-bat-dir (:opponent-bat-dir ss)
        new-opp-bat (if state-used?
                      (+ (:opponent-bat s) (* c/bat-speed new-opp-bat-dir))
                      (calc-bat-server s ss))

        [new-ball
         new-ball-dir
         new-ball-speed] (if state-used?
                           (calc-new-ball s)
                           (calc-new-ball-server s ss))

        [final-ball
         final-ball-dir
         final-ball-speed
         p-score-inc
         opp-score-inc] (check-reset new-ball 
                                     new-ball-dir 
                                     new-ball-speed
                                     (:can-score-inc? s))

        ; p-score (if state-used?
        ;           (:player-score s)
        ;           (+ (:player-score ss) p-score-inc))
        ; opp-score (if state-used?
        ;             (:opponent-score s)
        ;             (+ (:opponent-score ss) opp-score-inc))
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

    (when (< 0 (+ p-score-inc opp-score-inc))
      (swap! (q/state-atom) :can-score-inc? false))

    (when (and (:state-used? ss) (:can-score-inc? ss))
      (swap! (q/state-atom) :can-score-inc? true))

    (when (and (:game-on? next-state)
          (= (rem (q/frame-count) c/server-message-interval) 0))
      (send-state-to-server! next-state))
    (swap! server-state assoc :state-used? true)
    next-state))

(defn draw-keys []
  (q/text-size 30)
  (q/text "K" 0 c/k-height)
  (q/text "M" -2 c/m-height))

(defn draw-scores [{:keys [player-score opponent-score]}]
  (q/text-size 25)
  (q/text-num player-score c/p-score-width c/score-height)
  (q/text-num opponent-score c/opp-score-width c/score-height))

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
