(ns app.ui.toot-list
  (:require
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.wsscode.pathom.connect :as pc]))

(defsc Person [this
               {:person/keys [name age] :as props}
               {:keys [onDelete]}]
  {:query         [:person/id :person/name :person/age] ; (2)
   :ident         (fn [] [:person/id (:person/id props)]) ; (1)
   :initial-state (fn [{:keys [id name age] :as params}] {:person/id id :person/name name :person/age age})} ; (3)
  (dom/li
    (dom/h5 (str name " (age: " age ")") (dom/button {:onClick #(onDelete name)} "X")))) ; (4)

(def ui-person (comp/factory Person {:keyfn :person/id}))

(defmutation delete-person [{:keys [list item]}]
                (action [{:keys [state]}]
                        (prn state)
                        ))

(defsc PersonList [this {:list/keys [id label people] :as props}]
  {:query [:list/id :list/label {:list/people (comp/get-query Person)}] ; (5)
   :ident (fn [] [:list/id (:list/id props)])
   :initial-state
          (fn [{:keys [id label]}]
            {:list/id     id
             :list/label  label
             :list/people (if (= id :friends)
                            [(comp/get-initial-state Person {:id 1 :name "Sally" :age 32})
                             (comp/get-initial-state Person {:id 2 :name "Joe" :age 22})]
                            [(comp/get-initial-state Person {:id 3 :name "Fred" :age 11})
                             (comp/get-initial-state Person {:id 4 :name "Bobby" :age 55})])})}
  (let [delete-person (fn [item-id] (comp/transact! this [(delete-person {:list id :item item-id})]))] ; (4)
    (dom/div
      (dom/h4 label)
      (dom/ul
        (map #(ui-person (comp/computed % {:onDelete delete-person})) people)))))

(def ui-person-list (comp/factory PersonList))

(defsc Toot [this
                     {:keys [id uri content] :as props}
                     {:keys [onSelect] :as computed}]
  {:query [:id :uri :content]
   :ident (fn [] [:mastodon.toot/id (:id props)])
   :initial-state (fn [{:keys [id uri content] :as params}] {:id id :name name :content content :uri uri})}
  (dom/li {:onClick #(prn %) #_#(onSelect (item-ident props))}
          (dom/a {:href uri} (str "Toot uri " id " uri: " uri))))

(def ui-toot (comp/factory Toot {:keyfn :id})) ; ??? :mastodon.toot/id

(defsc TootList [this {:mastodon.list/keys [id label toots] :as props}]
  {:query         [:mastodon.list/id
                   :mastodon.list/label
                   {:mastodon.list/toots (comp/get-query Toot)}]
   :ident (fn [] [:mastodon.list/id (:mastodon.list/id props)])
   :initial-state (fn [{:keys [id label]}] {
                                            :mastodon.list/id id
                                            :mastodon.list/label label
                                            :mastodon.list/toots
                                            [
                                             (comp/get-initial-state Toot {:id "12321313", :content "bla" :uri "http://example.com"})
                                             ]})}
  (dom/div
    (dom/h4 label)
    (dom/ul :.ui.list
          (map (fn [i] (ui-toot i #_(comp/computed i {:onSelect onSelect}))) toots))))


(def ui-toot-list (comp/factory TootList))
(defsc Root [this {:keys [friends enemies toots]}]
  {:query         [{:friends (comp/get-query PersonList)}
                   {:enemies (comp/get-query PersonList)}
                   {:toots (comp/get-query TootList)}
                   ]
   :initial-state (fn [params] {:friends (comp/get-initial-state PersonList {:id :friends :label "Friends"})
                                :enemies (comp/get-initial-state PersonList {:id :enemies :label "Enemies"})
                                :toots (comp/get-initial-state TootList {:id :toots :label "mastodon Toots"})
                                })}
  (dom/div
    (dom/button {:onClick #(comp/transact! this `[(app.ui.mastodon-fulcro/get-mastodon-public-timeline {})])} "load timeline")
    (ui-toot-list toots)
    (ui-person-list friends)
    (ui-person-list enemies)))
