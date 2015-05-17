(ns clopen-data.open-canada.csv
  (:require [clj-http.client :as client]))

(def url "http://open.canada.ca/data/en/dataset")

(defn get-csv-index-page
  [page]
  (client/get url {:form-params {:res_format "CSV" :page page}}))

(defn file-csv-index-pages
  [filename]
  (doall
    (for [p (range 1 347)]
      (do
        (println "page:" p)
        (let [res (get-csv-index-page p)]
          (spit (str filename "-" p) (res :body)))))))
