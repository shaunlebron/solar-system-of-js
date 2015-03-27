(ns solar-system-of-js.core
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]])
  (:require
    [cljs.core.async :refer [put! take! <! >! timeout mult chan tap untap]]
    [figwheel.client :as fw]))

(enable-console-print!)

;;--------------------------------------------------------------------------------
;; State
;;--------------------------------------------------------------------------------

(def initial-state
  "Initial state of the application."
  {:slide 0
   :title {:x 0
           :y 0
           :alpha 1}
   :cam {:x 0
         :y 0
         :zoom 1
         :angle 0}
   :js-face {:x 900 
             :y 0
             :r 200
             :alpha 1
             :angle 0}
   :js-core {:x 0
             :y 0
             :r 200
             :alpha 0}
   })

;; Current state of the application.
(defonce state
  (atom initial-state))

;;--------------------------------------------------------------------------------
;; Math
;;--------------------------------------------------------------------------------

(def PI  (aget js/Math "PI"))
(def cos (aget js/Math "cos"))
(def sin (aget js/Math "sin"))

;;--------------------------------------------------------------------------------
;; Canvas
;;--------------------------------------------------------------------------------

(defonce canvas nil)
(defonce ctx nil)

(def width 1280)
(def height 720)

(defn init-canvas!
  []
  (set! canvas (.getElementById js/document "canvas"))
  (set! ctx    (.getContext canvas "2d"))

  (aset canvas "width" width)
  (aset canvas "height" height))

;; wrapping Canvas2D functions for convenience

(defn fill-style! [x] (aset ctx "fillStyle" x))
(defn stroke-style! [x] (aset ctx "strokeStyle" x))
(defn begin-path! [] (.beginPath ctx))
(defn close-path! [] (.closePath ctx))
(defn translate! [x y] (.translate ctx x y))
(defn scale! [x y] (.scale ctx x y))
(defn rotate! [x] (.rotate ctx x))
(defn save! [] (.save ctx))
(defn restore! [] (.restore ctx))
(defn fill-rect! [x y w h] (.fillRect ctx x y w h))
(defn stroke-rect! [x y w h] (.strokeRect ctx x y w h))
(defn arc! [x y r a0 a1 cc] (.arc ctx x y r a0 a1 cc))
(defn fill! [] (.fill ctx))
(defn stroke! [] (.stroke ctx))
(defn circle! [x y r] (begin-path!) (arc! x y r 0 (* 2 PI) false))
(defn get-global-alpha [] (aget ctx "globalAlpha"))
(defn set-global-alpha [x] (aset ctx "globalAlpha" x))
(defn global-alpha! [x] (set-global-alpha (* x (get-global-alpha))))
(defn font! [x] (aset ctx "font" x))
(defn fill-text! [text x y] (.fillText ctx text x y))
(defn stroke-text! [text x y] (.strokeText ctx text x y))
(defn text-align! [x] (aset ctx "textAlign" x))
(defn text-baseline! [x] (aset ctx "textBaseline" x))
(defn line-width! [x] (aset ctx "lineWidth" x))

;;--------------------------------------------------------------------------------
;; Slide Drawings
;;--------------------------------------------------------------------------------

(defn draw-title!
  [{:keys [x y alpha]}]
  (save!)
  (global-alpha! alpha)
  (font! "100 100px Roboto")
  (text-align! "center")
  (text-baseline! "middle")
  (fill-style! "#555")
  (fill-text! "Solar System of JS" x y)
  (restore!)
  )

(defn draw-js-face!
  [{:keys [x y r angle alpha]}]
  (save!)
  (global-alpha! alpha)
  (translate! x y)
  (rotate! angle)
  (circle! 0 0 r)
  (fill-style! "#f7df1e")
  (fill!)

  (font! "bold 150px Neutra Text")
  (text-align! "center")
  (text-baseline! "middle")
  (fill-style! "#222")
  (let [off 60]
    (fill-text! "JS" off off))

  (restore!))

(defn draw-js-core!
  [{:keys [x y r alpha]}]
  (save!)
  (global-alpha! alpha)
  (stroke-style! "#222")
  (doseq [[i label] (reverse (map-indexed vector ["ES3" "ES5" "ES6" "ES7" "ES8"]))]
    (let [cr (* (inc i) (/ r 5))]
      (circle! x y cr)
      (line-width! 10)
      (stroke!)
      (fill-style! "#f7df1e")
      (fill!)))
  (restore!)
  )


