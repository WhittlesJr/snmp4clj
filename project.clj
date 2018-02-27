(defproject org.clojars.maoe/snmp4clj "0.0.2-SNAPSHOT"
  :description "SNMP API for Clojure"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.snmp4j/snmp4j "2.6.0"]
                 [org.flatland/ordered "1.5.6"]
                 [crypto-random "1.2.0"]]
  :dev-dependencies [[autodoc "0.7.0"]]
  :namespaces [snmp4clj]
  :repl-options {:init-ns user}
  :profiles {:dev {:source-paths ["dev/src"]}}
  :main snmp4clj.examples)
