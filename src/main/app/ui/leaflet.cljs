(ns app.ui.leaflet
  (:require
    [app.ui.leaflet.layers :refer [overlay-class->component]]
    [app.ui.leaflet.tracking :refer [controlToggleTracking ControlToggleTracking]]
    [app.ui.leaflet.layers.extern.base :refer [baseLayers]]
    [app.ui.leaflet.layers.extern.mvt :refer [mvtLayer]]
    [com.fulcrologic.fulcro.components :refer [defsc factory get-query]]
    [com.fulcrologic.fulcro.algorithms.react-interop :refer [react-factory]]
    [app.ui.leaflet.layers.d3svg-lines :refer [D3SvgLines]]
    [app.ui.filtered-example :refer [geojson]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    ["react-leaflet" :refer [withLeaflet Map LayersControl LayersControl.Overlay Marker Popup GeoJSON Polyline LayersControl.BaseLayer TileLayer]]
    ["leaflet" :as l]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as comp]
    [app.model.geofeatures :as gf]))

(def leafletMap (react-factory Map))
(def layersControl (react-factory LayersControl))
(def layersControlOverlay (react-factory LayersControl.Overlay))
(def layersControlBaseLayer (react-factory LayersControl.BaseLayer))
(def tileLayer (react-factory TileLayer))
(def marker (react-factory Marker))
(def popup (react-factory Popup))
(def geoJson (react-factory GeoJSON))
(def polyline (react-factory Polyline))
(def d3SvgLines (factory D3SvgLines))

(defn overlay-filter-rule->filter [filter-rule]
  (if (empty? filter-rule)
      (constantly true)
      (fn [feature]
          (->> (map (fn [[path set_of_accepted_vals]]
                        (set_of_accepted_vals (get-in feature path)))
                    filter-rule)
          (reduce #(and %1 %2))))))

#_(defsc StartStopMarker
  [this {:keys [lat lng]}]
  {:query         [:lat :lng]
   :initial-state {:lat 51 :lng 13}
   }
   (marker {:position [lat lng]
           :icon     (.icon. l (clj->js
                                 {
                                  :iconUrl     "https://cdn.rawgit.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png"
                                  :shadowUrl   "https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png"
                                  :iconSize    [25, 41]
                                  :iconAnchor  [12, 41]
                                  :popupAnchor [1, -34]
                                  :shadowSize  [41, 41]
                                  }
                                 ))}))

#_(def startStopMarker (factory StartStopMarker {:key-fn #((hash [(:lat %) (:lng %)]))}))

(defn filterLatLng [{:keys [lat lng]}]
  {:lat lat :lng lng}
  )

(defn boundsFromMap [map]
     (let [bs (.getBounds map)]
       {
        :southWest (filterLatLng (js->clj (.getSouthWest bs) :keywordize-keys true))
        :northEast (filterLatLng (js->clj (.getNorthEast bs) :keywordize-keys true))}))


(defmutation set-bounds [{:keys [bbox]}]
             (action [{:keys [state]}]
                     (swap! state into {::bbox bbox})
                     ))

(defsc LeafletSimple
  [this {:as props ::keys [id center zoom bbox]
         :keys [style]
         :or {center [51.055 13.74]
              zoom 12
              bbox nil
              style {:height "100%" :width "100%"}}}]
  {:ident (fn [] [:leaflet/id id])
   :query [::id ::center ::zoom ::bbox
           ::gf/id :style
           ]}
  ;(routing-example (get-in props [:leaflet/datasets :vvo :data :geojson]))

  (leafletMap {:style     style
               :center    center :zoom zoom
               :onMoveend #(let [bounds (boundsFromMap (:target (js->clj % :keywordize-keys true)))]
                             (js/console.log bounds)
                             (comp/transact! this [(set-bounds {:bbox bounds})])
                             )
               :onZoomend #(js/console.log (js->clj % :keywordize-keys true))
               :onResize  #(js/console.log (js->clj % :keywordize-keys true))
               }
              (layersControl {}
                             ;mvtLayer
                             (layersControlBaseLayer
                               {:name "OSM Tiles"
                                :tile {:url "https://{s}.tile.osm.org/{z}/{x}/{y}.png"
                                       :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors"}}
                                                     (tileLayer
                                                       {:url "https://{s}.tile.osm.org/{z}/{x}/{y}.png"
                                                        :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors"}))
                             (layersControlOverlay {:key "test" :name "test-layer" :checked true :bbox bbox}
                                                   (do (js/console.log bbox)
                                                       (d3SvgLines {:react-key "test-layer-1232132"
                                                                                          :geojson   {:type "FeatureCollection" :features geojson}}))))))


(def leafletSimple (factory LeafletSimple))

(defsc Leaflet
  [this {:as props ::keys [id center zoom layers]
                   :keys [style]
                   :or {center [51.055 13.74]
                        zoom 12
                        style {:height "100%" :width "100%"}}}]
  {:ident (fn [] [:leaflet/id id])
   :query [::id ::layers ::center ::zoom
           ::gf/id :style
           {:background-location/state (comp/get-query ControlToggleTracking)}
           ]}
  ;(routing-example (get-in props [:leaflet/datasets :vvo :data :geojson]))

  (leafletMap {:style style
               :center center :zoom zoom}
    ;(controlOpenSidebar {})
    (controlToggleTracking (:background-location/state props))
    (layersControl {}
      ;mvtLayer

      (for [[layer-name layer] layers] [

           (if-let [base (:base layer)]
             (layersControlBaseLayer base
               (tileLayer (:tile base))))

           (let [overlays (->> (for [overlay (:overlays layer)
                                     :let [dataset-features (get-in props [::gf/id (:dataset overlay) ::gf/geojson :features])
                                           filtered-features (filter (overlay-filter-rule->filter (:filter overlay)) dataset-features)
                                           component (overlay-class->component (:class overlay))]]
                                    (if (and component (not (empty? filtered-features)))
                                      (do
                                        (js/console.log filtered-features)
                                        (js/console.log (:class overlay))
                                        (component {:react-key (str layer-name (hash overlay) (hash filtered-features))
                                                    :geojson   {:type "FeatureCollection" :features filtered-features}}))))
                               (remove nil?))]
                (if-not (empty? overlays)
                        (layersControlOverlay {:key layer-name :name layer-name :checked (boolean (:prechecked layer))}
                                              overlays)))]))))

(def leaflet (factory Leaflet))

