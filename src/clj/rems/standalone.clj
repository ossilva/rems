(ns rems.standalone
  "Run the REMS app in an embedded http server."
  (:require [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [luminus-migrations.core :as migrations]
            [luminus.http-server :as http]
            [luminus.repl-server :as repl]
            [mount.core :as mount]
            [rems.application.search :as search]
            [rems.config :refer [env]]
            [rems.db.api-key :as api-key]
            [rems.db.applications :as applications]
            [rems.db.core :as db]
            [rems.db.roles :as roles]
            [rems.db.test-data :as test-data]
            [rems.db.users :as users]
            [rems.handler :as handler]
            [rems.json :as json]
            [rems.validate :as validate])
  (:import [sun.misc Signal SignalHandler])
  (:refer-clojure :exclude [parse-opts])
  (:gen-class))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])

(mount/defstate
  ^{:on-reload :noop}
  http-server
  :start
  (http/start (assoc env :handler handler/handler))
  :stop
  (when http-server (http/stop http-server)))

(mount/defstate
  ^{:on-reload :noop}
  repl-server
  :start
  (when-let [nrepl-port (env :nrepl-port)]
    (repl/start {:port nrepl-port}))
  :stop
  (when repl-server
    (repl/stop repl-server)))

(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped")))

(defn- refresh-caches []
  (log/info "Refreshing caches")
  (applications/refresh-all-applications-cache!)
  (search/refresh!)
  (log/info "Caches refreshed"))

(defn start-app [& args]
  (doseq [component (-> args
                        (parse-opts cli-options)
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app))
  (validate/validate)
  (refresh-caches))

;; The default of the JVM is to exit with code 128+signal. However, we
;; shut down gracefully on SIGINT and SIGTERM due to the exit hooks
;; mount has installed. Thus exit code 0 is the right choice. This
;; also makes REMS standalone work nicely with systemd: by default it
;; uses SIGTERM to stop services and expects a succesful exit.
(defn exit-on-signals! []
  (let [exit (proxy [SignalHandler] []
               (handle [sig]
                 (log/info "Shutting down due to signal" (.getName sig))
                 (System/exit 0)))]
    (Signal/handle (Signal. "INT") exit) ;; e.g. ^C from terminal
    (Signal/handle (Signal. "TERM") exit) ;; default kill signal of systemd
    nil))

(defn -main
  "Arguments can be either arguments to mount/start-with-args, or one of
     \"migrate\" -- migrate database
     \"rollback\" -- roll back database migration
     \"reset\" -- empties database and runs migrations to empty db
     \"test-data\" -- insert test data into database
     \"demo-data\" -- insert data for demoing purposes into database
     \"validate\" -- validate data in db
     \"list-users\" -- list users and roles
     \"grant-role <role> <user>\" -- grant a role to a user
     \"remove-role <role> <user>\" -- remove a role from a user
     \"api-key get\" -- list all api keys
     \"api-key get <api-key>\" -- get details of api key
     \"api-key add <api-key> [<description>]\" -- add api key to db.
        <description> is an optional text comment.
        If a pre-existing <api-key> is given, update description for it.
     \"api-key delete <api-key>\" -- remove api key from db.
     \"api-key set-users <api-key> [<uid1> <uid2> ...]\" -- set allowed users for api key
        An empty set of users means all users are allowed.
        Adds the api key if it doesn't exist.
     \"api-key allow <api-key> <method> <regex>\" -- add an entry to the allowed method/path whitelist
        The special method `any` means any method.
        The regex is a (Java) regular expression that should match the whole path of the request.
        Example regex: /api/applications/[0-9]+/?
     \"api-key allow-all <api-key>\" -- clears the allowed method/path whitelist.
        An empty list means all methods and paths are allowed."
  [& args]
  (exit-on-signals!)
  (let [usage #(do
                 (println "Usage:")
                 (println (:doc (meta #'-main))))]
    (case (first args)
      "help"
      (usage)

      ("migrate" "rollback")
      (do
        (mount/start #'rems.config/env)
        (migrations/migrate args (select-keys env [:database-url])))

      "reset"
      (do
        (println "\n\n*** Are you absolutely sure??? Reset empties the whole database and runs migrations to empty db.***\nType 'YES' to proceed")
        (when (= "YES" (read-line))
          (do
            (println "Running reset")
            (mount/start #'rems.config/env)
            (migrations/migrate args (select-keys env [:database-url])))))

      "test-data"
      (do
        (mount/start #'rems.config/env
                     #'rems.db.core/*db*
                     #'rems.locales/translations)
        (log/info "Creating test data")
        (test-data/create-test-data!)
        (test-data/create-performance-test-data!)
        (log/info "Test data created"))

      "demo-data"
      (do
        (mount/start #'rems.config/env
                     #'rems.db.core/*db*
                     #'rems.locales/translations)
        (test-data/create-demo-data!))

      "api-key"
      (let [[_ command api-key & command-args] args]
        (mount/start #'rems.config/env #'rems.db.core/*db*)
        (case command
          "get" (do)
          "add" (api-key/update-api-key! api-key {:comment (str/join " " command-args)})
          "delete" (api-key/delete-api-key! api-key)
          "set-users" (api-key/update-api-key! api-key {:users command-args})
          "allow" (let [[method path] command-args
                        entry {:method method :path path}
                        old (:paths (api-key/get-api-key api-key))]
                    (api-key/update-api-key! api-key {:paths (conj old entry)}))
          "allow-all" (api-key/update-api-key! api-key {:paths nil})
          (do (usage)
              (System/exit 1)))
        (if api-key
          (prn (api-key/get-api-key api-key))
          (mapv prn (api-key/get-api-keys))))

      "list-users"
      (do
        (mount/start #'rems.config/env #'rems.db.core/*db*)
        (doseq [u (users/get-all-users)]
          (-> u
              (assoc :roles (roles/get-roles (:userid u)))
              json/generate-string
              println)))

      "grant-role"
      (let [[_ role user] args]
        (if (not (and role user))
          (do (usage)
              (System/exit 1))
          (do (mount/start #'rems.config/env #'rems.db.core/*db*)
              (roles/add-role! user (keyword role)))))

      "remove-role"
      (let [[_ role user] args]
        (if (not (and role user))
          (do (usage)
              (System/exit 1))
          (do (mount/start #'rems.config/env #'rems.db.core/*db*)
              (roles/remove-role! user (keyword role)))))

      "validate"
      (do
        (mount/start #'rems.config/env #'rems.db.core/*db*)
        (when-not (validate/validate)
          (System/exit 2)))

      ;; default
      (apply start-app args))))
