(ns clams.params
  (:require [clams.response :as response]
            [clams.util :refer [redef]]
            [schema.coerce :as coerce]
            [schema.core :as s]
            [schema.utils :as utils]))

(redef schema.core [Bool Int Keyword Num Str])

(def ^:private schema-key :params)

(defn- param-metadata
  "Extracts the parameter schema from function metadata if one exists.
  Note that you must pass in the var here, not the function itself."
  [fnvar]
  (let [metadata (meta fnvar)]
    (when (contains? metadata schema-key)
      (map vec (partition 2 (schema-key metadata))))))

(defn- metadata->schema
  "Transforms the params metadata format into a schema format
  appropriate for validation."
  [params]
  (into {} params))

(defn- parse-request-params
  "Parses the params in the request, including coercion from JSONish types
  into Clojure types, and validation against the specified schema."
  [req schema]
  (let [parser (coerce/coercer schema coerce/json-coercion-matcher)
        params (parser (:params req))]
    (if (utils/error? params)
      (response/bad-request! (str "Parameter validation failed. Got: " (:params req)))
      params)))

(defn wrap-controller
  "Wraps a controller function in a function that appropriately parses
  the controller's specified parameters. Note: Because we depend on its
  metadata, the controller function should be passed as a var."
  [ctrl]
  (if-let [params (param-metadata ctrl)]
    (fn [req]
      (let [valid-params (parse-request-params req (metadata->schema params))
            args         (for [[k _] params] (get valid-params k))]
        (apply ctrl args)))
    ctrl))
