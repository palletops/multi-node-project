(ns deployer.network
  (:require
   [clojure.tools.logging :refer [debugf]]
   [org.jclouds.ec2.ebs2 :as ebs]
   [org.jclouds.ec2.security-group2 :refer [groups sg-service]]
   [pallet.compute :refer [service-properties]]
   [pallet.crate :refer [compute-service group-name groups-with-role]]))

(defn authorize
  "Adds permissions to a security group."
  [compute group-name & {:keys [protocol from-group region]}]
  (debugf "authorize %s %s" group-name from-group)
  (let [group ((groups compute :region region) group-name)
        fgroup ((groups compute :region region) from-group)]
    (when-not fgroup
      (throw (ex-info
              (str "Can't find security group for name " group-name)
              {:group-name group-name})))
    (if group
      (.authorizeSecurityGroupIngressInRegion
       (sg-service compute)
       (ebs/get-region region)
       (.getName group)
       (org.jclouds.ec2.domain.UserIdGroupPair.
        (.getOwnerId fgroup)
        (.getName fgroup)))
      (throw (IllegalArgumentException.
              (str "Can't find security group for name " group-name))))))

(defn ec2-region [node]
  (.. node getLocation getParent getId))

(defn jclouds-group
  ([group-name]
     (str "jclouds#" (name group-name)))
  ([]
     (jclouds-group (group-name))))

;;; jclouds is limited to opening everything to another group
(defmulti open-to-roles
  "Open port for"
  (fn [roles] (:provider (service-properties (compute-service)))))

(defmethod open-to-roles :default [roles])

(defmethod open-to-roles :aws-ec2
  [roles]
  (require '[org.jclouds.ec2.security-group2])
  (let [groups (->> roles (mapcat #(groups-with-role %)) distinct)
        group (jclouds-group)]
    (debugf "open-to-roles group %s groups %s" group (mapv :group-name groups))
    (doseq [from-group (mapv :group-name groups)]
      (try
        (authorize
         (compute-service) group :from-group (jclouds-group from-group))
        (catch Exception e
          (debugf "While changing security group: %s" (.getMessage e)))))))
