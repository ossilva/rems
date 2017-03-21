(ns rems.routes.fake-shibboleth
  (:require [rems.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.response :refer [response redirect]]
            [buddy.auth.backends.session :refer [session-backend]]
            [clojure.java.io :as io]))

(defn- fake-login [{session :session username :fake-username}]
      (assoc (redirect "/catalogue")
             :session (assoc session :identity (or username "developer"))))

(defn- fake-logout [{session :session}]
  (-> (redirect "/")
      (assoc :session (dissoc session :identity))))

(defroutes fake-shibboleth-routes
  (GET "/Shibboleth.sso/Login" req (fake-login req))
  (GET "/Shibboleth.sso/Logout" req (fake-logout req)))
