(ns solar-system-of-js.core
  (:require-macros
    [cljs.core.async.macros :refer [go-loop]])
  (:require
    [cljs.core.async :refer [<! chan tap]]
    [solar-system-of-js.canvas :refer [init-canvas!]]
    [solar-system-of-js.control :refer [init-controls!]]
    [solar-system-of-js.tick :refer [tick!]]
    [solar-system-of-js.actions :refer [slide-actions
                                        skip-action!
                                        tick-orbits!
                                        tick-radar!]]
    [solar-system-of-js.nav :refer [save-slide-state!]]
    [solar-system-of-js.tick :refer [tick-tap]]
    [solar-system-of-js.draw :refer [draw!]]
    solar-system-of-js.caption
    ))

(enable-console-print!)

(defn start!
  "Start the main loop."
  []
  (let [c (chan)]
    (tap tick-tap c)
    (go-loop []
      (let [dt (<! c)]
        (tick-orbits! dt)
        (tick-radar! dt)
        (draw!))
      (recur))))

(defn main
  []

  ;; initialize drawing canvas
  (init-canvas!)

  ;; initialize touch and key controls
  (init-controls!)

  ;; start animation heartbeat
  (.requestAnimationFrame js/window tick!)

  ;; execute first slide action
  (let [action (first slide-actions)]
    (skip-action! action))

  ;; save state of first slide
  (save-slide-state!)

  ;; start the main loop
  (start!))

;; start when ready
(.addEventListener js/window "load" main)

