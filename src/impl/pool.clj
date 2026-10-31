;; **
;; * Author: Filipe Andrade
;; **/

(ns http-thread-pool.core
  (:import
   [java.net URI]
   [java.net.http HttpClient HttpClient$Version
    HttpRequest HttpResponse$BodyHandlers]
   [java.time Duration]
   [java.util.concurrent CompletableFuture
    Executors LinkedBlockingQueue RejectedExecutionHandler
    ThreadFactory ThreadPoolExecutor TimeUnit]))

(defn- daemon-thread-factory [prefix pool]
  (let [idx (atom 0)
        hash (System/identityHashCode pool)]
    (reify
      ThreadFactory
      (newThread [_ runnable]
        (doto (.newThread (Executors/defaultThreadFactory) runnable)
          (.setName (str prefix "@" (Integer/toHexString hash) "-" (swap! idx inc)))
          (.setDaemon true))))))

(defn http-cached-thread-pool []
  (let [min-threads 8
        max-threads 200
        pending-ops-limit 64
        pool (ThreadPoolExecutor.
              min-threads
              max-threads
              60 TimeUnit/SECONDS
              (LinkedBlockingQueue. (* 2 (int pending-ops-limit))))]
    (.setThreadFactory pool (daemon-thread-factory "http-client" pool))
    pool))

(comment
  (in-ns 'http-thread-pool.core)
  (require '[http-thread-pool.core :refer :all])
  (java.lang.ProcessHandle/current)
  (set! *print-length* 5)

  (def client1 (-> (HttpClient/newBuilder)
                   (.version HttpClient$Version/HTTP_1_1)
                   (.connectTimeout (Duration/ofMillis 5000))
                   (.executor (http-cached-thread-pool))
                   (.build)))
  (def client2 (-> (HttpClient/newBuilder)
                   (.version HttpClient$Version/HTTP_1_1)
                   (.connectTimeout (Duration/ofMillis 5000))
                   (.executor (http-cached-thread-pool))
                   (.build)))
  (def request (-> (HttpRequest/newBuilder)
                   (.uri (URI/create "https://datomic.com"))
                   (.timeout (Duration/ofMillis 5000))
                   (.GET)
                   (.build)))

  (mapv (fn [_]
          (.sendAsync client1 request (HttpResponse$BodyHandlers/ofString)))
        (range 500))

  (mapv (fn [_]
          (.sendAsync client2 request (HttpResponse$BodyHandlers/ofString)))
        (range 500)))
