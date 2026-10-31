;; **
;; * Author: Filipe Andrade
;; **/

;; test larger gzip bytes
(defn gzip-bytes
  [arr]
  (let [os (java.io.ByteArrayOutputStream.)]
    (with-open [gz (java.util.zip.GZIPOutputStream. os)]
      (.write gz arr))
    (.toByteArray os)))
(def arr1 (byte-array 65536 (byte 0)))
(def arr2 (gzip-bytes arr1 ))
(alength arr1)
;; 65536
(alength arr2)
;; 96

(alength (client/decompress-bytes  arr2))
;; 65536
