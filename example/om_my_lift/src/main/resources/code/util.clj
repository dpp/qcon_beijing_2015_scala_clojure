(ns code.util
  (:require [clojure.core.async :as async :refer [put!]])
  (:import (scala.collection Seq Iterator Map)
           (scala Product PartialFunction Symbol)
           (net.liftweb.actor LiftActor)
           (clojure.lang IFn)
           (code.lib MyActor)
           (clojure.core.async.impl.channels ManyToManyChannel)))

(defprotocol FromScala
  "Converts all manner of Scala stuff into Clojure stuff"
  (to-c [x] "Convert the Scala thing to the clojure thing"))

(extend Iterator FromScala
  {:to-c
   (fn [^Iterator it]
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
   (fn [^Map the-map]
     (into {} (map
                #(to-c %)
                (-> the-map .iterator to-c)))
     )})

(extend Product FromScala
  {:to-c
   (fn [^Product prod]
     (if
       ;; things that are a Prod and a Seq... treat as a Seq
       (instance? Seq prod) (seq-to prod)

       (mapv #(->> % (.productElement prod) to-c)
             (range 0 (.productArity prod)))))})

(extend LiftActor FromScala
  {:to-c
   (fn [^LiftActor actor]
     (fn [x] (.$bang actor x))
     )})

(extend Symbol FromScala
  {:to-c
   (fn [^Symbol s] (-> s .name keyword))})



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

(defprotocol Applyable
  "Does an application... treats whatever like a single param function"
  (apply-it [the-fn param] "applies the parameter"))

(extend IFn Applyable
  {:apply-it
   (fn [^IFn the-fn param] (the-fn param))})

(extend scala.Function1 Applyable
  {:apply-it
   (fn [^scala.Function1 the-fn param] (.apply the-fn param))})

(extend ManyToManyChannel Applyable
  {:apply-it
   (fn [^ManyToManyChannel the-chan param] (put! the-chan param))}
  )


(defn build-actor
  "Takes a Clojure function and invokes the function on every message received from the Actor"
  [the-fn]
  (require nil)
  (let [my-this (atom nil)
        ret
        (proxy [MyActor] []
          (messageHandler []
            (proxy [PartialFunction] []
              (isDefinedAt [x] true)
              (apply [x] (binding [*current-actor* @my-this]
                           (apply-it the-fn x))))))]
    (reset! my-this ret)
    ret
    )
  )

(defn actor-channel
  "Builds a Scala actor and a core.async channel. When a message is sent
  to the Actor, it's put on the channel. Returns a Vector containing the Actor
  and the channel"
  (let [my-chan (async.chan)
        the-actor (build-actor (fn [msg] (put! my-chan msg)))]
    [the-actor my-chan])

  )
