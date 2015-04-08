(ns solar-system-of-js.canvas
  (:require
    [solar-system-of-js.math :refer [PI]]))

(defonce canvas nil)
(defonce ctx nil)

(def width 1280)
(def height 720)

(defn resize-canvas-footer!
  []
  (let [w (aget canvas "offsetWidth")]
    (.log js/console canvas (aget canvas "style") w)
    (doto (.getElementById js/document "canvas-footer")
        (aset "style" "width" (str w "px"))
        (aset "style" "opacity" 1))))

(defn init-canvas!
  []
  (set! canvas (.getElementById js/document "canvas"))
  (set! ctx    (.getContext canvas "2d"))

  (aset canvas "width" width)
  (aset canvas "height" height)
  
  (resize-canvas-footer!)
  (.addEventListener js/window "resize" resize-canvas-footer!))

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
(defn draw-image!
  ([img dx dy] (.drawImage ctx img dx dy))
  ([img dx dy dWidth dHeight] (.drawImage ctx img dx dy dWidth dHeight))
  ([img sx sy sWidth sHeight dx dy dWidth dHeight] (.drawImage ctx img sx sy sWidth sHeight dx dy dWidth dHeight)))

