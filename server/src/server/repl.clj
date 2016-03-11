(ns server.repl
  (:require [server.db :as db]
            [server.edb :as edb]
            [server.log :as log]
            [server.smil :as smil]
            [server.compiler :as compiler]
            [server.serialize :as serialize]
            [server.exec :as exec]))

(def bag (atom 98))
(def user (atom 99))

(defn repl-error [& thingy]
  (throw thingy))

;; the distinction between edb and idb is alive here..skating over it
;; query currently needs to always have a projection
(defn build-reporting-select [db terms]
  (let [z (smil/expand terms)
        p (second z)
        v (apply concat
                 (rest (rest z))
                 (if (empty? p) () (list (list (list 'return p)))))]
    (compiler/compile-dsl db @bag v)))

(defn show [d expression]
   (let [prog (build-reporting-select d (second expression))]
     (println (exec/print-program prog))))

(defn diesel [d expression]
  ;; the compile-time error path should come up through here
  ;; fix external number of regs
  (let [prog (build-reporting-select d expression)
        ec  (exec/open d prog println)]
    (ec 'insert [])
    (ec 'flush [])))

;; xxx - this is now...in the language..not really?
(defn define [d expression]
  (let [z (smil/expand expression)]
    (db/insert-implication d (second z) (nth z 2) (rest (rest (rest z))) @user @bag)))


(declare read-all)

;; xxx - use the provenance compiler
(defn trace [db tuple] ())
  
  
(defn eeval [d term]
  (let [function ({'trace trace
                   'define! define
                   'show show
                   'load read-all
                   } (first term))]
    (if (nil? function)
      (diesel d term)
      (function d term))))

(import '[java.io PushbackReader])
(require '[clojure.java.io :as io])

(defn read-all [db expression]
  ;; trap file not found
  ;; need to implement load path here!

  (let [filename (second expression) 
        rdr (try (-> (.getPath (clojure.java.io/resource filename)) io/file io/reader PushbackReader.) 
                 (catch Exception e (-> filename io/file io/reader PushbackReader.)))]
    
    (loop []
      ;; terrible people, always throw an error, even on an eof, so cant print read errors? (println "load parse error" e)
      (let [form (try (read rdr) (catch Exception e ()))]
        (if (and form (not (empty? form)))
          (do 
            (eeval db form)
            (recur)))))))
  

(defn rloop [d]
  (loop [d d]
    (doto *out* 
      (.write "eve> ")
      (.flush))
    ;; need to handle read errors, in particular eof
    (recur (eeval d (try
                      ;; it would be nice if a newline on its own got us a new prompt
                      (read)
                      ;; we're-a-gonna assume that this was a graceful close
                       (catch Exception e 
                         (java.lang.System/exit 0)))))))

