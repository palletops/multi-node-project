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
