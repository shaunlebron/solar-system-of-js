(ns solar-system-of-js.core
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]])
  (:require
    [cljs.core.async :refer [put! take! <! >! timeout mult chan tap untap]]))

(enable-console-print!)

;;--------------------------------------------------------------------------------
;; Math
;;--------------------------------------------------------------------------------

(def PI  (aget js/Math "PI"))
(def cos (aget js/Math "cos"))
(def sin (aget js/Math "sin"))

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
   :es-captions {:es3 {:alpha 0}
                 :es5 {:alpha 0}
                 :es6 {:alpha 0}
                 :es7 {:alpha 0}
                 :es8 {:alpha 0}}
   :highlight-layer nil
   :transpiler {:x 900
                :y 0
                :alpha 0
                :font-alpha 0}
   :linter {:x 900
            :y 70
            :alpha 0
            :font-alpha 0}
   :modulesys {:x 900
               :y 140
               :alpha 0
               :font-alpha 0}
   :static {:title {:alpha 0}
            :sphere {:alpha 0
                     :r 200
                     :angle 0}
            :typescript {:alpha 0}
            :soundscript {:alpha 0}
            :flow {:alpha 0}}
   :coffeescript {:alpha 0
                  :size 50
                  :highlight false
                  :angle 0
                  :angle-speed (/ PI 5)
                  :r 900
                  }
   :dart {:alpha 0
          :size 100
          :highlight false
          :angle 0
          :angle-speed (/ PI 15)
          :r 1400
                  }
   :clojurescript {:alpha 0
                   :size 100
                   :highlight false
                   :angle 0
                   :angle-speed (/ PI 10)
                   :r 2100
                  }
   :radar {:orbit nil
           :offset 0}
   })

;; Current state of the application.
(defonce state
  (atom initial-state))

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
(defn move-to! [x y] (.moveTo ctx x y))
(defn line-to! [x y] (.lineTo ctx x y))
(defn stroke-line! [x0 y0 x1 y1] (begin-path!) (move-to! x0 y0) (line-to! x1 y1) (stroke!))
(defn get-global-alpha [] (aget ctx "globalAlpha"))
(defn set-global-alpha [x] (aset ctx "globalAlpha" x))
(defn global-alpha! [x] (set-global-alpha (* x (get-global-alpha))))
(defn font! [x] (aset ctx "font" x))
(defn fill-text! [text x y] (.fillText ctx text x y))
(defn stroke-text! [text x y] (.strokeText ctx text x y))
(defn text-align! [x] (aset ctx "textAlign" x))
(defn text-baseline! [x] (aset ctx "textBaseline" x))
(defn line-width! [x] (aset ctx "lineWidth" x))
(defn line-cap! [x] (aset ctx "lineCap" x))

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
  (fill-style! "#DDD")
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

(def core-layers
  "colors of js-core layers"
  [{:name "ES3"
    :desc "core features"
    :color "#DDD"
    }
   {:name "ES5"
    :desc "new functions and strict mode"
    :color "#BBB"
    }
   {:name "ES6"
    :desc "new syntax, concepts, etc."
    :color "#999"
    }
   {:name "ES7"
    :desc "observe, async, comprehensions, guards"
    :color "#777"
    }
   {:name "ES8"
    :desc "macros?"
    :color "#555"
    }
   ])

(defn get-core-dr
  "Get core layer delta radius."
  [r]
  (/ r (count core-layers)))

(defn get-core-radius
  "Get radius of the given core layer index and outer radius."
  [i r]
  (* (inc i) (get-core-dr r)))

(defn draw-js-core!
  [{:keys [x y r alpha]}]
  (save!)
  (global-alpha! alpha)
  (stroke-style! "#EEE")
  (doseq [[i layer] (reverse (map-indexed vector core-layers))]
    (let [cr (get-core-radius i r)]
      (circle! x y cr)
      (fill-style! (:color layer))
      (fill!)))
  (circle! x y r)
  (stroke-style! "#f7df1e")
  (line-width! 10)
  (stroke!)
  (restore!)
  )

