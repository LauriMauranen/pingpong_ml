(ns pingpong.gamelist
  (:require [pingpong.state :refer [app-state]]))


(defn game-list []
  (let [games (:games @app-state)
        p1s (map #(:p1-name %) games)
        p2s (map #(if (:p2-name %) (:p2-name %) "Opponent not found") games)
        scores (map #(str (:p1-score %) " - " (:p2-score %)) games)]
    [:div
      [:h3 "Games played now"]
      [:table>tbody
        (for [p1 p1s
              p2 p2s
              score scores]
          ^{:key p1}
          [:tr
            [:td {:style {:min-width "200px"}} p1]
            [:td {:style {:min-width "100px" :text-align "center"}} score]
            [:td {:style {:min-width "200px"}} p2]])]]))
