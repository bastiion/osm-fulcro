(ns app.ui.root
  (:require
    [clojure.string :as str]
    [goog.string :as gstring]
    [goog.string.format]
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
    [cljs-time.core :as tc]
    [cljs-time.local :as tl]
    [cljs-time.format :as tf]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.algorithms.react-interop :as interop]
    #_["react-markdown-editor" :as ReactMarkdownEditor]
    ["leaflet" :as l]
    ["react-mde" :as ReactMde]
    ["react-leaflet" :as ReactLeaflet :refer [withLeaflet Map
                                              LayersControl LayersControl.BaseLayer LayersControl.Overlay
                                              TileLayer Marker Popup]]
    ["react-leaflet-vectorgrid" :as VectorGrid]
    ["d3" :as d3]
    ["d3-shape" :as d3-shape]
    ["react-leaflet-d3" :refer [HexbinLayer]]
    ["react-leaflet-d3-svg-overlay" :as D3SvgOverlay]))


(defn arc [kwargs]
  ((d3-shape/arc) (clj->js kwargs)))

(def leafletMap (interop/react-factory Map))
(def layersControl (interop/react-factory LayersControl))
(def layersControlBaseLayer (interop/react-factory LayersControl.BaseLayer))
(def layersControlOverlay (interop/react-factory LayersControl.Overlay))
(def tileLayer (interop/react-factory TileLayer))
(def vectorGrid (interop/react-factory (withLeaflet VectorGrid)))
(def hexbinLayer (interop/react-factory (withLeaflet HexbinLayer)))
(def d3SvgOverlay (interop/react-factory (withLeaflet (.-ReactLeaflet_D3SvgOverlay D3SvgOverlay))))
(def marker (interop/react-factory Marker))
(def popup (interop/react-factory Popup))
#_(def markdownEditor (interop/react-factory (.MarkdownEditor ReactMarkdownEditor)))
#_(def reactMde (interop/react-factory ReactMde))
(def reactMde (com.fulcrologic.fulcro.components/factory ReactMde))

(def hexbinOptions {:colorScaleExtent  [1 nil]
                    :radiusScaleExtent [1 nil]
                    :colorRange        ["#ffffff" "#00ff00"]
                    :radiusRange       [5 12]})

(defn color-by-accessibility [d]
  ({"yes"     "green"
    "no"      "red"
    "limited" "yellow"}
   (get-in (js->clj d :keywordize-keys true)
           [:properties :wheelchair])
   "blue"))

(defn lngLat->Point [proj [lng lat]]
  (.latLngToLayerPoint proj (clj->js {:lat lat :lng lng})))

(defn bounds->circumcircleRadius [proj bounds]
  (some->> bounds
           (map (partial lngLat->Point proj))
           (#(.subtract (first %) (second %)))
           (#(js/Math.sqrt (+ (* (.-x %) (.-x %))
                              (* (.-y %) (.-y %)))))
           (* 0.5)))

