(ns rems.poller.email
  "Sending emails based on application events."
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [mount.core :as mount]
            [postal.core :as postal]
            [rems.config :refer [env]]
            [rems.context :as context]
            [rems.db.applications :as applications]
            [rems.db.events :as events]
            [rems.db.users :as users]
            [rems.poller.common :as common]
            [rems.scheduler :as scheduler]
            [rems.text :refer [text text-format with-language]]
            [rems.util :as util])
  (:import [org.joda.time Duration]))

;;; Mapping events to emails

;; TODO list of resources?
;; TODO use real name when addressing user?

;; move this to a util namespace if its needed somewhere else
(defn- link-to-application [application-id]
  (str (:public-url env) "#/application/" application-id))

(defn- invitation-link [token]
  (str (:public-url env) "accept-invitation?token=" token))

(defn- application-id-for-email [application]
  (case (util/getx env :application-id-column)
    :external-id (:application/external-id application)
    :id (:application/id application)))

(defn- resources-for-email [application]
  (->> (:application/resources application)
       (map #(get-in % [:catalogue-item/title context/*lang*]))
       (str/join ", ")))

;; There's a slight inconsistency here: we look at current members, so
;; a member might get an email for an event that happens before he was
;; added.
(defn- applicant-and-members [application]
  (conj (map :userid (:application/members application))
        (:application/applicant application)))

(defn- handlers [application]
  (get-in application [:application/workflow :workflow.dynamic/handlers]))

(defn- other-handlers [event application]
  (filter #(not= % (:event/actor event)) (handlers application)))

(defmulti ^:private event-to-emails-impl
  (fn [event _application] (:event/type event)))

(defmethod event-to-emails-impl :default [_event _application]
  [])

(defn- emails-to-recipients [recipients event application subject-text body-text]
  (vec
   (for [recipient recipients]
     {:to-user recipient
      :subject (text-format subject-text
                            recipient
                            (:event/actor event)
                            (application-id-for-email application)
                            (:application/applicant application)
                            (resources-for-email application)
                            (link-to-application (:application/id event)))
      :body (text-format body-text
                         recipient
                         (:event/actor event)
                         (application-id-for-email application)
                         (:application/applicant application)
                         (resources-for-email application)
                         (link-to-application (:application/id event)))})))

(defmethod event-to-emails-impl :application.event/approved [event application]
  (concat (emails-to-recipients (applicant-and-members application)
                                event application
                                :t.email.application-approved/subject
                                :t.email.application-approved/message-to-applicant)
          (emails-to-recipients (other-handlers event application)
                                event application
                                :t.email.application-approved/subject
                                :t.email.application-approved/message-to-handler)))

(defmethod event-to-emails-impl :application.event/rejected [event application]
  (concat (emails-to-recipients (applicant-and-members application)
                                event application
                                :t.email.application-rejected/subject
                                :t.email.application-rejected/message-to-applicant)
          (emails-to-recipients (other-handlers event application)
                                event application
                                :t.email.application-rejected/subject
                                :t.email.application-rejected/message-to-handler)))

(defmethod event-to-emails-impl :application.event/closed [event application]
  (concat (emails-to-recipients (applicant-and-members application)
                                event application
                                :t.email.application-closed/subject
                                :t.email.application-closed/message-to-applicant)
          (emails-to-recipients (other-handlers event application)
                                event application
                                :t.email.application-closed/subject
                                :t.email.application-closed/message-to-handler)))

(defmethod event-to-emails-impl :application.event/returned [event application]
  (concat (emails-to-recipients (applicant-and-members application)
                                event application
                                :t.email.application-returned/subject
                                :t.email.application-returned/message-to-applicant)
          (emails-to-recipients (other-handlers event application)
                                event application
                                :t.email.application-returned/subject
                                :t.email.application-returned/message-to-handler)))

(defmethod event-to-emails-impl :application.event/licenses-added [event application]
  (concat (emails-to-recipients (applicant-and-members application)
                                event application
                                :t.email.application-licenses-added/subject
                                :t.email.application-licenses-added/message-to-applicant)
          (emails-to-recipients (other-handlers event application)
                                event application
                                :t.email.application-licenses-added/subject
                                :t.email.application-licenses-added/message-to-handler)))

(defmethod event-to-emails-impl :application.event/submitted [event application]
  (emails-to-recipients (handlers application)
                        event application
                        :t.email.application-submitted/subject
                        :t.email.application-submitted/message))

(defmethod event-to-emails-impl :application.event/comment-requested [event application]
  (emails-to-recipients (:application/commenters event)
                        event application
                        :t.email.comment-requested/subject
                        :t.email.comment-requested/message))

(defmethod event-to-emails-impl :application.event/commented [event application]
  (emails-to-recipients (handlers application)
                        event application
                        :t.email.commented/subject
                        :t.email.commented/message))

(defmethod event-to-emails-impl :application.event/decided [event application]
  (emails-to-recipients (handlers application)
                        event application
                        :t.email.decided/subject
                        :t.email.decided/message))

(defmethod event-to-emails-impl :application.event/decision-requested [event application]
  (emails-to-recipients (:application/deciders event)
                        event application
                        :t.email.decision-requested/subject
                        :t.email.decision-requested/message))

(defmethod event-to-emails-impl :application.event/member-added [event application]
  ;; TODO email to applicant? email to handler?
  (emails-to-recipients [(:userid (:application/member event))]
                        event application
                        :t.email.member-added/subject
                        :t.email.member-added/message))

(defmethod event-to-emails-impl :application.event/member-invited [event _application]
  [{:to (:email (:application/member event))
    :subject (text-format :t.email.member-invited/subject
                          (:email (:application/member event))
                          (invitation-link (:invitation/token event)))
    :body (text-format :t.email.member-invited/message
                       (:email (:application/member event))
                       (invitation-link (:invitation/token event)))}])

;; TODO member-joined?

(defn event-to-emails [event]
  (when-let [app-id (:application/id event)]
    (event-to-emails-impl event (applications/get-unrestricted-application app-id))))

;;; Generic poller infrastructure

;;; Email poller

;; You can test email sending by:
;;
;; 1. running mailhog: docker run -p 1025:1025 -p 8025:8025 mailhog/mailhog
;; 2. adding {:mail-from "rems@example.com" :smtp-host "localhost" :smtp-port 1025} to dev-config.edn
;; 3. generating some emails
;;    - you can reset the email poller state with (common/set-poller-state! :rems.poller.email/poller nil)
;; 4. open http://localhost:8025 in your browser to view the emails

(defn mark-all-emails-as-sent! []
  (let [events (events/get-all-events-since 0)
        last-id (:event/id (last events))]
    (common/set-poller-state! ::poller {:last-processed-event-id last-id})))

(defn send-email! [email-spec]
  (let [host (:smtp-host env)
        port (:smtp-port env)]
    (if (not (and host port))
      (log/info "pretending to send email:" (pr-str email-spec))
      (let [email (assoc email-spec
                         :from (:mail-from env)
                         :body (str (:body email-spec)
                                    (text :t.email/footer))
                         :to (or (:to email-spec)
                                 (util/get-user-mail
                                  (users/get-user-attributes
                                   (:to-user email-spec)))))]
        ;; TODO check that :to is set
        (log/info "sending email:" (pr-str email))
        (try
          (postal/send-message {:host host :port port} email)
          (catch com.sun.mail.smtp.SMTPAddressFailedException e ; email address does not exist
            (log/warn e "failed sending email, skipping:" (pr-str email))))))))

(defn run []
  (common/run-event-poller ::poller (fn [event]
                                      (with-language (:default-language env)
                                        #(doseq [mail (event-to-emails event)]
                                           (send-email! mail))))))

(mount/defstate email-poller
  :start (scheduler/start! run (Duration/standardSeconds 10))
  :stop (scheduler/stop! email-poller))

(comment
  (mount/start #{#'email-poller})
  (mount/stop #{#'email-poller}))
