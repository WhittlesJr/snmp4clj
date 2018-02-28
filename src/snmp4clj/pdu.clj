(ns snmp4clj.pdu
  (:import [org.snmp4j PDU PDUv1 ScopedPDU]
           [org.snmp4j.smi OID VariableBinding])
  (:require [snmp4clj.session :as session]))

(defn init-pdu [pdu command oids {:keys [config] :as session}]
  (let [{:keys [max-repetitions]} config]
    (doto pdu
      (.setType command)
      (.setMaxRepetitions max-repetitions)
      (.addAll
       (into-array VariableBinding
                   (map #(VariableBinding. (OID. %)) oids))))))

(defmulti create-pdu
  (fn [command oids {:keys [config] :as session}]
    (let [{:keys [version]} config]
      (if (contains? #{:v1 :v2c :v3} version) version :v2c))))

(defmethod create-pdu :v1
  [command oids session]
  (init-pdu (PDUv1.) command oids session))

(defmethod create-pdu :default
  [command oids session]
  (init-pdu (PDU.) command oids session))

(defmethod create-pdu :v3
  [command oids {:keys [usm config] :as session}]

  (let [engine-id (session/get-local-engine-id usm config)
        pdu       (doto (ScopedPDU.)
                    (.setContextEngineID engine-id))]

    (init-pdu pdu command oids session)))
