(ns solar-system-of-js.draw
  (:require
    [solar-system-of-js.state :refer [state]]
    [solar-system-of-js.canvas :refer [width height
                                       fill-style!
                                       stroke-style!
                                       begin-path!
                                       close-path!
                                       translate!
                                       scale!
                                       rotate!
                                       save!
                                       restore!
                                       fill-rect!
                                       stroke-rect!
                                       arc!
                                       fill!
                                       stroke!
                                       circle!
                                       move-to!
                                       line-to!
                                       stroke-line!
                                       get-global-alpha
                                       set-global-alpha
                                       global-alpha!
                                       font!
                                       fill-text!
                                       stroke-text!
                                       text-align!
                                       text-baseline!
                                       line-width!
                                       line-cap!
                                       draw-image!]]
    [solar-system-of-js.math :refer [PI cos sin]]))


;;--------------------------------------------------------------------------------
;; Drawing Specific Objects
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

  (fill-style! "#566")
  (font! "100 40px Roboto")
  (fill-text! "Space City JS - 28 March 2015" x (+ 120 y))

  (restore!)
  )

(def js-logo (.getElementById js/document "jsLogo"))

(defn draw-js-face!
  [{:keys [x y r angle alpha]}]
  (save!)
  (global-alpha! alpha)
  (translate! x y)
  (rotate! angle)
  (circle! 0 0 r)
  (fill-style! "#f7df1e")
  (fill!)

  (let [off -100
        size 240]
    (draw-image! js-logo off off size size))

  ;; (font! "bold 150px Neutra Text")
  ;; (text-align! "center")
  ;; (text-baseline! "middle")
  ;; (fill-style! "#222")
  ;; (let [off 60]
  ;;   (fill-text! "JS" off off))

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
  (draw-static-text! opts "TYPESCRIPT" -100))

(defn draw-flow!
  [opts]
  (draw-static-text! opts "FLOW" 0))

(defn draw-closure!
  [opts]
  (draw-static-text! opts "CLOSURE" 100))

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
          num-arcs 4
          da (/ (* 2 PI) num-arcs)
          start-a (+ a (* 2 da))]

      (draw-static-arc! start-a (+ start-a angle) "#FFF")

      ;; hack to grey out previously visited arcs
      (let [second-a (cond
                       (> angle (* 3 da)) (* 3 da)
                       (> angle (* 2 da)) (* 2 da)
                       (> angle da) da)]
        (draw-static-arc! start-a (+ start-a second-a) "#CDD"))

      ;; draw dividers
      (when (= 1 alpha)
        (line-width! 24)
        (stroke-style! "#BCC")
        (let [r (-> @state :static :sphere :r)]
          (dotimes [i num-arcs]
            (stroke-line! 0 0 0 r)
            (rotate! da)))))
    )
  (restore!))

(defn draw-static!
  [{:keys [title sphere angle typescript soundscript flow closure]}]
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
    (draw-closure! closure)

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
;; Drawing Dispatch
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

