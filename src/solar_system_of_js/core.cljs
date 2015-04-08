(ns solar-system-of-js.core
  (:require-macros
    [cljs.core.async.macros :refer [go-loop]])
  (:require
    [cljs.core.async :refer [<! chan tap]]
    [solar-system-of-js.canvas :refer [init-canvas!]]
    [solar-system-of-js.control :refer [init-controls!]]
    [solar-system-of-js.tick :refer [tick!]]
    [solar-system-of-js.actions :refer [tick-orbits!
                                        tick-radar!
                                        start-loops!]]
    [solar-system-of-js.nav :refer [init-first-slide! sync-slide-to-hash!]]
    [solar-system-of-js.tick :refer [tick-tap]]
    [solar-system-of-js.draw :refer [draw!]]
    solar-system-of-js.caption
    ))

(enable-console-print!)

(defn main
  []

  ;; initialize drawing canvas
  (init-canvas!)

  ;; initialize touch and key controls
  (init-controls!)

  ;; start animation heartbeat
  (.requestAnimationFrame js/window tick!)

  ;; do some state setup for the first slide
  (init-first-slide!)

  ;; go to slide listed in the url hash
  (sync-slide-to-hash!)

  ;; start any animation loops
  (start-loops!)

  ;; start drawing (self-schedules after first draw)
  (draw!))

;; start when ready
(.addEventListener js/window "load" main)

