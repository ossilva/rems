(ns rems.administration.create-form
  "Form editor for administrators used for editing and creating new forms.

  NB: Each field has a generated distinct `:field/id` so that we can track moving fields,
  their answers and especially references between fields. The `:field/id` is optional in
  the API, and would be generated by the backend, but we always send the one we have created.
  We also maintain a separate `:field/index` to automatically number fields for the user's
  benefit.

  NB: We need to sometimes track when a field has been rendered anew. We use the
  `data-field-index` property for this. When a field is e.g. moved, it takes a while for
  React to re-render it. So we want to wait until the element is rendered to the new place
  with the new index before we can scroll to the new position."
  (:require [clojure.string :as str]
            [goog.string :refer [parseInt]]
            [medley.core :refer [find-first]]
            [re-frame.core :as rf]
            [rems.administration.administration :as administration]
            [rems.administration.components :refer [checkbox localized-text-field organization-field radio-button-group text-field text-field-inline]]
            [rems.administration.items :as items]
            [rems.atoms :as atoms :refer [document-title]]
            [rems.collapsible :as collapsible]
            [rems.common.form :refer [field-visible? generate-field-id validate-form-template] :as common-form]
            [rems.common.util :refer [parse-int]]
            [rems.fetcher :as fetcher]
            [rems.fields :as fields]
            [rems.flash-message :as flash-message]
            [rems.focus :as focus]
            [rems.common.roles :as roles]
            [rems.spinner :as spinner]
            [rems.text :refer [text text-format]]
            [rems.util :refer [navigate! fetch put! post! normalize-option-key trim-when-string visibility-ratio focus-input-field]]))

(rf/reg-event-fx
 ::enter-page
 (fn [{:keys [db]} [_ form-id edit-form?]]
   {:db (assoc db
               ::form nil
               ::form-errors nil
               ::form-id form-id
               ::edit-form? edit-form?)
    :dispatch-n [(when form-id [::form])]}))

(fetcher/reg-fetcher ::form "/api/forms/:id" {:path-params (fn [db] {:id (::form-id db)})})

;;;; form state

(defn- field-editor-id [id]
  (str "field-editor-" id))

(defn- field-editor-selector [id index]
  (str "#" (field-editor-id id) "[data-field-index='" index "']"))

(defn- track-moved-field-editor! [id index button-selector]
  (when-some [element (js/document.getElementById (field-editor-id id))]
    (let [before (.getBoundingClientRect element)]
      ;; NB: the element exists already but we wait for it to reappear with the new index
      (focus/on-element-appear (field-editor-selector id index)
                               (fn [element]
                                 (let [after (.getBoundingClientRect element)]
                                   (focus/scroll-offset before after)
                                   (focus/focus-without-scroll (.querySelector element button-selector))))))))

(defn- focus-field-editor! [id]
  (let [selector "textarea"] ;; focus first title field
    (focus/on-element-appear (str "#" (field-editor-id id))
                             (fn [element]
                               (focus/scroll-to-top element)
                               (collapsible/open-component (field-editor-id id))
                               (.focus (.querySelector element selector))))))

