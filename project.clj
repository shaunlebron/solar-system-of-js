(defproject solar-system-of-js "0.1.0-SNAPSHOT"
  :description "Visualizing the state of JS as a solar system"
  :url "https://github.com/shaunlebron/solar-system-of-js"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3058"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [figwheel "0.2.5"]]

  :node-dependencies [[source-map-support "0.2.8"]]

  :plugins [[lein-cljsbuild "1.0.4"]
            [lein-npm "0.4.0"]
            [lein-figwheel "0.2.5"]]

  :source-paths ["src" "target/classes"]

  :clean-targets ["resources/public/js/out"
                  "resources/public/js/out-adv"]

  :figwheel {:http-server-root "public" ;; this will be in resources/
             :server-port 3449          ;; default
             }

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]
              :compiler {
                :output-to "resources/public/js/solar_system_of_js.js"
                :output-dir "resources/public/js/out"
                :optimizations :none
                :cache-analysis true
                :source-map true}}
             {:id "release"
              :source-paths ["src"]
              :compiler {
                :output-to "resources/public/js/solar_system_of_js.min.js"
                :output-dir "resources/public/js/out-adv"
                :optimizations :advanced
                :pretty-print false}}]})
