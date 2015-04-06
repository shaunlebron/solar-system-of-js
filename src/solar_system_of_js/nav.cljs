(ns solar-system-of-js.nav
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]])
  (:require
    [cljs.core.async :refer [put! take! <! >! timeout mult chan tap untap]]
    [solar-system-of-js.actions :refer [slide-actions animate-action!]]
    [solar-system-of-js.state :refer [state]]))

(def num-slides
  (count slide-actions))

;; Saved slide states, so we can revisit previous slides.
(defonce slide-states
  (atom []))

(defn save-slide-state!
  "Save the current application state as the current slide's initial state."
  []
  (let [i (:slide @state)]
    (swap! slide-states assoc i @state)))

(def in-transition?
  "True if we are in the middle of a slide's actions (i.e. animation)"
  (atom false))

(defn next-slide!
  "Go to next slide if we can."
  []
  (when-not @in-transition?
    (when-let [action (get slide-actions (inc (:slide @state)))]
      (save-slide-state!)
      (swap! state update-in [:slide] inc)
      (reset! in-transition? true)
      (go
        (<! (animate-action! action))
        (reset! in-transition? false)))))

(defn prev-slide!
  "Go to previous slide if we can."
  []
  (when-not @in-transition?
    (when-let [s (get @slide-states (dec (:slide @state)))]
      (reset! state s))))

(defn skip-slide!
  "Go to next slide skipping transitions if we can."
  []
  (when-not @in-transition?
    (when-let [s (get @slide-states (inc (:slide @state)))]
      (reset! state s))))

