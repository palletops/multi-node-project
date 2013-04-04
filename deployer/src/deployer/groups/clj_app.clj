(ns deployer.groups.clj-app
  "Node definitions for a clojure app."
  (:require
   [clojure.tools.logging :refer [debugf]]
   [deployer.groups.common :refer [role-location-file]]
   [deployer.config :refer [resolved-artifacts]]
   [pallet.actions :refer [package package-manager package-source remote-file]]
   [pallet.api :refer [cluster-spec group-spec node-spec plan-fn] :as api]
   [pallet.crate :refer [assoc-settings defmethod-plan defplan get-settings]]
   [pallet.crate.automated-admin-user :refer [automated-admin-user]]
   [pallet.crate.java :as java]
   [pallet.crate.runit :as runit]
   [pallet.crate.service
    :refer [service-supervisor-config supervisor-config]
    :as service]))

(defn supervisor-config-map
  [{:keys [run-command service-name user] :as settings}]
  {:service-name service-name
   :run-file {:content (str "#!/bin/sh\nexec chpst -u " user " " run-command)}})

(defplan settings
  "Settings for an application."
  [{:keys [app-kw runit] :as settings} {:keys [instance-id] :as options}]
  {:pre [app-kw]}
  (let [settings (assoc settings :supervisor :runit)]
    (assoc-settings app-kw settings options)
    (service-supervisor-config :runit (supervisor-config-map settings) runit)))

(defplan deploy
  [app-kw]
  (doseq [artifact (resolved-artifacts app-kw)]
    (debugf "deploy %s : %s" app-kw (pr-str artifact))
    (remote-file
     (str (name app-kw) ".jar")
     :local-file artifact)))

(defplan service
  "Run an application under service management."
  [app-kw & {:keys [action if-flag if-stopped instance-id]
             :or {action :manage}
             :as options}]
  (let [{:keys [supervision-options] :as settings}
        (get-settings app-kw {:instance-id instance-id})]
    (service/service
     settings
     (merge supervision-options (dissoc options :instance-id)))))

(defn- kw-for [prefix app-kw]
  (keyword (str prefix "-" (name app-kw))))

(defn server-spec
  "Define a server spec for a clojure application.

  The server-spec has phases for start, stop and restart, as well as
  application specific start-<app>, stop-<app> and restart-<app>."
  [{:keys [app-kw] :as settings} & {:keys [instance-id] :as options}]
  (assert app-kw "Must specify an :app-kw setting as a keyword")
  (assert (keyword app-kw) "The :app-kw setting must be a keyword")
  (let [service-fn (fn [kw]
                     [[kw
                       (plan-fn (service app-kw :action kw))]
                      [(kw-for (name kw) app-kw)
                       (plan-fn (service app-kw :action kw))]])]
    (api/server-spec
     :extends [(runit/server-spec {}) (java/server-spec {})]
     :phases (merge
              {:settings (plan-fn (deployer.groups.clj-app/settings
                                   settings options))
               :configure (plan-fn
                            (role-location-file)
                            (service app-kw :action :enable))
               :deploy (plan-fn
                         (deploy app-kw))}
              (into {} (mapcat service-fn [:start :stop :restart])))
     :roles #{:clojure-app app-kw})))
