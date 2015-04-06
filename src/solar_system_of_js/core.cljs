(ns solar-system-of-js.core
  (:require
    [solar-system-of-js.canvas :refer [init-canvas!]]
    [solar-system-of-js.control :refer [init-controls!]]
    [solar-system-of-js.tick :refer [tick!]]
    [solar-system-of-js.actions :refer [slide-actions
                                        skip-action!]]
    [solar-system-of-js.nav :refer [save-slide-state!]]
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

  ;; execute first slide action
  (let [action (first slide-actions)]
    (skip-action! action))

  ;; save state of first slide
  (save-slide-state!))

;; start when ready
(.addEventListener js/window "load" main)

