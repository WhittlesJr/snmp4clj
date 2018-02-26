(ns snmp4clj.target
  (:import [org.snmp4j CommunityTarget]
           [org.snmp4j.mp SnmpConstants]
           [org.snmp4j.smi GenericAddress OctetString]))

(defn- get-address
  [{:keys [transport address port] :as config}]

  (str transport ":" address "/" port))

(defmulti create-target
  (fn [version config]
    (if (contains? #{:v1 :v2c :v3} version)
      version
      :v2c)))

(defmethod create-target :default
  [version {:keys [community] :as config}]
  (doto (CommunityTarget.)
    (.setCommunity (OctetString. community))
    (.setVersion SnmpConstants/version2c)
    (.setAddress (GenericAddress/parse (get-address config)))))

(defmethod create-target :v3
  [version {:keys [community address] :as config}]
  (println "create-target: not implemented yet"))
