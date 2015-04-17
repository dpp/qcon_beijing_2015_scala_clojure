(ns code.core
  (:require [code.util :as util]
            [clojure.core.async :as async]))

(def chat-server (async/chan))

(defn post-msg
  "Posts a message to the chat-server"
  [msg]
  (->> msg util/to-c (async/put! chat-server)))

(async/go-loop [chats []
                listeners []]
  (let [msg (async/<! chat-server)]
    (cond
      (string? msg)
      (do
        (doseq [f listeners] (f msg))
        (recur (conj chats msg) listeners)
        )

      (= :add (first msg))
      (let [f (second msg)]
        (f (take-last 40 chats))
        (recur chats (conj listeners f))
        )

      :else
      (do
        (println "Got " msg " dunno what to do with it")
        (recur chats listeners)))
    ))