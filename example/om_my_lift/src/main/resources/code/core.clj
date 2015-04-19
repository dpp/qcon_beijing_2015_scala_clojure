(ns code.core
  (:require [code.util :as util :refer [send!]]
            [clojure.core.match :as match :refer [match]]
            [clojure.core.async :as async :refer [chan put! <!]]))

(def chat-server (chan))

(defn post-msg
  "Posts a message to the chat-server"
  [msg]
  (->> msg util/to-c (send! chat-server)))

(async/go-loop [chats [] listeners []]
    (match (<! chat-server)
      [:add f]
      (do
        (send! f (take-last 40 chats))
        (recur chats (conj listeners f)))

      [:remove f]
      (recur chats (remove #(identical? f %) listeners))

      (msg :guard string?)
      (do
        (doseq [f listeners] (send! f msg))
        (recur (conj chats msg) listeners))

      :else
      (recur chats listeners)
      ))