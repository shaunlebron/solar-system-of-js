(ns solar-system-of-js.state
  (:require
    [solar-system-of-js.math :refer [PI]]))

(def initial-state
  "Initial state of the application."
  {:slide 0
   :caption ""
   :title {:x 0
           :y -100
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
            :closure {:alpha 0}
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

