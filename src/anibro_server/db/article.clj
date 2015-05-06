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

(def db (mg/get-db (mg/connect) "anibro"))

(defn add-article
  [id data room]
  (let [coll "article"]
    (mc/insert db coll
               {:user-id id
                :text data
                :chat-room room
                :date (tl/local-now)})))
