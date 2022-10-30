(ns pingpong.server
  (:require [ring.adapter.jetty :as jetty]))

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "JEEJEEJEE"})

(jetty/run-jetty handler {:port 3000})
