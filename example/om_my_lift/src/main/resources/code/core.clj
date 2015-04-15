(ns code.core
  (:require [code.util :as util]
            [clojure.core.async :as async]))

(defonce chat-server (async/chan))


(async/go-loop [chats []
                listeners []]
  (let [msg (async/<! chat-server)]
    (cond
      (string? msg)
      (do
        (doseq [x listeners] (async/put! x msg))
        (recur (conj chats x) listeners)
        )

      (= :add (first msg))
      (let [chan (second msg)]
        (async/put! chan (take-last 40 chats))
        (recur chats (conj listeners chan))
        )

      (= :remove (first msg))

      :else (recur chats listeners))
    ))