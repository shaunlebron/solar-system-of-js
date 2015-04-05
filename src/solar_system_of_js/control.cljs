(ns solar-system-of-js.control
  (:require
    [solar-system-of-js.actions :refer [slide-actions]]
    [solar-system-of-js.nav :refer []]
    cljsjs.hammer))

(def key-names
  {37 :left
   38 :up
   39 :right
   40 :down
   32 :space})

(def key-name #(-> % .-keyCode key-names))

(defn key-down [e]
  (let [kname (key-name e)
        shift (.-shiftKey e)]
    (case kname
      :space (do
               (next-slide!)
               (.preventDefault e))
      :left  (do
               (prev-slide!)
               (.preventDefault e))
      :right (do
               (if shift
                 (skip-slide!)
                 (next-slide!))
               (.preventDefault e))
      nil)))

(defn on-swipe!
  [type-]
  (case type-
    "tap" (next-slide!)
    "swipeleft" (next-slide!)
    "swiperight" (prev-slide!)
    nil))

(defn init-touch!
  []
  (doto (js/Hammer. canvas)
    (.on "swipeleft swiperight tap"
      #(on-swipe! (aget % "type")))))


(defn init-controls!
  []
  (init-touch!)
  (.addEventListener js/window "keydown" key-down))

