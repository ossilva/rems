(ns rems.user-settings
  (:require [re-frame.core :as rf]
            [rems.flash-message :as flash-message]
            [rems.language :as language]
            [rems.util :refer [fetch put!]]))

(rf/reg-event-fx
 :loaded-user-settings
 (fn [{:keys [db]} [_ user-settings]]
   (language/update-language (:language user-settings))
   {:db (assoc db :user-settings user-settings)}))

(defn fetch-user-settings! []
  (fetch "/api/user-settings"
         {:handler #(rf/dispatch [:loaded-user-settings %])
          :error-handler (flash-message/default-error-handler :top "Fetch user settings")}))

(rf/reg-event-fx
 ::update-user-settings
 (fn [_ [_ settings]]
   (put! "/api/user-settings"
         {:params settings
          :handler (fn [_] (fetch-user-settings!))
          :error-handler (flash-message/default-error-handler :top "Update user settings")})
   nil))
