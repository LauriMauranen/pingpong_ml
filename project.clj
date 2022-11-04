(defproject pingpong "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/clojurescript "1.11.60" :scope "provided"]
                 [quil "3.1.0"]
                 [compojure "1.7.0"]
                 [com.taoensso/sente "1.17.0"]
                 [com.bhauman/figwheel-main "0.2.18"]
                 [http-kit "2.6.0"]
                 [com.bhauman/rebel-readline-cljs "0.1.4"]]

  :main pingpong.server

  :source-paths ["src/clj" "src/cljs" "src/cljc"]

  :resource-paths ["target" "resources"]

  :aliases {"build-dev" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]})
