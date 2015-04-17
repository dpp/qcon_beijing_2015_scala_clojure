(defproject omish "0.1.0-SNAPSHOT"
  :description "The Om front-end"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2760"]
                 [com.cognitect/transit-cljs "0.8.207"]
                 [org.omcljs/om "0.8.8"]]

  :plugins [[lein-cljsbuild "1.0.5"]]

  :clean-targets ["../src/main/webapp/gen_js"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :compiler {
                           :output-to "../src/main/webapp/gen_js/omish.js"
                           :main omish.core
                           :source-map-timestamp true
                           :cache-analysis true
                           :output-dir "../src/main/webapp/gen_js/"
                           :optimizations :none
                           :source-map true}}]})
