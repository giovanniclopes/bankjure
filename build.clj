(ns build
  (:require [clojure.tools.build.api :as b]
            [clojure.java.io :as io]))

(def lib 'bankjure/core)
(def version "0.1.0-SNAPSHOT")
(def class-dir "target/classes")
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn- basis []
  (b/create-basis {:project "deps.edn"}))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (let [bd (basis)]
    (io/make-parents (io/file class-dir ".keep"))
    (when (.exists (io/file "resources"))
      (b/copy-dir {:src-dirs ["resources"]
                   :target-dir class-dir}))
    (b/compile-clj {:basis bd
                    :src-dirs ["src"]
                    :class-dir class-dir})
    (b/uber {:class-dir class-dir
             :uber-file uber-file
             :basis bd
             :main 'ledger.server})))
