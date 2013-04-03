(ns deployer.groups.deployer
  "Node defintions for deployer"
  (:require
   [deployer.groups.clj-app :as clj-app]
   [pallet.actions :refer [package package-manager package-source]]
   [pallet.api :refer [cluster-spec group-spec server-spec node-spec plan-fn]]
   [pallet.crate.automated-admin-user :refer [automated-admin-user]]
   [pallet.crate.service
    :refer [supervisor-config supervisor-config-map] :as service]))

(def vmfest-node-spec
  (node-spec
   :image {:os-family :ubuntu :os-version-matches "12.04" :os-64-bit true}))

(def aws-ec2-node-spec
  (node-spec
   :image {:os-family :ubuntu :os-version-matches "12.04" :os-64-bit true
           :image-id "us-east-1/ami-e2861d8b"}))

(def
  ^{:doc "Defines the type of node deployer will run on"}
  base-server
  (server-spec
   :phases
   {:bootstrap (plan-fn
                 (package-manager :update)
                 (automated-admin-user))}))

;;; ### Databases
(def
  ^{:doc "Define a server spec for postgresql"}
  postgres
  (server-spec
   :phases
   {:install (plan-fn (package "postgresql"))}
   :roles #{:postgres}))

(def
  ^{:doc "Define a server spec for redis"}
  redis
  (server-spec
   :phases
   {:install (plan-fn
               (package "redis-server"))}
   :roles #{:redis}))

;;; ### Simple App
(def simple-app
  "A server-spec for the simple app."
  (clj-app/server-spec {:app-kw :simple-app
                        :service-name "simple-app"
                        :run-command "java -jar simple-app.jar"
                        :user "appuser"}))

;;; ### Groups
(def
  ^{:doc "Defines a group spec with postgres, riak and the application."}
  all-in-one
  (group-spec
   "deployer"
   :extends [base-server postgres redis simple-app]
   :roles #{:all-in-one}))
