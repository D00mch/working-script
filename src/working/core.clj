(ns working.core
  (:require
   [clojure.tools.cli :as cli]
   [robot.core :as robot]
   [clojure.java.shell :refer [sh]])
  (:gen-class))

(defn cmd! [cmd] (sh  "sh" "-c" cmd))

(defn- open-docker! [] 
  (cmd! "open -a Docker")
  (while (-> (cmd! "docker stats --no-stream") :out empty?)
    (Thread/sleep 2000)))

(def cli-options
  [["-d" "--directory PATH" "Path to directory with the project"
    :default "~/IdeaProjects/work/server/"
    :parse-fn str]])

(defn- paste! [s]
  (robot/clipboard-put! s)
  (robot/hot-keys! [:cmd :v])
  (robot/sleep 200)
  (robot/type! :enter))

(defn- run-docker-compose! [path]
  (cmd! "open /Applications/WezTerm.app")
  (robot/sleep 300)
   ;; be sure to open new tab as terminal may be do somthing in the current tab
  (robot/hot-keys! [:cmd :t])
  (robot/sleep 100)
  (paste! (str "cd " path))
  (robot/sleep 100)
  (paste! "docker-compose up"))

(defn- run-repl-in-new-tab! []
  (robot/sleep 100)
  (robot/hot-keys! [:cmd :t])
  (robot/sleep 100)
  (paste! "lein with-profile +test repl"))

(defn -main [& args]
  (let [{{dir :directory} :options} (cli/parse-opts args cli-options)]
    (println "Starting docker process")
    (open-docker!)
    (println "Running docker-compose in" dir)
    (run-docker-compose! dir)
    (println "Starting lein repl")
    (run-repl-in-new-tab!)))

(comment
  (-main "~/IdeaProjects/work/server")

  (run-docker-compose! "~/IdeaProjects/work/server")
  (do 
    (cmd! "open /Applications/WezTerm.app")
    (run-repl-in-new-tab!))
  (cli/parse-opts '("-d" "abc") cli-options))

