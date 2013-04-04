(ns deployer.resolve
  "Resolve maven dependencies with aether for deployment"
  (:refer-clojure :exclude [resolve])
  (:require
   [clojure.java.io :as io]
   [clojure.tools.logging :refer [debugf]]
   [pallet.utils :refer [apply-map]]
   [cemerick.pomegranate.aether :as aether :refer :all])
  (:import
   [org.sonatype.aether.util.artifact ArtifactProperties DefaultArtifact]
   org.sonatype.aether.resolution.ArtifactRequest))

(defn artifact
  [[group-artifact version & {:keys [scope optional exclusions]
                              :as opts
                              :or {scope "compile"
                                   optional false}}
    :as dep-spec]]
  (DefaultArtifact. (#'aether/coordinate-string dep-spec)))


(defn resolve-artifacts*
  "Resolves artifacts for the coordinates kwarg, using repositories from the
   `:repositories` kwarg.
   If you don't want to mess with the Aether
   implmeentation classes, then use `resolve-artifacts` instead.

    :coordinates - [[group/name \"version\" & settings] ..]
      settings:
      :extension  - the maven extension (type) to require
      :classifier - the maven classifier to require
      :scope      - the maven scope for the dependency (default \"compile\")
      :optional   - is the dependency optional? (default \"false\")
      :exclusions - which sub-dependencies to skip : [group/name & settings]
        settings:
        :classifier (default \"*\")
        :extension  (default \"*\")

    :repositories - {name url ..} | {name settings ..}
      (defaults to {\"central\" \"http://repo1.maven.org/maven2/\"}
      settings:
      :url - URL of the repository
      :snapshots - use snapshots versions? (default true)
      :releases - use release versions? (default true)
      :username - username to log in with
      :password - password to log in with
      :passphrase - passphrase to log in wth
      :private-key-file - private key file to log in with
      :update - :daily (default) | :always | :never
      :checksum - :fail (default) | :ignore | :warn

    :local-repo - path to the local repository (defaults to ~/.m2/repository)
    :offline? - if true, no remote repositories will be contacted
    :transfer-listener - the transfer listener that will be notifed of dependency
      resolution and deployment events.
      Can be:
        - nil (the default), i.e. no notification of events
        - :stdout, corresponding to a default listener implementation that writes
            notifications and progress indicators to stdout, suitable for an
            interactive console program
        - a function of one argument, which will be called with a map derived from
            each event.
        - an instance of org.sonatype.aether.transfer.TransferListener

    :proxy - proxy configuration, can be nil, the host scheme and type must match
      :host - proxy hostname
      :type - http  (default) | http | https
      :port - proxy port
      :non-proxy-hosts - The list of hosts to exclude from proxying, may be null
      :username - username to log in with, may be null
      :password - password to log in with, may be null
      :passphrase - passphrase to log in wth, may be null
      :private-key-file - private key file to log in with, may be null

    :mirrors - {matches settings ..}
      matches - a string or regex that will be used to match the mirror to
                candidate repositories. Attempts will be made to match the
                string/regex to repository names and URLs, with exact string
                matches preferred. Wildcard mirrors can be specified with
                a match-all regex such as #\".+\".  Excluding a repository
                from mirroring can be done by mapping a string or regex matching
                the repository in question to nil.
      settings include these keys, and all those supported by :repositories:
      :name         - name/id of the mirror
      :repo-manager - whether the mirror is a repository manager"

  [& {:keys [repositories coordinates files retrieve local-repo
             transfer-listener offline? proxy mirrors repository-session-fn]
      :or {retrieve true}}]
  (debugf "resolve-artifacts* %s" (pr-str coordinates))
  (let [repositories (or repositories maven-central)
        system (#'aether/repository-system)
        mirror-selector-fn (memoize (partial #'aether/mirror-selector-fn mirrors))
        mirror-selector (#'aether/mirror-selector mirror-selector-fn proxy)
        session ((or repository-session-fn
                     repository-session)
                 {:repository-system system
                  :local-repo local-repo
                  :offline? offline?
                  :transfer-listener transfer-listener
                  :mirror-selector mirror-selector})
        deps (->> coordinates
                  (map #(if-let [local-file (get files %)]
                          (.setArtifact
                           (artifact %)
                           (-> (artifact %)
                               .getArtifact
                               (.setProperties
                                {ArtifactProperties/LOCAL_PATH
                                 (.getPath (io/file local-file))})))
                          (artifact %)))
                  vec)
        repositories (vec (map #(let [repo (#'aether/make-repository % proxy)]
                                  (-> session
                                      (.getMirrorSelector)
                                      (.getMirror repo)
                                      (or repo)))
                               repositories))]
    (debugf "deps %s" (pr-str deps))

    (doall
     (for [dep deps]
       (.resolveArtifact
        system session (ArtifactRequest. dep repositories nil))))))

(defn resolve-artifacts
  "Same as `resolve-artifacts*`, but returns a sequence of dependencies; each
   dependency's metadata contains the source Aether Dependency object, and
   the dependency's :file on disk. "
  [& args]
  (->> (apply resolve-artifacts* args)
       (mapv #(.getArtifact %))
       (mapv #(.getFile %))))

(defn resolve
  "Resolve an artifact vector into a local repository, and return the local file
  path."
  [{:keys [repository local-repo] :as options} coordinates]
  (debugf "resolve %s %s" options coordinates)
  (apply-map
   resolve-artifacts
   :coordinates coordinates
   :retrieve true
   options))
