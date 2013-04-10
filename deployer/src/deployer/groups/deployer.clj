(ns deployer.groups.deployer
  "Node defintions for deployer"
  (:require
   [deployer.groups.clj-app :as clj-app]
   [deployer.config :refer [with-config]]
   [deployer.network :refer [open-to-roles]]
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
   {:install (plan-fn
               (package "postgresql")
               (open-to-roles #{:app :redis}))
    :network (plan-fn
               (open-to-roles #{:app :redis}))}
   :roles #{:postgres}))
;; by default, postgres listens on 5432

(def
  ^{:doc "Define a server spec for redis"}
  redis
  (server-spec
   :phases
   {:install (plan-fn
               (package "redis-server")
               (open-to-roles #{:app}))
    :network (plan-fn
               (open-to-roles #{:app}))}
   :roles #{:redis}))
;; by default, redis listens on 6379

;;; ### Simple App
(def simple-app
  "A server-spec for the simple app."
  (clj-app/server-spec {:app-kw :simple-app
                        :service-name "simple-app"}))

;;; ### Groups
(defn redis-group [config-kw]
  (group-spec "redis"
    :extends [base-server (with-config config-kw) redis]))

(defn postgres-group [config-kw]
  (group-spec "pg"
    :extends [base-server (with-config config-kw) postgres]))

(defn db-group
  "A group with combined redis and postgres server."
  [config-kw]
  (group-spec "db"
    :extends [base-server (with-config config-kw) postgres redis]))

(defn simple-app-group [config-kw]
  (group-spec "simple"
    :extends [base-server (with-config config-kw) simple-app]))

;;; ### Composites

;;; These are some example clusters - they are tagged with roles, so that
;;; lein pallet up can be used with the --roles switch to select which
;;; to operate on
(def
  ^{:doc "Defines a group spec with postgres, riak and the application.  It uses
  a :local-dev config (see deployer.config/config)."}
  all-in-one
  (group-spec "deployer"
    :extends [base-server (with-config :local-dev) postgres redis simple-app]
    :roles #{:all-in-one}))

(def production-cluster
  ^{:doc "Defines a cluster with postgres, riak and the
  application on separate nodes.  It uses a :production config (see
  deployer.config/config)."}
  (cluster-spec
   "prod"
   :groups [(redis-group :production)
            (postgres-group :production)
            (simple-app-group :production)]
   :roles #{:production}))

(def dev-cluster
  ^{:doc "Defines a cluster with postgres, riak together on a single node and
   the application on a separate node.  It uses a :dev config (see
   deployer.config/config)."}
  (cluster-spec
   "dev"
   :groups [(db-group :dev) (simple-app-group :dev)]
   :roles #{:dev}))
