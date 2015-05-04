(ns anibro-server.api.chat
  (:require [cheshire.core :refer :all]
            [org.httpkit.server :refer
             [with-channel websocket? on-receive send! on-close close run-server]]
            [org.httpkit.timer :refer [schedule-task]]
            [clj-time.local :as tl]))

(def chat-channel-hub (atom {}))

(defn count-population
  [channels]
  (for [m (frequencies (vals @channels))
        :let [res {}]]
    (conj res {"roomId" (key m) "population" (val m)})))

(defn send-data
  [channel id data]
  (send!
   channel
   {:status 200
    :headers {"Content-Type" "application/json; charset=utf-8"}
    :body (generate-string {:id id :data data :time (.toString (tl/local-now))})}
   ))

(defn send-population
  [channel populations]
  (send!
   channel
   {:status 200
    :headers {"Content-Type" "application/json; charset=utf-8"}
    :body (generate-string {:populations populations})}
   false ; falseはsend!の後にクローズしない
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
      (println "params: " (req :params))
      (println "param-seq: " param-seq "room: " (get param-seq 0) "id: " (get param-seq 1))
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

;; チャットルームごとの人数を配信する
(defn streaming-handler [request]
  "WebSocketのストリーム通信を行います。あるインターバルで指定された回数、サーバーからpushします"
  (with-channel request channel
    (on-close channel (fn [status] (println "チャネルがクローズされました, " status)))
    (loop [id 0]
      (when (< id 10000) ;; 10000回クライアントに送ります。
        (schedule-task 
         (* id 1000) ;; 1000msごとに通信する。
         (send-population channel (count-population chat-channel-hub))
         (println "streeeeam" (count-population chat-channel-hub)))
        (recur (inc id))))
    ;; (while true
    ;;   (Thread/sleep 1000)
    ;;   (send-population channel (count-population chat-channel-hub))
    ;;   (println "streeeeam" (count-population chat-channel-hub)))
    (schedule-task 600000 (close channel)))) ;; 600秒経ったらクローズします。


;;; デバッグ用
(defonce server (atom nil)) ;; 競合を避けるためatomを使う。
(defn stop-server 
  "msの指定時間を待ってサーバーをgracefulに停止させます。タイムアウトのオプションがなければ即時に停止させます。"
  []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))
;(stop-server)
;(reset! server (run-server #'chat-handler {:port 8080}))
;(reset! server (run-server #'streaming-handler {:port 8081}))
