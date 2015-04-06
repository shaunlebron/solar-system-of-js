(ns solar-system-of-js.tick
  (:require
    [cljs.core.async :refer [put! mult chan]]
    [solar-system-of-js.draw :refer [draw!]]))

(def tick-chan
  "This channel receives dt (delta time from last frame) in milliseconds."
  (chan))

(def tick-tap
  "Allows anything to tap the tick channel (e.g. for animation)."
  (mult tick-chan))

(def prev-time
  "Timestamp of the last tick."
  nil)

(defn tick!
  "Creates heartbeat by hooking requestAnimationFrame to tick-chan."
  [curr-time]
  (draw!)
  (let [delta-ms (if prev-time
                   (- curr-time prev-time)
                   (/ 1000 60))
        dt (/ delta-ms 1000)]
    (set! prev-time curr-time)
    (put! tick-chan dt))
  (.requestAnimationFrame js/window tick!))

