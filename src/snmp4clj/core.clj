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

(defn- snmp-get-request
  ([command oids config]
   (session/with-snmp-session s
     (snmp-get-request command s oids config)))

  ([command session oids config]
   (let [{:keys [async] :as config} (merge const/default-config config)

         oids    (get-oids oids)
         pdu     (pdu/create-pdu command oids config)
         target  (target/create-target config)]
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

(defn- snmp-table->clojure-maps
  [table oids]
  (->> table
       (map (comp seq (memfn getColumns)))
       (map snmp->clojure)
       (apply merge)))

(defn snmp-table-walk
  ([oids config]
   (session/with-snmp-session s
     (snmp-table-walk s oids config)))

  ([session oids config]
   (let [{:keys [async max-rows-per-pdu max-cols-per-pdu lower-bound upper-bound]
          :as   config} (merge const/default-config config)

         oids   (get-oids oids)
         target (target/create-target config)
         table  (doto (TableUtils. session (DefaultPDUFactory.))
                  (.setMaxNumRowsPerPDU max-rows-per-pdu)
                  (.setMaxNumColumnsPerPDU max-cols-per-pdu))]
     (if async
       (.getTable table target async nil (OID. (str lower-bound)) (OID. (str upper-bound)))
       (let [tbl (.getTable table target
                            (into-array OID (map #(OID. %) oids))
                            (OID. (str lower-bound))
                            (OID. (str upper-bound)))]
         (snmp-table->clojure-maps tbl oids))))))
