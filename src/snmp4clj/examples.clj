(ns snmp4clj.examples
  (:require [snmp4clj.session :as session]
            [snmp4clj.core :as core]
            [clojure.pprint :as pprint])
  (:import [org.snmp4j.event ResponseListener])
  (:gen-class))

(defn -main []
  (session/with-snmp-session session
    (println "retrieve a name of the first network interface")
    (println (core/snmp-get-next session "1.3.6.1.2.1.31.1.1.1.1"))

    ;; (println "retrieve asynchronously")
    ;; (let [listener (proxy [ResponseListener] []
    ;;                  (onResponse [event]
    ;;                              (print (.getResponse event))
    ;;                              (println " (async)")))]
    ;;   (dotimes [_ 100]
    ;;     (core/snmp-get-next session
    ;;                    :async listener
    ;;                    "1.3.6.1.2.1.31.1.1.1.1")))

    ;; (println "retrieve a status map of all network interfaces")
    ;; (-?> (core/snmp-table-walk session
    ;;        "1.3.6.1.2.1.31.1.1.1.1"
    ;;        "1.3.6.1.2.1.31.1.1.1.2"
    ;;        "1.3.6.1.2.1.31.1.1.1.3")
    ;;      (pprint))

    (println "table-walk with for-table-* utilities")
    (println
     (pprint/pprint
      (core/snmp-table-walk session
                       "1.3.6.1.2.1.31.1.1.1.1"
                       "1.3.6.1.2.1.31.1.1.1.2"
                       "1.3.6.1.2.1.31.1.1.1.3")))

    (println
     (pprint/pprint
      (core/snmp-table-walk session
                       :address "udp:192.168.6.57/161"
                       "1.0.8802.1.1.2.1.4.1.1.7"
                       "1.0.8802.1.1.2.1.4.1.1.9")))))
    ;;   (println
    ;;    (apply +
    ;;           (for-table-with-row r table
    ;;             (count r))))
    ;;   (println
    ;;    (for-table-with-column c table
    ;;      (.getVariable c))))
    ;; ))
