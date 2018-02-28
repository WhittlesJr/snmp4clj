(ns snmp4clj.target
  (:require [snmp4clj.constants :as const])
  (:import [org.snmp4j CommunityTarget UserTarget]
           org.snmp4j.security.SecurityModel
           [org.snmp4j.smi GenericAddress OctetString]))

(defn get-bytes [string] (byte-array (.getBytes string)))
(defn- get-address
  [{:keys [transport address port] :as config}]

  (-> (str transport ":" address "/" port)
      GenericAddress/parse))

(defn- configure-target-base
  [target {:keys [version retries timeout max-pdu] :as config}]
  (doto target
    (.setVersion (get const/version-map version))
    (.setRetries retries)
    (.setTimeout timeout)
    (.setMaxSizeRequestPDU max-pdu)
    (.setAddress (get-address config))))

(defmulti create-target
  (fn [{:keys [version] :as config}]
    (if (contains? #{:v1 :v2c :v3} version)
      version
      :v2c)))

(defmethod create-target :default
  [{:keys [community retries timeout max-pdu] :as config}]
  (doto (configure-target-base (CommunityTarget.) config)
    (.setCommunity (OctetString. community))))

(defmethod create-target :v3
  [{:keys [community user-name auth-engine-id] :as config}]
  (doto (configure-target-base (UserTarget.) config)
    (.setAuthoritativeEngineID (get-bytes auth-engine-id))
    (.setSecurityName (OctetString. user-name))
    (.setSecurityModel SecurityModel/SECURITY_MODEL_USM)
    (.setSecurityLevel (const/get-security-level config))))