(defn draw-highlight-layer!
  [i]
  (when i
    (let [r (-> @state :js-face :r)
          dr (get-core-dr r)
          cr (- (get-core-radius i r) (/ dr 2))]

      ;; highlight the layer
      (line-width! dr)
      (circle! 0 0 cr)
      (stroke-style! "#1EA")
      (stroke!))))

(defn draw-es-captions!
  [{:keys [es3 es5 es6 es7 es8]}]
  (save!)
  (doseq [[i es] (map-indexed vector [es3 es5 es6 es7 es8])]
    (when-not (zero? (:alpha es))
      (save!)
      (global-alpha! (:alpha es))
      (let [angle (/ PI 4)
            dx (cos angle)
            dy (sin angle)
            r (-> @state :js-face :r)
            dr (get-core-dr r)
            cr (- (get-core-radius i r) (/ dr 2))
            y (- (* dy cr))
            x0 (* dx cr)
            x1 250
            a 10
            at (/ a 3)]

        ;; highlight the layer
        (draw-highlight-layer! i)

        ;; point at layer
        (line-width! 5)
        (line-cap! "round")
        (stroke-line! x0 y x1 y)
        (stroke!)

        ;; draw caption
        (let [layer (get core-layers i)]
          (text-baseline! "middle")
          (text-align! "left")
          (fill-style! "#EEE")
          (font! "100 40px Roboto")
          (fill-text! (:name layer) (+ 10 x1) y)
          (fill-style! "#AAA")
          (font! "100 14px Roboto")
          (fill-text! (:desc layer) (+ 15 x1) (+ 30 y)))

        )
      (restore!)
      ))
  (restore!))

(defn draw-sign!
  [{:keys [x y alpha font-alpha highlight]} caption offset]
  (when-not (zero? alpha)
    (save!)
    (translate! x y)
    (let [a 10
          w 100
          h 30]
      (translate! (- (* 2 a (* 1.2 offset))) 0)

      (save!)
      (global-alpha! alpha)
      (begin-path!)
      (move-to! (- a w) (- h))
      (line-to! (+ a w) (- h))
      (line-to! (- w a) (+ h))
      (line-to! (- (+ w a)) (+ h))
      (fill-style! (if highlight "#1EA" "#EEE"))
      (fill!)
      (restore!)

      (global-alpha! font-alpha)
      (text-baseline! "middle")
      (text-align! "center")
      (font! "100 24px Roboto")
      (fill-style! "#222")
      (fill-style! (if highlight "#222" "#222"))
      (fill-text! caption 0 0))
    (restore!)))

(defn draw-transpiler!
  [opts]
  (draw-sign! opts "TRANSPILER" 0))

(defn draw-linter!
  [opts]
  (draw-sign! opts "LINTER" 1))

(defn draw-modulesys!
  [opts]
  (draw-sign! opts "MODULE SYS" 2))

(defn draw-static-arc!
  [start-a stop-a color]
  (save!)
  (let [r-out (-> @state :static :sphere :r)
        r-in (-> @state :js-face :r)
        dr (- r-out r-in)
        r (+ r-in (/ dr 2))
        thick (* 0.8 dr)]
    (line-width! thick)
    (stroke-style! color)
    (begin-path!)
    (arc! 0 0 r start-a stop-a false)
    (stroke!)
    )
  (restore!))

(defn draw-static-text!
  [{:keys [alpha]} text y]
  (when-not (zero? alpha)
    (save!)
    (global-alpha! alpha)
    (text-baseline! "middle")
    (text-align! "left")
    (font! "100 100px Roboto")
    (fill-style! "#FFF")
    (fill-text! text 800 y)
    (restore!)))

(defn draw-typescript!
  [opts]
  (draw-static-text! opts "TYPESCRIPT" 0))

