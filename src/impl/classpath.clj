(ns impl.classpath
  (:require [clojure.java.shell :as sh]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import java.io.Writer))

(def ^:dynamic *print-string-length* nil)

;; how many items
(set! *print-length* 5)

;; how many levels deep
(set! *print-level* 5)

(defonce default-print-method (get-method print-method String))
(defmethod print-method String [^String s, ^Writer w]
  (let [slen *print-string-length*]
    (if (and slen (< slen (count s)))
      (default-print-method (str (subs s 0 slen) "...") w)
      (default-print-method s w))))

(defn build-jar-path
  [& coords]
  (let [user-root (System/getProperty "user.home")]
    (map (fn [jar]
           (str user-root "/.m2/repository" jar)) coords)))

(defn inspect-jar
  "inspect jar in the local .m2 repository given jar coordinates"
  [coords]
  (let [jar-path (build-jar-path coords)
        {:keys [exit out]} (sh/sh "jar" "-tf" jar-path)]
    (if (zero? exit)
      (str/split out #"\n"))))

(def jars ["/org/clojure/clojure/1.12.0/clojure-1.12.0.jar"
           ;; FIXME
            "/org/clojure/core.specs.alpha/0.4.74/core.specs.alpha-0.4.74.jar"
            "/org/clojure/spec.alpha/0.5.238/spec.alpha-0.5.238.jar"

           ;; TODO
           "/http-kit/http-kit/2.8.1/http-kit-2.8.1.jar"
           ])

(def sources ["src"])
(def lib ["lib"])

(defn build-classpath*
  "compute the classpath for a clojure application"
  []
  (let [cp (concat sources
                   (apply build-jar-path jars))]
    (str/join ":" cp)))

(defn build-classpath [_]
  (println (build-classpath*)))

(comment
  (in-ns 'impl.classpath)
  (def user-root (System/getProperty "user.home"))
  (def coords ["/org/clojure/clojure/1.12.0/clojure-1.12.0.jar"])


  (def out "foo,bar")
  (alter-var-root #'*print-string-length* (constantly 25))
  (alter-var-root #'*print-string-length* (constantly nil))
  (inspect-jar coords)

  ;; download deps in`to .m2
  clj -Sdeps '{:deps {http-kit/http-kit {:mvn/version "2.8.1"}}}' -Srepro -Stree 

  ;; generate cp from cli
  clj -X impl.classpath/build-classpath

  ;; invoke clojure from java
  java -cp `clj -X impl.classpath/build-classpath` clojure.main
  )
