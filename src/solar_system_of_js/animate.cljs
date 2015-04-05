(ns solar-system-of-js.animate
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]])
  (:require
    [cljs.core.async :refer [put! take! <! >! timeout mult chan tap untap]]
    [solar-system-of-js.tick :refer [tick-tap]]))

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
  [state-path {:keys [a b duration tween] :or {tween :swing} :as opts}]
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
  [pairs]
  (let [anims (mapv #(apply animate! pairs))]
    (go
      (doseq [a anims]
        (<! a)))))

