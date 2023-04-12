(ns tg-method.core
  (:require [babashka.curl      :as curl]
            [clojure.java.io    :as io]
            [cheshire.core      :as json]
            [clojure.core.async :as a]))

(def base-url "https://api.telegram.org/bot")

(defn find-index [pred arr]
  (->> arr
       (map-indexed #(if (pred %2) %1))
       (filter number?)
       first))

(defn tg-do [token method data]
  (-> (curl/post (str base-url token method) data)
      :body
      (json/parse-string true)))

(defmacro tg-method [name method & parameters]
  `(defn ~name
     ([token ~@parameters] (~name token ~@parameters {}))
     ([token ~@parameters opts]
      (tg-do token ~method
             {:form-params
              (into opts
                    ~(reduce into
                             (map #(hash-map (keyword %) %) ; '(a b c d) => {:a a, :b b, :c c, :d d}
                                  parameters)))}))))

(tg-method get-updates "/getUpdates")
(tg-method send-message "/sendMessage" chat_id text)
(tg-method send-photo "/sendPhoto" chat_id photo)
(tg-method forward-message "/forwardMessage" chat_id from_chat_id message_id)
(tg-method delete-message "/deleteMessage" chat_id message_id)
(tg-method copy-message "/copyMessage" chat_id from_chat_id message_id)
(tg-method send-location "/sendLocation" chat_id latitude longitude message_id)
(tg-method get-file "/getFile" file_id)

(defn get-updates* [token offset]
  (-> (get-updates token {:offset offset})
      :result
      seq))

(defn async-map [xs f]
  (doseq [x xs]
    (a/go (f x))))

(defn polling [token handler timeout]
  (loop [last-update-id 0]
   (if-let [updates (get-updates* token last-update-id)] ; If updates is not nil (seq turns empty lists to nil)
       (do (async-map updates handler) ; Apply handler to every update asynchronously
           (Thread/sleep timeout)
           (-> updates last :update_id inc recur))
       (recur last-update-id))))

(defn sender-first-name [msg]
  (-> msg :from :first_name))
(defn sender-id [msg]
  (-> msg :from :id))
(defn message? [upd]
  (contains? :message upd))
(defn message-text-is [msg text]
  (= text (:text msg)))
(defn answer-keyboard [markup]
  (json/generate-string {:keyboard markup :one_time_keyboard true}))
