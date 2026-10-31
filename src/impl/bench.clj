;; **
;; * Author: Filipe Andrade
;; * Created At: 10/30/26
;; **/

(ns gzip-bench
  (:import
   [java.io ByteArrayInputStream
    ByteArrayOutputStream]
   [java.util.zip GZIPInputStream]))

(set! *warn-on-reflection* true)

(defn gzip
  [arr]
  (let [os (java.io.ByteArrayOutputStream.)]
    (with-open
      [gz (java.util.zip.GZIPOutputStream. os)]
      (.write gz arr))
    (.toByteArray os)))

(defn gunzipTransferTo
  [^bytes arr]
  (with-open
    [is (ByteArrayInputStream. arr)
     gz (GZIPInputStream. is 4096)
     os (ByteArrayOutputStream.
         (* 2 (alength arr)))]
    (.transferTo gz os)
    (.toByteArray os)))

(defn gunzipLoop
  [^bytes arr]
  (with-open [is (ByteArrayInputStream. arr)
              gz (GZIPInputStream. is 4096)
              os (ByteArrayOutputStream.
                  (* 2 (alength arr)))]
    (let [buf (byte-array 4096)]
      (loop [n (.read gz buf)]
        (when (pos? n)
          (.write os buf 0 n)
          (recur (.read gz buf)))))
    (.toByteArray os)))

(comment
  (in-ns 'gzip-bench)
  (def arr (gzip (byte-array 65536 (byte 0))))

  ;; warmup
  (dotimes [_ 1000] (gunzipTransferTo arr))
  (dotimes [_ 1000] (gunzipLoop arr))

  ;; bench
  (dotimes [_ 10]
    (time (dotimes [_ 1000]
            (gunzipTransferTo arr))))

  (dotimes [_ 10]
    (time (dotimes [_ 1000]
            (gunzipLoop arr))))

  (alength (gunzipLoop arr))
  )