(defn draw-flow!
  [opts]
  (draw-static-text! opts "FLOW" 100))

(defn draw-soundscript!
  [opts]
  (draw-static-text! opts "SOUNDSCRIPT" 200))


(defn draw-staticsphere!
  [{:keys [angle alpha r]}]
  (save!)
  (when-not (zero? alpha)
    (global-alpha! alpha)
    (circle! 0 0 r)
    (fill-style! "#BCC")
    (fill!)

    ;; draw the arc
    (let [a (/ PI 2)
          da (/ (* 2 PI) 3)
          start-a (+ a da)]

      (draw-static-arc! start-a (+ start-a angle) "#FFF")

      ;; hack to grey out previously visited arcs
      (let [second-a (cond
                       (> angle (* 2 da)) (* 2 da)
                       (> angle da) da)]
        (draw-static-arc! start-a (+ start-a second-a) "#CDD")))

    ;; draw dividers
    (when (= 1 alpha)
      (line-width! 24)
      (stroke-style! "#BCC")
      (let [r (-> @state :static :sphere :r)]
        (dotimes [i 3]
          (stroke-line! 0 0 0 r)
          (rotate! (/ (* 2 PI) 3)))))
    )
  (restore!))

(defn draw-static!
  [{:keys [title sphere angle typescript soundscript flow]}]
  (when-not (zero? (:alpha sphere))
    (save!)

    (save!)
    (global-alpha! (:alpha title))
    (font! "100 90px Roboto")
    (text-baseline! "middle")
    (text-align! "center")
    (fill-style! "#677")
    (fill-text! "STATIC TYPING" 0 -600)
    (restore!)

    (draw-staticsphere! sphere)
    (draw-typescript! typescript)
    (draw-soundscript! soundscript)
    (draw-flow! flow)

    (restore!)))

(defn draw-orbit!
  [{:keys [r]}]
  (line-width! 10)
  (circle! 0 0 r)
  (stroke-style! "#566")
  (stroke!))

(defn draw-radar!
  [{:keys [orbit offset]}]
  (when orbit
    (save!)
    (let [o (-> @state orbit)
          r (:r o)
          dx (cos (:angle o))
          dy (sin (:angle o))
          gap 40
          offset (mod offset (* 2 gap))
          x (* (:r o) dx)
          y (* (:r o) dy)
          lx (- dy)
          ly dx
          rx dy
          ry (- dx)
          ]
      (global-alpha! (/ (:alpha o) 2))
      (line-width! 20)
      (stroke-style! "#f7df1e")

      (loop [i 0]
        (let [r0 (+ offset (* i gap))
              w (* (+ 0.2 (/ r0 r)) gap)
              x0 (+ (- x (* dx r0)) (* lx w))
              y0 (+ (- y (* dy r0)) (* ly w))
              x1 (+ (- x (* dx r0)) (* rx w))
              y1 (+ (- y (* dy r0)) (* ry w))]
          (when (<= r0 r)
            (stroke-line! x0 y0 x1 y1)
            (recur (+ i 1)))))
      )
    (restore!)))

(defn draw-planet!
  [{:keys [size angle r highlight]}]
  (let [x (* r (cos angle))
        y (* r (sin angle))]

    (circle! x y size)
    (fill-style! (if highlight "#FFF" "#566"))
    (fill!)
    ))

(defn draw-coffeescript!
  [{:keys [alpha highlight] :as opts}]
  (when-not (zero? alpha)
    (save!)
    (global-alpha! alpha)
    (draw-orbit! opts)
    (draw-planet! opts)
    (when highlight
      (font! "100 200px Roboto")
      (text-baseline! "middle")
      (text-align! "center")
      (fill-style! "#566")
      (fill-text! "COFFEESCRIPT" 0 -1400))
    (restore!)))

