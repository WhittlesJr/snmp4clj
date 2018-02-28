(ns snmp4clj.constants
  (:require [crypto.random :as cr-rand])
  (:import [org.snmp4j.mp MPv3 SnmpConstants]
           [org.snmp4j.security
            AuthHMAC128SHA224 AuthHMAC192SHA256 AuthHMAC256SHA384
            AuthHMAC384SHA512 AuthMD5 AuthSHA Priv3DES
            PrivAES128 PrivAES192 PrivAES256 SecurityLevel SecurityModel]
           [org.snmp4j.smi GenericAddress OctetString ]))

(def default-config {:community        "public"
                     :transport        "udp"
                     :port             161
                     :address          "localhost"
                     :max-repetitions  10
                     :engine-boots     0
                     :version          :v2c
                     :async            nil
                     :engine-id   nil
                     :local-engine-id  nil
                     :auth             nil
                     :auth-pass        nil
                     :priv             nil
                     :priv-pass        nil
                     :max-rows-per-pdu 10
                     :max-cols-per-pdu 10
                     :lower-bound      0
                     :upper-bound      999999
                     :timeout          10
                     :retries          3
                     :max-pdu          65535})

(def version-map {:v1  SnmpConstants/version1
                  :v2c SnmpConstants/version2c
                  :v3  SnmpConstants/version3})

(def security-level-map {[false false] SecurityLevel/NOAUTH_NOPRIV
                         [true false]  SecurityLevel/AUTH_NOPRIV
                         [true true]   SecurityLevel/AUTH_PRIV})

(def auth-protocol-id-map {:hmac-128-sha-224 AuthHMAC128SHA224/ID
                           :hmac-192-sha-256 AuthHMAC192SHA256/ID
                           :hmac-256-sha-384 AuthHMAC256SHA384/ID
                           :hmac-384-sha-512 AuthHMAC384SHA512/ID
                           :md5              AuthMD5/ID
                           :sha              AuthSHA/ID})

(def privacy-protocol-id-map {:3des    Priv3DES/ID
                              :aes-128 PrivAES128/ID
                              :aes-192 PrivAES192/ID
                              :aes-256 PrivAES256/ID})

(def auth-protocols (-> auth-protocol-id-map keys set))
(def privacy-protocols (-> privacy-protocol-id-map keys set))

(defn get-bytes [string] (if (some? string) (byte-array (.getBytes string))))
(defn get-octet-string [string] (if (some? string) (OctetString. string)))

(defn sanitize-version
  [version]
  (if (contains? #{:v1 :v2c :v3} version)
    version
    :v2c))

(defn get-random-engine-id
  []
  (-> (OctetString. (cr-rand/bytes 8))
      MPv3/createLocalEngineID))

(defn get-auth-key
  [security-protocols {:keys [auth-id auth-pass engine-id] :as config}]
  (if (every? some? [auth-id auth-pass engine-id])
    (.passwordToKey security-protocols auth-id auth-pass engine-id)))

(defn get-priv-key
  [security-protocols {:keys [priv-id priv-pass engine-id] :as config}]
  (if (every? some? [priv-id priv-pass engine-id])
    (.passwordToKey security-protocols priv-id priv-pass engine-id)))

(defn get-full-address
  [{:keys [transport address port] :as config}]

  (-> (str transport ":" address "/" port)
      GenericAddress/parse))

(defn get-security-level
  [{:keys [auth priv] :as config}]

  (let [auth? (boolean (auth-protocols auth))
        priv? (boolean (privacy-protocols priv))]
    (get security-level-map [auth? priv?] SecurityLevel/NOAUTH_NOPRIV)))

(defn get-config
  [config-map security-protocols]
  (-> (merge default-config config-map)
      (assoc :security-model SecurityModel/SECURITY_MODEL_USM) ;;TODO: others?
      (as-> m (assoc m :engine-id-str (get-octet-string (:engine-id m))))
      (update :engine-id get-bytes)
      (update :local-engine-id #(or % (get-random-engine-id)))
      (update :local-engine-id get-octet-string)
      (update :user-name get-octet-string)
      (update :community get-octet-string)
      (update :version sanitize-version)
      (as-> m (assoc m
                     :security-level (get-security-level m)
                     :version-id (get version-map (:version m))
                     :full-address (get-full-address m)
                     :auth-id (get auth-protocol-id-map (:auth m))
                     :priv-id (get privacy-protocol-id-map (:priv m))))
      (as-> m (assoc m
                     :auth-key (get-auth-key security-protocols m)
                     :priv-key (get-priv-key security-protocols m)))))
