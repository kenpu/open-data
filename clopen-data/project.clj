(defproject clopen-data "0.1.0-SNAPSHOT"
  :description "Helps to download known open datasets"
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [io.aviso/pretty "0.1.17"]
                 [clj-http "1.1.2"]
                 [org.clojure/data.json "0.2.6"]]
  :repl-options {:nrepl-middleware [io.aviso.nrepl/pretty-middleware]})
