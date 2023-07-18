(ns pingpong.components.homepage
  (:require [pingpong.components.pong :refer [pingpong-component]]
            [pingpong.components.gamelist :refer [game-list]]
            [pingpong.client :refer [ask-games-data! send-username-to-server!]]))


(defn main-component []
  (when-let [username (js/prompt "Give username" "Player 1")]
    (send-username-to-server! username)
    (js/setTimeout #(ask-games-data!) 1000)
    (js/setInterval #(ask-games-data!) 10000)
    [:div {:style {:display "flex"}}
      [:div {:style {:margin-right "50px"}} [pingpong-component]]
      [game-list]]))
