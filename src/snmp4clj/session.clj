(ns snmp4clj.session
  (:require [snmp4clj.constants :as const])
  (:import org.snmp4j.mp.MPv3
           [org.snmp4j.security SecurityModels SecurityProtocols USM UsmUser]
           org.snmp4j.smi.OctetString
           org.snmp4j.Snmp
           org.snmp4j.transport.DefaultUdpTransportMapping))

(defn create-snmp-process []
  (doto (Snmp. (DefaultUdpTransportMapping.))
    (.listen)))

(defn create-default-security-protocols
  []
  (-> (SecurityProtocols/getInstance)
      .addDefaultProtocols))

(defn add-usm-user
  [process {:keys [engine-id user-name auth-id auth-key priv-id priv-key]
            :as   config}]
  (if user-name
    (doto (.getUSM process)
      (.addLocalizedUser engine-id user-name auth-id auth-key priv-id priv-key))))

(defn- get-random-message-id
  [{:keys [engine-boots] :as config}]
  (MPv3/randomMsgID engine-boots))

(defn- init-v3-mpm
  [mpm {:keys [local-engine-id] :as config}]

  (doto mpm
    (.addEngineID (Address. full-address) engine-id-str)
    (.setLocalEngineID (.getValue local-engine-id))
    (.setCurrentMsgID (get-random-message-id config))))

(defn get-mpm
  [process {:keys [version version-id] :as config}]

  (let [mpm (.getMessageProcessingModel process version-id)]
    (cond (= version :v3) (init-v3-mpm mpm config)
          :else           mpm)))

(defn create-usm
  [security-protocols {:keys [local-engine-id] :as config}]
  (USM. security-protocols local-engine-id 0))

(defn create-security-models
  [model mpm]

  (let [security-models (doto (SecurityModels/getInstance)
                          (.addSecurityModel model))]
    (.setSecurityModels mpm security-models)
    security-models))

(defn get-snmp-session
  [& [config]]

  (let [process            (create-snmp-process)
        security-protocols (create-default-security-protocols)
        config             (const/get-config config security-protocols)
        mpm                (get-mpm process config)
        usm                (create-usm security-protocols config)
        security-models    (create-security-models usm mpm)
        _                  (add-usm-user process config)]

    {:config             config
     :process            process
     :security-protocols security-protocols
     :mpm                mpm
     :usm                usm
     :security-models    security-models}))