(defn draw-dart!
  [{:keys [alpha highlight] :as opts}]
  (when-not (zero? alpha)
    (save!)
    (global-alpha! alpha)
    (draw-orbit! opts)
    (draw-planet! opts)
    (when highlight
      (font! "100 200px Roboto")
      (text-baseline! "middle")
      (text-align! "center")
      (fill-style! "#566")
      (fill-text! "DART" 0 -1700))
    (restore!)))

(defn draw-clojurescript!
  [{:keys [alpha highlight] :as opts}]
  (when-not (zero? alpha)
    (save!)
    (global-alpha! alpha)
    (draw-orbit! opts)
    (draw-planet! opts)
    (when highlight
      (font! "100 300px Roboto")
      (text-baseline! "middle")
      (text-align! "center")
      (fill-style! "#DEE")
      (fill-text! "CLOJURESCRIPT" 3800 0))
    (restore!)))

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
  (draw-es-captions! (:es-captions @state))
  (draw-highlight-layer! (:highlight-layer @state))

  (draw-transpiler! (:transpiler @state))
  (draw-linter! (:linter @state))
  (draw-modulesys! (:modulesys @state))

  (draw-static! (:static @state))

  (draw-radar! (:radar @state))

  (draw-js-face! (:js-face @state))

  (draw-coffeescript! (:coffeescript @state))
  (draw-dart! (:dart @state))
  (draw-clojurescript! (:clojurescript @state))

  

  (restore!))

;;--------------------------------------------------------------------------------
;; Loops
;;--------------------------------------------------------------------------------

(defn update-orbit!
  [name- dt]
  (let [v (* dt (get-in @state [name- :angle-speed]))]
    (swap! state update-in [name- :angle] + v)))

(defn tick-orbits!
  [dt]
  (when (:enable-orbits? @state)
    (update-orbit! :coffeescript dt)
    (update-orbit! :dart dt)
    (update-orbit! :clojurescript dt)))

