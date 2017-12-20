(ns rems.layout
  (:require [hiccup.element :refer [link-to]]
            [hiccup.page :refer [html5 include-css include-js]]
            [rems.config :refer [env]]
            [rems.context :as context]
            [rems.guide :refer :all]
            [rems.language-switcher :refer [language-switcher]]
            [rems.roles :refer [when-role when-roles]]
            [rems.text :refer :all]
            [rems.util :refer [get-username]]
            [ring.util.http-response :as response]))

(defn external-link []
  [:i {:class "fa fa-external-link"}])

(defn- url-dest
  [dest]
  (str context/*root-path* dest))

(defn- nav-link [path title & [active? external?]]
  [:a {:class (str "nav-item nav-link" (if active? " active" ""))
       :href (url-dest path)
       :target (if external? "_blank" nil)}
   title
   (when external? (list " " (external-link)))])

(defn user-switcher [user]
  (when user
    [:div.user.px-2.px-sm-0
     [:i.fa.fa-user]
     [:span.user-name (str (get-username) " /")]
     (link-to {:class (str "px-0 nav-link")} (url-dest "/logout") (text :t.navigation/logout))]))

(defn- navbar-items [e page-name user]
  [e
   ;; TODO configurable brand?
   ;; [:a.navbar-brand {:href "/"} "REMS"]
   [:div.navbar-nav.mr-auto
    (if user
      (list
       (when-role :applicant
         (nav-link "/catalogue" (text :t.navigation/catalogue) (= page-name "catalogue")))
       (when-role :applicant
         (nav-link "/applications" (text :t.navigation/applications) (= page-name "applications")))
       (when-roles #{:approver :reviewer}
         (nav-link "/actions" (text :t.navigation/actions) (= page-name "actions"))))
      (nav-link "/" (text :t.navigation/home) (= page-name "home")))
    (for [{:keys [id url translation-key translations external?]} (:extra-pages env)]
      (nav-link url
                (if translation-key (text translation-key) (translations context/*lang*))
                (= page-name id)
                external?))]
   (language-switcher)])

(defn- navbar
  [page-name user]
  (list
   [:div.navbar-flex
    [:nav.navbar.navbar-toggleable-sm {:role "navigation"}
     [:button.navbar-toggler
      {:type "button" :data-toggle "collapse" :data-target "#small-navbar"}
      "&#9776;"]
     (navbar-items :div#big-navbar.collapse.navbar-collapse page-name user)]
    [:div.navbar (user-switcher user)]]
   (navbar-items :div#small-navbar.collapse.navbar-collapse.collapse.hidden-md-up page-name user)))

(defn- footer []
  [:footer.footer
   [:div.container [:nav.navbar [:div.navbar-text (text :t/footer)]]]])

(defn- logo []
  [:div.logo [:div.container.img]])

(defn flash-message [{status :status contents :contents}]
  [:div.alert
   {:class (case status
             :success "alert-success"
             :warning "alert-warning"
             :failure "alert-danger"
             :info "alert-info")}
   contents])

(defn- page-template
  [page-name nav content footer message]
  (html5 [:head
          [:meta {:http-equiv "Content-Type" :content "text/html; charset=UTF-8"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
          [:link {:rel "icon" :href "/img/favicon.ico" :type "image/x-icon"}]
          [:link {:rel "shortcut icon" :href "/img/favicon.ico" :type "image/x-icon"}]
          [:title "Welcome to rems"]
          (include-css "/assets/bootstrap/css/bootstrap.min.css")
          (include-css "/assets/font-awesome/css/font-awesome.min.css")
          (include-css "/css/screen.css")
          [:body
           [:div.fixed-top
            [:div.container nav]]
           (logo)
           [:div.container message]
           [:div.container.main-content content]
           footer
           (include-js "/assets/jquery/jquery.min.js")
           (include-js "/assets/tether/dist/js/tether.min.js")
           (include-js "/assets/bootstrap/js/bootstrap.min.js")]]))

(defn render
  "renders HTML generated by Hiccup

   params: :status -- status code to return, defaults to 200
           :headers -- map of headers to return, optional
           :content-type -- optional, defaults to \"text/html; charset=utf-8\"
           :bare -- don't include navbar, footer or flash"
  [page-name content & [params]]
  (let [nav (when-not (:bare params)
              (navbar page-name context/*user*))
        footer (when-not (:bare params)
                 (footer))
        message (when-not (:bare params)
                  (when context/*flash*
                    (map flash-message context/*flash*)))
        content-type (:content-type params "text/html; charset=utf-8")
        status (:status params 200)
        headers (:headers params {})]
    (response/content-type
     {:status status
      :headers headers
      :body (page-template page-name nav content footer message)}
     content-type)))

(defn- error-content
  [error-details]
  [:div.container-fluid
   [:div.row-fluid
    [:div.col-lg-12
     [:div.centering.text-center
      [:div.text-center
       [:h1
        [:span.text-danger (str "Error: " (error-details :status))]
        [:hr]
        (when-let [title (error-details :title)]
          [:h2.without-margin title])
        (when-let [message (error-details :message)]
          [:h4.text-danger message])]]]]]])

(defn error-page
  "error-details should be a map containing the following keys:
   :status - error status
   :title - error title (optional)
   :message - detailed error message (optional)
   :bare - don't render navbar and footer (optional)

   returns a response map with the error page as the body
   and the status specified by the status key"
  [error-details]
  (render "error page" (error-content error-details) error-details))

(defn guide
  "Component guide fragment"
  []
  (list
   (component-info nav-link)
   (example "nav-link"
            (nav-link "example/path" "link text"))
   (example "nav-link active"
            (nav-link "example/path" "link text" "page-name" "page-name"))
   (example "nav-item"
            (nav-link "example/path" "link text" "page-name" "li-name"))

   (component-info language-switcher)
   (example "language-switcher"
            (language-switcher))

   (component-info navbar)
   (example "navbar guest"
            (binding [context/*roles* nil]
              (navbar "example-page" nil)))
   (example "navbar for applicant"
            (binding [context/*roles* #{:applicant}]
              (navbar "example-page" "Eero Esimerkki")))
   (example "navbar for approver"
            (binding [context/*roles* #{:approver}]
              (navbar "example-page" "Aimo Approver")))
   (example "navbar for admin"
            (binding [context/*roles* #{:applicant :approver :reviewer}]
              (navbar "example-page" "Antero Admin")))

   (component-info footer)
   (example "footer"
            (footer))

   (component-info logo)
   (example "logo" (logo))

   (component-info flash-message)
   (example "flash success"
            (flash-message {:status :success
                            :contents [:p "Message " [:strong "contents"] " here"]}))
   (example "flash warning"
            (flash-message {:status :warning
                            :contents [:p "Message " [:strong "contents"] " here"]}))

   (component-info error-content)
   (example "error-content"
            (error-content {:status 123 :title "Error title" :message "Error message"}))))
