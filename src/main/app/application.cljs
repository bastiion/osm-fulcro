(ns app.application
  (:require [com.fulcrologic.fulcro.networking.http-remote :as net]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp]
            [app.mastodon]
            ["osmtogeojson" :as osmtogeojson]
            ))

(def secured-request-middleware
  ;; The CSRF token is embedded via server_components/html.clj
  (->
    (net/wrap-csrf-token (or js/fulcro_network_csrf_token "TOKEN-NOT-IN-HTML!"))
    (net/wrap-fulcro-request)))


(defn extractGeoURI [message]
  (let [re-res (re-find #"geo:(\d+(?:\.\d+)?),(\d+(?:\.\d+))" message)]
    (if (nil? re-res)
      nil
      {
       :lat  (get re-res 1)
       :long (get re-res 2)
       }
      )))
(defn toot->geoToot [toot]
  (merge toot (extractGeoURI (:content toot)))
  )

(def mastodon-remote
  (net/fulcro-http-remote
    {:url                 (str (:api_url app.mastodon/mastodon-config) "timelines/public")
     :request-middleware  (fn [req] (assoc req :headers {"Content-Type" "application/json"}
                                               :method :get
                                               ))

     :response-middleware (fn [resp] (let [data (some-> (:body resp)
                                                        js/JSON.parse
                                                        (js->clj :keywordize-keys true)
                                                        )]
                                       (assoc resp :body {:mastodon.toot/timeline data})))
     }))

(defonce SPA (app/fulcro-app
               {;; This ensures your client can talk to a CSRF-protected server.
                ;; See middleware.clj to see how the token is embedded into the HTML
                :remotes {:remote (net/fulcro-http-remote
                                    {:url                "/api"
                                     :request-middleware secured-request-middleware})

                          :overpass (net/fulcro-http-remote
                                      {:url "http://overpass-api.de/api/interpreter"
                                       :request-middleware (fn [req] (assoc req :headers {"Content-Type" "text/plain"}
                                                                                :body (str "[out:json];"
                                                                                           "area[name=\"Dresden\"]->.city;"
                                                                                           "nwr(area.city)[operator=\"DVB\"]->.connections;"
                                                                                           "node.connections[public_transport=stop_position];"
                                                                                           "out;")))
                                       :response-middleware (fn [resp] (let [data (some-> (:body resp)
                                                                                          js/JSON.parse
                                                                                          osmtogeojson
                                                                                          (js->clj :keywordize-keys true))]
                                                                         (assoc resp :body {:geojson.vvo/geojson data})))})
                          :mastodon mastodon-remote
                          }}))

(comment
  (-> SPA (::app/runtime-atom) deref ::app/indexes))
