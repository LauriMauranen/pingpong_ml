(ns pingpong.state
  (:require [reagent.core]))


(defonce app-state
  (reagent.core/atom {:games []}))
