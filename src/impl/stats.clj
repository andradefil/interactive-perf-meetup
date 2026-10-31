;; extract metrics from the JXM handlers

(import [org.eclipse.jetty.server.handler StatisticsHandler])
  (def s (:server j))
  (when-let [stats (.getDescendant s StatisticsHandler)]
    (.getRequestTotal stats))
