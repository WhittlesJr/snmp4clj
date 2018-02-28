(ns snmp4clj.session
  (:require [crypto.random :as cr-rand]
            [snmp4clj.constants :as const])
  (:import org.snmp4j.mp.MPv3
           [org.snmp4j.security SecurityModels SecurityProtocols USM UsmUser]
           org.snmp4j.smi.OctetString
           org.snmp4j.Snmp
           org.snmp4j.transport.DefaultUdpTransportMapping))

(defn create-default-protocols
  []
  (-> (SecurityProtocols/getInstance)
      .addDefaultProtocols))

(defn- local-engine-id-addr
  [obj]
  (OctetString. (.getLocalEngineID obj)))

(defn get-local-engine-id
  [{:keys [mpm] :as session}]

  (local-engine-id-addr mpm))

(defn create-usm
  [protocols config mpm]
  (USM. protocols (local-engine-id-addr mpm) 0))

(defn create-security-models
  [model]

  (doto (SecurityModels/getInstance)
    (.addSecurityModel model)))

(defn create-usm-user
  [mpm {:keys [user-name auth auth-pass priv priv-pass] :as config}]

  (if user-name
    (let [engine-id (local-engine-id-addr mpm)
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

(defn get-random-engine-id
  []
  (-> (OctetString. (cr-rand/bytes 8))
      MPv3/createLocalEngineID
      OctetString.))

(defn- gen-local-engine-id
  [{:keys [local-engine-id] :as config}]
  (if (some? local-engine-id)
    (OctetString. local-engine-id)
    (get-random-engine-id)))

(defn- init-v3-mpmessing
  [mpm config]

  (doto mpm
    (.setLocalEngineID (.getValue (gen-local-engine-id config)))))

(defn get-mpm
  [process {:keys [version] :as config}]

  (let [version-id (get const/version-map version)
        mpm        (-> process .getMessageDispatcher (.getMessageProcessingModel version-id))]
    (cond (= version :v3) (init-v3-mpmessing mpm config)
          :else           mpm)))

(defn get-snmp-session
  [& [config]]

  (let [config    (merge const/default-config config)
        process   (create-snmp-process)
        mpm       (get-mpm process config)
        protocols (create-default-protocols)
        usm       (create-usm protocols config mpm)
        user      (create-usm-user usm config)
        _         (add-usm-user process user)]

    {:config          config
     :process         process
     :protocols       protocols
     :user            user
     :usm             usm
     :mpm             mpm
     :security-models (create-security-models usm)}))
