(ns solar-system-of-js.actions
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]])
  (:require
    [cljs.core.async :refer [<! timeout chan tap]]
    [solar-system-of-js.state :refer [state]]
    [solar-system-of-js.animate :refer [tick-tap multi-animate!]]
    [solar-system-of-js.math :refer [PI]]))

;; When we change slides, there can be multiple actions to perform.
;;
;; For example:
;;
;;  Slide I:             <--- When I press the NEXT key, perform all of the following steps...
;;
;;    1. A, B, C, D      <--- animate A, B, C, D in parallel (then wait for all to finish)
;;    2. E, F            <--- animate E, F in parallel       (then wait for all to finish)
;;
;;  Slide II:            <--- When I press the NEXT key, perform all of the following steps...
;;
;;    3. G, H, I         <--- animate G, H in parallel (then wait for all to finish)
;;    4. J               <--- animate J (then wait for it to finish)
;;
;;
;; DEFINITIONS:
;; 
;;  Slide:      a sequence of Actions to take after a single keypress of the NEXT key
;;  Action:     a group of concurrent state setters (possibly animated)
;;
;; DETAILED DEFINITIONS:
;;
;;  An action is just a Map.
;;  Let's look at possible example values in this _example-action map:
;;

(def _example-action
  "An example action (state setters that are possible animated)"
  {
   ;;---------------------------------------------------------
   ;; IMMEDIATE SETTERS (no animation)
   ;;---------------------------------------------------------

   ;; set :foo key in our app state to 1
   :foo  1           

   ;; set [:bar :baz] key in our app state to 2
   [:bar :baz]  2    

   ;;---------------------------------------------------------
   ;; ANIMATED SETTERS
   ;;---------------------------------------------------------

   ;; animate :foo1 key in our app state from 0 to 1 over 2 seconds
   :foo1 {:a 0 :b 1 :duration 2}  

   ;; (same, but animate from whatever its current value is, indicated by ":_")
   :foo2 {:a :_ :b 1 :duration 2}

   ;;---------------------------------------------------------
   ;; CUSTOM ANIMATION
   ;;---------------------------------------------------------

   ;; use a core.async go-block to do a custom animation.
   ;; (Note that you must make this action SKIPPABLE by choosing the correct
   ;;  destination value "b" that our value will be set to when the go-block is finished.
   ;;  the source value "a" is ignored here.)
   :foo3 {:a :_ :b 3
         :go-block #(go (doseq [x [1 2 3]]
                          (<! (timeout 100))
                          (swap! state assoc :foo x)))}
   })

;;----------------------------------------------------------------------
;; Normalization of Action data
;; (since action has some short-hand, we use these functions to expand it)
;;----------------------------------------------------------------------

(defn normalize-path
  "A path is either a keyword or sequence of keywords, a state path."
  [path]
  (if (vector? path) path [path]))

(defn normalize-value
  "A value is either a scalar, or a map containing FROM(a) and TO(b) keys."
  [value]
  (if (map? value) value {:a :_ :b value}))

(defn normalize-step
  "Assuming a single action step. Return normalized step."
  [action]
  (zipmap (map normalize-path (keys action))
          (map normalize-value (vals action))))

(defn normalize-action
  "An action can have a single or multiple steps. Return normalized steps."
  [step-or-steps]
  (let [steps (if (sequential? step-or-steps) step-or-steps [step-or-steps])]
    (mapv normalize-step steps)))

;;----------------------------------------------------------------------
;; Converting actions to animations or skippers
;;----------------------------------------------------------------------

;; Determine action type:
;;
;;   immediate? (set state immediately to b)
;;   animation? (simple tween from a to b over duration)
;;   custom?    (use go-block for custom animation)
;;
(defn animation? [[path value]] (:duration value))
(defn custom?    [[path value]] (:go-block value))
(defn immediate? [kv] (and (not (animation? kv))
                           (not (custom? kv))))

;; Every action is skippable since they have a destination "b".
;; We can ignore the extra animation information if given.

(defn skip-step!
  [step]
  (doseq [[path value] step]
    (swap! state assoc-in path (:b value))))

(defn skip-action!
  [action]
  (doseq [step action]
    (skip-step! step)))

;; We gather all the animation info from a step map and evaluate them
;; to a) start the animations and to b) get a channel for notification
;; when they are complete.

