(ns pingpong.server
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as route]
            [org.httpkit.server :refer [run-server]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [pingpong.websocket :refer [ring-ajax-get-or-ws-handshake ring-ajax-post ch-chsk]]))


(defroutes main-routes
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req))
  (route/not-found "Page not found"))


(defn run-pingpong-server [port production]
    (let [environment (if production "production" "development")
          handler (if (not production)
                    (wrap-reload (wrap-defaults #'main-routes site-defaults))
                    (wrap-defaults main-routes site-defaults))]
    (prn "Using environment" environment)
    (run-server handler {:port port})
    (prn "Server is running in port" port)))
