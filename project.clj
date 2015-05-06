(defproject anibro-server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]])

(defproject anibro-server "0.1.0-SNAPSHOT"
  :description "bradcast api server and web site"
  :url "http://circularuins/anibro"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring/ring-core "1.3.2"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [compojure "1.3.2"]
                 [http-kit "2.1.18"]
                 [cheshire "5.4.0"]
                 [com.novemberain/monger "2.0.0"]
                 [org.clojure/clojurescript "0.0-3126"]]
  :dev-dependencies [[ring/ring-devel "1.3.2"]]
  :plugins [[lein-ring "0.9.3"]
            [lein-cljsbuild "1.0.5"]]
  :main anibro-server.core
  :aot [anibro-server.core]
  :ring {:handler anibro-server.core/app}
  :profiles {:dev {:dependencies [[ring/ring-mock "0.2.0"]]}}
  ; clojurescriptのビルド関連
  :hooks [leiningen.cljsbuild]
  :cljsbuild {
    :builds {
      :main {
        :source-paths ["src/cljs"]
        :compiler {:output-to "resources/public/js/cljs.js"
                   :optimizations :simple
                   :pretty-print true}
        :jar true}}}
  )
