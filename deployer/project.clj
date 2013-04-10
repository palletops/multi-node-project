(def pallet-version "0.8.0-beta.7")
(def jclouds-version "1.5.5")

(defproject deployer "0.1.0-SNAPSHOT"
  :description "FIXME Pallet project for deployer"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [cheshire "5.0.2"]
                 [com.cemerick/pomegranate "0.2.0"]
                 [com.palletops/pallet ~pallet-version]
                 [com.palletops/java-crate "0.8.0-beta.2"]
                 [com.palletops/runit-crate "0.8.0-alpha.1"]
                 [org.cloudhoist/pallet-jclouds "1.5.2"]
                 [org.jclouds.provider/aws-ec2 ~jclouds-version]
                 [org.jclouds.provider/aws-s3 ~jclouds-version]
                 [org.jclouds.driver/jclouds-sshj ~jclouds-version]
                 [org.jclouds.driver/jclouds-slf4j ~jclouds-version
                  ;; the declared version is old and can overrule the
                  ;; resolved version
                  :exclusions [org.slf4j/slf4j-api]]
                 [ch.qos.logback/logback-classic "1.0.9"]
                 [org.slf4j/jcl-over-slf4j "1.7.3"]]
  :exclusions [commons-logging]
  :profiles {:dev
             {:dependencies [[com.palletops/pallet ~pallet-version
                              :classifier "tests"]]
              :plugins [[com.palletops/pallet-lein "0.6.0-beta.8"]]}})
