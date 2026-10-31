;; **
;; * Author: Filipe Andrade
;; * Created At: 10/30/26
;; **/

(require '[clojure.core.async :as a])

(a/<!! (a/thread (Thread/sleep 3000) "Foo"))

(def c (a/chan 2))
(def o (a/thread (a/<!! c)))

(a/>!! c "Foo")

(Thread/getAllStackTraces)


(defn list-threads
  []
  (map (fn [t] {:thread t
                :name (.getName t)}) (.keySet (Thread/getAllStackTraces))))

(defn interrupt-thread-by-name
  [threads thread-name]
  (doseq [th threads]
    (if (= thread-name (:name th))
      (do (.interrupt (:thread th))
          true)
      false)))

(defn interrupt-all-threads
  [threads]
  (doseq [th threads]
    (.interrupt (:thread th))))

(def threads (list-threads))

(interrupt-thread-by-name threads "async-mixed-1")
(interrupt-thread-by-name threads "async-mixed-7")
(interrupt-thread-by-name threads "main")

(.interrupt (:thread (first threads)))
(.isInterrupted (:thread (first threads)))
(.stop (:thread (first threads)))

(interrupt-all-threads threads)


(interrupt-thread-by-name threads "main")
(.isInterrupted (Thread/currentThread))
(Thread/sleep 5000)

(Thread/sleep 3000)
(.isInterrupted (Thread/currentThread))
