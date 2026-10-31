;; **
;; * Author: Filipe Andrade
;; **/

(ns http.socks
  (:require [http-thread-pool.core :as core])
  (:import
   [java.io BufferedReader InputStream InputStreamReader]
   [java.net InetSocketAddress Proxy Proxy$Type Socket]
   [java.util.concurrent Future TimeUnit]))

(defn build-socks-proxy
  [{:keys [proxy-host proxy-port]
    :or   {proxy-host "localhost"
           proxy-port 1080}}]
  (Proxy. Proxy$Type/SOCKS
          (InetSocketAddress. ^String proxy-host (int proxy-port))))

(defn send-request
  [{:keys [url timeout-ms]
    :or   {url "http://httpbin.org/get"
           timeout-ms 5000}}
   ^Proxy proxy]
  (let [uri    (java.net.URI. url)
        host   (.getHost uri)
        port   (let [p (.getPort uri)] (if (neg? p) 80 p))
        path   (let [p (.getPath uri)] (if (empty? p) "/" p))
        sock   (doto (Socket. proxy)
                 (.setSoTimeout (int timeout-ms))
                 (.connect (InetSocketAddress. ^String host (int port))
                           (int timeout-ms)))
        out    (.getOutputStream sock)
        in     (BufferedReader. (InputStreamReader. (.getInputStream sock)))]
    (try
      (.write out
              (.getBytes (str "GET " path " HTTP/1.1\r\n"
                              "Host: " host "\r\n"
                              "Connection: close\r\n"
                              "\r\n")))
      (.flush out)
      (let [status-line (.readLine in)
            headers     (loop [hdrs {}]
                          (let [line (.readLine in)]
                            (if (or (nil? line) (empty? line))
                              hdrs
                              (let [[k v] (.split line ": " 2)]
                                (recur (assoc hdrs (.toLowerCase k) v))))))
            content-len (some-> (get headers "content-length") Integer/parseInt)
            body        (if content-len
                          (let [buf (char-array content-len)]
                            (.read in buf 0 content-len)
                            (String. buf))
                          (slurp in))]
        {:status  (Integer/parseInt (second (.split status-line " " 3)))
         :headers headers
         :body    body})
      (finally
        (.close sock)))))

(defn exercise-requests
  [{:keys [url n-requests timeout-ms]
    :or   {url "http://httpbin.org/get"
           n-requests 100
           timeout-ms 5000}}
   ^Proxy proxy]
  (let [pool    (core/http-cached-thread-pool)
        start   (System/nanoTime)
        futures (mapv
                 (fn [_]
                   (let [t0 (System/nanoTime)]
                     {:future
                      (.submit
                       pool ^Callable
                       (fn []
                         (send-request {:url url :timeout-ms timeout-ms} proxy)))
                      :start  t0}))
                 (range n-requests))
        results (mapv
                 (fn [{:keys [^Future future start]}]
                   (try
                     (let [resp (.get future timeout-ms TimeUnit/MILLISECONDS)]
                       {:status  (:status resp)
                        :time-ms (/ (double (- (System/nanoTime) start)) 1e6)})
                     (catch Exception e
                       {:error   (.getMessage e)
                        :time-ms (/ (double (- (System/nanoTime) start)) 1e6)})))
                      futures)
        total-ms (/ (double (- (System/nanoTime) start)) 1e6)
        times    (map :time-ms results)
        sorted   (sort times)]
    (.shutdown pool)
    {:total-ms   total-ms
     :n-requests n-requests
     :successes  (count (filter :status results))
     :errors     (count (filter :error results))
     :mean-ms    (/ (reduce + times) (count times))
     :p50-ms     (nth sorted (int (* 0.5 (count sorted))))
     :p99-ms     (nth sorted (int (* 0.99 (count sorted))))
     :min-ms     (first sorted)
     :max-ms     (last sorted)}))

(comment
  (require '[http-thread-pool.socks :as socks] :reload)
  (require '[http-thread-pool.socks-proxy :as proxy] :reload)

  ;; Start local SOCKS5 proxy
  (def p (proxy/start-proxy 1080))

  ;; Single request through proxy
  (def prx (socks/build-socks-proxy {:proxy-port 1080}))
  (socks/send-request {:url "http://httpbin.org/get"} prx)

  ;; Benchmark concurrent requests through proxy
  (socks/exercise-requests
    {:url "http://httpbin.org/get" :n-requests 20 :timeout-ms 10000}
    prx)

  ;; Stop proxy
  ((:stop! p)))
