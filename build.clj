(ns build
  "Geo build script.

  clojure -T:build ci

  clojure -T:build install

  clojure -T:build deploy"

  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]))

(def lib 'Factual/geo)
(def version "3.0.1")

(defn ci "Run the CI pipeline of tests (and build the JAR)." [opts]
  (-> opts
      (assoc :lib lib :version version)
      ;(bb/run-tests) ;; Pending move to clojure.test and cljs
      (bb/clean)
      (bb/jar)))

(defn install "Install the JAR locally." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/install)))

(defn deploy "Deploy the JAR to Clojars." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/install)))
