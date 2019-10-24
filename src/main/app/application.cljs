(ns app.application
  (:require
    [app.helper.query :as queryh]
    [com.fulcrologic.fulcro.networking.http-remote :as net]
    [com.fulcrologic.fulcro.algorithms.tx-processing :as tx]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp]
    [edn-query-language.core :as eql]
    ["osmtogeojson" :as osmtogeojson]
    ["@mapbox/vt2geojson" :as vt2geojson]))

(def secured-request-middleware
  ;; This ensures your client can talk to a CSRF-protected server.
  ;; See middleware.clj to see how the token is embedded into the HTML
  ;; The CSRF token is embedded via server_components/html.clj
  (-> (net/wrap-csrf-token (or js/fulcro_network_csrf_token "TOKEN-NOT-IN-HTML!"))
      (net/wrap-fulcro-request)))

(defn mvt-remote []
  {:active-requests (atom {})
   :transmit!       (fn transmit! [remote {::tx/keys [result-handler update-handler ast] :as send-node}]
                      (let [query (get-in ast [:children 0 :params :query])
                            edn (eql/ast->query ast)]
                        (vt2geojson (clj->js query)
                                    (fn [error result]
                                      (result-handler {:body        {:_ (if error error (js->clj result :keywordize-keys true))}
                                                       :transaction edn
                                                       :status-code (if error 500 200)})))))})

(defn graphhopper-cordova-remote []
  {:active-requests (atom {})
   :transmit!       (fn transmit! [remote {::tx/keys [result-handler update-handler ast] :as send-node}]
                      (let [query (get-in ast [:children 0 :params :query])
                            edn (eql/ast->query ast)]
                        (.calcRoutes js/navigator.graphhopper
                                     (clj->js query)
                                     (fn [result]
                                       (result-handler {:body        {:_ (js->clj result :keywordize-keys true)}
                                                        :transaction edn
                                                        :status-code 200}))
                                     (fn [error]
                                       (result-handler {:body        {:_ error}
                                                        :transaction edn
                                                        :status-code 500}))
                                     )))})


(defn latlng->point
  "converts a map like {:lat \"51.2\" :lng \"13.9\"} to a string point \"51.2,13.9\""
  [point]
  (str (:lat point) "," (:lng point))
  )

(defonce SPA (app/fulcro-app
               {:remotes {:pathom          (net/fulcro-http-remote {:url                "/api"
                                                                    :request-middleware secured-request-middleware})
                          :overpass        (net/fulcro-http-remote
                                             {:url                 "http://overpass-api.de/api/interpreter"
                                              :request-middleware  (fn [req] (let [query (->> req :body first (apply hash-map) :_ :query)]
                                                                               (assoc req :headers {"Content-Type" "text/plain"}
                                                                                          :body (str "[out:json];" (apply str query) "out;"))))
                                              :response-middleware (fn [resp] (let [data (some-> (:body resp)
                                                                                                 js/JSON.parse
                                                                                                 osmtogeojson
                                                                                                 (js->clj :keywordize-keys true))]
                                                                                (assoc resp :body {:_ data})))})
                          :mvt             (mvt-remote)

                          :graphhopper-web (net/fulcro-http-remote
                                             {:url                 "http://localhost:8989/route"

                                              :request-middleware  (fn [req] (let [query (->> req :body first (apply hash-map) :graphhopper/route)
                                                                                   startPoint (latlng->point (:start query))
                                                                                   endPoint (latlng->point (:end query))
                                                                                   ]
                                                                               (js/console.log query)
                                                                               (assoc req :headers {"Content-Type" "text/plain"}
                                                                                          :method :get
                                                                                          :url (str "http://localhost:8989/route" (queryh/get-query-params-str {
                                                                                                                                                                :point          [startPoint
                                                                                                                                                                                 endPoint]
                                                                                                                                                                ; :point "51.02932979285427,13.729509787911619"
                                                                                                                                                                ; :point "51.02328458336499,13.805051393513118"
                                                                                                                                                                :vehicle        "car"
                                                                                                                                                                :locale         "en"
                                                                                                                                                                :calc_points    "true"
                                                                                                                                                                :points_encoded "false"
                                                                                                                                                                :instructions   "false"
                                                                                                                                                                :key            "api_key"

                                                                                                                                                                })))))
                                              :response-middleware (fn [resp] (let [data (some-> (:body resp)
                                                                                                 js/JSON.parse
                                                                                                 (js->clj :keywordize-keys true))
                                                                                    resp1 (assoc resp :body {:graphhopper/route data})]
                                                                                (prn resp1)
                                                                                resp1
                                                                                ))})

                          :graphhopper     (graphhopper-cordova-remote)
                          }}))