(defn tick-radar!
  [dt]
  (when (:enable-orbits? @state)
    (swap! state update-in [:radar :offset] + (* dt 400))))

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
    (put! tick-chan dt)

    (tick-orbits! dt)
    (tick-radar! dt)
    )
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
  [{:keys [a b duration tween] :or {tween :swing} :as opts} state-path]
  (let [tween (resolve-tween tween)
        c (chan)
        resolve-var #(if (= % :_) (get-in @state state-path) %)
        a (resolve-var a)
        dv (- b a)]
    (tap tick-tap c)
    (go-loop [t 0]
      (let [dt (<! c)
            t (+ t dt)
            percent (-> (/ t duration)
                        (min 1)
                        (tween))
            v (+ a (* percent dv))]
        (swap! state assoc-in state-path v)
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

(def slide-actions
  "Actions to take for each slide."
  (vec (flatten
    [;; no action for first slide
     nil

     ;; slide in the JS logo
     #(go
        (<! (multi-animate!
              {:a :_ :b 0 :duration 1} [:js-face :x]
              {:a :_ :b 0 :duration 0.4} [:title :alpha])))

     ;; peel back JS logo to see its layers
     #(go
        (<! (multi-animate!
              {:a :_ :b 400 :duration 2} [:cam :x]
              {:a :_ :b -600 :duration 2} [:js-face :y]
              {:a :_ :b 600 :duration 2} [:js-face :x]
              {:a :_ :b (* 2 PI) :duration 2} [:js-face :angle]
              {:a :_ :b 0 :duration 2} [:js-face :alpha]
              {:a :_ :b 1 :duration 1} [:js-core :alpha]
              {:a :_ :b 2 :duration 2} [:cam :zoom])))

     ;; highlight individual JS versions
     (let [t 0.01 ;; time between each transition
           low 0] ;; visited version alpha (faded out)
       [
        #(go
           (<! (animate!
                 {:a :_ :b 1 :duration t} [:es-captions :es3 :alpha])))
        #(go
           (<! (multi-animate!
                 {:a :_ :b low :duration t} [:es-captions :es3 :alpha]
                 {:a :_ :b 1 :duration t} [:es-captions :es5 :alpha])))
        #(go
           (<! (multi-animate!
                 {:a :_ :b low :duration t} [:es-captions :es5 :alpha]
                 {:a :_ :b 1 :duration t} [:es-captions :es6 :alpha])))
        #(go
           (<! (multi-animate!
                 {:a :_ :b low :duration t} [:es-captions :es6 :alpha]
                 {:a :_ :b 1 :duration t} [:es-captions :es7 :alpha])))
        #(go
           (<! (multi-animate!
                 {:a :_ :b low :duration t} [:es-captions :es7 :alpha]
                 {:a :_ :b 1 :duration t} [:es-captions :es8 :alpha])))
        ])

     ;; flash all layers and show transpiler
     #(go
        (swap! state assoc-in [:transpiler :highlight] true)
        (let [chans [(multi-animate!
                       {:a :_ :b 0 :duration 0.01} [:es-captions :es8 :alpha]
                       {:a :_ :b 200 :duration 1} [:cam :x]
                       {:a :_ :b 1.3 :duration 1} [:cam :zoom]
                       {:a :_ :b 450 :duration 1} [:transpiler :x]
                       {:a :_ :b 1 :duration 1} [:transpiler :alpha]
                       {:a :_ :b 1 :duration 1} [:transpiler :font-alpha]
                       )
                     (go
                       (<! (timeout 50))
                       (doseq [i [1 2 3 4 3 2 1 2 3 4 3 2 1 2 3 4 nil]]
                         (swap! state assoc :highlight-layer i)
                         (<! (timeout 70))))]]
          (doseq [c chans]
            (<! c))))

     ;; show linter
     #(go
        (swap! state assoc-in [:transpiler :highlight] false)
        (swap! state assoc-in [:linter :highlight] true)
        (<! (multi-animate!
              {:a :_ :b 1 :duration 1} [:linter :alpha]
              {:a :_ :b 1 :duration 1} [:linter :font-alpha]
              {:a :_ :b 450 :duration 1} [:linter :x])))

     ;; show module sys
     #(go
        (swap! state assoc-in [:linter :highlight] false)
        (swap! state assoc-in [:modulesys :highlight] true)
        (<! (multi-animate!
              {:a :_ :b 1 :duration 1} [:modulesys :alpha]
              {:a :_ :b 1 :duration 1} [:modulesys :font-alpha]
              {:a :_ :b 450 :duration 1} [:modulesys :x])))

     ;; put JS back together, showing it required tools
     #(go
        (swap! state assoc-in [:modulesys :highlight] false)
        (let [t 2]
          (<! (multi-animate!
                {:a :_ :b 0 :duration t} [:transpiler :font-alpha]
                {:a :_ :b 0 :duration t} [:linter :font-alpha]
                {:a :_ :b 0 :duration t} [:modulesys :font-alpha]
                {:a :_ :b 220 :duration t} [:transpiler :x]
                {:a :_ :b 220 :duration t} [:linter :x]
                {:a :_ :b 220 :duration t} [:modulesys :x]
                {:a :_ :b 0 :duration t} [:js-face :y]
                {:a :_ :b 0 :duration t} [:js-face :x]
                {:a :_ :b 0 :duration t} [:js-face :angle]
                {:a 1 :b 1 :duration t} [:js-face :alpha]
                {:a :_ :b 1 :duration t} [:cam :zoom]
                {:a :_ :b 0 :duration t} [:cam :x]
                {:a :_ :b 0 :duration t} [:cam :y]
                ))))

     ;; show staticsphere
     #(go
        (<! (multi-animate!
              {:a :_ :b 0 :duration 1} [:transpiler :alpha]
              {:a :_ :b 0 :duration 1} [:linter :alpha]
              {:a :_ :b 0 :duration 1} [:modulesys :alpha]
              {:a :_ :b 0.5 :duration 1} [:cam :zoom]
              {:a :_ :b -50 :duration 1} [:cam :y]
              ))
        (<! (multi-animate!
              {:a :_ :b 1 :duration 1} [:static :title :alpha]
              {:a :_ :b 1 :duration 1} [:static :sphere :alpha]
              {:a :_ :b 400 :duration 1} [:static :sphere :r])))

     ;; show static language titles
     (let [angle (/ (* 2 PI) 3) ;; arc angle
           low 0.2]             ;; alpha of faded out title
       [;; show typescript
        #(go
           (<! (multi-animate!
                 {:a :_ :b 300 :duration 1} [:cam :x]
                 {:a :_ :b 1 :duration 1} [:static :typescript :alpha]
                 {:a :_ :b angle :duration 1} [:static :sphere :angle]
                 )))

        ;; show flow
        #(go
           (swap! state assoc-in [:static :typescript :alpha] low)
           (<! (multi-animate!
                 {:a :_ :b 1 :duration 1} [:static :flow :alpha]
                 {:a :_ :b (* 2 angle) :duration 1} [:static :sphere :angle]
                 )))

        ;; show soundscript
        #(go
           (swap! state assoc-in [:static :flow :alpha] low)
           (<! (multi-animate!
                 {:a :_ :b 1 :duration 1} [:static :soundscript :alpha]
                 {:a :_ :b (* 3 angle) :duration 1} [:static :sphere :angle]
                 )))])

     ;; fade out staticsphere details
     #(go
        (<! (multi-animate!
              {:a :_ :b 0 :duration 1} [:static :typescript :alpha]
              {:a :_ :b 0 :duration 1} [:static :flow :alpha]
              {:a :_ :b 0 :duration 1} [:static :soundscript :alpha]
              {:a :_ :b 0 :duration 1} [:cam :x]
              {:a :_ :b 0 :duration 1} [:static :sphere :angle]
              {:a :_ :b 0 :duration 1} [:static :title :alpha]
              {:a :_ :b 0.2 :duration 2} [:cam :zoom]
              )))

     ;; show coffeescript
     #(go
        (swap! state assoc :enable-orbits? true)
        (swap! state assoc-in [:radar :orbit] :coffeescript)
        (swap! state assoc-in [:coffeescript :highlight] true)
        (<! (multi-animate!
              {:a :_ :b 1 :duration 1} [:coffeescript :alpha]
              )))

     ;; show dart
     #(go
        (swap! state assoc-in [:coffeescript :highlight] false)
        (swap! state assoc-in [:dart :highlight] true)
        (swap! state assoc-in [:radar :orbit] :dart)
        (<! (multi-animate!
              {:a :_ :b 1 :duration 1} [:dart :alpha]
              ;;{:a :_ :b 0.1 :duration 1} [:cam :zoom]
              )))

     ;; show clojurescript
     #(go
        (swap! state assoc-in [:dart :highlight] false)
        (swap! state assoc-in [:clojurescript :highlight] true)
        (swap! state assoc-in [:clojurescript :angle] (- (/ PI 10)))
        (swap! state assoc-in [:radar :orbit] :clojurescript)
        (<! (multi-animate!
              {:a :_ :b 1 :duration 1} [:clojurescript :alpha]
              {:a :_ :b 0 :duration 1} [:cam :y]
              {:a :_ :b 300 :duration 1} [:cam :x]
              {:a :_ :b 0.15 :duration 1} [:cam :zoom]
              )))

     ])))

(def num-slides
  (count slide-actions))

;;--------------------------------------------------------------------------------
;; Slide Control
;;--------------------------------------------------------------------------------

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

(defn skip-slide!
  "Go to next slide skipping transitions if we can."
  []
  (when-not @in-transition?
    (when-let [s (get @slide-states (inc (:slide @state)))]
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
               (if shift
                 (skip-slide!)
                 (next-slide!))
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

