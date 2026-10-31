;; **
;; * Author: Filipe Andrade
;; **/
(import '[jakarta.servlet.http HttpServlet HttpServletRequest HttpServletResponse]
        '[org.eclipse.jetty.ee11.servlet ServletContextHandler ServletHolder]
        '[org.eclipse.jetty.server Server ServerConnector])

(let [servlet (proxy [HttpServlet] []
                (doGet [^HttpServletRequest req ^HttpServletResponse resp]
                  (let [conn (.getServletConnection req)]
                    (doto resp
                      (.setStatus 200)
                      (-> .getWriter (.println (str "ok, connection: " conn)))))))
      ctx (doto (ServletContextHandler.)
            (.addServlet (ServletHolder. servlet) "/*"))
      server (doto (Server.)
               (.setHandler ctx))]
  (.addConnector server (doto (ServerConnector. server) (.setPort 3000)))
  (.start server)
  (println "Server started"))


(import '[org.eclipse.jetty.server Server ServerConnector])

;; ee8 servlet (javax.servlet)
(let [javax-servlet (proxy [javax.servlet.http.HttpServlet] []
                      (doGet [^javax.servlet.http.HttpServletRequest req
                              ^javax.servlet.http.HttpServletResponse resp]
                        (doto resp
                          (.setStatus 200)
                          (-> .getWriter (.println "ok from javax.servlet (ee8)")))))
      ee8-ctx (doto (org.eclipse.jetty.ee8.servlet.ServletContextHandler.)
                (.addServlet (org.eclipse.jetty.ee8.servlet.ServletHolder.
                              javax-servlet) "/*"))
      server1 (doto (Server.)
                (.setHandler (.get ee8-ctx)))]
  (.addConnector server1 (doto (ServerConnector. server1) (.setPort 3000)))
  (.start server1)
  (println "ee8 server started on :3000"))

;; ee10 servlet (jakarta.servlet)
(let [jakarta-servlet (proxy [jakarta.servlet.http.HttpServlet] []
                        (doGet [^jakarta.servlet.http.HttpServletRequest req
                                ^jakarta.servlet.http.HttpServletResponse resp]
                          (let [conn (.getServletConnection req)]
                            (doto resp
                              (.setStatus 200)
                              (-> .getWriter
                                  (.println
                                   (str "ok from jakarta.servlet (ee10), connection: " conn)))))))
      ee10-ctx
      (doto (org.eclipse.jetty.ee10.servlet.ServletContextHandler.)
        (.addServlet
         (org.eclipse.jetty.ee10.servlet.ServletHolder. jakarta-servlet) "/*"))
      server2 (doto (Server.)
                (.setHandler ee10-ctx))]
  (.addConnector server2 (doto (ServerConnector. server2) (.setPort 3001)))
  (.start server2)
  (println "ee10 server started on :3001"))
