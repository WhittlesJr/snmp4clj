(ns snmp4clj.constants
  (:import (org.snmp4j.mp SnmpConstants)
           (org.snmp4j.security SecurityLevel
                                Priv3DES PrivDES PrivAES128
                                PrivAES192 PrivAES256 AuthMD5 AuthSHA
                                AuthHMAC128SHA224 AuthHMAC192SHA256
                                AuthHMAC256SHA384 AuthHMAC384SHA512)))

(def default-config {:community        "public"
                     :transport        "udp"
                     :port             161
                     :address          "localhost"
                     :max-repetitions  10
                     :version          :v2c
                     :async            nil
                     :auth-engine-id   nil
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

(defn get-security-level
  [{:keys [auth priv] :as config}]

  (let [auth? (boolean (auth-protocols auth))
        priv? (boolean (privacy-protocols priv))]
    (get security-level-map [auth? priv?] SecurityLevel/NOAUTH_NOPRIV)))
