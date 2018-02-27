(ns snmp4clj.pdu
  (:require [snmp4clj.target :as target]
            [snmp4clj.constants :as const]
            [crypto.random :as cr-rand])
  (:import (org.snmp4j.smi OctetString
                           OID
                           VariableBinding)
           (org.snmp4j.security SecurityModels
                                SecurityProtocols
                                USM UsmUser
                                Priv3DES PrivDES PrivAES128
                                PrivAES192 PrivAES256 AuthMD5 AuthSHA
                                AuthHMAC128SHA224 AuthHMAC192SHA256
                                AuthHMAC256SHA384 AuthHMAC384SHA512)
           (org.snmp4j Snmp PDUv1 PDU
                       ScopedPDU)
           (org.snmp4j.mp MPv3)))

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

(defn get-random-engine-id
  []
  (-> (OctetString. (cr-rand/bytes 8))
      MPv3/createLocalEngineID
      OctetString.))

(defn- get-local-engine-id
  [usm {:keys [local-engine-id] :as config}]

  (cond (some? local-engine-id) (OctetString. local-engine-id)
        (some? usm)             (.getLocalEngineID usm)
        :else                   (get-random-engine-id)))

(defn- add-usm-user
  [usm {:keys [user-name local-engine-id auth auth-pass priv priv-pass] :as config}]
  (let [usm-user (UsmUser. (OctetString. user-name)
                           (get const/auth-protocol-id-map auth)
                           (if auth-pass (OctetString. auth-pass))
                           (get const/privacy-protocol-id-map priv)
                           (if priv-pass (OctetString. priv-pass))
                           (get-local-engine-id usm config))]
    (doto usm .addUser usm-user)))

(defn- create-default-protocols
  []
  (-> (SecurityProtocols/getInstance)
      .addDefaultProtocols))

(defn- create-usm-with-user
  [config]
  (-> (USM. (create-default-protocols)
            (get-local-engine-id nil config)
            0)
      (add-usm-user config)))

(defmethod create-pdu :v3
  [type oids {:keys [user-name] :as config}]

  (let [usm             (create-usm-with-user config)
        security-models (doto (SecurityModels/getInstance)
                          (.addSecurityModel usm))]

    (init-pdu (ScopedPDU.) type oids config)))
