(defproject pingpong "1.0.0"
  :description "Pingpong multiplayer game"
  :url "http://pingpong.wtf"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/clojurescript "1.11.60" :scope "provided"]
                 [quil "3.1.0"]
                 [compojure "1.7.0"]
                 [com.taoensso/sente "1.17.0"]
                 [com.bhauman/figwheel-main "0.2.18"]
                 [com.bhauman/rebel-readline-cljs "0.1.4"]
                 [http-kit "2.6.0"]
                 [environ "1.2.0"]]

  :plugins [[lein-environ "1.2.0"]]

  :main pingpong.main

  :source-paths ["src/clj" "src/cljs" "src/cljc"]

  :aliases {"fig" ["trampoline" "run" "-m" "figwheel.main"]
            "build-dev" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
            "build-prod" ["run" "-m" "figwheel.main" "-O" "advanced" "-bo" "prod"]}
  
  :profiles {:uberjar {:aot [pingpong.main]}
             :dev {:resource-paths ["resources" "target"]
                   :clean-targets ^{:protect false} ["target"]
                   :env {:pingpong-backend-port 8090
                         :pingpong-production false}}})
