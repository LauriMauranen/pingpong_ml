(ns pingpong.pong
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [pingpong.utils :refer [check-reset calc-bat-dir calc-new-ball-dir]]
            [pingpong.client :refer [send-state-to-server!]]
            [pingpong.constants :as c]))


;; Here we store server state.
(def server-state (atom {:ball [0 0]
                             :ball-dir [(dec (* 2 (rand-int 2))) 0]
                             :ball-speed c/ball-start-speed
                             :player-bat (- (/ c/bat-height 2))
                             :opponent-bat  (- (/ c/bat-height 2))
                             :player-bat-dir 0
                             :opponent-bat-dir 0
                             :player-score 0
                             :opponent-score 0
                             :game-on? false
                             :host? true
                             :state-used? false}))


(defn setup []
  (q/frame-rate 60)
  @server-state)

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

; (defn distance [v1 v2]
;   (q/dist (first v1) (first v2) (second v1) (second v2)))

; (defn check-and-fix-errors
;   [{:as state :keys [ball ball-speed player-bat]} s-state]
;   (let [s-ball (:ball s-state)
;         s-bat (:player-bat s-state)
;         s-speed (:ba1l-speed s-state)]
;     (cond-> state
;       (> (distance ball s-ball) ball-error)
;         (assoc :ball s-ball)
;       (> (q/abs (- player-bat s-bat)) bat-error)
;         (assoc :player-bat s-bat))))

(defn update-state [state]
  (let [{:as s-state
         :keys [player-bat-dir opponent-bat opponent-bat-dir player-score
                opponent-score game-on? host? state-used?]} @server-state

        game-state (if game-on?
                    (if (not state-used?)
                      (do (swap! server-state assoc :state-used? true)
                        (-> state ;;(check-and-fix-errors state s-state)
                          (update :player-bat + (* c/bat-speed player-bat-dir))
                          (update :opponent-bat + (* c/bat-speed
                                                     opponent-bat-dir))
                          (assoc :player-bat-dir player-bat-dir)
                          (assoc :opponent-bat-dir opponent-bat-dir)))
                      (-> state
                        (update :player-bat + (* c/bat-speed
                                                 (:player-bat-dir state)))
                        (update :opponent-bat + (* c/bat-speed
                                                   (:opponent-bat-dir state)))))
                    (-> state
                      (update :player-bat +
                              (* c/bat-speed (:player-bat-dir state)))
                      (assoc :player-score 0)
                      (assoc :opponent-score 0)))

        new-ball (mapv + (:ball game-state) (map #(* (:ball-speed game-state) %)
                                                 (:ball-dir game-state)))
        new-ball-dir (calc-new-ball-dir game-state)
        new-ball-speed (+ (:ball-speed game-state) c/speed-inc)

        [final-ball
         final-ball-dir
         final-ball-speed
         p-score-inc
         opp-score-inc] (check-reset new-ball new-ball-dir new-ball-speed)

        [p-score opp-score] (if host?
                              [(+ (:player-score game-state) p-score-inc)
                               (+ (:opponent-score game-state) opp-score-inc)]
                              [player-score opponent-score])

        next-state (-> game-state
                     (assoc :game-on? game-on?)
                     (assoc :ball final-ball)
                     (assoc :ball-dir final-ball-dir)
                     (assoc :ball-speed final-ball-speed)
                     (assoc :player-bat-dir (calc-bat-dir state))
                     (assoc :player-score p-score)
                     (assoc :opponent-score opp-score))]
      
      (send-state-to-server! state)
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
