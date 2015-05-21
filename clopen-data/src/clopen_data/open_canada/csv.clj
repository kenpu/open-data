(ns clopen-data.open-canada.csv
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.java.shell :as shell]))

(def url "http://open.canada.ca/data/en/dataset")
(def action-url "http://open.canada.ca/data/api/action/package_show")

(defn get-csv-index-page
  [page]
  (client/get url {:query-params {:res_format "CSV" :page page}}))

;
; (file-csv-index-pages "dl/index-pages/csv-index")
;
(defn file-csv-index-pages
  [filename]
  (dorun
    (for [p (range 1 347)]
      (do
        (println "page:" p)
        (let [res (get-csv-index-page p)]
          (spit (str filename "-" p) (res :body)))))))

(defn extract-datasets
  [filename]
  (let [pat #"/data/en/dataset/([a-z0-9-]+)"
        content (slurp filename)]
    (do
      (map #(% 1) (re-seq pat content)))))

;
; (file-dataset-ids "dl/csv-ids.json"
;
(defn file-dataset-ids [out-filename]
  (let [ids (flatten 
              (for [p (range 1 347)] 
                (let [filename (format "dl/csv-index-%d" p)] 
                  (extract-datasets filename))))]
    (println (format "Writing %d to %s" (count ids) out-filename))
    (->> ids json/write-str (spit out-filename))))

(defn exists [filename]
  (.exists (clojure.java.io/as-file filename)))

;
; (dl-json-descriptors)
; this causes timeout due to bandwidth limiter
; so will require numerous retries
;
(defn dl-json-descriptors
  []
  (let [idx-file "dl/csv-ids.json"
        ids (-> idx-file slurp json/read-str)]
    (println (format "Starting to download %d JSON descriptors" (count ids)))
    (dorun
      (for [[i id] (map-indexed vector ids)]
        (let [filename (format "dl/json/%s.json" id)]
          (if (exists filename)
            (println i ": skipping " filename)
            (do
              (print (str i ": fetching " id " ..."))
              (flush)
              (let [res (client/get action-url {:query-params {:id id}})]
                (do 
                  (println (format " [%d]" (res :status)))
                  (Thread/sleep 100)
                  (spit filename (res :body)))))))))))

;
; (build-urls "dl/download.txt")
;
(defn build-urls
  [out-filename]
  (with-open [out (clojure.java.io/writer out-filename)]
    (dorun
      (for [filename (->> "dl/json" (new java.io.File) .list)]
        (do
          (println "filename:" filename)
          (let [data (json/read-str (slurp (str "dl/json/" filename)))]
            (dorun
              (for [r ((data "result") "resources") :when (= (r "format") "CSV")]
                (let [id (r "id")
                      url (r "url")]
                  (println id)
                  (.write out (format "%s\n" url)))))))))))


(defn download
  [prefix url]
  (println "trying:" url)
  (if (exists (str prefix "/success"))
    (println "skip" prefix url)
    (do
      (clojure.java.io/make-parents prefix)
      (let [result (shell/sh "wget" "-P" prefix url)]
        (if (zero? (result :exit))
          (do
            (println "Done" prefix url)
            (spit (str prefix "/success") "ok"))
          (throw (Exception. (str prefix " failed " (:err result)))))))))

;
; (...)
;
(defn get-files
  []
  (with-open [rdr (clojure.java.io/reader "dl/download.txt")]
    (dorun
      (for [[i url] (map-indexed vector (line-seq rdr))]
        (try 
          (download (str "dl/files/" i) url)
          (catch Exception e nil))))))

