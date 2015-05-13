(ns anibro-server.core
  (:use [compojure.core]
        [ring.util.response]
        [ring.middleware.content-type]
        [ring.middleware.params])
  (:require [anibro-server.api.chat :as chat]
            [anibro-server.db.article :as article]
            [ring.adapter.jetty :as jetty]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [cheshire.core :as json]
            [org.httpkit.server :refer [run-server]]
            [org.httpkit.timer :refer [schedule-task]])
  (:gen-class :main :true))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json/generate-string data)})

(defroutes api-routes
  (GET "/chat" req (chat/chat-handler req))
  (GET "/stream" [] chat/streaming-handler)
  (GET "/articles" req (json-response (article/get-articles req)))
  (route/resources "/")
  (route/files "/demo/" {:root "demo"})
  (route/not-found "Page not found"))

(defn wrap-dir-index
  "Middleware to force request for / to return index.html"
  [handler]
  (fn [req]
    (handler (update-in req [:uri] #(if (= "/" %) "/index.html" %)))))

(def app (-> api-routes
             wrap-params
             wrap-content-type
             wrap-dir-index
             handler/site))

(defonce server (atom nil))

(defn -main
  "lein runで呼ばれるメインプログラムです"
  [& args]
  (println "starting server ...")
  (reset! server (run-server (handler/site app) {:port 3003})))
