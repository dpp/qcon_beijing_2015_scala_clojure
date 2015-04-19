(ns code.util
  (:require [clojure.core.async :as async :refer [put!]]
            [clojure.core.match :as match :refer [match]])
  (:import (scala.collection Seq Iterator Map)
           (scala Product PartialFunction Symbol)
           (net.liftweb.actor LiftActor)
           (clojure.lang IFn IPersistentVector Keyword)
           (code.lib MyActor)
           (clojure.core.async.impl.channels ManyToManyChannel)

           ))

(defprotocol FromScala
  "Converts all manner of Scala stuff into Clojure stuff"
  (to-c [x] "Convert the Scala thing to the clojure thing"))

(extend Iterator FromScala
  {:to-c
   (fn [it]
     (letfn [(build
               []
               (if (.hasNext it)
                 (cons (to-c (.next it))
                       (lazy-seq (build)))
                 nil))]
       (build)))})

(defn seq-to [^Seq seq] (-> seq .iterator to-c))

(extend Seq FromScala
  {:to-c seq-to})

(extend Map FromScala
  {:to-c
   (fn [the-map]
     (into {} (map
                #(to-c %)
                (-> the-map .iterator to-c)))
     )})

(extend Product FromScala
  {:to-c
   (fn [prod]
     (if
       ;; things that are a Prod and a Seq... treat as a Seq
       (instance? Seq prod) (seq-to prod)

       (mapv #(->> % (.productElement prod) to-c)
             (range 0 (.productArity prod)))))})

(extend Symbol FromScala
  {:to-c
   (fn [s] (-> s .name keyword))})



(defn extend-a-func
  [arity]
  `(extend ~(symbol (str "scala.Function" arity))  FromScala
     {:to-c
      (fn [func#]
        (fn ~(mapv #(symbol (str "p" % "#")) (range 0 arity))
          (.apply func# ~@(map #(symbol (str "p" % "#")) (range 0 arity)) )))
      }))

(defmacro extend-all
  []
  `(do ~@(map #(extend-a-func %) (range 0 20))))

(extend-all)

(extend Object FromScala
  {:to-c identity})

(def ^:dynamic *current-actor* nil)

(defprotocol Sendable
  "Send the message to the thing... whatever that thing is"
  (send! [the-fn param] "applies the parameter"))

(extend IFn Sendable
  {:send!
   (fn [the-fn param] (the-fn param))})

(extend scala.Function1 Sendable
  {:send!
   (fn [the-fn param] (.apply the-fn param))})

(extend ManyToManyChannel Sendable
  {:send!
   (fn [the-chan param] (put! the-chan param))}
  )

(extend LiftActor Sendable
  {:send!
   (fn [actor param] (.$bang actor param))}
  )


(defprotocol ClojureToScala
  "Converts a Clojure data structyre to a Scala data structure"
  (to-s [x] "converts x to a Scala data structure"))

(extend Object ClojureToScala
  {:to-s identity})

(defn kw-to-symbol
  "Keyword or symbol"
  [kw]
  (.apply scala.Symbol$/MODULE$ (name kw)))

(defn map-to-scala
  "Converts a Map to Scala"
  [^java.util.Map the-map]
  (reduce
    (fn [^scala.collection.Map m [k v]]
      (.$plus m (scala.Tuple2. (to-s k) (to-s v))))
    (.empty scala.collection.immutable.Map$/MODULE$)
    the-map)
  )

(defn list-to-scala
  "Convert a java.util.List to Scala"
  [^java.util.List the-vec]
  (-> (.asScalaBuffer scala.collection.JavaConversions$/MODULE$ (map to-s the-vec)) .toVector))

(extend IFn ClojureToScala
  {:to-s (fn [the-fn]
           (cond
             (or (keyword? the-fn)
                 (symbol? the-fn))                         ;; keywords are functions, too
             (kw-to-symbol the-fn)

             (instance? java.util.Map the-fn) (map-to-scala the-fn)

             (instance? java.util.List the-fn) (list-to-scala the-fn)

             :else
             (do
               (println "Dealing with function " the-fn " class " (.getClass the-fn))
               (code.lib.MySingleFunc the-fn))))})

(extend java.util.Map ClojureToScala
  {:to-s map-to-scala})

(extend java.util.List ClojureToScala
  {:to-s list-to-scala})

(extend nil ClojureToScala
  {:to-s (fn [_]  scala.collection.immutable.Nil$/MODULE$)})

(extend Keyword ClojureToScala
  {:to-s kw-to-symbol})

(extend clojure.lang.Symbol ClojureToScala
  {:to-s kw-to-symbol})


(defn build-actor
  "Takes a Clojure function and invokes the function on every message received from the Actor"
  [the-fn]
  (let [my-this (atom nil)
        ret
        (proxy [MyActor] []
          (messageHandler []
            (proxy [PartialFunction] []
              (isDefinedAt [x] true)
              (apply [x] (binding [*current-actor* @my-this]
                           (send! the-fn x))))))]
    (reset! my-this ret)
    ret
    )
  )


(defn actor-channel
  "Builds a Scala actor and a core.async channel. When a message is sent
  to the Actor, it's put on the channel. Returns a Vector containing the Actor
  and the channel"
  []
  (let [my-chan (async/chan)
        the-actor (build-actor (fn [msg] (put! my-chan msg)))]
    [the-actor my-chan])

  )
