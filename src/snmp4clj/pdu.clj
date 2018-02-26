(ns snmp4clj.pdu
  (:import [org.snmp4j Snmp PDUv1 PDU ScopedPDU]
           [org.snmp4j.smi OID VariableBinding]))

(defn init-pdu [pdu type oids {:keys [max-repetitions] :as config}]
  (doto pdu
    (.setType type)
    (.setMaxRepetitions max-repetitions)
    (.addAll
      (into-array VariableBinding
        (map #(VariableBinding. (OID. %)) oids)))))

(defmulti create-pdu
  (fn [type oids {:keys [version] :as config}]
    (if (contains? #{:v1 :v2c :v3} version) version :v2c)))

(defmethod create-pdu :v1
  [type oids config]
  (init-pdu (PDUv1.) type oids config))

(defmethod create-pdu :default
  [type oids config]
  (init-pdu (PDU.) type oids config))

(defmethod create-pdu :v3
  [type oids config]
  (init-pdu (ScopedPDU.) type oids config))
