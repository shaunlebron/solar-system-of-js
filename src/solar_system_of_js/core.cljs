(ns solar-system-of-js.core
  (:require
    [solar-system-of-js.canvas :refer [init-canvas!]]
    [solar-system-of-js.control :refer [init-controls!]]
    [solar-system-of-js.tick :refer [tick!]]
    [solar-system-of-js.actions :refer [slide-actions]]
    ))

(enable-console-print!)

(defn main
  []

  ;; initialize drawing canvas
  (init-canvas!)

  (init-controls!)

  ;; start animation heartbeat
  (.requestAnimationFrame js/window tick!)

  ;; execute first slide action
  (let [action (first slide-actions)]

    (f))

  ;; save state of first slide
  (save-slide-state!))

;; start when ready
(.addEventListener js/window "load" main)

