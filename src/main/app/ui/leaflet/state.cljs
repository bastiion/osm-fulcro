(ns app.ui.leaflet.state
  (:require
    [app.application :refer [SPA]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.components :refer [transact!]]
    [com.fulcrologic.fulcro.data-fetch :refer [load!]]
    [com.fulcrologic.fulcro.application :refer [current-state]]
    [app.utils.ring-buffer :as rb]
    ))

(defmutation mutate-datasets-load
  "For now we don't require arguments, but always reload all datasets"
  [_]
  (action [{:keys [app]}]
          (doseq [[ds-name ds] (:leaflet/datasets (current-state SPA))]
            (let [source (:source ds)
                  [ident params] (if (keyword? (:query source))
                                   [(:query source) nil]
                                   [:_ {:query (:query source)}])]
              (load! app ident nil {:remote (:remote source)
                                    :params params
                                    :target [:leaflet/datasets ds-name :data :geojson]})))))

(defmutation mutate-datasets [{:keys [path data]}]
  (action [{:keys [app state]}]
          (swap! state update-in (concat [:leaflet/datasets] path)
                 (fn [d_orig d_new] (if (map? d_new) (merge d_orig d_new) d_new))
                 data)
          (transact! app [(mutate-datasets-load)])))

(defmutation mutate-layers [{:keys [path data]}]
  (action [{:keys [state]}]
          (swap! state update-in (concat [:leaflet/layers] path)
                 (fn [d_orig d_new] (if (map? d_new) (merge d_orig d_new) d_new))
                 data)))

(def graphhopper-remote #_:graphhopper :graphhopper-web)

(defn load-route
  [fromPoint toPoint app]
  (let [request {:remote graphhopper-remote
                 :params {
                          :start fromPoint
                          :end   toPoint
                          }
                 :target [:leaflet/datasets :routing :data]
                 }]
    (prn "Will request")
    (prn request)
    (load! app :graphhopper/route nil request))

  )

(defmutation current-point-select [props]
  (action [{:keys [app state]}]
          (let [points (:selected/points @state)]
            (if (> 1 (count points))
              nil
              (load-route (:selected/latlng (last points)) props app)
              )
            (swap! state into {:selected/points (vec (conj (:selected/points @state) {:selected/latlng props}))})

            )))


(defmutation new-sensor-data [{:keys [values sensor_type]}]
  (action [{:keys [state]}]
          (let [values (vec values)
                keywd (keyword "sensors" sensor_type)
                ]
            (do
              (let [rbuf
                    (if (nil? (keywd @state))
                      (rb/ring-buffer 10)
                      (keywd @state)
                      )]
                (swap! state into {keywd (conj rbuf values)}))
                ))))

(defmutation new-location-data [{:keys [values sensor_type]}]
  (action [{:keys [state]}]
          (let [values (vec values)
                keywd (keyword "sensors" sensor_type)
                ]
            (do
              (let [rbuf
                    (if (nil? (keywd @state))
                      (vec [])
                      (keywd @state)
                      )]
                (swap! state into {keywd (conj rbuf values)}))
              ))))
