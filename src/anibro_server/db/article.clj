(ns anibro-server.db.article
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.query :as mq]
            [monger.operators :as mo]
            [clojure.string :as str]
            [clj-time
             [core :as t]
             [local :as tl]
             [coerce :as coerce]])
  (:import java.util.Date
           java.util.regex.Pattern
           org.bson.types.ObjectId))

(defn fix-object [object]
  (let [id (:_id object)]
    (-> object
        (assoc :id (str id))
        (dissoc :_id))))

(def db (mg/get-db (mg/connect) "anibro"))

(defn add-article
  [id data room]
  (let [coll "article"]
    (mc/insert db coll
               {:user-id id
                :text data
                :chat-room room
                :date (.toString (tl/local-now))})))

(defn get-articles
  [req]
  (let [room ((req :params) :room)]
    (->> (mq/with-collection db "article"
           (mq/find {:chat-room room})
           (mq/sort (array-map :date 1)))
         (map fix-object))))
