(ns pingpong.server
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.reload :refer [wrap-reload]]))

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "JEEJEEJEE"})

(defn app (-> handler
              wrap-reload))

(jetty/run-jetty handler {:port 3000})
