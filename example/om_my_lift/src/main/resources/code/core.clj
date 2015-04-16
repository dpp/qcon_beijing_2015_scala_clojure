(ns code.core
  (:require [code.util :as util]
            [clojure.core.async :as async]))

(defonce chat-server (async/chan))

(def- send-msg
      "Send a message to the channel or function or whatever"
      [chan msg]
      (cond
        (ifn? chan) (chan msg)
        :else (async/put! chan msg))
      )

(async/go-loop [chats []
                listeners []]
  (let [msg (async/<! chat-server)]
    (cond
      (string? msg)
      (do
        (doseq [x listeners] (send-msg x msg))
        (recur (conj chats x) listeners)
        )

      (= :add (first msg))
      (let [chan (second msg)]
        (send-msg chan (take-last 40 chats))
        (recur chats (conj listeners chan))
        )

      (= :remove (first msg))
      (let [chan (second msg)]
        (recur chats (vec (remove #(identical? % chan) listeners))))

      :else (recur chats listeners))
    ))