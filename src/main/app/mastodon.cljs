(ns app.mastodon
  (:require
            ["masto" :refer [Masto]]
            )
  )



(def mastodon-config
  {:access_token        "o5uzMZlKayXoaqHfOWMfvfr2rR6E1bX5FarYHWpvMOo"
   ;; account number you see when you log in and go to your profile
   ;; e.g: https://mastodon.social/web/accounts/294795
   :account-id          "1"
   :api_url             "https://social.gra.one/api/v1/"
   :instance_url        "https://social.gra.one"
   :streaming_url        "https://media-social.gra.one"
   ;; optional boolean to mark content as sensitive
   :sensitive?          true
   ;; optional boolean defaults to false
   ;; only sources containing media will be posted when set to true
   :media-only?         true
   ;; optional visibility flag: direct, private, unlisted, public
   ;; defaults to public
   :visibility          "unlisted"
   ;; optional limit for the post length
   :max-post-length     300
   ;; optional flag specifying wether the name of the account
   ;; will be appended in the post, defaults to false
   :append-screen-name? false
   ;; optional signature for posts
   :signature           "#newsbot"
   ;; optionally try to resolve URLs in posts to skip URL shorteners
   ;; defaults to false
   :resolve-urls?       true
   ;; optional content filter regexes
   ;; any posts matching the regexes will be filtered out
   :content-filters     [".*bannedsite.*"]
   ;; optional keyword filter regexes
   ;; any posts not matching the regexes will be filtered out
   :keyword-filters     [".*"]}
  )


(defn exit-with-error [error]
      (js/console.error error))


(defn js->edn [data]
      (js->clj data :keywordize-keys true))


(def masto-cli (.login Masto #js {
                                  :uri (:instance_url mastodon-config)
                                  :version "v2"
                                  :streamingApiUrl (:streaming_url mastodon-config)
                                  :accessToken (:access_token mastodon-config)
                                  }))


(defn streamPublicTimeline [onNotification onUpdate]
      (.then masto-cli
             (fn [cli]
                 (.then (.streamPublicTimeline cli)
                        (fn [stream]
                            (.on stream "update"
                                 #(let [response (-> % js->edn)]
                                       (onUpdate response)))
                            (.on stream "notification"
                                 #(let [response (-> % js->edn)]
                                       (onNotification response))))))))


(defn getPublicTimeline [callback]
      (.then masto-cli
             (fn [cli]
                 (let [res (.fetchPublicTimeline cli)]
                      (if res
                        (.then (.next res)
                          #(let [response (-> % js->edn)]
                                (callback (:value response))))
                        (callback [])
                        )))))





