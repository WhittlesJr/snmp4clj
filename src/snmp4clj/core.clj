(ns snmp4clj.core
  (:require [flatland.ordered.map :refer [ordered-map]]
            [snmp4clj.pdu :as pdu]
            [snmp4clj.target :as target]
            [snmp4clj.session :as session]
            [snmp4clj.constants :as const])
  (:import [org.snmp4j Snmp PDU]
           [org.snmp4j.smi OID]
           [org.snmp4j.event ResponseListener]
           [org.snmp4j.util TableUtils TreeUtils DefaultPDUFactory])
  (:gen-class))

(defn get-oids [oids] (if (string? oids) [oids] oids))

(defn snmp->clojure
  [var-binds]

  (reduce (fn [m var-bind]
            (if (some? var-bind)
              (assoc m
                     (-> var-bind .getOid .toDottedString)
                     (-> var-bind .toValueString))
              m))
          (ordered-map)
          var-binds))

(defn- snmp-session-get-request
  [command {:keys [config process] :as session} oids]

  (let [{:keys [async]} config

        oids   (get-oids oids)
        pdu    (pdu/create-pdu command oids config)
        target (target/create-target config)]
    (if async
      (.send process pdu target nil async)
      (some-> (.send process pdu target)
              (.getResponse)
              (.getVariableBindings)
              (seq)
              snmp->clojure))))

(defn- snmp-get-request
  [command oids config]

  (snmp-session-get-request command (session/get-snmp-session config) oids))

(def snmp-get (partial snmp-get-request PDU/GET))
(def snmp-get-next (partial snmp-get-request PDU/GETNEXT))
(def snmp-get-bulk (partial snmp-get-request PDU/GETBULK))

(def snmp-session-get (partial snmp-session-get-request PDU/GET))
(def snmp-session-get-next (partial snmp-session-get-request PDU/GETNEXT))
(def snmp-session-get-bulk (partial snmp-session-get-request PDU/GETBULK))

(defn- snmp-table->clojure-maps
  [table oids]
  (->> table
       (map (comp seq (memfn getColumns)))
       (map snmp->clojure)
       (apply merge)))

(defn snmp-session-table-walk
  [{:keys [process config] :as session} oids]
  (let [{:keys [async max-rows-per-pdu max-cols-per-pdu
                lower-bound upper-bound]} config

        oids   (get-oids oids)
        target (target/create-target config)
        table  (doto (TableUtils. process (DefaultPDUFactory.))
                 (.setMaxNumRowsPerPDU max-rows-per-pdu)
                 (.setMaxNumColumnsPerPDU max-cols-per-pdu))]
    (if async
      (.getTable table target async nil (OID. (str lower-bound)) (OID. (str upper-bound)))
      (let [tbl (.getTable table target
                           (into-array OID (map #(OID. %) oids))
                           (OID. (str lower-bound))
                           (OID. (str upper-bound)))]
        (snmp-table->clojure-maps tbl oids)))))

(defn snmp-table-walk
  [oids config]

  (snmp-session-table-walk (session/get-snmp-session config) oids))
