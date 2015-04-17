(ns omish.core
  (:require [goog.events :as events]
            [om.core :as om]
            [om.dom :as dom]
            [cognitect.transit :as t]))

(enable-console-print!)

(def t-reader (t/reader :json))

(def t-writer (t/writer :json))

(def app-state (atom {:chats []}))

(defn receive
      "receive from server"
      [x]
      (let
        [native (t/read t-reader x)]
        (cond
          (seq? native)
          (swap! app-state assoc-in [:chats] (vec native))

          (string? native)
          (swap! app-state update-in [:chats] conj native)

          :else nil)))

(defn by-id [id] (. js/document (getElementById id)))

(defn send
      "send data to the server"
      [data]
      (js/sendToServer (t/write t-writer data)))

(om/root
  (fn [data owner]
      (reify om/IRender
             (render [x]
                     (apply
                       dom/ul
                       nil
                       (map #(dom/li nil %) (:chats data))))))
  app-state
  {:target (by-id "chats")})

(defn send-chat
      []
      (let [box (by-id "in")]
           (send (.-value box))
           (set! (.-value box) "")
           ))

(set! (.-onclick (by-id "send")) send-chat )