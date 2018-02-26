(ns snmp4clj.pdu
  (:import [org.snmp4j Snmp PDUv1 PDU ScopedPDU]
           [org.snmp4j.smi OID VariableBinding]))

(defn init-pdu [pdu type oids]
  (doto pdu
    (.setType type)
    (.addAll
      (into-array VariableBinding
        (map #(VariableBinding. (OID. %)) oids)))))

(defmulti create-pdu
  (fn [version type oids]
    (if (contains? #{:v1 :v2c :v3} version) version :v2c)))

(defmethod create-pdu :v1
  [version type oids]
  (init-pdu (PDUv1.) type oids))

(defmethod create-pdu :default
  [version type oids]
  (init-pdu (PDU.) type oids))

(defmethod create-pdu :v3
  [version type oids]
  (init-pdu (ScopedPDU.) type oids))
