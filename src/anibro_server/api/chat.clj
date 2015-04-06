(ns anibro-server.api.chat
  (:require [cheshire.core :refer :all]
            [org.httpkit.server :refer
             [with-channel websocket? on-receive send! on-close close]]
            [org.httpkit.timer :refer [schedule-task]]))

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
      (println "start" @chat-channel-hub)
      (println "param-seq: " param-seq "id: " id)
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
         (println "on-recieve" @chat-channel-hub)
         (if (= data "h8ze@91bmkfp3")
           (do
             (swap! chat-channel-hub dissoc channel) ;ハブからチャネルを削除
             (doseq [channel (keys (filter #(= room (second %)) @chat-channel-hub))]
               (leave-notification channel id)))
           (doseq [channel (keys (filter #(= room (second %)) @chat-channel-hub))]
             (println "send!!" channel)
             (send-data channel id data)))))
    )))
