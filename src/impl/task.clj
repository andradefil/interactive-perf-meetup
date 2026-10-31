;; **
;; * Author: Filipe Andrade
;; * Created At: 10/30/26
;; **/

(ns system.process)
;; Define a Long Running Task
(defprotocol LongRunningProcessLifecycle
  (start [this] "Start a long running task")
  (finish [this] "Stop the runnig task"))

;; Implements lifecycle protocol
(defrecord LongRunningProcess
    [worker]
    LongRunningProcessLifecycle
    (start [this]
      (reset!
       worker
       (Thread.
        (fn []
          (try
            (loop []
              (println "dispatches op")
              (println "sleeping 1000ms")              
              (Thread/sleep 1000)
              (recur))
            (catch InterruptedException _)))))
      (.start @worker))
    (finish [this]
      (.interrupt @worker)
      (reset! worker nil)))

(defn create-process
  []
  (->LongRunningProcess (atom nil)))

(comment
  (require '[system.process :as p])
  (def proc (p/create-process))
  (p/start proc)
  (p/finish proc))