;;--------------------------------------------------------------------------------
;; Drawing
;;--------------------------------------------------------------------------------

(defn set-cam!
  "Set camera's position, zoom, and rotation."
  [{:keys [x y zoom angle]}]
  (translate! (/ width 2) (/ height 2))
  (translate! (- x) (- y))
  (scale! zoom zoom)
  (rotate! angle))

(defn draw!
  "Draw the current state of the application."
  []
  (save!)
  (fill-style! "#222")
  (fill-rect! 0 0 width height)
  (set-cam! (:cam @state))

  (draw-title! (:title @state))
  (draw-js-core! (:js-core @state))
  (draw-js-face! (:js-face @state))

  (restore!))

;;--------------------------------------------------------------------------------
;; Timing
;;--------------------------------------------------------------------------------

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

;;--------------------------------------------------------------------------------
;; Animating
;;--------------------------------------------------------------------------------

(def tweens
  "In-betweening animation functions."
  {:linear identity
   :swing #(- 0.5 (/ (cos (* % PI)) 2)) ;; from jquery

   ;; find more: https://github.com/danro/jquery-easing/blob/master/jquery.easing.js

   })

(defn resolve-tween
  "Resolve the tween to a function if it's a name."
  [tween]
  (cond-> tween
    (keyword? tween) tweens))

(defn animate!
  "Pass given animation values to the given callback.
   Returns a channel that closes when done."
  [{:keys [a b duration tween] :or {tween :swing} :as opts} callback]
  (let [tween (resolve-tween tween)
        c (chan)
        dv (- b a)]
    (tap tick-tap c)
    (go-loop [t 0]
      (let [dt (<! c)
            t (+ t dt)
            percent (-> (/ t duration)
                        (min 1)
                        (tween))
            v (+ a (* percent dv))]
        (callback v)
        (when (< t duration)
          (recur t)))
      (untap tick-tap c))))

(defn multi-animate!
  "Helper for concurrent animations with `animate!`.
   Returns a channel that closes when all are done."
  [& arg-pairs]
  (let [anims (->> (partition 2 arg-pairs)
                   (mapv #(apply animate! %)))]
    (go
      (doseq [a anims]
        (<! a)))))

;;--------------------------------------------------------------------------------
;; Slide Animations
;;--------------------------------------------------------------------------------

(defn go-go-slide1!
  []
  (go
    (<! (multi-animate!
          {:a 900 :b 0 :duration 1}
           #(swap! state assoc-in [:js-face :x] %)
           {:a 1 :b 0 :duration 0.4}
           #(swap! state assoc-in [:title :alpha] %)
           ))))

(defn go-go-slide2!
  []
  (go
    (<! (multi-animate!
          {:a 0 :b -600 :duration 2}
          #(swap! state assoc-in [:js-face :y] %)
          {:a 0 :b 600 :duration 2}
          #(swap! state assoc-in [:js-face :x] %)
          {:a 0 :b (* 2 PI) :duration 2}
          #(swap! state assoc-in [:js-face :angle] %)
          {:a 1 :b 0 :duration 2}
          #(swap! state assoc-in [:js-face :alpha] %)
          {:a 0 :b 1 :duration 1}
          #(swap! state assoc-in [:js-core :alpha] %)
          {:a 1 :b 1.5 :duration 1}
          #(swap! state assoc-in [:cam :zoom] %)
          ))))

;;--------------------------------------------------------------------------------
;; Slide Control
;;--------------------------------------------------------------------------------

(def slide-actions
  "Actions to take for each slide."
  [nil
   go-go-slide1!
   go-go-slide2!
   ])

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
        (<! (action))
        (reset! in-transition? false)))))

(defn prev-slide!
  "Go to previous slide if we can."
  []
  (when-not @in-transition?
    (when-let [s (get @slide-states (dec (:slide @state)))]
      (reset! state s))))

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
      :left  (do
               (prev-slide!)
               (.preventDefault e))
      :right (do
               (next-slide!)
               (.preventDefault e))
      nil)))


;;--------------------------------------------------------------------------------
;; Entry
;;--------------------------------------------------------------------------------

(defn main
  []

  ;; initialize drawing canvas
  (init-canvas!)

  ;; start animation heartbeat
  (.requestAnimationFrame js/window tick!)

  ;; add controls
  (.addEventListener js/window "keydown" key-down)

  ;; save state of first slide
  (save-slide-state!))

;; start when ready
(.addEventListener js/window "load" main)

;; start figwheel
(fw/start {})

