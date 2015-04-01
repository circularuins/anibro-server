(ns anibro-server.core
  (:use [compojure.core]
        [ring.util.response]
        [ring.middleware.content-type]
        [ring.middleware.params])
  (:require [ring.adapter.jetty :as jetty]
            [cheshire.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [org.httpkit.server :refer
             [with-channel websocket? on-receive send! on-close close run-server]]
            [org.httpkit.timer :refer [schedule-task]])
  (:gen-class :main :true))

;; チャット
(def chat-channel-hub (atom {}))

(defn send-data
  [channel id data]
  (send!
   channel
   {:status 200
    :headers {"Content-Type" "application/json; charset=utf-8"}
    :body (generate-string {:id id :data data :time (new java.util.Date)})}
   ))

(defn enter-notification
  [channel id]
  (send!
   channel
   {:status 200
    :headers {"Content-Type" "application/json; charset=utf-8"}
    :body (generate-string {:message (str id "が入室しました！") :time (new java.util.Date)})}
   ))

(defn leave-notification
  [channel id]
  (send!
   channel
   {:status 200
    :headers {"Content-Type" "application/json; charset=utf-8"}
    :body (generate-string {:message (str id "が退出しました！") :time (new java.util.Date)})}
   ))

(defn delete-all-channel
  []
  (reset! chat-channel-hub {}))

;; wscat -c ws://localhost:3003/chat?user=soukyuuno/123 こんな感じのクエリでコネクション作成する
(defn chat-handler
  "チャットを実現する"
  [req]
  (with-channel req channel
    (let [param-seq (.split ((req :params) :user) "/")
          room (get param-seq 0)
          id (get param-seq 1)]
      ;; ハブにユーザーチャネルとルームのペアを登録する
      (swap! chat-channel-hub assoc channel room)
      ;; 接続解除時の処理
      (on-close
       channel
       (fn [status]
         (println "チャネル閉鎖")
         (swap! chat-channel-hub dissoc channel) ;ハブからチャネルを削除
         (doseq [channel (keys (filter #(= room (second %)) @chat-channel-hub))]
              (leave-notification channel id))
         ))
      ;; 接続開始時の処理
      (if (websocket? channel)
        (do (println "WebSocketチャネル生成")
            (doseq [channel (keys (filter #(= room (second %)) @chat-channel-hub))]
              (enter-notification channel id)))
        (println "HTTPチャネル生成"))
      ;; データ受信時の処理
      (on-receive
       channel
       (fn [data]
         (doseq [channel (keys (filter #(= room (second %)) @chat-channel-hub))]
           (send-data channel id data))))
    )))

(defroutes api-routes
  (GET "/chat" req (chat-handler req))
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
