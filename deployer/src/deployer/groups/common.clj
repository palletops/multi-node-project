(ns deployer.groups.common
  "Functions used across apps of different types."
  (:require
   [cheshire.core :as json]
   [pallet.actions :refer [remote-file]]
   [pallet.api :refer [plan-fn server-spec]]
   [pallet.crate :refer [defplan nodes-with-role]]
   [pallet.node :refer [primary-ip]]))

(defn role-locations
  "Return a map with role as key, and a sequence of IP addresses as values."
  ([roles]
     (into {}
           (map
            (fn [role]
              [role (map (comp primary-ip :node) (nodes-with-role role))])
            roles)))
  ([]
     (role-locations [:redis :postgres])))

(defplan role-location-file
  []
  (remote-file
   "role-locations.json"
   :content (json/generate-string (role-locations))))


;;; Probably not the best location for this, and it possibly needs some
;;; mechanism for overriding from the command line.
(def config
  "Cluster specific configuration"
  {:local-dev {}
   :dev {}
   :staging {}
   :production {}})

(defplan cluster-config-file
  [config-kw]
  (let [config (config config-kw)]
    (when-not config
      (throw (ex-info (str "Couldn't find configuration details for " config-kw)
                      {:config-kw config-kw})))
    (remote-file
     "config.json"
     :content (json/generate-string config))))

(defn with-config [config-kw]
  (server-spec
   :phases {:configure (plan-fn (cluster-config-file config-kw))}))
