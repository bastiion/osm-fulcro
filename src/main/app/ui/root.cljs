(ns app.ui.root
  (:require
    [clojure.string :as str]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div ul li p h3 button]]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.ui-state-machines :as uism :refer [defstatemachine]]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro-css.css :as css]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [taoensso.timbre :as log]
    [app.mastodon :as mastodon]
    [com.fulcrologic.fulcro.algorithms.react-interop :as interop]
    ["react-leaflet" :as ReactLeaflet :refer [withLeaflet Map
                                              LayersControl LayersControl.BaseLayer LayersControl.Overlay
                                              TileLayer Marker Popup]]
    ["react-leaflet-vectorgrid" :as VectorGrid]))

(def leafletMap (interop/react-factory Map))
(def layersControl (interop/react-factory LayersControl))
(def layersControlBaseLayer (interop/react-factory LayersControl.BaseLayer))
(def layersControlOverlay (interop/react-factory LayersControl.Overlay))
(def tileLayer (interop/react-factory TileLayer))
(def vectorGrid (interop/react-factory (withLeaflet VectorGrid)))
(def marker (interop/react-factory Marker))
(def popup (interop/react-factory Popup))

(defsc GeoJSON
  "a GeoJSON dataset"
  [this {:as props}]
  {:query         [:type :features :timestamp :generator :copyright]
   :ident         (fn [] [:data :vvo])
   :initial-state {:type {}}})

(defsc GeoToots
  "a Geo URL enriched mastodon toot"
  [this {:as props}]
  {:query         [:mastodon/toots]
   :ident         (fn [] [:url])
   :initial-state {:mastodon/toots []}})

(defsc MastodonEvents
  "mastodon events on a map"
  [this {:as props}]
  {:query         [:toots]  #_[:url :tags :mentions :created_at :content]
   :ident         (fn [] [:url])
   :initial-state {:toots []}})


(defonce toots (atom []))


(defn extractGeoURI [message]
  (let [re-res (re-find #"geo:(\d+(?:\.\d+)?),(\d+(?:\.\d+))" message)]
    (if (nil? re-res)
      nil
      {
       :lat  (get re-res 1)
       :long (get re-res 2)
       }
      )))

(comment

  (extractGeoURI "dsadsadsa geo:51.5212,13.7286?z=15 das dsadsad")

  )


(def demoToot
  {:mentions               [],
   :emojis                 [],
   :tags                   [],
   :reblog                 nil,
   :long                   "13.7286",
   :replies_count          0,
   :in_reply_to_account_id nil,
   :reblogs_count          0,
   :application            nil,
   :content                "<p>bar8<br />geo:51.0756,13.7286?z=15</p>",
   :sensitive              false,
   :favourites_count       0,
   :in_reply_to_id         nil,
   :poll                   nil,
   :card                   nil,
   :language               "de",
   :id                     "102928533748870571",
   :url                    "https://social.gra.one/@teleporter/102928533748870571",
   :lat                    "51.0756",
   :media_attachments      [],
   :uri                    "https://social.gra.one/users/teleporter/statuses/102928533748870571",
   :visibility             "public",
   :created_at             "2019-10-08T19:59:45.010Z",
   :spoiler_text           ""}
  )

(defmutation bump-number [ignored]
  (action [{:keys [state]}]
          (do
            (prn (:component/id @state))
            (swap! state update :ui/number inc))))

(defmutation new-toot [{:keys [toot]}]
  (action [{:keys [state]}]
          (let [tootWG (merge toot (extractGeoURI (:content toot)))]
            (do
              (prn toot)
              (swap! state into {:mastodon/toots (vec (conj (:mastodon/toots @state) tootWG))}))
            )))


(def startStreamPublicTimeline
  (mastodon/streamPublicTimeline
    (fn [toot]
      (let [tootWG (merge toot (extractGeoURI (:content toot)))]
        (swap! toots conj tootWG)))
    (fn [toot]
      (do
        (prn "Will call mutation")
        (comp/transact! app.application/SPA [(new-toot {:toot toot})])
        ))))

(defsc NumberDiv
  [this {:ui/keys [number]}]
  {:query [:ui/number]
   :initial-state (fn [_] {:ui/number 3})
   }
  (dom/h3 (str "number " number))
  )


(def ui-number (comp/factory NumberDiv))

(defsc OSM
  [this {:ui/keys [number] :osm/keys [geojson] :mastodon/keys [toots] :as props}]
  {:query         [
                   {:ui/number (comp/get-query NumberDiv)}
                   {:mastodon/toots (comp/get-query GeoToots)}
                   {:osm/geojson (comp/get-query GeoJSON)}
                   ]
   :ident (fn [] [:component/id :osm])
   :initial-state (fn [{:as props}]
                    {:osm/geojson    (comp/get-initial-state GeoJSON)
                     :mastodon/toots []
                     :ui/number      1
                     })}
  (dom/div {:style {:height "600px" :width "100%"}}
           (dom/h2 (str "count " (count toots) " " number))
           (ui-number nil)
           (dom/p "this is a test")
           (dom/button {:onClick #(comp/transact! this `[(new-toot {:toot ~demoToot})])} "demo toot")
           (dom/button {:onClick #(comp/transact! this `[(bump-number {})])} "bumb")
           ))

(def ui-osm (comp/factory OSM))

(defsc DemoSimple
  [this {:ui/keys [number] :as props}]
  {:query         [:ui/number]
   :initial-state (fn [{:as props}]
                    :ui/number 1
                    )}
  (dom/div {:style {:height "600px" :width "100%"}}
           (dom/h1 "Roger")
           (dom/h2 (str "number " number))
           (dom/button {:onClick #(comp/transact! this `[(bump-number {})])} "bumb")
           ))


(def ui-demo (comp/factory DemoSimple))

(defsc Root [this {:as props}]
  {:query         [{:ui/number (comp/get-query DemoSimple)}
                   ]
   :initial-state (fn [{:as props}] :ui/number -1)}
  (dom/div {:style {:height "100%" :width "100%"}}
    (ui-demo {}))
  )
