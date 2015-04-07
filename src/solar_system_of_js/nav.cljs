(ns solar-system-of-js.nav
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [solar-system-of-js.actions :refer [slide-actions animate-action! skip-action!]]
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

(defn hash-slide
  "Get the current url hash slide number (if valid)."
  []
  (let [i (-> (aget js/location "hash") (subs 1) (js/parseInt))]
    (when (<= 0 i (dec num-slides))
      i)))

(defn init-first-slide!
  "Do some setup that the first slide requires."
  []
  ;; execute first slide action
  (let [action (first slide-actions)]
    (skip-action! action))

  ;; save state of first slide
  (save-slide-state!)

  ;; set initial hash
  (when-not (hash-slide)
    (aset js/location "hash" 0)))

(def in-transition?
  "True if we are in the middle of a slide's actions (i.e. animation)"
  (atom false))

(defn sync-hash-to-slide!
  "Make url hash equal to current slide"
  []
  (aset js/location "hash" (:slide @state)))

(defn next-slide!
  "Go to next slide if we can."
  []
  (when-not @in-transition?
    (when-let [action (get slide-actions (inc (:slide @state)))]
      (save-slide-state!)
      (swap! state update-in [:slide] inc)
      (sync-hash-to-slide!)
      (reset! in-transition? true)
      (go
        (<! (animate-action! action))
        (reset! in-transition? false)
        ))))

(defn prev-slide!
  "Go to previous slide if we can."
  []
  (when-not @in-transition?
    (when-let [s (get @slide-states (dec (:slide @state)))]
      (reset! state s)
      (sync-hash-to-slide!))))

(defn skip-slide!
  "Go to next slide, skipping any transitions."
  ([] (skip-slide! true))
  ([sync-hash?]
   (when-not @in-transition?
     (when-let [action (get slide-actions (inc (:slide @state)))]
       (save-slide-state!)
       (swap! state update-in [:slide] inc)
       (when sync-hash?
         (sync-hash-to-slide!))
       (skip-action! action)))))

(defn seek-slide!
  "Instantly go to the given slide."
  [i]
  (let [cur (:slide @state)]
    (when (and (not= cur i) (not @in-transition?))
      (if-let [s (get @slide-states i)]
        (reset! state s)
        (when (> i cur)
          (dotimes [_ (- i cur)]
            (skip-slide! false))))
      (sync-hash-to-slide!))))

(defn sync-slide-to-hash!
  "Make sure the slide reflects the current url hash."
  []
  (when-let [i (hash-slide)]
    (seek-slide! i)))

(.addEventListener js/window "hashchange" sync-slide-to-hash!)


