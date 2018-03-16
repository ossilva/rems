(ns rems.api
  (:require [compojure.api.sweet :refer :all]
            [compojure.api.exception :as ex]
            [rems.api.application :refer [application-api]]
            [rems.api.schema :refer :all]
            [rems.config :refer [env]]
            [rems.context :as context]
            [rems.db.applications :as applications]
            [rems.db.catalogue :as catalogue]
            [rems.db.entitlements :as entitlements]
            [rems.form :as form]
            [rems.locales :as locales]
            [ring.util.http-response :refer :all]
            [schema.core :as s])
  (:import [org.joda.time DateTime]
           rems.auth.NotAuthorizedException))

(def GetTranslationsResponse
  s/Any)

(def GetThemeResponse
  s/Any)

(def ExtraPage
  {s/Keyword s/Any})

(def GetConfigResponse
  {:authentication s/Keyword
   (s/optional-key :extra-pages) [ExtraPage]})

(def GetApplicationsResponse
  [Application])

(def GetCatalogueResponse
  [CatalogueItem])

(def GetActionsResponse
  {:approver? s/Bool
   :reviewer? s/Bool
   :approvals [Application]
   :handled-approvals [Application]
   :reviews [Application]
   :handled-reviews [Application]})

(def Entitlement
  {:resource s/Str
   :application-id s/Num
   :start s/Str
   :mail s/Str})

(def CreateCatalogueItemCommand
  {:title s/Str
   :form s/Num
   :resid s/Num
   :wfid s/Num})

(def CreateCatalogueItemResponse
  CatalogueItem)

(def CreateCatalogueItemLocalizationCommand
  {:id s/Num
   :langcode s/Str
   :title s/Str})

(def CreateCatalogueItemLocalizationResponse
  {:success s/Bool})

(defn unauthorized-handler
  [exception ex-data request]
  (unauthorized "unauthorized"))

(def api-routes
  (api
   {:exceptions {:handlers {rems.auth.NotAuthorizedException (ex/with-logging unauthorized-handler)
                            ;; add logging to validation handlers
                            ::ex/request-validation (ex/with-logging ex/request-validation-handler)
                            ::ex/request-parsing (ex/with-logging ex/request-parsing-handler)
                            ::ex/response-validation (ex/with-logging ex/response-validation-handler)}}
    :swagger {:ui "/swagger-ui"
              :spec "/swagger.json"
              :data {:info {:version "1.0.0"
                            :title "Sample API"
                            :description "Sample Services"}}}}

   (context "/api" []
     :header-params [{x-rems-api-key s/Str}
                     {x-rems-user-id s/Str}]

     (context "/translations" []
       :tags ["translations"]

       (GET "/" []
         :summary "Get translations"
         :return GetTranslationsResponse
         (ok locales/translations)))

     (context "/theme" []
       :tags ["theme"]

       (GET "/" []
         :summary "Get current layout theme"
         :return GetThemeResponse
         (ok context/*theme*)))

     (context "/config" []
       :tags ["config"]

       (GET "/" []
         :summary "Get configuration that is relevant to UI"
         :return GetConfigResponse
         (ok (select-keys env [:authentication :extra-pages]))))

     (context "/actions" []
       :tags ["actions"]

       (GET "/" []
         :summary "Get actions page reviewable and approvable applications"
         :return GetActionsResponse
         (ok {:approver? true
              :reviewer? true
              :approvals (applications/get-approvals)
              :handled-approvals (applications/get-handled-approvals)
              :reviews (applications/get-applications-to-review)
              :handled-reviews (applications/get-handled-reviews)})))

     application-api

     (context "/applications" []
       :tags ["applications"]

       (GET "/" []
         :summary "Get current user's all applications"
         :return GetApplicationsResponse
         (ok (applications/get-my-applications))))

     (context "/catalogue" []
       :tags ["catalogue"]

       (GET "/" []
         :summary "Get catalogue items"
         :return GetCatalogueResponse
         (binding [context/*lang* :en]
           (ok (catalogue/get-localized-catalogue-items))))

       (PUT "/create" []
         :summary "Create a new catalogue item"
         :body [command CreateCatalogueItemCommand]
         :return CreateCatalogueItemResponse
         (ok (catalogue/create-catalogue-item-command! command)))

       (PUT "/create-localization" []
         :summary "Create a new catalogue item localization"
         :body [command CreateCatalogueItemLocalizationCommand]
         :return CreateCatalogueItemLocalizationResponse
         (ok (catalogue/create-catalogue-item-localization-command! command))))

     (context "/entitlements" []
       :tags ["entitlements"]

       (GET "/" []
         :summary "Get all entitlements"
         :return [Entitlement]
         (ok (entitlements/get-entitlements-for-api)))))))