(defn- assign-field-index [form]
  (update form :form/fields #(vec (map-indexed (fn [i field] (assoc field :field/index i)) %))))

(rf/reg-sub ::form-data (fn [db] (assign-field-index (get-in db [::form :data]))))
(rf/reg-sub ::form-errors (fn [db _] (::form-errors db)))
(rf/reg-sub ::edit-form? (fn [db _] (::edit-form? db)))
(rf/reg-event-db ::set-form-field (fn [db [_ keys value]] (assoc-in db (concat [::form :data] keys) value)))

(rf/reg-event-db
 ::add-form-field
 (fn [db [_ & [index]]]
   (let [new-item (merge (generate-field-id (get-in db [::form :data :form/fields]))
                         {:field/type :text})]
     (focus-field-editor! (:field/id new-item))
     (update-in db [::form :data :form/fields] items/add new-item index))))

(rf/reg-event-db
 ::remove-form-field
 (fn [db [_ field-index]]
   (update-in db [::form :data :form/fields] items/remove field-index)))

(rf/reg-event-db
 ::move-form-field-up
 (fn [db [_ field-index]]
   (track-moved-field-editor! (get-in db [::form :data :form/fields field-index :field/id])
                              (dec field-index)
                              ".move-up")
   (update-in db [::form :data :form/fields] items/move-up field-index)))

(rf/reg-event-db
 ::move-form-field-down
 (fn [db [_ field-index]]
   (track-moved-field-editor! (get-in db [::form :data :form/fields field-index :field/id])
                              (inc field-index)
                              ".move-down")
   (update-in db [::form :data :form/fields] items/move-down field-index)))

(rf/reg-event-db
 ::add-form-field-option
 (fn [db [_ field-index]]
   (update-in db [::form :data :form/fields field-index :field/options] items/add {})))

(rf/reg-event-db
 ::remove-form-field-option
 (fn [db [_ field-index option-index]]
   (update-in db [::form :data :form/fields field-index :field/options] items/remove option-index)))

(rf/reg-event-db
 ::move-form-field-option-up
 (fn [db [_ field-index option-index]]
   (update-in db [::form :data :form/fields field-index :field/options] items/move-up option-index)))

(rf/reg-event-db
 ::move-form-field-option-down
 (fn [db [_ field-index option-index]]
   (update-in db [::form :data :form/fields field-index :field/options] items/move-down option-index)))

;; TODO column code is an exact duplication of option code

(rf/reg-event-db
 ::add-form-field-column
 (fn [db [_ field-index]]
   (update-in db [::form :data :form/fields field-index :field/columns] items/add {})))

(rf/reg-event-db
 ::remove-form-field-column
 (fn [db [_ field-index column-index]]
   (update-in db [::form :data :form/fields field-index :field/columns] items/remove column-index)))

(rf/reg-event-db
 ::move-form-field-column-up
 (fn [db [_ field-index column-index]]
   (update-in db [::form :data :form/fields field-index :field/columns] items/move-up column-index)))

(rf/reg-event-db
 ::move-form-field-column-down
 (fn [db [_ field-index column-index]]
   (update-in db [::form :data :form/fields field-index :field/columns] items/move-down column-index)))

;;;; form submit

(defn- localized-field-title [field lang]
  (get-in field [:field/title lang]))

(defn build-localized-string [lstr languages]
  (let [v (into {} (for [language languages]
                     [language (trim-when-string (get lstr language ""))]))]
    (when-not (every? str/blank? (vals v))
      v)))

(defn- build-request-field [field languages]
  (merge {:field/id (:field/id field)
          :field/title (build-localized-string (:field/title field) languages)
          :field/type (:field/type field)
          :field/optional (if (common-form/supports-optional? field)
                            (boolean (:field/optional field))
                            false)}
         (when (common-form/supports-info-text? field)
           (when-let [v (build-localized-string (:field/info-text field) languages)]
             {:field/info-text v}))
         (when (common-form/supports-placeholder? field)
           (when-let [v (build-localized-string (:field/placeholder field) languages)]
             {:field/placeholder v}))
         (when (common-form/supports-max-length? field)
           {:field/max-length (parse-int (:field/max-length field))})
         (when (common-form/supports-options? field)
           {:field/options (for [{:keys [key label]} (:field/options field)]
                             {:key key
                              :label (build-localized-string label languages)})})
         (when (common-form/supports-columns? field)
           {:field/columns (for [{:keys [key label]} (:field/columns field)]
                             {:key key
                              :label (build-localized-string label languages)})})
         (when (common-form/supports-privacy? field)
           (when (= :private (get-in field [:field/privacy]))
             {:field/privacy (:field/privacy field)}))
         (when (common-form/supports-visibility? field)
           (when (= :only-if (get-in field [:field/visibility :visibility/type]))
             {:field/visibility (select-keys (:field/visibility field)
                                             [:visibility/type :visibility/field :visibility/values])}))))

(defn build-request [form languages]
  {:organization {:organization/id (get-in form [:organization :organization/id])}
   :form/internal-name (trim-when-string (:form/internal-name form))
   :form/external-title (build-localized-string (:form/external-title form) languages)
   :form/fields (mapv #(build-request-field % languages) (:form/fields form))})

;;;; form validation

(defn- page-title [edit-form?]
  (if edit-form?
    (text :t.administration/edit-form)
    (text :t.administration/create-form)))

(rf/reg-event-fx
 ::send-form
 (fn [{:keys [db]} [_]]
   (let [edit? (::edit-form? db)
         request (build-request (get-in db [::form :data]) (:languages db))
         form-errors (validate-form-template request (:languages db))
         send-verb (if edit? put! post!)
         send-url (str "/api/forms/" (if edit?
                                       "edit"
                                       "create"))
         description [page-title edit?]
         request (merge request
                        (when edit?
                          {:form/id (::form-id db)}))]
     (when-not form-errors
       (send-verb send-url
                  {:params request
                   :handler (flash-message/default-success-handler
                             :top
                             description
                             (fn [response]
                               (navigate! (str "/administration/forms/"
                                               (if edit?
                                                 (::form-id db)
                                                 (response :id))))))
                   :error-handler (flash-message/default-error-handler :top description)}))
     {:db (assoc db ::form-errors form-errors)})))

;;;; preview auto-scrolling

(defn true-height [element]
  (let [style (.getComputedStyle js/window element)]
    (+ (.-offsetHeight element)
       (js/parseInt (.-marginTop style))
       (js/parseInt (.-marginBottom style)))))

(defn set-visibility-ratio [frame element ratio]
  (when (and frame element)
    (let [element-top (- (.-offsetTop element) (.-offsetTop frame))
          element-height (true-height element)
          top-margin (/ (.-offsetHeight frame) 4)
          position (+ element-top element-height (* -1 ratio element-height) (- top-margin))]
      (.scrollTo frame 0 position))))

(defn first-partially-visible-edit-field []
  (let [fields (array-seq (.querySelectorAll js/document "#create-form .form-field:not(.new-form-field)"))
        visibility? #(<= 0 (-> % .getBoundingClientRect .-bottom))]
    (first (filter visibility? fields))))

(defn autoscroll []
  (when-let [edit-field (first-partially-visible-edit-field)]
    (let [id (last (str/split (.-id edit-field) #"-"))
          preview-frame (.querySelector js/document "#preview-form-contents")
          preview-field (-> js/document
                            (.getElementById (str "field-preview-" id)))
          ratio (visibility-ratio edit-field)]
      (set-visibility-ratio preview-frame preview-field ratio))))

(defn enable-autoscroll! []
  (set! (.-onscroll js/window) autoscroll))

;;;; UI

(def ^:private context
  {:get-form ::form-data
   :get-form-errors ::form-errors
   :update-form ::set-form-field})

(defn- form-organization-field []
  [organization-field context {:keys [:organization]}])

(defn- form-internal-name-field []
  [text-field context {:keys [:form/internal-name]
                       :label (text :t.administration/internal-name)}])

(defn- form-external-title-field []
  [localized-text-field context {:keys [:form/external-title]
                                 :label (text :t.administration/external-title)}])

(defn- form-field-id-field [field-index]
  [text-field-inline context {:keys [:form/fields field-index :field/id]
                              :label (text :t.create-form/field-id)}])

(defn- form-field-title-field [field-index]
  [localized-text-field context {:keys [:form/fields field-index :field/title]
                                 :label (text :t.create-form/field-title)}])

(defn- form-field-placeholder-field [field-index]
  [localized-text-field context {:keys [:form/fields field-index :field/placeholder]
                                 :collapse? true
                                 :label (text :t.create-form/placeholder)}])

(defn- form-field-info-text [field-index]
  [localized-text-field context {:keys [:form/fields field-index :field/info-text]
                                 :collapse? true
                                 :label (text :t.create-form/info-text)}])

(defn- form-field-max-length-field [field-index]
  [text-field-inline context {:keys [:form/fields field-index :field/max-length]
                              :label (text :t.create-form/maxlength)}])

(defn- add-form-field-option-button [field-index]
  [:a.add-option {:href "#"
                  :id (str "fields-" field-index "-add-option")
                  :on-click (fn [event]
                              (.preventDefault event)
                              (rf/dispatch [::add-form-field-option field-index]))}
   [atoms/add-symbol] " " (text :t.create-form/add-option)])

(defn- remove-form-field-option-button [field-index option-index]
  [items/remove-button #(rf/dispatch [::remove-form-field-option field-index option-index])])

(defn- move-form-field-option-up-button [field-index option-index]
  [items/move-up-button #(rf/dispatch [::move-form-field-option-up field-index option-index])])

(defn- move-form-field-option-down-button [field-index option-index]
  [items/move-down-button #(rf/dispatch [::move-form-field-option-down field-index option-index])])

(defn- form-field-option-field [field-index option-index]
  [:div.form-field-option
   [:div.form-field-header
    [:h4 (text-format :t.create-form/option-n (inc option-index))]
    [:div.form-field-controls
     [move-form-field-option-up-button field-index option-index]
     [move-form-field-option-down-button field-index option-index]
     [remove-form-field-option-button field-index option-index]]]
   [text-field-inline context {:keys [:form/fields field-index :field/options option-index :key]
                               :label (text :t.create-form/option-key)
                               :normalizer normalize-option-key}]
   [localized-text-field context {:keys [:form/fields field-index :field/options option-index :label]
                                  :label (text :t.create-form/option-label)}]])

(defn- form-field-option-fields [field-index]
  (let [form @(rf/subscribe [::form-data])]
    (into (into [:div]
                (for [option-index (range (count (get-in form [:form/fields field-index :field/options])))]
                  [form-field-option-field field-index option-index]))
          [[:div.form-field-option.new-form-field-option
            [add-form-field-option-button field-index]]])))

;; TODO column code is an exact duplication of option code
;; TODO column code reuses some option styles

(defn- add-form-field-column-button [field-index]
  [:a.add-option {:href "#"
                  :id (str "fields-" field-index "-add-column")
                  :on-click (fn [event]
                              (.preventDefault event)
                              (rf/dispatch [::add-form-field-column field-index]))}
   (text :t.create-form/add-column)])

(defn- remove-form-field-column-button [field-index column-index]
  [items/remove-button #(rf/dispatch [::remove-form-field-column field-index column-index])])

(defn- move-form-field-column-up-button [field-index column-index]
  [items/move-up-button #(rf/dispatch [::move-form-field-column-up field-index column-index])])

(defn- move-form-field-column-down-button [field-index column-index]
  [items/move-down-button #(rf/dispatch [::move-form-field-column-down field-index column-index])])

(defn- form-field-column-field [field-index column-index]
  [:div.form-field-option
   [:div.form-field-header
    [:h4 (text-format :t.create-form/column-n (inc column-index))]
    [:div.form-field-controls
     [move-form-field-column-up-button field-index column-index]
     [move-form-field-column-down-button field-index column-index]
     [remove-form-field-column-button field-index column-index]]]
   [text-field-inline context {:keys [:form/fields field-index :field/columns column-index :key]
                               :label (text :t.create-form/column-key)
                               :normalizer normalize-option-key}]
   [localized-text-field context {:keys [:form/fields field-index :field/columns column-index :label]
                                  :label (text :t.create-form/column-label)}]])

(defn- form-field-column-fields [field-index]
  (let [form @(rf/subscribe [::form-data])]
    (into [:div {:id (str "fields-" field-index "-columns")}]
          (concat
           (for [column-index (range (count (get-in form [:form/fields field-index :field/columns])))]
             [form-field-column-field field-index column-index])
           [[:div.form-field-option.new-form-field-option
             [add-form-field-column-button field-index]]]))))

(defn- form-fields-that-can-be-used-in-visibility [form]
  (filter #(contains? {:option :multiselect} (:field/type %))
          (:form/fields form)))

(defn- form-field-values [form field-id]
  (let [field (find-first (comp #{field-id} :field/id) (:form/fields form))]
    (case (:field/type field)
      :option (let [options (:field/options field)]
                (map (fn [o] {:value (:key o)
                              :title (:label o)})
                     options))
      [])))

(rf/reg-event-db
 ::form-field-visibility-type
 (fn [db [_ field-index visibility-type]]
   (assoc-in db [::form :data :form/fields field-index :field/visibility :visibility/type] visibility-type)))

(rf/reg-event-db
 ::form-field-visibility-field
 (fn [db [_ field-index visibility-field]]
   (assoc-in db [::form :data :form/fields field-index :field/visibility :visibility/field] visibility-field)))

(rf/reg-event-db
 ::form-field-visibility-value
 (fn [db [_ field-index visibility-value]]
   (assoc-in db [::form :data :form/fields field-index :field/visibility :visibility/values] visibility-value)))

(defn- form-field-visibility
  "Component for specifying form field visibility rules"
  [field-index]
  (let [form @(rf/subscribe [::form-data])
        form-errors @(rf/subscribe [::form-errors])
        error-type (get-in form-errors [:form/fields field-index :field/visibility :visibility/type])
        error-field (get-in form-errors [:form/fields field-index :field/visibility :visibility/field])
        error-value (get-in form-errors [:form/fields field-index :field/visibility :visibility/values])
        lang @(rf/subscribe [:language])
        id-type (str "fields-" field-index "-visibility-type")
        id-field (str "fields-" field-index "-visibility-field")
        id-value (str "fields-" field-index "-visibility-value")
        label-type (text :t.create-form/type-visibility)
        label-field (text :t.create-form.visibility/field)
        label-value (text :t.create-form.visibility/has-value)
        visibility (get-in form [:form/fields field-index :field/visibility])
        visibility-type (:visibility/type visibility)
        visibility-field (:visibility/field visibility)
        visibility-value (:visibility/values visibility)]
    [:div {:class (when (= :only-if visibility-type) "form-field-visibility")}
     [:div.form-group.field.row {:id (str "container-field" field-index)}
      [:label.col-sm-2.col-form-label {:for id-type} label-type]
      [:div.col-sm-10
       [:select.form-control
        {:id id-type
         :class (when error-type "is-invalid")
         :on-change #(rf/dispatch [::form-field-visibility-type field-index (keyword (.. % -target -value))])
         :value (or visibility-type "")}
        [:option {:value "always"} (text :t.create-form.visibility/always)]
        [:option {:value "only-if"} (text :t.create-form.visibility/only-if)]]
       [:div.invalid-feedback
        (when error-type (text-format error-type label-type))]]]
     (when (= :only-if visibility-type)
       [:<>
        [:div.form-group.field.row
         [:label.col-sm-2.col-form-label {:for id-field} label-field]
         [:div.col-sm-10
          [:select.form-control
           {:id id-field
            :class (when error-field "is-invalid")
            :on-change #(rf/dispatch [::form-field-visibility-field field-index {:field/id (.. % -target -value)}])
            :value (or (:field/id visibility-field) "")}
           ^{:key "not-selected"} [:option ""]
           (doall
            (for [field (form-fields-that-can-be-used-in-visibility form)]
              ^{:key (str field-index "-" (:field/id field))}
              [:option {:value (:field/id field)}
               (text-format :t.create-form/field-n (inc (:field/index field)) (localized-field-title field lang))]))]
          [:div.invalid-feedback
           (when error-field (text-format error-field label-field))]]]
        (when (:field/id visibility-field)
          [:div.form-group.field.row
           [:label.col-sm-2.col-form-label {:for id-value} label-value]
           [:div.col-sm-10
            [:select.form-control
             {:id id-value
              :class (when error-value "is-invalid")
              :on-change #(rf/dispatch [::form-field-visibility-value field-index [(.. % -target -value)]])
              :value (or (first visibility-value) "")}
             ^{:key "not-selected"} [:option ""]
             (doall
              (for [value (form-field-values form (:field/id visibility-field))]
                ^{:key (str field-index "-" (:value value))}
                [:option {:value (:value value)} (get-in value [:title lang])]))]
            [:div.invalid-feedback
             (when error-value (text-format error-value label-value))]]])])]))

(rf/reg-event-db
 ::form-field-privacy
 (fn [db [_ field-index privacy]]
   (assoc-in db [::form :data :form/fields field-index :field/privacy] privacy)))

(defn- form-field-privacy
  "Component for specifying form field privacy rules.

  Privacy concerns the reviewers as they can see only public fields."
  [field-index]
  (let [form @(rf/subscribe [::form-data])
        form-errors @(rf/subscribe [::form-errors])
        error (get-in form-errors [:form/fields field-index :field/privacy])
        lang @(rf/subscribe [:language])
        id (str "fields-" field-index "-privacy-type")
        label (text :t.create-form/type-privacy)
        privacy (get-in form [:form/fields field-index :field/privacy])]
    [:div.form-group.field.row {:id (str "container-field" field-index)}
     [:label.col-sm-2.col-form-label {:for id} label]
     [:div.col-sm-10
      [:select.form-control
       {:id id
        :class (when error "is-invalid")
        :on-change #(rf/dispatch [::form-field-privacy field-index (keyword (.. % -target -value))])
        :value (or privacy "public")}
       [:option {:value "public"} (text :t.create-form.privacy/public)]
       [:option {:value "private"} (text :t.create-form.privacy/private)]]]]))

(defn- form-field-type-radio-group [field-index]
  [radio-button-group context {:id (str "radio-group-" field-index)
                               :keys [:form/fields field-index :field/type]
                               :label (text :t.create-form/field-type)
                               :orientation :horizontal
                               :options [{:value :description :label (text :t.create-form/type-description)}
                                         {:value :text :label (text :t.create-form/type-text)}
                                         {:value :texta :label (text :t.create-form/type-texta)}
                                         {:value :option :label (text :t.create-form/type-option)}
                                         {:value :multiselect :label (text :t.create-form/type-multiselect)}
                                         {:value :table :label (text :t.create-form/type-table)}
                                         {:value :date :label (text :t.create-form/type-date)}
                                         {:value :email :label (text :t.create-form/type-email)}
                                         {:value :phone-number :label (text :t.create-form/type-phone-number)}
                                         {:value :attachment :label (text :t.create-form/type-attachment)}
                                         {:value :label :label (text :t.create-form/type-label)}
                                         {:value :header :label (text :t.create-form/type-header)}]}])

(defn- form-field-optional-checkbox [field]
  [checkbox context {:keys [:form/fields (:field/index field) :field/optional]
                     :label (text :t.create-form/optional)}])

(defn- form-field-table-optional-checkbox [field]
  [checkbox context {:keys [:form/fields (:field/index field) :field/optional]
                     :negate? true
                     :label (text :t.create-form/required-table)}])

(defn- add-form-field-button [index]
  [:a.add-form-field {:href "#"
                      :on-click (fn [event]
                                  (.preventDefault event)
                                  (rf/dispatch [::add-form-field index]))}
   [atoms/add-symbol] " " (text :t.create-form/add-form-field)])

(defn- remove-form-field-button [field-index]
  [items/remove-button #(when (js/confirm (text :t.create-form/confirm-remove-field))
                          (rf/dispatch [::remove-form-field field-index]))])

(defn- move-form-field-up-button [field-index]
  [items/move-up-button #(rf/dispatch [::move-form-field-up field-index])])

(defn- move-form-field-down-button [field-index]
  [items/move-down-button #(rf/dispatch [::move-form-field-down field-index])])

(defn- save-form-button [on-click]
  [:button.btn.btn-primary
   {:type :button
    :id :save
    :on-click (fn []
                (rf/dispatch [:rems.spa/user-triggered-navigation]) ;; scroll to top
                (on-click))}
   (text :t.administration/save)])

(defn- cancel-button []
  [atoms/link {:class "btn btn-secondary"}
   "/administration/forms"
   (text :t.administration/cancel)])

(defn- format-validation-link [target content]
  [:li [:a {:href "#" :on-click (focus-input-field target)}
        content]])

(defn- format-error-for-localized-field [error label lang]
  (text-format error (str (text label) " (" (str/upper-case (name lang)) ")")))

(defn- format-field-validation [field field-errors]
  (let [field-index (:field/index field)
        lang @(rf/subscribe [:language])]
    [:li (text-format :t.create-form/field-n (inc field-index) (localized-field-title field lang))
     (into [:ul]
           (concat
            (for [[lang error] (:field/title field-errors)]
              (format-validation-link (str "fields-" field-index "-title-" (name lang))
                                      (format-error-for-localized-field error :t.create-form/field-title lang)))
            (for [[lang error] (:field/placeholder field-errors)]
              (format-validation-link (str "fields-" field-index "-placeholder-" (name lang))
                                      (format-error-for-localized-field error :t.create-form/placeholder lang)))
            (for [[lang error] (:field/info-text field-errors)]
              (format-validation-link (str "fields-" field-index "-info-text-" (name lang))
                                      (format-error-for-localized-field error :t.create-form/info-text lang)))
            (when (:field/max-length field-errors)
              [(format-validation-link (str "fields-" field-index "-max-length")
                                       (str (text :t.create-form/maxlength) ": " (text (:field/max-length field-errors))))])
            (when (-> field-errors :field/visibility :visibility/type)
              [(format-validation-link (str "fields-" field-index "-visibility-type")
                                       (str (text :t.create-form/type-visibility) ": " (text-format (-> field-errors :field/visibility :visibility/type) (text :t.create-form/type-visibility))))])
            (when (-> field-errors :field/visibility :visibility/field)
              [(format-validation-link (str "fields-" field-index "-visibility-field")
                                       (str (text :t.create-form/type-visibility) ": " (text-format (-> field-errors :field/visibility :visibility/field) (text :t.create-form.visibility/field))))])
            (when (-> field-errors :field/visibility :visibility/values)
              [(format-validation-link (str "fields-" field-index "-visibility-value")
                                       (str (text :t.create-form/type-visibility) ": " (text-format (-> field-errors :field/visibility :visibility/values) (text :t.create-form.visibility/has-value))))])
            (if (= :t.form.validation/options-required (:field/options field-errors))
              [[:li
                [:a {:href "#" :on-click #(focus/focus-selector (str "#fields-" field-index "-add-option"))}
                 (text :t.form.validation/options-required)]]]
              (for [[option-id option-errors] (into (sorted-map) (:field/options field-errors))]
                [:li (text-format :t.create-form/option-n (inc option-id))
                 [:ul
                  (when (:key option-errors)
                    (format-validation-link (str "fields-" field-index "-options-" option-id "-key")
                                            (text-format (:key option-errors) (text :t.create-form/option-key))))
                  (into [:<>]
                        (for [[lang error] (:label option-errors)]
                          (format-validation-link (str "fields-" field-index "-options-" option-id "-label-" (name lang))
                                                  (format-error-for-localized-field error :t.create-form/option-label lang))))]]))
            (if (= :t.form.validation/columns-required (:field/columns field-errors))
              [[:li
                [:a {:href "#" :on-click #(focus/focus-selector (str "#fields-" field-index "-add-column"))}
                 (text :t.form.validation/columns-required)]]]
              (for [[column-id column-errors] (into (sorted-map) (:field/columns field-errors))]
                [:li (text-format :t.create-form/column-n (inc column-id))
                 [:ul
                  (when (:key column-errors)
                    (format-validation-link (str "fields-" field-index "-columns-" column-id "-key")
                                            (text-format (:key column-errors) (text :t.create-form/option-key))))
                  (into [:<>]
                        (for [[lang error] (:label column-errors)]
                          (format-validation-link (str "fields-" field-index "-columns-" column-id "-label-" (name lang))
                                                  (format-error-for-localized-field error :t.create-form/option-label lang))))]]))))]))

(defn format-validation-errors [form-errors form lang]
  ;; TODO: deduplicate with field definitions
  (into [:ul
         (when-let [error (:organization form-errors)]
           (format-validation-link "organization"
                                   (text-format error (text :t.administration/organization))))

         (when-let [error (:form/internal-name form-errors)]
           (format-validation-link "internal-name"
                                   (text-format error (text :t.administration/internal-name))))

         (for [[lang error] (:form/external-title form-errors)]
           (format-validation-link (str "external-title-" (name lang))
                                   (format-error-for-localized-field error :t.administration/external-title lang)))]

        (for [[field-index field-errors] (into (sorted-map) (:form/fields form-errors))]
          (let [field (get-in form [:form/fields field-index])]
            [format-field-validation field field-errors lang]))))

(defn- validation-errors-summary []
  (let [form @(rf/subscribe [::form-data])
        errors @(rf/subscribe [::form-errors])
        lang @(rf/subscribe [:language])]
    (when errors
      [:div.alert.alert-danger (text :t.actions.errors/submission-failed)
       [format-validation-errors errors form lang]])))

(defn- form-fields [fields]
  (into [:div
         [:div.form-field.new-form-field
          [add-form-field-button 0]]]

        (for [{index :field/index :as field} fields]
          [:<>
           [:div.form-field {:id (field-editor-id (:field/id field))
                             :key index
                             :data-field-index index}
            [collapsible/minimal
             {:id (field-editor-id (:field/id field))
              :always
              [:div.form-field-header.d-flex
               [:h3 (text-format :t.create-form/field-n (inc index) (localized-field-title field @(rf/subscribe [:language])))]
               [:div.form-field-controls.text-nowrap.ml-auto
                [move-form-field-up-button index]
                [move-form-field-down-button index]
                [remove-form-field-button index]]]
              :collapse
              [:div
               {:id (str (field-editor-id (:field/id field)) "-contents")
                :tab-index "-1"}
               [form-field-title-field index]
               [form-field-type-radio-group index]
               (when (common-form/supports-optional? field)
                 (if (= :table (:field/type field))
                   [form-field-table-optional-checkbox field]
                   [form-field-optional-checkbox field]))
               (when (common-form/supports-info-text? field)
                 [form-field-info-text index])
               (when (common-form/supports-placeholder? field)
                 [form-field-placeholder-field index])
               (let [id (str "fields-" index "-additional")]
                 [:div.form-group.field
                  [:label.administration-field-label
                   (text :t.create-form/additional-settings)
                   " "
                   [collapsible/controls id (text :t.collapse/show) (text :t.collapse/hide) false]]
                  [:div.collapse.solid-group {:id id}
                   [form-field-id-field index]
                   (when (common-form/supports-max-length? field)
                     [form-field-max-length-field index])
                   (when (common-form/supports-privacy? field)
                     [form-field-privacy index])
                   (when (common-form/supports-visibility? field)
                     [form-field-visibility index])]])
               (when (common-form/supports-options? field)
                 [form-field-option-fields index])
               (when (common-form/supports-columns? field)
                 [form-field-column-fields index])]}]]

           [:div.form-field.new-form-field
            [add-form-field-button (inc index)]]])))

(rf/reg-event-db
 ::set-field-value
 (fn [db [_ field-id field-value]]
   (assoc-in db [::preview field-id] field-value)))

(rf/reg-sub
 ::preview
 (fn [db _]
   (::preview db {})))

(defn form-preview [form]
  (let [preview @(rf/subscribe [::preview])
        lang @(rf/subscribe [:language])]
    [collapsible/component
     {:id "preview-form"
      :title (text :t.administration/preview)
      :always (into [:div#preview-form-contents]
                    (for [field (:form/fields form)]
                      [:div.field-preview {:id (str "field-preview-" (:field/id field))}
                       [fields/field (assoc field
                                            :form/id 1 ; dummy value
                                            :on-change #(rf/dispatch [::set-field-value (:field/id field) %])
                                            :field/value (get-in preview [(:field/id field)]))]
                       (when-not (field-visible? field preview)
                         [:div {:style {:position :absolute
                                        :top 0
                                        :left 0
                                        :right 0
                                        :bottom 0
                                        :z-index 1
                                        :display :flex
                                        :flex-direction :column
                                        :justify-content :center
                                        :align-items :flex-end
                                        :border-radius "0.4rem"
                                        :margin "-0.5rem"
                                        :background-color "rgba(230,230,230,0.5)"}}
                          [:div.pr-4 (text :t.create-form.visibility/hidden)]])]))}]))

(defn create-form-page []
  (enable-autoscroll!)
  (let [form @(rf/subscribe [::form-data])
        edit-form? @(rf/subscribe [::edit-form?])
        loading? @(rf/subscribe [::form :fetching?])]
    [:div
     [administration/navigator]
     [document-title (page-title edit-form?)]
     [flash-message/component :top]
     (if loading?
       [:div [spinner/big]]
       [:<>
        [validation-errors-summary]
        [:div.row
         [:div.col-lg
          [collapsible/component
           {:id "create-form"
            :class "fields"
            :title [text :t.administration/form]
            :always [:div
                     [form-organization-field]
                     [form-internal-name-field]
                     [form-external-title-field]
                     [form-fields (:form/fields form)]
                     [:div.col.commands
                      [cancel-button]
                      [save-form-button #(rf/dispatch [::send-form])]]]}]]
         [:div.col-lg
          [form-preview form]]]])]))
