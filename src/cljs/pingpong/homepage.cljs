(ns pingpong.homepage
  (:require [pingpong.pong :refer [pingpong-component]]
            [pingpong.gamelist :refer [game-list]]
            [pingpong.client :refer [ask-games-data!]]))


(defn main-component []
  (js/setTimeout #(ask-games-data!) 1000)
  (js/setInterval #(ask-games-data!) 10000)
  [:div
    [pingpong-component]
    [game-list]])
