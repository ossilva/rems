(ns ^:integration rems.db.test-user-settings
  (:require [clojure.test :refer :all]
            [rems.db.testing :refer [rollback-db-fixture test-db-fixture]]
            [rems.db.user-settings :as user-settings]
            [rems.db.users :as users]
            [schema.core :as s]))

(use-fixtures :once test-db-fixture)
(use-fixtures :each rollback-db-fixture)

(deftest test-user-settings
  (users/add-user! "user" {:eppn "user"})
  (users/add-user! "unrelated" {:eppn "unrelated"})

  (testing "default settings for a new user"
    (is (= {:language :en
            :notification-email nil}
           (s/validate user-settings/UserSettings
                       (user-settings/get-user-settings "user")))))

  (testing "add settings"
    (is (= {:success true}
           (user-settings/update-user-settings! "user" {:language :fi})))
    (is (= {:language :fi
            :notification-email nil}
           (user-settings/get-user-settings "user"))))

  (testing "modify settings"
    (is (= {:success true}
           (user-settings/update-user-settings! "user" {:notification-email "user@example.com"})))
    (is (= {:language :fi
            :notification-email "user@example.com"}
           (user-settings/get-user-settings "user"))))

  (testing "updating with empty settings does not change settings"
    (is (= {:success true}
           (user-settings/update-user-settings! "user" {})))
    (is (= {:language :fi
            :notification-email "user@example.com"}
           (user-settings/get-user-settings "user"))))

  (testing "updating with invalid settings does not change settings"
    (is (= {:success false}
           (user-settings/update-user-settings! "user" {:notification-email "foo"}))
        "completely invalid")
    (is (= {:success false}
           (user-settings/update-user-settings! "user" {:notification-email "bar@example.com" ; valid
                                                        :language :de})) ; invalid
        "partially invalid")
    (is (= {:language :fi
            :notification-email "user@example.com"}
           (user-settings/get-user-settings "user"))))

  (testing "changing one user's settings does not change another user's settings"
    (is (= {:success true}
           (user-settings/update-user-settings! "unrelated" {:notification-email "unrelated@example.com"})))
    (is (= {:success true}
           (user-settings/update-user-settings! "user" {:notification-email "changed@example.com"})))
    (is (= {:language :en
            :notification-email "unrelated@example.com"}
           (user-settings/get-user-settings "unrelated")))))

(deftest test-language
  (testing "valid language"
    (is (= {:language :en} (user-settings/validate-settings {:language :en})))
    (is (= {:language :fi} (user-settings/validate-settings {:language :fi}))))

  (testing "undefined language"
    (is (= {} (user-settings/validate-settings {:language :de}))))

  (testing "nil language"
    (is (= {} (user-settings/validate-settings {:language nil})))))

(deftest test-notification-email
  (testing "valid email"
    (is (= {:notification-email "somebody@example.com"}
           (user-settings/validate-settings {:notification-email "somebody@example.com"}))))

  (testing "invalid email"
    (is (= {}
           (user-settings/validate-settings {:notification-email "somebody"}))))

  (testing "empty email will clear the setting"
    (is (= {:notification-email nil}
           (user-settings/validate-settings {:notification-email ""}))))

  (testing "nil email will clear the setting"
    (is (= {:notification-email nil}
           (user-settings/validate-settings {:notification-email nil})))))
