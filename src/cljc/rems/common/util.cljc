(ns rems.common.util
  (:require [medley.core :refer [map-vals]]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

;; regex from https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input/email#Validation
(def +email-regex+ #"[a-zA-Z0-9.!#$%&'*+\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*")

(def +phone-number-regex+ #"^\+[1-9][0-9\s]{4,26}$")

(deftest test-phone-number-regex
  (is (= "+358450000000"
         (re-matches +phone-number-regex+ "+358450000000")))
  (is (= nil
         (re-matches +phone-number-regex+ "+058450000000")))
  (is (= "+3 5 8 4 5 0 0 0 0 0 0 0 "
         (re-matches +phone-number-regex+ "+3 5 8 4 5 0 0 0 0 0 0 0 ")))
  (is (= nil
         (re-matches +phone-number-regex+ "+35845000000000000000000000000000"))))

;; regex from https://stackoverflow.com/questions/5284147/validating-ipv4-addresses-with-regexp
(def +valid-ip-address-regex+ #"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")

(def +valid-ip-address-regex-version-six+ #"(([0-9]|[a-f]){1,4}(\:)){7}(([0-9]|[a-f]){1,4})$")

;; https://stackoverflow.com/questions/2814002/private-ip-address-identifier-in-regular-expression
;; for now, this test should test for the following private IPv4 address pattaerns
;; 0.x.x.x
;; 10.x.x.x - Private network, local communications
;; 100.64.x.x–100.127.x.x - Private network, communications between a service provider and its subscribers
;; 127.x.x.x - host, loopback addresses to the local host
;; 169.254.x.x - Subnet, Used for link-local addresses[7] between two hosts on a single link when no IP address is otherwise specified
;; 172.16.x.x – 172.31.x.x - Private network, local communications within a private network
;; 192.0.0.x - Private network, IETF Protocol Assignments
;; 192.0.2.x - Documentation, Assigned as TEST-NET-1, documentation and examples.
;; 192.168.x.x - Private network, local communications within a private network
;; 198.18.x.x - 198.19.x.x - Private network, benchmark testing of inter-network communications between two separate subnets
;; 198.51.100.x - Documentation, Assigned as TEST-NET-2, documentation and examples.
;; 203.0.113.x - Documentation, Assigned as TEST-NET-3, documentation and examples.
;; 240.x.x.x–255.x.x.x - multicast
(def +reserved-ip-address-range-regex+
  (re-pattern
   (str
    "((0|10|127|224?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$)|"
    "((100?)\\.([6-9][4-9]|1[1-2][1-7]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$)|"
    "((169?)\\.(254?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$)|"
    "((172?)\\.(1[6-9]|2[0-9]|31?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$)|"
    "((192?)\\.(0?)\\.(0|2?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$)|"
    "((192?)\\.(168?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$)|"
    "((198?)\\.(1[8-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$)|"
    "((198?)\\.(51?)\\.(100?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$)|"
    "((203?)\\.(0?)\\.(113?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$)|"
    "((2[4-5][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$)")))

;; https://simpledns.plus/private-ipv6
;; https://serverfault.com/questions/546606/what-are-the-ipv6-public-and-private-and-reserved-ranges
(def +reserved-ip-address-range-regex-version-six+
  (re-pattern
   (str "(fdce|fc00|fd00)\\:(([0-9]|[a-f]){1,4}(\\:)){6}(([0-9]|[a-f]){1,4})$")))

(deftest test-ip-address-regex
  (is (= "0.0.0.0"
         (first (re-matches +valid-ip-address-regex+ "0.0.0.0"))))
  (is (= "255.255.255.255"
         (first (re-matches +valid-ip-address-regex+ "255.255.255.255"))))
  (is (= "127.0.0.1"
         (first (re-matches +valid-ip-address-regex+ "127.0.0.1"))))
  (is (= "192.168.0.0"
         (first (re-matches +valid-ip-address-regex+ "192.168.0.0"))))
  (is (= "142.250.74.110"
         (first (re-matches +valid-ip-address-regex+ "142.250.74.110"))))
  (is (= "10.255.255.255"
         (first (re-matches +valid-ip-address-regex+ "10.255.255.255"))))
  (is (= nil
         (first (re-matches +valid-ip-address-regex+ "256.255.255.255"))))
  (is (= nil
         (first (re-matches +valid-ip-address-regex+ "10.foo.bar.255"))))
  (is (= "10.0.0.0"
         (first (re-matches +reserved-ip-address-range-regex+ "10.0.0.0"))))
  (is (= "10.26.167.0"
         (first (re-matches +reserved-ip-address-range-regex+ "10.26.167.0"))))
  (is (= "10.0.255.255"
         (first (re-matches +reserved-ip-address-range-regex+ "10.0.255.255"))))
  (is (= "192.0.0.255"
         (first (re-matches +reserved-ip-address-range-regex+ "192.0.0.255"))))
  (is (= "192.0.2.255"
         (first (re-matches +reserved-ip-address-range-regex+ "192.0.2.255"))))
  (is (= "192.168.10.255"
         (first (re-matches +reserved-ip-address-range-regex+ "192.168.10.255"))))
  (is (= "172.16.0.255"
         (first (re-matches +reserved-ip-address-range-regex+ "172.16.0.255"))))
  (is (= "100.64.0.255"
         (first (re-matches +reserved-ip-address-range-regex+ "100.64.0.255"))))
  (is (= "100.127.78.10"
         (first (re-matches +reserved-ip-address-range-regex+ "100.127.78.10"))))
  (is (= "169.254.1.1"
         (first (re-matches +reserved-ip-address-range-regex+ "169.254.1.1"))))
  (is (= nil
         (first (re-matches +reserved-ip-address-range-regex+ "142.250.74.110"))))
  (is (= "198.51.100.78"
         (first (re-matches +reserved-ip-address-range-regex+ "198.51.100.78"))))
  (is (= "203.0.113.89"
         (first (re-matches +reserved-ip-address-range-regex+ "203.0.113.89"))))
  (is (= nil
         (first (re-matches +reserved-ip-address-range-regex+ "203.0.111.89"))))
  (is (= "2001:db8:1870:999:128:7648:3849:688"
         (first (re-matches +valid-ip-address-regex-version-six+ "2001:db8:1870:999:128:7648:3849:688"))))
  (is (= nil
         (first (re-matches +valid-ip-address-regex-version-six+ "2001:db8:1g70:999:128:7648:3849:688"))))
  (is (= nil
         (first (re-matches +reserved-ip-address-range-regex-version-six+ "2001:db8:1g70:999:128:7648:3849:688"))))
  (is (= "fdce:de09:d25d:b23e:de8:d0e8:de8:de8"
         (first (re-matches +reserved-ip-address-range-regex-version-six+ "fdce:de09:d25d:b23e:de8:d0e8:de8:de8")))))

;; TODO remove separate clj and cljs implementations of getx and getx-in
(defn getx
  "Like `get` but throws an exception if the key is not found."
  [m k]
  (let [e (get m k ::sentinel)]
    (if-not (= e ::sentinel)
      e
      (throw (ex-info "Missing required key" {:map m :key k})))))

(defn getx-in
  "Like `get-in` but throws an exception if the key is not found."
  [m ks]
  (reduce getx m ks))

(def conj-set (fnil conj #{}))

(def conj-vec (fnil conj []))

(defn select-vals
  "Select values in map `m` specified by given keys `ks`.

  Values will be returned in the order specified by `ks`.

  You can specify a `default-value` that will be used if the
  key is not found in the map. This is like `get` function."
  [m ks & [default-value]]
  (vec (reduce #(conj %1 (get m %2 default-value)) [] ks)))

(defn build-index
  "Index the `coll` with given keys `:keys` and map values with given
  `:value-fn` (defaults to `identity`).

  Results is nested map, `(count keys)` levels deep, e.g.
    (build-index {:keys [:a :b] :value-fn :c}
                 [{:a 1 :b \"x\" :c :a} {:a 1 :b \"y\" :c :b}])
      ==> {1 {\"x\" :a
              \"y\" :b}}

  In case of non-unique keys, `build-index` picks the first value, e.g.

    (build-index {:keys [:a]} [{:a 1 :b \"x\"} {:a 1 :b \"y\"}])
      ==> {1 {:a 1 :b \"x\"}}

  You can override this behaviour by passing in a `:collect-fn`, which
  is applied to the sequence of values. The default `:collect-fn` is
  `first`."
  [{key-seq :keys
    value-fn :value-fn
    collect-fn :collect-fn}
   coll]
  (if-let [[k & ks] (seq key-seq)]
    (->> coll
         (group-by k)
         (map-vals #(build-index {:keys ks
                                  :value-fn value-fn
                                  :collect-fn collect-fn} %)))
    ((or collect-fn first) (map (or value-fn identity) coll))))

(deftest test-build-index
  (testing "unique keys"
    (is (= {1 {"x" :a "y" :b}}
           (build-index {:keys [:a :b] :value-fn :c}
                        [{:a 1 :b "x" :c :a} {:a 1 :b "y" :c :b}])))
    (is (= {"x" {1 :a} "y" {1 :b}}
           (build-index {:keys [:b :a] :value-fn :c}
                        [{:a 1 :b "x" :c :a} {:a 1 :b "y" :c :b}]))))
  (testing "non-unique keys"
    (is (= {:a 1} (build-index {:keys []}
                               [{:a 1} {:b 2}])))
    (is (= {:b 2} (build-index {:keys [] :collect-fn second}
                               [{:a 1} {:b 2}])))
    (is (= #{{:a 1} {:b 2}} (build-index {:keys [] :collect-fn set}
                                         [{:a 1} {:b 2}])))
    (is (= {1 #{10 11} 2 #{10}}
           (build-index {:keys [:a] :value-fn :c :collect-fn set}
                        [{:a 1 :c 10} {:a 1 :c 11} {:a 2 :c 10}])))))

(defn index-by
  "Index the collection coll with given keys `ks`.
  Result is a nested map, `(count ks)` levels deep, e.g.

    (index-by [:a :b] [{:a 1 :b \"x\"} {:a 1 :b \"y\"}])
      ==> {1 {\"x\" {:a 1 :b \"x\"}
              \"y\" {:a 1 :b \"y\"}}}

  In case of non-unique keys, index-by picks the first value, e.g.

    (index-by [:a] [{:a 1 :b \"x\"} {:a 1 :b \"y\"}])
      ==> {1 {:a 1 :b \"x\"}}"
  [ks coll]
  (build-index {:keys ks} coll))

(deftest test-index-by
  (is (= {1 {"x" {:a 1 :b "x"}
             "y" {:a 1 :b "y"}}}
         (index-by [:a :b] [{:a 1 :b "x"} {:a 1 :b "y"}])))
  (is (= {false 1 true 2}
         (index-by [even?] [1 2 3 4]))))

(defn distinct-by
  "Remove duplicates from sequence, comparing the value returned by key-fn.
   The first element that key-fn returns a given value for is retained.

   Order of sequence is not preserved in any way."
  [key-fn sequence]
  (map first (vals (group-by key-fn sequence))))

(defn andstr
  "Like `apply str coll` but only produces something if all the
  values are truthy like with `and`.

  Useful for statements like
  ```clj
  (str (andstr (:foo x) \"/\") (:bar y))
  ```
  See also `test-andstr` for examples."
  [& coll]
  (when (every? identity coll)
    (apply str coll)))

(deftest test-andstr
  (testing "when any argument is falsey the result is nil"
    (is (= nil (andstr nil 1 2 3)))
    (is (= nil (andstr 1 2 false 3))))
  (testing "when all arguments are truthy the results are concatenated"
    (let [x {:foo 2}]
      (is (= "2/" (andstr (:foo x) "/")))
      (is (= "(2)" (andstr "(" (:foo x) ")"))))))

(defn deep-merge
  "Recursively merges maps and sequentials so that the values in `b`
  will replace the values at the same key or index in `a`."
  [a b]
  (cond (and (sequential? a) (sequential? b))
        (let [max-length (max (count a) (count b))
              a (take max-length (concat a (repeat nil)))
              b (take max-length (concat b (repeat nil)))]
          (doall (map deep-merge a b)))

        (map? a)
        (merge-with deep-merge a b)

        :else b))

(deftest test-deep-merge
  (testing "merge nil"
    (is (= nil
           (deep-merge nil
                       nil)))
    (is (= {:a 1}
           (deep-merge nil
                       {:a 1})))
    (is (= {:a 1}
           (deep-merge {:a 1}
                       nil))))
  (testing "preserve false"
    (is (= {:b false}
           (deep-merge {:b :anything}
                       {:b false}))))
  (testing "merge maps"
    (is (= {:a 2}
           (deep-merge {:a 1}
                       {:a 2})))
    (is (= {:a 1 :b 2 :c 2}
           (deep-merge {:a 1 :b 1}
                       {:b 2 :c 2})))
    (is (= {:a {:b {:c 100 :d 2}}}
           (deep-merge {:a {:b {:c 1 :d 2}}}
                       {:a {:b {:c 100}}}))))
  (testing "merge vectors"
    (is (= [{:a 2}]
           (deep-merge [{:a 1}]
                       [{:a 2}])))
    (is (= [{:a 1 :b 2 :c 2}]
           (deep-merge [{:a 1 :b 1}]
                       [{:b 2 :c 2}])))
    (is (= [{:a 1} {:b 2}]
           (deep-merge [{:a 1}]
                       [nil {:b 2}])))
    (is (= [{:b 2} {:a 1}]
           (deep-merge [nil {:a 1}]
                       [{:b 2}]))))
  (testing "merge lists"
    (is (= [{:a 2}]
           (deep-merge '({:a 1})
                       '({:a 2}))))
    (is (= [{:a 1 :b 2 :c 2}]
           (deep-merge '({:a 1 :b 1})
                       '({:b 2 :c 2}))))))

(defn recursive-keys [m]
  (apply set/union
         (for [[k v] m]
           (if (map? v)
             (set (map (partial cons k) (recursive-keys v)))
             #{(list k)}))))

(deftest test-recursive-keys
  (is (= #{[:a] [:b]} (recursive-keys {:a [1] :b "foo"})))
  (is (= #{[:a :b] [:a :c] [:a :d :e] [:a :d :f]}
         (recursive-keys {:a {:b 1 :c nil :d {:e "foo" :f [3]}}}))))

(defn parse-int [s]
  #?(:clj (try
            (when s
              (java.lang.Integer/parseInt s))
            (catch NumberFormatException e
              nil))
     :cljs (let [x (js/parseInt s)]
             (when-not (js/isNaN x)
               x))))


(deftest test-parse-int
  (is (= nil (parse-int nil)))
  (is (= nil (parse-int "")))
  (is (= nil (parse-int "a")))
  (is (= 7 (parse-int "7"))))

(defn remove-empty-keys
  "Given a map, recursively remove keys with empty map or nil values.

  E.g., given {:a {:b {:c nil} :d {:e :f}}}, return {:a {:d {:e :f}}}."
  [m]
  (into {} (filter (fn [[_ v]] (not ((if (map? v) empty? nil?) v)))
                   (mapv (fn [[k v]] [k (if (map? v)
                                          (remove-empty-keys v)
                                          v)])
                         m))))

(deftest test-remove-empty-keys
  (is (= (remove-empty-keys {}) {}))
  (is (= (remove-empty-keys {:a :b}) {:a :b}))
  (is (= (remove-empty-keys {:a nil}) {}))
  (is (= (remove-empty-keys {:a {:b {:c nil} :d {:e :f}}}) {:a {:d {:e :f}}})))

(defn normalize-file-path
  "A file path may contain local filesystem parts that we want to remove
  so that we can use the path to refer to e.g. project GitHub."
  [path]
  (str/replace (subs path (str/index-of path "src"))
               "\\" "/"))

(deftest normalize-file-path-test
  (is (= "src/foo/bar.clj" (normalize-file-path "/home/john/rems/src/foo/bar.clj")))
  (is (= "src/foo/bar.clj" (normalize-file-path "C:\\Users\\john\\rems\\src\\foo/bar.clj"))))
