(ns solar-system-of-js.core
  (:require-macros
    [cljs.core.async.macros :refer [go-loop]]
    [hiccups.core :refer [html]])
  (:require
    hiccups.runtime
    [cljs.core.async :refer [<! chan tap]]
    [solar-system-of-js.canvas :refer [init-canvas!]]
    [solar-system-of-js.control :refer [init-controls!]]
    [solar-system-of-js.actions :refer [start-loops!]]
    [solar-system-of-js.nav :refer [init-first-slide! sync-slide-to-hash!]]
    [solar-system-of-js.animate :refer [start-ticking!]]
    [solar-system-of-js.draw :refer [draw!]]
    solar-system-of-js.caption
    ))

(enable-console-print!)

(def page
  (html
    [:div#container
      [:div#caption "Insert slide captions here"]
      [:div#canvas-container
        [:canvas#canvas]]
      [:div#canvas-footer
        [:table#keys
         [:tr [:td [:kbd "&#8594;"]] [:td "= next"]]
         [:tr [:td [:kbd "SHIFT"] " + " [:kbd "&#8594;"]] [:td "= hurry"]]]
        [:a {:href "https://github.com/shaunlebron/solar-system-of-js" :target "_blank"}
         [:div#github "Made with " [:img#cljsLogo {:src "cljs.svg"}] " ClojureScript"]]]]))

(defn init-page!
  []
  (doto (.getElementById js/document "container")
    (aset "outerHTML" page)))

(defn main
  []

  (init-page!)

  ;; initialize drawing canvas
  (init-canvas!)

  ;; initialize touch and key controls
  (init-controls!)

  ;; start animation heartbeat
  (start-ticking!)

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

