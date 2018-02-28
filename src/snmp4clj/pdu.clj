(ns snmp4clj.pdu
  (:import [org.snmp4j PDU PDUv1 ScopedPDU]
           [org.snmp4j.smi OID VariableBinding]))

(defn init-pdu
  [pdu command oids {:keys [max-repetitions] :as config}]
  (doto pdu
    (.setType command)
    (.setMaxRepetitions max-repetitions)
    (.addAll
     (into-array VariableBinding
                 (map #(VariableBinding. (OID. %)) oids)))))

(defmulti create-pdu (fn [command oids config] (:version config)))

(defmethod create-pdu :v1
  [command oids config]
  (init-pdu (PDUv1.) command oids config))

(defmethod create-pdu :default
  [command oids config]
  (init-pdu (PDU.) command oids config))

(defmethod create-pdu :v3
  [command oids {:keys [local-engine-id user-name] :as config}]

  (let [pdu (doto (ScopedPDU.)
              (.setContextEngineID local-engine-id)
              (.setContextName user-name))]
    (init-pdu pdu command oids config)
    (doto pdu (println "PDU"))))
