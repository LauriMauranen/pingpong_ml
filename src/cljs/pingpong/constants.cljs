(ns pingpong.constants)

(def background-color 0)
(def bat-color 255)
(def ball-color 255)
(def size [500 500])
(def ball-diameter 30)
(def bat-width 35)
(def speed-inc 0)
(def bat-speed 6)
(def ball-error 200)
(def bat-error 30)
(def bat-height 100)
(def ball-start-speed 5)
(def server-message-interval 10) ; frames
(def frame-rate 60)
(def server-lag-offset 1.0)
(def timeout 200) ; ms
(def wa-weight-local 3)
(def wa-weight-server 1)
(def wa-div (+ wa-weight-local wa-weight-server))
(def k-height (* (/ (second size) 2) 0.7))
(def m-height (* (/ (second size) 2) 0.85))
(def p-score-width (- (/ (first size) 2) 50))
(def opp-score-width (- 50 (/ (first size) 2)))
(def score-height (- 50 (/ (second size) 2)))