(defn animate-step!
  "Animate the given step concurrently."
  [step-map]
  (let [_              (->> (filter immediate? step-map) (skip-step!))
        anim-chan      (->> (filter animation? step-map) (multi-animate!))
        go-block-chans (->> (filter custom? step-map) (map second) (map :go-block) (mapv #(%)))
        wait-chans     (cons anim-chan go-block-chans)]
    (go
      (doseq [c wait-chans]
        (<! c)))))

(defn animate-action!
  "Animate the given action steps in sequence."
  [action]
  (go
    (doseq [step action]
      (<! (animate-step! step)))))

;;----------------------------------------------------------------------
;; Slide action definitions
;;----------------------------------------------------------------------

;; some constants
(def static-num-arcs 4)
(def static-angle (/ (* 2 PI) static-num-arcs))
(def static-low-alpha 0.2)

(def other-high 1)
(def other-low 0.4)
(def other-high-delay 0.1)
(def other-low-delay 0.1)

(def slide-actions
  "Actions to take for each slide."
  (mapv normalize-action
  [;; title slide
   {:caption
    (str "Hello, I'm [@shaunlebron](http://twitter.com/shaunlebron)"
         " and I made this for [#spacecityjs](http://spacecityjs.com/)"
         " to visualize the current state of languages on the JS platform.")}

   ;; slide in the JS logo
   {:caption
    "This is JavaScript, the one and only programming language that today's browsers understand."
    [:js-face :x]   {:a :_ :b 0 :duration 1}
    [:title :alpha] {:a :_ :b 0 :duration 0.4}}

   ;; peel back JS logo to see its layers
   {:caption
    "If we look inside JavaScript, we find that it is a multi-layered, growing language."
    [:cam :x]         {:a :_ :b 400 :duration 2}
    [:cam :y]         {:a :_ :b -100 :duration 2}
    [:js-face :y]     {:a :_ :b -600 :duration 2}
    [:js-face :x]     {:a :_ :b 600 :duration 2}
    [:js-face :angle] {:a :_ :b (* 2 PI) :duration 2}
    [:js-face :alpha] {:a :_ :b 0 :duration 2}
    [:js-core :alpha] {:a :_ :b 1 :duration 1}
    [:cam :zoom]      {:a :_ :b 2 :duration 2}}

   ;; ES3
   {:caption
    "At its core is ES3, which we consider the base version of JS supported by legacy browsers."
    [:es-captions :es3 :alpha] 1}

   ;; ES5
   {:caption
    (str "ES4 was abandoned, but ES5 was a conservative addition with new functions and a "
         "[\"use strict\"](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Strict_mode) mode.")
    [:es-captions :es3 :alpha] 0
    [:es-captions :es5 :alpha] 1}

   ;; ES6
   {:caption
    (str "ES6 is the current version, with a [rather huge](https://github.com/lukehoban/es6features)"
         " addition of syntax and concepts.")
    [:es-captions :es5 :alpha] 0
    [:es-captions :es6 :alpha] 1}

   ;; ES7
   {:caption
    (str "ES7 is the next version which will include"
         " [Object.observe](http://www.html5rocks.com/en/tutorials/es7/observe/),"
         " [async](http://code.tutsplus.com/tutorials/a-primer-on-es7-async-functions--cms-22367),"
         " [comprehensions](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Array_comprehensions),"
         " and [guards](http://wiki.ecmascript.org/doku.php?id=strawman:guards).")
    [:es-captions :es6 :alpha] 0
    [:es-captions :es7 :alpha] 1}

   ;; ES8
   {:caption
    (str "ES8 is even further away, but it may bring [macros](http://www.2ality.com/2011/09/es6-8.html),"
         " a powerful way to grow JS syntax yourself.")
    [:es-captions :es7 :alpha] 0
    [:es-captions :es8 :alpha] 1}

   ;; flash all layers and show transpiler
   {:caption
    (str "But current/future JS features are not yet available in Browsers."
         " [Babel](https://babeljs.io/) or [Traceur](https://github.com/google/traceur-compiler)"
         " help alleviate this by transpiling to ES5.")
    [:transpiler :highlight] true
    [:es-captions :es8 :alpha] 0
    [:cam :x]                  {:a :_ :b 200 :duration 1}
    [:cam :y]                  {:a :_ :b 0 :duration 1}
    [:cam :zoom]               {:a :_ :b 1.3 :duration 1}
    [:transpiler :x]           {:a :_ :b 450 :duration 0.2}
    [:transpiler :alpha]       {:a :_ :b 1 :duration 0.2}
    [:transpiler :font-alpha]  {:a :_ :b 1 :duration 0.1}

    :highlight-layer
    {:a :_ :b nil
     :go-block #(go
                  (<! (timeout 50))
                  (doseq [i [1 2 3 4 3 2 1 2 3 4 3 2 1 2 3 4 nil]]
                    (swap! state assoc :highlight-layer i)
                    (<! (timeout 70))))}}

   ;; show linter
   {:caption
    (str "Also, to catch common JS language gotchas in our code, we rely on linters like"
         " [JSHint](http://jshint.com/about/) or [ESLint](http://eslint.org/).")

    [:transpiler :highlight] false
    [:linter :highlight] true
    [:linter :alpha]      {:a :_ :b 1 :duration 0.2}
    [:linter :font-alpha] {:a :_ :b 1 :duration 0.1}
    [:linter :x]          {:a :_ :b 450 :duration 0.2}}

   ;; show module sys
   {:caption
    (str "And for proper dependency loading, we must choose a module tool like"
         " [Browserify](http://browserify.org/),"
         " [webpack](http://webpack.github.io/),"
         " [SystemJS](https://github.com/systemjs/systemjs),"
         " [RequireJS](http://requirejs.org/), etc.")

    [:linter :highlight] false
    [:modulesys :highlight] true

    [:modulesys :alpha]      {:a :_ :b 1 :duration 0.2}
    [:modulesys :font-alpha] {:a :_ :b 1 :duration 0.1}
    [:modulesys :x]          {:a :_ :b 450 :duration 0.2}}

   ;; put JS back together, showing it required tools
   {:caption
    "Thus, anyone using JS directly today must use some combination of these tools as foundation for the language."
    [:modulesys :highlight] false
    [:transpiler :font-alpha] {:a :_ :b 0   :duration 2}
    [:linter :font-alpha]     {:a :_ :b 0   :duration 2}
    [:modulesys :font-alpha]  {:a :_ :b 0   :duration 2}
    [:transpiler :x]          {:a :_ :b 220 :duration 2}
    [:linter :x]              {:a :_ :b 220 :duration 2}
    [:modulesys :x]           {:a :_ :b 220 :duration 2}
    [:js-face :y]             {:a :_ :b 0   :duration 2}
    [:js-face :x]             {:a :_ :b 0   :duration 2}
    [:js-face :angle]         {:a :_ :b 0   :duration 2}
    [:js-face :alpha]         {:a 1 :b 1    :duration 2}
    [:cam :zoom]              {:a :_ :b 1   :duration 2}
    [:cam :x]                 {:a :_ :b 0   :duration 2}
    [:cam :y]                 {:a :_ :b 0   :duration 2}}

   ;; show staticsphere
   [{:caption
     "There are even several attempts to extend JS for extra type safety and optimization."
     [:transpiler :alpha] {:a :_ :b 0 :duration 1}
     [:linter :alpha]     {:a :_ :b 0 :duration 1}
     [:modulesys :alpha]  {:a :_ :b 0 :duration 1}
     [:cam :zoom]         {:a :_ :b 0.5 :duration 1}
     [:cam :y]            {:a :_ :b -50 :duration 1}}

    {[:static :title :alpha]  {:a :_ :b 1 :duration 1}
     [:static :sphere :alpha] {:a :_ :b 1 :duration 1}
     [:static :sphere :r]     {:a :_ :b 400 :duration 1}}]

   ;; show typescript
   {:caption
    (str "Microsoft's [TypeScript](http://www.typescriptlang.org/)"
         " extends ES6 with optional type annotation syntax and interfaces, and extra editor support.")
    [:cam :x]                    {:a :_ :b 300 :duration 1}
    [:static :typescript :alpha] {:a :_ :b 1 :duration 1}
    [:static :sphere :angle]     {:a :_ :b static-angle :duration 1}}

   ;; show flow
   {:caption
    (str "Facebook's [Flow](http://flowtype.org/) will check for type safety as well,"
         " even for plain JS without type annotations.")

    [:static :typescript :alpha] static-low-alpha
    [:static :flow :alpha]   {:a :_ :b 1 :duration 1}
    [:static :sphere :angle] {:a :_ :b (* 2 static-angle) :duration 1}}

   ;; show closure
   {:caption
    (str "Google makes heavy usage of its own [Closure Tools](Closure Tools),"
         " supporting [jsdoc tags](https://developers.google.com/closure/compiler/docs/js-for-compiler)"
         " for aggressive type optimization.")
    [:static :flow :alpha] static-low-alpha

    [:static :closure :alpha] {:a :_ :b 1 :duration 1}
    [:static :sphere :angle]  {:a :_ :b (* 3 static-angle) :duration 1}}

   ;; show soundscript
   {:caption
    (str "Google is also experimenting with [SoundScript](https://developers.google.com/v8/experiments),"
         " a new \"use stricter+types\" mode for VM-level optimizations.")
    [:static :closure :alpha] static-low-alpha
    [:static :soundscript :alpha] {:a :_ :b 1 :duration 1}
    [:static :sphere :angle]      {:a :_ :b (* 4 static-angle) :duration 1}}

   ;; fade out staticsphere details
   {:caption
    "There are also languages that are completely separate from JS, but still compile to it."
    [:static :typescript :alpha]  {:a :_ :b 0 :duration 1}
    [:static :flow :alpha]        {:a :_ :b 0 :duration 1}
    [:static :closure :alpha]     {:a :_ :b 0 :duration 1}
    [:static :soundscript :alpha] {:a static-low-alpha :b 0 :duration 1}
    [:cam :x]                     {:a :_ :b 0 :duration 1}
    [:static :sphere :angle]      {:a :_ :b 0 :duration 1}
    [:static :title :alpha]       {:a :_ :b 0 :duration 1}
    [:cam :zoom]                  {:a :_ :b 0.2 :duration 2}}

   ;; show coffeescript
   {:caption
    "[CoffeeScript](http://coffeescript.org/) is separate from JS but remains close.  Its simpler syntax and idioms have influenced features in ES6."
    :enable-orbits? true
    [:radar :orbit] :coffeescript
    [:coffeescript :highlight] true
    [:coffeescript :alpha] {:a :_ :b 1 :duration 1}}

   ;; show dart
   {:caption
    "[Dart](https://www.dartlang.org/) is larger. It embraces OOP with optional typing, a large core library, and development tools."
    [:coffeescript :highlight] false
    [:dart :highlight] true
    [:radar :orbit] :dart
    [:dart :alpha] {:a :_ :b 1 :duration 1}}

   ;; show clojurescript
   {:caption
    "[ClojureScript](http://clojurescript.org) eschews imperative/OO in favor of immutability, plain data, and a more expressive syntax."
    [:dart :highlight] false
    [:clojurescript :highlight] true
    [:clojurescript :angle] (- (/ PI 10))
    [:radar :orbit] :clojurescript

    [:clojurescript :alpha] {:a :_ :b 1 :duration 1}
    [:cam :y]               {:a :_ :b 0 :duration 1}
    [:cam :x]               {:a :_ :b 300 :duration 1}
    [:cam :zoom]            {:a :_ :b 0.15 :duration 1}}

   ;; show other languages
   [{:caption
    "There are many other active & mature languages/tool suites that build upon JavaScript's foundation."
    [:clojurescript :highlight] false
    [:radar :orbit] nil
    [:cam :zoom] {:a :_ :b 0.1 :duration 1}
    [:cam :x] {:a :_ :b 600 :duration 1}
    }

    {[:gwt :alpha] {:a :_ :b other-high :duration other-high-delay}}

    {[:gwt :alpha] {:a :_ :b other-low :duration other-low-delay}
     [:websharper :alpha] {:a :_ :b other-high :duration other-high-delay}}

    {[:websharper :alpha] {:a :_ :b other-low :duration other-low-delay}
     [:objective-j :alpha] {:a :_ :b other-high :duration other-high-delay}}

    {[:objective-j :alpha] {:a :_ :b other-low :duration other-low-delay}
     [:scala.js :alpha] {:a :_ :b other-high :duration other-high-delay}}

    {[:scala.js :alpha] {:a :_ :b other-low :duration other-low-delay}
     [:elm :alpha] {:a :_ :b other-high :duration other-high-delay}}

    {[:elm :alpha] {:a :_ :b other-low :duration other-low-delay}
     [:purescript :alpha] {:a :_ :b other-high :duration other-high-delay}}

    {[:purescript :alpha] {:a :_ :b other-low :duration other-low-delay}
     [:js_of_ocaml :alpha] {:a :_ :b other-high :duration other-high-delay}}

    {[:js_of_ocaml :alpha] {:a :_ :b other-low :duration other-low-delay}
     [:asm :alpha] {:a :_ :b other-high :duration other-high-delay}}

    {[:asm :alpha] {:a :_ :b other-low :duration other-low-delay}}]

   {:caption
    (str "I hope that this has added some visual order to the overwhelming space of JS. Happy travels!"
         " -- (((<i class='fa fa-space-shuttle'></i>")
    [:cam :x] {:a :_ :b 0 :duration 2}
    }

]))

;;----------------------------------------------------------------------
;; Action Loops
;;----------------------------------------------------------------------

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

(defn start-loops!
  "These are looping animations that cannot be represented as slide transitions.
  (Slide transitions have to end at some point.)"
  []
  (let [c (chan)]
    (tap tick-tap c)
    (go-loop []
      (let [dt (<! c)]
        (tick-orbits! dt)
        (tick-radar! dt))
      (recur))))
