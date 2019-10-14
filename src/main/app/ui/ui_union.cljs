(ns app.ui.ui-union
  (:require
    [com.fulcrologic.fulcro.dom :as dom :refer [div table td tr th tbody]]
    [com.fulcrologic.fulcro.routing.legacy-ui-routers :as r :refer [defsc-router]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [taoensso.timbre :as log]))

(defn person? [props] (contains? props :person/id))
(defn place? [props] (contains? props :place/id))
(defn thing? [props] (contains? props :thing/id))
(defn toot? [props] (contains? props :mastodon.toot/id))

(defn item-ident
  "Generate an ident from a person, place, or thing."
  [props]
  (cond
    (person? props) [:person/id (:person/id props)]
    (place? props) [:place/id (:place/id props)]
    (thing? props) [:thing/id (:thing/id props)]
    (toot? props) [:mastodon.toot/id (:mastodon.toot/id props)]
    :else (log/error "Cannot generate a valid ident. Invalid props." props)))

(defn item-key
  "Generate a distinct react key for a person, place, or thing"
  [props] (str (item-ident props)))

(defn make-person [id n] {:person/id id :person/name n})
(defn make-place [id n] {:place/id id :place/name n})
(defn make-thing [id n] {:thing/id id :thing/label n})
(defn make-toot [id n] {
                         :mastodon.toot/mentions               [],
                         :mastodon.toot/emojis                 [],
                         :mastodon.toot/tags                   [],
                         :mastodon.toot/reblog                 nil,
                         :mastodon.toot/long                   "13.7286",
                         :mastodon.toot/replies_count          0,
                         :mastodon.toot/in_reply_to_account_id nil,
                         :mastodon.toot/reblogs_count          0,
                         :mastodon.toot/application            nil,
                         :mastodon.toot/content                n,
                         :mastodon.toot/sensitive              false,
                         :mastodon.toot/favourites_count       0,
                         :mastodon.toot/in_reply_to_id         nil,
                         :mastodon.toot/poll                   nil,
                         :mastodon.toot/card                   nil,
                         :mastodon.toot/language               "de",
                         :mastodon.toot/id                     id,
                         :mastodon.toot/url                    "https://social.gra.one/@teleporter/102928533748870571",
                         :mastodon.toot/lat                    "51.0756",
                         :mastodon.toot/media_attachments      [],
                         :mastodon.toot/uri                    "https://social.gra.one/users/teleporter/statuses/102928533748870571",
                         :mastodon.toot/visibility             "public",
                         :mastodon.toot/created_at             "2019-10-08T19:59:45.010Z",
                         :mastodon.toot/spoiler_text           ""

                        })
(defsc PersonDetail [this {:person/keys [id name] :as props}]
  ; defsc-router expects there to be an initial state for each possible target. We'll cause this to be a "no selection"
  ; state so that the detail screen that starts out will show "Nothing selected". We initialize all three in case
  ; we later re-order them in the defsc-router.
  {:ident         (fn [] (item-ident props))
   :query         [:person/id :person/name]
   :initial-state {:person/id :no-selection}}
  (dom/div
    (if (= id :no-selection)
      "Nothing selected"
      (str "Details about person " name))))

(defsc TootDetail [this {:mastodon.toot/keys [id content] :as props}]
  ; defsc-router expects there to be an initial state for each possible target. We'll cause this to be a "no selection"
  ; state so that the detail screen that starts out will show "Nothing selected". We initialize all three in case
  ; we later re-order them in the defsc-router.
  {:ident         (fn [] (item-ident props))
   :query         [:mastodon.toot/id :mastodon.toot/content]
   :initial-state {:mastodon.toot/id :no-selection}}
  (dom/div
    (if (= id :no-selection)
      "Nothing selected"
      (str "Details about toot " content))))

(defsc PlaceDetail [this {:place/keys [id name] :as props}]
  {:ident         (fn [] (item-ident props))
   :query         [:place/id :place/name]
   :initial-state {:place/id :no-selection}}
  (dom/div
    (if (= id :no-selection)
      "Nothing selected"
      (str "Details about place " name))))

(defsc ThingDetail [this {:thing/keys [id label] :as props}]
  {:ident         (fn [] (item-ident props))
   :query         [:thing/id :thing/label]
   :initial-state {:thing/id :no-selection}}
  (dom/div
    (if (= id :no-selection)
      "Nothing selected"
      (str "Details about thing " label))))

(defsc PersonListItem [this
                       {:person/keys [id name] :as props}
                       {:keys [onSelect] :as computed}]
  {:ident (fn [] (item-ident props))
   :query [:person/id :person/name]}
  (dom/li {:onClick #(onSelect (item-ident props))}
    (dom/a {} (str "Person " id " " name))))

(def ui-person (comp/factory PersonListItem {:keyfn item-key}))

(defsc TootListItem [this
                       {:mastodon.toot/keys [id uri] :as props}
                       {:keys [onSelect] :as computed}]
  {:ident (fn [] (item-ident props))
   :query [:mastodon.toot/id :mastodon.toot/uri]}
  (dom/li {:onClick #(onSelect (item-ident props))}
          (dom/a {:href uri} (str "Toot uri " id " " uri))))

(def ui-toot (comp/factory TootListItem {:keyfn item-key}))

(defsc PlaceListItem [this {:place/keys [id name] :as props} {:keys [onSelect] :as computed}]
  {:ident (fn [] (item-ident props))
   :query [:place/id :place/name]}
  (dom/li {:onClick #(onSelect (item-ident props))}
    (dom/a {} (str "Place " id " : " name))))

(def ui-place (comp/factory PlaceListItem {:keyfn item-key}))

(defsc ThingListItem [this {:thing/keys [id label] :as props} {:keys [onSelect] :as computed}]
  {:ident (fn [] (item-ident props))
   :query [:thing/id :thing/label]}
  (dom/li {:onClick #(onSelect (item-ident props))}
    (dom/a {} (str "Thing " id " : " label))))

(def ui-thing (comp/factory ThingListItem item-key))

(defsc-router ItemDetail [this props]
  {:router-id      :detail-router
   :ident          (fn [] (item-ident props))
   :default-route  PersonDetail
   :router-targets {:person/id PersonDetail
                    :place/id  PlaceDetail
                    :mastodon.toot/id TootDetail
                    :thing/id  ThingDetail}}
  (dom/div "No route"))

(def ui-item-detail (comp/factory ItemDetail))

(defsc ItemUnion [this props]
  {:ident (fn [] (item-ident props))
   :query (fn [] {:person/id (comp/get-query PersonListItem)
                  :place/id  (comp/get-query PlaceListItem)
                  :mastodon.toot/id (comp/get-query TootListItem)
                  :thing/id  (comp/get-query ThingListItem)})}
  (cond
    (person? props) (ui-person props)
    (place? props) (ui-place props)
    (thing? props) (ui-thing props)
    (toot? props) (ui-toot props)
    :else (dom/div "Invalid ident used in app state.")))

(def ui-item-union (comp/factory ItemUnion {:keyfn item-key}))

(defsc ItemList [this {:keys [items]} {:keys [onSelect]}]
  {
   :initial-state (fn [p]
                    ; These would normally be loaded...but for demo purposes we just hand code a few
                    {:items [(make-person 1 "Tony")
                             (make-thing 2 "Toaster")
                             (make-place 3 "New York")
                             (make-person 4 "Sally")
                             (make-thing 5 "Pillow")
                             (make-toot 9 "a toot")
                             (make-place 6 "Canada")]})
   :ident         (fn [] [:lists/id :singleton])
   :query         [{:items (comp/get-query ItemUnion)}]}
  (dom/ul :.ui.list
    (map (fn [i] (ui-item-union (comp/computed i {:onSelect onSelect}))) items)))

(def ui-item-list (comp/factory ItemList))

(defsc Root [this {:keys [item-list item-detail]}]
  {:query         [{:item-list (comp/get-query ItemList)}
                   {:item-detail (comp/get-query ItemDetail)}]
   :initial-state (fn [p] (merge
                            (r/routing-tree
                              (r/make-route :detail [(r/router-instruction :detail-router [:param/kind :param/id])]))
                            {:item-list   (comp/get-initial-state ItemList nil)
                             :item-detail (comp/get-initial-state ItemDetail nil)}))}
  (let [; This is the only thing to do: Route the to the detail screen with the given route params!
        showDetail (fn [[kind id]]
                     (comp/transact! this `[(r/route-to {:handler :detail :route-params {:kind ~kind :id ~id}})]))]
    ; devcards, embed in iframe so we can use bootstrap css easily
    (div {:key "example-frame-key"}
      (dom/style ".boxed {border: 1px solid black}")
      (table :.ui.table {}
        (tbody
          (tr
            (th "Items")
            (th "Detail"))
          (tr
            (td (ui-item-list (comp/computed item-list {:onSelect showDetail})))
            (td (ui-item-detail item-detail))))))))