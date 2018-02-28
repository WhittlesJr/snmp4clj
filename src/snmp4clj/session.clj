(ns snmp4clj.session
  (:require [crypto.random :as cr-rand]
            [snmp4clj.constants :as const])
  (:import org.snmp4j.mp.MPv3
           [org.snmp4j.security SecurityModels SecurityProtocols USM UsmUser]
           org.snmp4j.smi.OctetString
           org.snmp4j.Snmp
           org.snmp4j.transport.DefaultUdpTransportMapping))

(defn get-random-engine-id
  []
  (-> (OctetString. (cr-rand/bytes 8))
      MPv3/createLocalEngineID
      OctetString.))

(defn get-local-engine-id
  [usm {:keys [local-engine-id] :as config}]
  (cond (some? local-engine-id) (OctetString. local-engine-id)
        (some? usm)             (.getLocalEngineID usm)
        :else                   (get-random-engine-id)))

(defn create-default-protocols
  []
  (-> (SecurityProtocols/getInstance)
      .addDefaultProtocols))

(defn create-usm
  [protocols config]
  (USM. protocols
        (get-local-engine-id nil config)
        0))

(defn create-security-models
  [model]

  (doto (SecurityModels/getInstance)
    (.addSecurityModel model)))

(defn create-usm-user
  [usm {:keys [user-name local-engine-id auth auth-pass priv priv-pass] :as config}]

  (if user-name
    (let [engine-id (get-local-engine-id usm config)
          user-name (OctetString. user-name)]
      (UsmUser. user-name
                (get const/auth-protocol-id-map auth)
                (if auth-pass (OctetString. auth-pass))
                (get const/privacy-protocol-id-map priv)
                (if priv-pass (OctetString. priv-pass))
                engine-id))))

(defn add-usm-user
  [process user]

  (if user
    (.addUser (.getUSM process) (.getSecurityName user)
              (.getLocalizationEngineID user)
              user)))

(defn create-snmp-process []
  (doto (Snmp. (DefaultUdpTransportMapping.))
    (.listen)))

(defn get-snmp-session
  [& [config]]

  (let [config    (merge const/default-config config)
        process   (create-snmp-process)
        protocols (create-default-protocols)
        usm       (create-usm protocols config)
        user      (create-usm-user usm config)
        _         (add-usm-user process user)]

    {:config          config
     :process         process
     :protocols       protocols
     :user            user
     :usm             usm
     :security-models (create-security-models usm)}))