(defn d3DrawCallback [sel proj data]
  (let [radius (->> (js->clj data :keywordize-keys true)
                    first
                    :bounds
                    (bounds->circumcircleRadius proj))
        numbers (map :n (js->clj data :keywordize-keys true))
        arcs ((js/d3.pie) (clj->js numbers))
        arc-data (map-indexed (fn [i d] (assoc d :path (arc (merge {:innerRadius (* 0.5 radius) :outerRadius radius}
                                                                   (js->clj (nth arcs i) :keywordize-keys true)))))
                              (js->clj data :keywordize-keys true))
        upd (-> sel
                (.selectAll "a")
                (.data (clj->js arc-data)))]
    (-> (.enter upd)
        (.append "a")
        (.append "path")
        (.attr "transform" (fn [d] (let [[lng lat] (get-in (js->clj d :keywordize-keys true)
                                                           [:geometry :coordinates])
                                         latLng (clj->js {:lat lat :lng lng})
                                         point (.latLngToLayerPoint proj latLng)]
                                     (str "translate(" (.-x point) ","
                                          (.-y point) ")"))))
        (.attr "d" #(:path (js->clj % :keywordize-keys true)))
        (.attr "fill" #(:color (js->clj % :keywordize-keys true)))
        (.attr "fill-opacity" "0.5")
        (.on "click" (fn [d i ds] (js/console.log (js->clj d)))))))

(defsc GeoJSON
  "a GeoJSON dataset"
  [this {:as props}]
  {:query [:type :features :timestamp :generator :copyright]})

(comment
  (def example-account
    {:acct            "echo_pbreyer@chaos.social",
     :emojis          [],
     :bot             true,
     :following_count 2,
     :avatar_static
                      "https://social.gra.one/system/accounts/avatars/000/000/024/original/9488f280cf3134d2.png?1571220642",
     :fields          [],
     :username        "echo_pbreyer",
     :header_static   "https://social.gra.one/headers/original/missing.png",
     :statuses_count  533,
     :header          "https://social.gra.one/headers/original/missing.png",
     :note
                      "<p>Inoffizieller Spiegel von echo_pbreyer@twitter.com. <br>Offizieller Account von Dr. Patrick Breyer im Fediverse: <a href=\"https://pirati.cc/patrickbreyer\" rel=\"nofollow noopener\" target=\"_blank\"><span class=\"invisible\">https://</span><span class=\"\">pirati.cc/patrickbreyer</span><span class=\"invisible\"></span></a></p>",
     :locked          false,
     :id              "24",
     :avatar
                      "https://social.gra.one/system/accounts/avatars/000/000/024/original/9488f280cf3134d2.png?1571220642",
     :url             "https://chaos.social/@echo_pbreyer",
     :display_name    "patrickbreyer@pirati.cc",
     :followers_count 66,
     :created_at      "2019-10-16T10:10:43.065Z"}))

(defn optimal-ago [interval]
  (let [ms-ago (tc/in-millis interval)]
    (if (< 1000 ms-ago)
      (if (< (* 60 1000) ms-ago)
        (if (< (* 60 60 1000) ms-ago)
          (if (< (* 24 60 60 1000) ms-ago)
            {:ago (tc/in-days interval) :label "days"}
            {:ago (tc/in-hours interval) :label "hours"}
            )
          {:ago (tc/in-minutes interval) :label "minutes"}
          )
        {:ago (tc/in-seconds interval) :label "seconds"}
        )
      {:ago (tc/in-millis interval) :label "milliseconds"}
      ))
  )

(defsc MastodonAccount
  "an avatar in the fediverse"
  [this {:as props}]
  {:query [:id :acct :note :username :avatar :display_name :url]}
  )

(defsc MastodonToot
  "the public timeline of a mastodon endpoint"
  [this {:as props}]
  {:query [:id :uri :url :content :language :created_at :favourites_count
           {:account (comp/get-query MastodonAccount)}]})

(defsc MastodonTootEvent
  "a semantic ui event item representing a toot"
  [this {:keys [id uri url content language created_at favourites_count account] :as props}]
  {:query [:id :uri :url :content :language :created_at :favourites_count
           {:account (comp/get-query MastodonAccount)}]}
  (dom/div :.event
           (dom/div :.label (dom/img {:src (:avatar account)}))
           (dom/div :.content
                    (dom/div :.summary
                             (dom/span
                               {:dangerouslySetInnerHTML {:__html content}})
                             (dom/div :.date (let [ago (optimal-ago (tc/interval
                                                                      (tl/from-local-string created_at)
                                                                      (tc/now)
                                                                      ))]

                                               (str (:ago ago) " " (:label ago) " ago")
                                               ))

                             )


                    (dom/div :.meta (dom/a :.like (dom/i :.like.icon) (str favourites_count " Likes")))
                    ) #_(comp/computed i {:onSelect onSelect}
                                       ))

  )

(comment
  (def example-card
    {:description
                    "Ein Foto sorgt für Empörung im Netz: Es zeigt Windkraftanlagen mitten im Wald, für die offenbar viele Bäume gefällt wurden. Nur stammt das Bild gar nicht aus Deutschland. Und auch die Behauptung, der gerodete Wald hätte mehr CO2 aufgenommen als die Windkraftanlage einspare, ist falsch. ",
     :author_url "https://correctiv.org/team/alice-echtermann/",
     :width 400,
     :type "link",
     :embed_url "",
     :title
                    "Nein, der abgeholzte Wald für eine Windkraftanlage nimmt nicht mehr CO2 auf, als die Anlage vermeiden kann",
     :provider_name "correctiv.org",
     :url
                    "https://correctiv.org/faktencheck/wirtschaft-und-umwelt/2019/09/27/eine-windkraftanlage-spart-mehr-co2-als-der-wald-der-fuer-sie-gerodet-wird",
     :author_name "Alice Echtermann",
     :image
                    "https://social.gra.one/system/preview_cards/images/000/000/018/original/fd843fc72b22fd99.png?1571222979",
     :provider_url "https://correctiv.org",
     :height 268,
     :html ""}

    )
  )

(defsc MastodonTootMediaCard
  "a card belongs to a toot"
  [this {:keys [width height author_url type image provider_url] :as props}]
  {:query [:width :height :author_url :type :image :provider_url]}
  )

(defsc MastodonTootCard
  "a semantic ui event item representing a toot"
  [this {:keys [id uri url content language created_at favourites_count account card] :as props}]
  {:query [:id :uri :url :content :language :created_at :favourites_count
           {:account (comp/get-query MastodonAccount)}
           {:card (comp/get-query MastodonTootMediaCard)}
           ]}
  (dom/div
    :.ui.card
    {:id (str "card-" id)}
    (dom/div
      :.content
      (dom/div :.right.floated.meta
               (let [ago (optimal-ago (tc/interval
                                        (tl/from-local-string created_at)
                                        (tc/now)
                                        ))]

                 (str (:ago ago) " " (:label ago) " ago")
                 ))
      (dom/img :.ui.avatar.image {:src (:avatar account)})
      (:display_name account)
      (dom/span
        {:dangerouslySetInnerHTML {:__html content}})
      )
    (if (nil? card)
      []
      (dom/div :.image (dom/img {:src (:image card)  #_(str "https://api.adorable.io/avatars/285/" (:acct account) ".png")})))
    (dom/div
      :.content
      (dom/span
        :.right.floated
        (dom/i :.heart.outline.like.icon)
        (str favourites_count " Likes")
        )
      (dom/i :.comment.icon)
      "3 comments")
    (dom/div
      :.extra.content
      {:id (str "abId0." id)}
      (dom/div
        :.ui.large.transparent.left.icon.input
        {:id (str "abId1." id)}
        (dom/i :.heart.outline.icon)
        (dom/input {:type "text", :placeholder "Add Comment..."}))))

  )
(def ui-mastodon-toot-card (comp/factory MastodonTootCard {:key-fn :id}))

(def ui-mastodon-toot-event (comp/factory MastodonTootEvent {:key-fn :id}))


(defmutation current-point-select [props]
  (action [{:keys [state]}]
          (swap! state into {:selected/latlng props})))

(defmutation set-current-toot-text [{:keys [value]}]
  (action [{:keys [state]}]
          (swap! state into {:selected/latlng (merge (:selected/latlng state) {:tootText value})})))

(defsc CurrentMarker
  "a marker indicating the current selection"
  [this {:keys [lat lng tootText ] :as props}]
  {:query         [:lat :lng :tootText]
   :initial-state (fn [params] {:lat 51.055 :lng 13.74 :tootText "Test"})
   }
  (marker {:position [lat lng]
           :icon     (.icon. l (clj->js
                                 {
                                  :iconUrl     " https://cdn.rawgit.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png"
                                  :shadowUrl   "https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png"
                                  :iconSize    [25, 41]
                                  :iconAnchor  [12, 41]
                                  :popupAnchor [1, -34]
                                  :shadowSize  [41, 41]
                                  }

                                 ))
           }
          (popup {}
                 (dom/div
                   :.ui.card
                   {:id "abId0.5099070158082626"}
                   (dom/div
                     :.content
                     (dom/div :.right.floated.meta "14h")
                     (dom/img :.ui.avatar.image {:src (str "https://api.adorable.io/avatars/285/tele@porter.de.png")})
                     "My Avatar")
                   (dom/div :.image (dom/img {:src "http://lorempixel.com/output/nature-q-g-640-480-5.jpg"}))
                   (dom/div
                     :.content
                     (dom/span
                       :.right.floated
                       (dom/i :.heart.outline.like.icon)
                       "17 likes")
                     (dom/i :.comment.icon)
                     "0 comments")
                   (dom/div
                     :.extra.content
                     {:id "abId0.7625026767361125"}
                     (dom/div
                       :.ui.large.transparent.left.icon.input
                       {:id        "abId0.7424083382566915",
                        :abineguid "A005719A21D54D2889360C7FFB91C9F2"}
                       (dom/i :.heart.outline.icon)
                       (if (nil? tootText)
                         []
                         (dom/input {:id "sadsadedede22" :type "text" :value tootText :onChange (fn [event] (comp/transact! this '[(set-current-toot-text ~{:value (.value event)})])) :placeholder "Add Comment..."})))))
                 ))

  )

(def ui-current-marker (comp/factory CurrentMarker))

(defsc OSM
  [this {:geojson.vvo/keys [geojson] :mastodon.toot/keys [timeline] :selected/keys [latlng] :as props}]
  {:query             [{:geojson.vvo/geojson (comp/get-query GeoJSON)}
                       {:mastodon.toot/timeline (comp/get-query MastodonToot)}
                       {:selected/latlng (comp/get-query CurrentMarker)}
                       ]
   :initial-state     (fn [params]
                        {:geojson.vvo/geojson    []
                         :mastodon.toot/timeline []
                         :selected/latlng        (comp/get-initial-state CurrentMarker)})
   :componentDidMount (fn [params] (do
                                     (prn params)
                                     ))
   }
  [(dom/div
     :.ui.large.top.fixed.hidden.menu
     (dom/div
       :.ui.container
       (dom/a :.active.item "Home")
       (dom/a :.item "New Toot")
       (dom/a :.item "Timeline")
       (dom/a :.item "Hashtags")
       (dom/div
         :.right.menu
         (dom/div :.item (dom/a :.ui.button "Log in"))
         (dom/div :.item (dom/a :.ui.primary.button "Sign Up")))))
   (dom/div
     :.ui.vertical.inverted.sidebar.menu
     (dom/a :.active.item "Home")
     (dom/a :.item "New Toot")
     (dom/a :.item "Timeline")
     (dom/a :.item "Hashtags")
     (dom/a :.item "Login")
     (dom/a :.item "Signup"))
   (dom/div
     :.pusher
     (dom/div
       :.ui.inverted.vertical.masthead.center.aligned.segment
       {:style {:padding 0}}
       (dom/div
         :.ui.container
         (dom/div
           :.ui.large.secondary.inverted.pointing.menu
           (dom/a :.toc.item (dom/i :.sidebar.icon))
           (dom/a :.active.item "Home")
           (dom/a :.item "New Toot")
           (dom/a :.item "Timeline")
           (dom/a :.item "Hashtags")
           (dom/div
             :.right.item
             (dom/a :.ui.inverted.button "Log in")
             (dom/a :.ui.inverted.button "Sign Up"))))
       (dom/div

         (leafletMap  {
                       :className "leaflet-map"
                      :center  [51.055 13.74] :zoom 12
                      :onClick (fn [event] (let [latlng (:latlng (js->clj event :keywordize-keys true))]
                                             (comp/transact! this `[(current-point-select
                                                                      ~{:lat (:lat latlng)
                                                                        :lng (:lng latlng)
                                                                        })])

                                             ))
                      }
                     (layersControl {}
                                    (layersControlBaseLayer {:name "Esri Aearial" :checked true}
                                                            (tileLayer {:url         "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}.png"
                                                                        :attribution "&copy; <a href=\"http://esri.com\">Esri</a>, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community"}))
                                    (layersControlBaseLayer {:name "OSM Tiles"}
                                                            (tileLayer {:url         "https://{s}.tile.osm.org/{z}/{x}/{y}.png"
                                                                        :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors"}))
                                    (layersControlBaseLayer {:name "PublicTransport (MeMOMaps)"}
                                                            (tileLayer {:url         "https://tileserver.memomaps.de/tilegen/{z}/{x}/{y}.png"
                                                                        :attribution "<a href=\"https://memomaps.de\">MeMOMaps"}))
                                    (layersControlBaseLayer {:name "PublicTransport (openptmap)"}
                                                            (tileLayer {:url         "http://openptmap.org/tiles/{z}/{x}/{y}.png"
                                                                        :attribution "<a href=\"https://wiki.openstreetmap.org/wiki/Openptmap\">Openptmap"}))
                                    (layersControlBaseLayer {:name    "NONE (only overlays)"
                                                             :checked true}
                                                            (tileLayer {:url ""}))
                                    (layersControlOverlay {:name "Graphhopper MVT example"}
                                                          (vectorGrid {:type                  "protobuf" :url "http://localhost:8989/mvt/{z}/{x}/{y}.mvt" :subdomains ""
                                                                       :vectorTileLayerStyles {"roads" (fn [properties zoom] {})}
                                                                       :zIndex                1}))
                                    (layersControlOverlay {:name "GeoJSON example"}
                                                          (if (:features geojson)
                                                            (vectorGrid {:type "slicer" :zIndex 1 :data geojson})))
                                    (layersControlOverlay {:name "GeoJSON D3 Hexbin example"}
                                                          (if (:features geojson)
                                                            (hexbinLayer (merge {:data geojson #_#_:zIndex 1} hexbinOptions))))

                                    (layersControlOverlay {:name "test" :checked true}
                                                          (if (:lat latlng)
                                                            (ui-current-marker latlng))
                                                          )
                                    (layersControlOverlay {:name "mastodon public timeline overlay" :checked true}
                                                          (map (fn [toot]
                                                                 (let [geotoot (app.ui.mastodon-fulcro/toot->geoToot toot)]
                                                                   (if (:lat geotoot)
                                                                     (marker {:key      (:id geotoot)
                                                                              :position [(:lat geotoot) (:long geotoot)]
                                                                              :icon     (.icon. l (clj->js
                                                                                                    {
                                                                                                     :iconUrl  (:avatar (:account toot))
                                                                                                     :iconSize [40 40]
                                                                                                     }))
                                                                              }
                                                                             (popup {}
                                                                                    (dom/div :.ui.feed
                                                                                             (ui-mastodon-toot-card toot))
                                                                                    ))))
                                                                 )
                                                               timeline))
                                    #_(layersControlOverlay {:name "GeoJSON D3 SvgOverlay example" :checked false}
                                                            (if (:features geojson)
                                                              (d3SvgOverlay {:data         (->> (:features geojson)
                                                                                                (filter #(and (#{"Point"} (get-in % [:geometry :type]))
                                                                                                              (#{"stop_position"} (get-in % [:properties :public_transport]))))
                                                                                                ;; group features
                                                                                                ((fn [features]
                                                                                                   (let [centroid (js/d3.geoCentroid (clj->js {:type "FeatureCollection" :features features}))
                                                                                                         bounds (js/d3.geoBounds (clj->js {:type "FeatureCollection" :features features}))]
                                                                                                     (map (fn [[color ds]]
                                                                                                            (let [d (first ds)
                                                                                                                  coords (get-in (js->clj d :keywordize-keys true)
                                                                                                                                 [:geometry :coordinates])]
                                                                                                              {:geometry {:coordinates centroid}
                                                                                                               :color    color
                                                                                                               :n        (count ds)
                                                                                                               :bounds   bounds}))
                                                                                                          (group-by color-by-accessibility features))))))
                                                                             :drawCallback d3DrawCallback})))))


         ))




     (dom/div :.ui.main.text.container {:style {:width "100%" :padding-top "55px"}}


              (dom/h1 :.ui.header "New GEO Toot")
              (dom/div
                (dom/input :.ui.input {:value
                                       (if (:lat latlng)
                                         (str "geo:" (gstring/format "%.5f" (:lat latlng)) "," (gstring/format "%.5f" (:lng latlng)) "?z=15")
                                         (str "geo:0,0?z=15")
                                         )})
                #_(markdownEditor {:initialContent "Test" :iconSet "font-awesome"})
                #_(reactMde {:value "Test"})
                )

              (dom/h1 :.ui.header "Fediverse Timeline")
              (dom/div
                (dom/div :.ui.feed
                         (map (fn [i]
                                (ui-mastodon-toot-event i)
                                )
                              timeline))))
     (dom/div
       :.ui.inverted.vertical.footer.segment
       (dom/div
         :.ui.container
         (dom/div
           :.ui.stackable.inverted.divided.equal.height.stackable.grid
           (dom/div
             :.three.wide.column
             (dom/h4 :.ui.inverted.header "About")
             (dom/div
               :.ui.inverted.link.list
               (dom/a :.item {:href "#"} "Sitemap")
               (dom/a :.item {:href "#"} "Contact Us")
               (dom/a :.item {:href "#"} "GPL v. 3")
               (dom/a :.item {:href "#"} "Multimodal Routing")))
           (dom/div
             :.three.wide.column
             (dom/h4 :.ui.inverted.header "Services")
             (dom/div
               :.ui.inverted.link.list
               (dom/a :.item {:href "#"} "Routing App beta")
               (dom/a :.item {:href "https://docs.graphhopper.com/"} "Graphopper FAQ")
               (dom/a :.item {:href "http://book.fulcrologic.com/fulcro3/"} "Fulcro Developer Guide")
               (dom/a :.item {:href "https://wilkerlucio.github.io/pathom/"} "Pathom  Developer Guide")))
           (dom/div
             :.seven.wide.column
             (dom/h4 :.ui.inverted.header "Footer Header")
             (dom/p
               "Extra space for a call to action inside the footer that could help re-engage users."))))))
   ]
  )

(def ui-osm (comp/factory OSM))

(defsc Root [this props]
  {:query (fn [] (comp/get-query OSM))}
  (ui-osm props))

#_(app.ui.mastodon-fulcro/startStreamPublicTimeline)
