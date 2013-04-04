(ns deployer.config
  "Deployment Configuration."
  (:refer-clojure :exclude [resolve])
  (:require
   [cheshire.core :as json]
   [clojure.tools.logging :refer [debugf]]
   [deployer.resolve :refer [resolve]]
   [pallet.actions :refer [remote-file]]
   [pallet.api :refer [plan-fn server-spec]]
   [pallet.crate :refer [assoc-settings defplan get-settings nodes-with-role
                         update-settings]]
   [pallet.node :refer [primary-ip]]))

;;; This is configured to point a the test-repo, but should be changed to point
;;; at a S3, Nexus or Archiva repository.
(def config
  "Cluster specific configuration"
  {:deploy-defaults
   {:repositories
    {"test-repo"
     {:url "file:../target/test-repo" :snapshots true}}
    :local-repo "file:../target/local-repo"}
   ;; For each configuration, specify the coords of the artifact to be
   ;; deployed. The key in the :deploy map should match the app-kw passed to
   ;; clj-app/server-spec.
   :local-dev
   {:deploy
    {:simple-app '[[simple-app "0.1.0-SNAPSHOT" :classifier "standalone"]]}}

   :dev
   {:deploy
    {:simple-app '[[simple-app "0.1.0-SNAPSHOT" :classifier "standalone"]]}}

   :staging
   {:deploy
    {:simple-app '[[simple-app "0.1.0-SNAPSHOT" :classifier "standalone"]]}}

   :production
   {:deploy
    {:simple-app '[[simple-app "0.1.0-SNAPSHOT" :classifier "standalone"]]}}})

(defplan settings
  [config-kw]
  (let [config-map (config config-kw)
        config (merge (:deploy-defaults config) config-map)]
    (when-not config-map
      (throw (ex-info (str "Couldn't find configuration details for " config-kw)
                      {:config-kw config-kw})))
    (debugf "deploy-config %s" config)
    (assoc-settings :deploy-config  config {})))

(defplan cluster-config-file
  []
  (let [settings (get-settings :deploy-config {})]
    (remote-file
     "config.json"
     :content (json/generate-string config))))

(defplan resolve-config
  "Takes the current config from settings and resolves the artifacts in the
  deploy map, associng the result into the :resolved key of the :deploy-config
  settings."  []
  (let [{:keys [deploy] :as settings} (get-settings :deploy-config {})
        coords (vec (mapcat second deploy))
        resolved (resolve settings coords)]
    (debugf "resolve-config coords : %s  resolved : %s" coords resolved)
    (update-settings :deploy-config assoc :resolved resolved)
    (debugf "resolve-config resolved : %s"
            (:resolved (get-settings :deploy-config {})))))

(defn resolved-artifacts
  "Looks up the resolved artifacts for the given app-kw."
  [app-kw]
  (let [{:keys [deploy resolved] :as settings} (get-settings :deploy-config)
        coords (get deploy app-kw)]
    (debugf "resolved-artifacts settings : %s" settings)
    (debugf "resolved-artifacts resolved : %s" resolved)
    (when-not coords
      (throw
       (ex-info (str "No deploy specified for " app-kw)
                {:app-kw app-kw})))
    (let [paths resolved]
      (when-not (every? identity paths)
        (throw
         (ex-info (str "Failed to resolve for " app-kw)
                  {:coords coords
                   :app-kw app-kw})))
      (debugf "resolved-artifacts paths : %s" paths)
      paths)))

(defn with-config [config-kw]
  (server-spec
   :phases {:settings (plan-fn (settings config-kw))
            :configure (plan-fn (cluster-config-file))
            :deploy (plan-fn (resolve-config))}))
