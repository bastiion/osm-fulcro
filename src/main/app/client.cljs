(ns app.client
  (:require
    [app.application :refer [SPA]]
    [com.fulcrologic.fulcro.application :as app]
    [app.ui.numbers :as ui4]
    [app.ui.root :as ui]
    [app.ui.ui-union :as ui3]
    [app.ui.toot-list :as ui1]
    [app.ui.countdown-many :as ui2]
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
    ))

#_(defn load! []
  (df/load! SPA :geojson.vvo/geojson #_app.ui.numbers/GeoJSON nil
            {:target [:data :vvo]
             :post-action #(app/schedule-render! SPA {:force-root? true})}))

(defn load! []
  (df/load! SPA :geojson.vvo/geojson nil {:remote :overpass}))

(defn load-timeline! []
  (df/load! SPA :mastodon.toot/timeline nil {:remote :mastodon}))

(defn ^:export refresh []
  (log/info "Hot code Remount")
  (cssi/upsert-css "componentcss" {:component ui/Root})
  (app/mount! SPA ui/Root "app")
  #_(load!))

(defn ^:export init []
  (log/info "Application starting.")
  (cssi/upsert-css "componentcss" {:component ui/Root})
  ;(inspect/app-started! SPA)
  (app/set-root! SPA ui/Root {:initialize-state? true})
  (dr/initialize! SPA)
  (app/mount! SPA ui/Root "app" {:initialize-state? false})
  (load!)
  (load-timeline!)
  )
