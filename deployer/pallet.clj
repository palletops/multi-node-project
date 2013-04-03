;;; Pallet Project configuration for the deployer
(require '[deployer.groups.deployer :as deployer])

(defproject deployer
  :provider {:vmfest {:node-spec
                      {:image {:os-family :ubuntu
                               :os-version-matches "12.04"
                               :os-64-bit true}}
                      :group-suffix "u1204"
                      :selectors #{:default :all
                                   :ubuntu :ubuntu-12 :ubuntu-12-04}}
             :aws-ec2 {:node-spec
                       {:image {:os-family :ubuntu
                                :os-version-matches "12.04"
                                :os-64-bit true
                                :image-id "us-east-1/ami-e2861d8b"}}
                       :group-suffix "u1204"
                       :selectors #{:default :all
                                    :ubuntu :ubuntu-12 :ubuntu-12-04}}}

  :groups [deployer/all-in-one])
