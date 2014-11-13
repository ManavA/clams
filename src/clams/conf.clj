(ns clams.conf
  (:refer-clojure :rename {get core-get})
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(defn- normalize-key
  [key]
  (if-not (nil? key)
    (-> key
        string/lower-case
        (string/replace "_" "-")
        (string/replace "." "-")
        keyword)))

(defn- read-config
  [name]
  (let [resource (io/resource (format "conf/%s.edn" name))]
    (if (nil? resource)
      {}  ;; Config file not found.
      (edn/read-string (slurp resource)))))

(defn- getenv
  []
  (System/getenv))

(defn- read-env
  []
  (into {} (for [[k v] (getenv)]
    [(normalize-key k) v])))

(defonce ^:private full-conf (atom nil))

(defn load!
  []
  (reset! full-conf (merge (read-config "base")
                           (read-config "default")
                           (read-env))))

(defn unload!
  []
  (reset! full-conf nil))

(defn assert-loaded
  []
  (assert (not (nil? @full-conf)) "Config not loaded!"))

(defn get
  [k]
  (assert-loaded)
  (core-get @full-conf k))

(defn get-all
  []
  (assert-loaded)
  @full-conf)
