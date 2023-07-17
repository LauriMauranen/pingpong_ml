(ns ^:figwheel-hooks pingpong.app
  (:require [reagent.dom]
            [reagent.core]
            [pingpong.components.homepage :refer [main-component]]))


(defn run []
  (when-let [el (js/document.getElementById "app")]
    (reagent.dom/render [main-component] el)))

(run)

(defn ^:after-load on-reload []
  (run)
  ; (swap! app-state update-in [:__figwheel_counter] inc)
)
