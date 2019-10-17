(ns app.client
  (:require
    [app.application :refer [SPA]]
    [com.fulcrologic.fulcro.application :as app]
    [app.ui.root :as countdown1]
    [app.ui.ui-union :as countdown3]
    [app.ui.toot-list :as countdown]
    [app.ui.countdown-many :as countdown2]
    [com.fulcrologic.fulcro.networking.http-remote :as net]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro-css.css-injection :as cssi]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.inspect.inspect-client :as inspect]
    [com.fulcrologic.fulcro.rendering.keyframe-render :refer [render!]]
    [app.ui.root]))

(defn load! []
  (df/load! SPA :geojson.vvo/geojson #_app.ui.root/GeoJSON nil
            {:target [:data :vvo]
             :post-action #(app/schedule-render! SPA {:force-root? true})}))

(defn ^:export refresh []
  (log/info "Hot code Remount")
  (cssi/upsert-css "componentcss" {:component countdown/Root})
  (app/mount! SPA countdown/Root "app")
  #_(load!))

(defn ^:export init []
  (log/info "Application starting.")
  (cssi/upsert-css "componentcss" {:component countdown/Root})
  ;(inspect/app-started! SPA)
  (app/set-root! SPA countdown/Root {:initialize-state? true})
  (dr/initialize! SPA)
  (app/mount! SPA countdown/Root "app" {:initialize-state? false})
  #_(load!))
