(defproject simple-app "0.1.0-SNAPSHOT"
  :description "FIXME: An app to be deployed"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.0"]]

  ;; We add a plugin that allows us to deploy an uberjar.
  ;; There is a pull request to the canonical plugin repo for lein-package
  ;; with the updates that are in the org.clojars.hugoduncan version.
  :plugins [[org.clojars.hugoduncan/lein-package "0.1.1"]]
  :hooks [leiningen.package.hooks.deploy   ; deploy configured packages
          leiningen.package.hooks.install] ; install configured packages

  :package {:artifacts
            [{:build "uberjar"        ; lein command used to build the artifact.
              :extension "jar"        ; the extension/suffix built
              :classifier "standalone"  ; the classifier built
              :autobuild true}]}        ; flag to build automatically

  ;; We'll aot to give better startup times
  :main simple-app.core
  :aot [simple-app.core])
