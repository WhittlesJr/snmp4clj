(ns snmp4clj.core
  (:require [snmp4clj.pdu :as pdu]
            [snmp4clj.target :as target]
            [snmp4clj.session :as session])
  (:import [org.snmp4j Snmp PDU]
           [org.snmp4j.smi OID]
           [org.snmp4j.event ResponseListener]
           [org.snmp4j.util TableUtils TreeUtils DefaultPDUFactory])
  (:gen-class))

(def default-config {:community        "public"
                     :transport        "udp"
                     :port             161
                     :address          "localhost"
                     :max-repetitions  10
                     :version          :v2c
                     :async            nil
                     :max-rows-per-pdu 10
                     :max-cols-per-pdu 10
                     :lower-bound      nil
                     :upper-bound      nil
                     :timeout          10
                     :retries          3
                     :max-pdu          65535})

(defn snmp->clojure
  [var-binds]

  (reduce (fn [m var-bind]
            (assoc m
                   (-> var-bind .getOid .toDottedString)
                   (-> var-bind .toValueString)))
          {}
          var-binds))

(defn- snmp-get-request
  ([command oids config]
   (session/with-snmp-session s
     (snmp-get-request command s oids config)))

  ([command session oids config]
  (let [{:keys [version community async] :as config} (merge default-config config)

        oids    (if (string? oids) [oids] oids)
        pdu     (pdu/create-pdu command oids config)
        target  (target/create-target version config)]
    (if async
      (.send session pdu target nil async)
      (some-> (.send session pdu target)
              (.getResponse)
              (.getVariableBindings)
              (seq)
              snmp->clojure)))))

(def snmp-get (partial snmp-get-request PDU/GET))
(def snmp-get-next (partial snmp-get-request PDU/GETNEXT))
(def snmp-get-bulk (partial snmp-get-request PDU/GETBULK))

(defn snmp-table-walk
  [session oids & [config]]
  (let [{:keys [version community async max-rows-per-pdu max-cols-per-pdu
                lower-bound upper-bound] :as config} (merge default-config config)

        target  (target/create-target version config)
        table   (doto (TableUtils. session (DefaultPDUFactory.))
                  (.setMaxNumRowsPerPDU max-rows-per-pdu)
                  (.setMaxNumColumnsPerPDU max-cols-per-pdu))]
    (if async
      (.getTable table target async nil (OID. (str lower-bound)) (OID. (str upper-bound)))
      ;; FIXME: ugly
      (let [tbl (.getTable table target
                           (into-array OID (map #(OID. %) oids))
                           (OID. (str lower-bound))
                           (OID. (str upper-bound)))]
        (let [ret (map (comp seq (memfn getColumns)) tbl)]
          (if (first ret)
            ret
            '()))))))
