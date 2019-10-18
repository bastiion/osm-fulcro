(ns app.ui.mastodon-fulcro
  (:require
    [app.application :refer [SPA]]
    [clojure.string :as str]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.fulcro.dom :as dom :refer [div ul li p h3 button]]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.ui-state-machines :as uism :refer [defstatemachine]]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro-css.css :as css]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.wsscode.common.async-cljs :refer [go-catch <!p <?]]
    [taoensso.timbre :as log]
    [app.mastodon :as mastodon]
    [com.fulcrologic.fulcro.algorithms.react-interop :as interop]
    [com.wsscode.pathom.connect :as pc]
    [com.wsscode.pathom.core :as p]
    [clojure.core.async :as async]
    [com.fulcrologic.fulcro.networking.http-remote :as net]))

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



(defn toot->geoToot [toot]
  (merge toot (extractGeoURI (:content toot)))
  )

(defmutation new-toot [{:keys [toot]}]
  (action [{:keys [state]}]
          (let [tootWG (toot->geoToot toot)]
            (do
              (prn "loaded")
              (swap! state into {:mastodon.toot/timeline (vec (conj (:mastodon.toot/timeline @state) tootWG))}))
            )))

(defonce toots (atom []))


(def startStreamPublicTimeline
  (mastodon/streamPublicTimeline
    (fn [toot]
      (let [tootWG (toot->geoToot toot)]
        (swap! toots conj tootWG)))
    (fn [toot]
      (do
        (prn "Will call mutation")
        (comp/transact! app.application/SPA [(new-toot {:toot toot})])
        ))))



(pc/defresolver mastodon-public-timeline [env {:keys []}]
  {::pc/output [:mastodon.toot/toots]}
  (go-catch
    {:mastodon.toot/toots
     (->
       app.mastodon/masto-cli <!p
       (.fetchPublicTimeline)
       (.next) <!p
       (js->clj :keywordize-keys true)
       (:value)
       )}))



(def mastodon-parser
  (p/parallel-parser
    {::p/env     {::p/reader               [p/map-reader
                                            pc/parallel-reader
                                            pc/open-ident-reader
                                            p/env-placeholder-reader]
                  ::p/placeholder-prefixes #{">"}}
     ::p/plugins [(pc/connect-plugin {::pc/register
                                      [mastodon-public-timeline]})
                  p/error-handler-plugin
                  p/trace-plugin]}))

(defn key-by [fk ms]
  (into {} (map (fn [m] [(get m fk) m]) ms)))

(defmutation augment-toots [{:mastodon.toot/keys [toots]}]
  (action [{:keys [state]}]
          (swap! state into {:mastodon.toot/timeline toots}))
  )

(defmutation get-normalized-mastodon-public-timeline [{:keys []}]
  (action [{:keys [state]}]
          (async/take! (mastodon-parser {::p/entity (atom {})} [:mastodon.toot/toots])
                         (fn [toots]
                           (do
                             (js/console.log "Will mutate")
                             (comp/transact! app.application/SPA
                                     `[(augment-toots {:mastodon.toot/toots ~(key-by :id (:mastodon.toot/toots toots #_(map toot->geoToot toots)))})]
                                             {:mastodon.toot/timeline {:loading {}}}
                                             ))))))

(defmutation get-mastodon-public-timeline [{:keys []}]
  (action [{:keys [state]}]
          (async/take! (mastodon-parser {::p/entity (atom {})} [:mastodon.toot/toots])
                       (fn [toots]
                         (do
                           (js/console.log "Will mutate")
                           (comp/transact! app.application/SPA
                                           `[(augment-toots {:mastodon.toot/toots ~(:mastodon.toot/toots toots #_(map toot->geoToot toots))})]
                                           {:mastodon.toot/timeline {:loading {}}}
                                           ))))))
