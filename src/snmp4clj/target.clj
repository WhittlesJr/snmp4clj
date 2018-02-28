(ns snmp4clj.target
  (:import [org.snmp4j CommunityTarget UserTarget]))

(defn- configure-target-base
  [target {:keys [version-id full-address retries timeout max-pdu] :as config}]
  (doto target
    (.setVersion version-id)
    (.setRetries retries)
    (.setTimeout timeout)
    (.setMaxSizeRequestPDU max-pdu)
    (.setAddress full-address)))

(defmulti create-target (fn [config] (:version config)))

(defmethod create-target :default
  [{:keys [community retries timeout max-pdu] :as config}]
  (doto (configure-target-base (CommunityTarget.) config)
    (.setCommunity community)))

(defmethod create-target :v3
  [{:keys [community security-level security-model user-name engine-id] :as config}]
  (println "NAME" user-name)
  (doto (configure-target-base (UserTarget.) config)
    (.setAuthoritativeEngineID engine-id)
    (.setSecurityName user-name)
    (.setSecurityModel security-model)
    (.setSecurityLevel security-level)
    (println "TARGET")))
