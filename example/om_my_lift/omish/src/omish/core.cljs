(ns omish.core
  (:require [goog.events :as events]
            [om.core :as om]
            [om.dom :as dom]
            [cognitect.transit :as t]))

(enable-console-print!)

(def t-reader (t/reader :json))

(def t-writer (t/writer :json))

(def app-state (atom {:chats []}))

(defn by-id [id] (. js/document (getElementById id)))

