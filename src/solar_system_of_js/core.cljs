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
  {:slide 0
   :cam {:x 0
         :y 0
         :zoom 1
         :angle 0}
   :js-face {:x 900 
             :y 0
             :r 200
             :alpha 1}
   })

(defonce state (atom initial-state))

;;--------------------------------------------------------------------------------
;; Math
;;--------------------------------------------------------------------------------

(def PI (.-PI js/Math))
(def cos (.-cos js/Math))

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

  (set! (.-width canvas) width)
  (set! (.-height canvas) height)
  )

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
(defn circle! [x y r]
  (begin-path!)
  (arc! x y r 0 (* 2 PI) false))

(defn get-global-alpha [] (aget ctx "globalAlpha"))
(defn set-global-alpha [x] (aset ctx "globalAlpha" x))
(defn global-alpha! [x] (set-global-alpha (* x (get-global-alpha))))

(defn font! [x] (aset ctx "font" x))
(defn fill-text! [text x y] (.fillText ctx text x y))
(defn stroke-text! [text x y] (.strokeText ctx text x y))
(defn text-align! [x] (aset ctx "textAlign" x))
(defn text-baseline! [x] (aset ctx "textBaseline" x))

;;--------------------------------------------------------------------------------
;; Slide 1
;;--------------------------------------------------------------------------------

(defn draw-js!
  [{:keys [x y r alpha]}]

  (save!)
  (global-alpha! alpha)
  (circle! x y r)
  (fill-style! "#f7df1e")
  (fill!)

  (font! "bold 150px Neutra Text")
  (text-align! "center")
  (text-baseline! "middle")
  (fill-style! "#222")
  (let [off 60]
    (fill-text! "JS" (+ x off) (+ y off)))

  (restore!))

;;--------------------------------------------------------------------------------
;; Drawing
;;--------------------------------------------------------------------------------

(defn set-cam!
  [{:keys [x y zoom angle]}]
  (translate! (/ width 2) (/ height 2))
  (translate! (- x) (- y))
  (scale! zoom zoom)
  (rotate! angle))

(defn draw!
  []
  (save!)
  (fill-style! "#222")
  (fill-rect! 0 0 width height)
  (set-cam! (:cam @state))

  (draw-js! (:js-face @state))

  (restore!))

;;--------------------------------------------------------------------------------
;; Timing
;;--------------------------------------------------------------------------------

(def tick-chan (chan))
(def tick-tap (mult tick-chan))
(def prev-time nil)

(defn tick!
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
  {:linear identity
   :swing #(- 0.5 (/ (cos (* % PI)) 2)) ;; from jquery

   ;; find more: https://github.com/danro/jquery-easing/blob/master/jquery.easing.js

   })

(defn animate!
  [{:keys [a b duration tween]
    :or {tween (:swing tweens)}} callback]
  (let [c (chan)
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
  [opts callbacks]
  (let [anims (mapv animate! opts callbacks)]
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
          [{:a 0 :b 1 :duration 1}
           {:a 900 :b 0 :duration 1}]
          [#(swap! state assoc-in [:js-face :alpha] %)
           #(swap! state assoc-in [:js-face :x] %)]))))

;;--------------------------------------------------------------------------------
;; Slide Control
;;--------------------------------------------------------------------------------

(def slide-actions
  [nil
   go-go-slide1!
   ])

(def num-slides (count slide-actions))

(defonce slide-states (atom []))

(defn save-slide-state!
  []
  (let [i (:slide @state)]
    (swap! slide-states assoc i @state)))

(defn next-slide!
  []
  ;; FIXME: prevent going to next slide until animation is done
  (when-let [action (get slide-actions (inc (:slide @state)))]
    (save-slide-state!)
    (swap! state update-in [:slide] inc)
    (action)))

(defn prev-slide!
  []
  (when-let [s (get @slide-states (dec (:slide @state)))]
    (reset! state s)))

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
  (init-canvas!)
  (.requestAnimationFrame js/window tick!)
  (.addEventListener js/window "keydown" key-down)

  (save-slide-state!))

(.addEventListener js/window "load" main)

;; start figwheel
(fw/start {})

