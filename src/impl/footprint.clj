;; **
;; * Author: Filipe Andrade
;; **/

(require
 '[clojure.test.check.generators :as tgen]
 '[clojure.core.async :as a])


(import '[java.nio ByteBuffer])


(def gen-kvs
  (tgen/hash-map
   :k (tgen/fmap str tgen/uuid)
   :v (tgen/hash-map
       :val (tgen/fmap #(ByteBuffer/wrap %)
                       (tgen/fmap byte-array
                                  (tgen/vector tgen/byte 2 25))))))

(defn display-mem-footprint
  [f]
  (System/gc)
  (println (format "available mem (MB)=> %.2f"
                       (/ (.freeMemory (Runtime/getRuntime)) 1024.0 1024.0)))
  (println "generating data...")
  (let [membf (/ (.freeMemory (Runtime/getRuntime)) 1024.0 1024.0)
        _     (f)
        memaf (/ (.freeMemory (Runtime/getRuntime)) 1024.0 1024.0)]
    (println "aprox mem footprint (MB)=> " (- membf memaf))))


(defn genvec
  []
  (tgen/generate (tgen/vector-distinct gen-kvs
                                       {:min-elements 2048
                                        :max-elements 2048})))

(defn genvec+reduce
  []
  (let [all (tgen/generate (tgen/vector-distinct gen-kvs
                                                 {:min-elements 2048
                                                  :max-elements 2048}))
        d (reduce (fn [a i] (conj a {:new i})) [] all)]
    (def all all)
    (def d d)
    [all d]))

(defn genvec-async
  []
  (let [all (tgen/generate (tgen/vector-distinct gen-kvs
                                                 {:min-elements 2048
                                                  :max-elements 2048}))
        c (a/chan)
        consumer (a/thread
                   (println "starting consumer thread...")
                   (let [complete (loop [items []]
                                    (let [o (a/<!! c)]
                                      (if o
                                        (recur (conj items o))
                                        items)))]
                     complete))
        producer (a/thread
                   (println "starting producer thread...")
                   (doseq [i all]
                     (a/>!! c i))
                   (a/close! c))]
    (let [doubled (reduce (fn [out in] (conj out in)) [] (a/<!! consumer))]
      (= doubled all))))

(defn genvec-async-reduce
  []
  (let [all (tgen/generate (tgen/vector-distinct gen-kvs
                                                 {:min-elements 2048
                                                  :max-elements 2048}))
        c (a/chan)
        consumer (a/thread
                   (println "starting consumer thread...")
                   (let [complete (loop [items []]
                                    (let [o (a/<!! c)]
                                      (if o
                                        (recur (conj items o))
                                        items)))]
                     complete))
        producer (a/thread
                   (println "starting producer thread...")
                   (doseq [i all]
                     (a/>!! c i))
                   (a/close! c))]
    (let [doubled (a/<!! (a/reduce (fn [out in] (conj out in)) [] consumer))]
      (= doubled all))))

;; dump heap

;; jmap -dump:format=b,file=heap.hprof 91130

;; capture jfr
;; jcmd $pid JFR.start duration=30m settings=profile filename=flight.jfr`

(display-mem-footprint genvec)
;; [1594.514s][info   ][gc] GC(7893) Pause Full (System.gc()) 83M->25M(1024M) 27.529ms
;; available mem (MB)=> 998.31
;; generating data...
;; aprox mem footprint (MB)=>  53.0

(display-mem-footprint genvec+reduce)
;; [1629.064s][info   ][gc] GC(7894) Pause Full (System.gc()) 79M->25M(1024M) 20.138ms
;; available mem (MB)=> 998.38
;; generating data...
;; aprox mem footprint (MB)=>  53.0

(display-mem-footprint genvec-async)
;; [1650.456s][info   ][gc] GC(7895) Pause Full (System.gc()) 79M->25M(1024M) 27.546ms
;; available mem (MB)=> 998.41
;; generating data...
;; starting consumer thread...
;; starting producer thread...
;; aprox mem footprint (MB)=>  56.0

(display-mem-footprint genvec-async-reduce)
;; [1681.270s][info   ][gc] GC(7896) Pause Full (System.gc()) 82M->25M(1024M) 12.710ms
;; available mem (MB)=> 998.40
;; generating data...
;; starting producer thread...
;; starting consumer thread...
;; aprox mem footprint (MB)=>  56.0

