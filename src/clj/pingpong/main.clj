(ns pingpong.main
  (:gen-class)
  (:require [pingpong.server :refer [run-pingpong-server]]
            [environ.core :refer [env]]))

(def PORT (Integer/parseInt (env :pingpong-backend-port)))
(def PRODUCTION (= (env :pingpong-production) "true"))

(defn -main [& args]
  (run-pingpong-server PORT PRODUCTION))
