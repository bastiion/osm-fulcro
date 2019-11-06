(ns app.background-geolocation
  (:require [com.fulcrologic.fulcro.components :as comp]
            [app.application :refer [SPA]]
            [app.ui.leaflet.state :refer [new-sensor-data new-location-data]]
            ))


#_(def bg-properties [
    "CY_MEDIUM"
    "DESIRED_ACCURACY_LOW"
    "DESIRED_ACCURACY_VERY_LOW"
    "DESIRED_ACCURACY_THREE_KILOMETER"
    "AUTHORIZATION_STATUS_NOT_DETERMINED"
    "AUTHORIZATION_STATUS_RESTRICTED"
    "AUTHORIZATION_STATUS_DENIED"
    "AUTHORIZATION_STATUS_ALWAYS"
    "AUTHORIZATION_STATUS_WHEN_IN_USE"
    "NOTIFICATION_PRIORITY_DEFAULT"
    "NOTIFICATION_PRIORITY_HIGH"
    "NOTIFICATION_PRIORITY_LOW"
    "NOTIFICATION_PRIORITY_MAX"
    "NOTIFICATION_PRIORITY_MIN"
    "ACTIVITY_TYPE_OTHER"
    "ACTIVITY_TYPE_AUTOMOTIVE_NAVIGATION"
    "ACTIVITY_TYPE_FITNESS"
    "ACTIVITY_TYPE_OTHER_NAVIGATION"
    "PERSIST_MODE_ALL"
    "PERSIST_MODE_LOCATION"
    "PERSIST_MODE_GEOFENCE"
    "PERSIST_MODE_NONE"
    "deviceSettings"
    "logger"
    "ready"
    "configure"
    "reset"
    "requestPermission"
    "getProviderState"
    "onLocation"
    "onMotionChange"
    "onHttp"
    "onHeartbeat"
    "onProviderChange"
    "onActivityChange"
    "onGeofence"
    "onGeofencesChange"
    "onSchedule"
    "onEnabledChange"
    "onConnectivityChange"
    "onPowerSaveChange"
    "onNotificationAction"
    "on"
    "un"
    "removeListener"
    "removeListeners"
    "getState"
    "start"
    "stop"
    "startSchedule"
    "stopSchedule"
    "startGeofences"
    "startBackgroundTask"
    "stopBackgroundTask"
    "finish"
    "changePace"
    "setConfig"
    "getLocations"
    "getCount"
    "destroyLocations"
    "clearDatabase"
    "insertLocation"
    "sync"
    "getOdometer"
    "resetOdometer"
    "setOdometer"
    "addGeofence"
    "removeGeofence"
    "addGeofences"
    "removeGeofences"
    "getGeofences"
    "getGeofence"
    "geofenceExists"
    "getCurrentPosition"
    "watchPosition"
    "stopWatchPosition"
    "registerHeadlessTask"
    "setLogLevel"
    "getLog"
    "destroyLog"
    "emailLog"
    "isPowerSaveMode"
    "getSensors"
    "playSound"
    "transistorTrackerParams"
    "test"])

(def bg js/window.BackgroundGeolocation)



(defn bg-ready!
  []
  (.ready
    bg
    (clj->js {
              :debug             true
              :logLevel          bg.LOG_LEVEL_VERBOSE
              :stopTimeout       1
              :distanceFilter    10
              :desiredAccuracy   bg.DESIRED_ACCURACY_HIGH
              ;:params (.transistorTrackerParams bg js/window.device)
              :stopOnTerminate   false
              :startOnBoot       true
              :foregroundService true}
      ),
    (fn [state]
      (js/console.log "success " state)
      )
    )
  )

(defn listenForEvents!
  []
  (.onLocation
    bg
    (fn [location]
      #(js/console.log "location" location)
      (comp/transact! SPA [(new-location-data {:values (js->clj location :keywordize-keys true) :sensor_type "LOCATION"})]))
    )
  (.onMotionChange
    bg
    (fn [motion] (comp/transact! SPA [(new-location-data {:values (js->clj motion :keywordize-keys true) :sensor_type "MOTION"})]))
    )
  (.onProviderChange
    bg
    (fn [provider] (comp/transact! SPA [(new-location-data {:values (js->clj provider :keywordize-keys true) :sensor_type "PROVIDER"})]))
    )

  )


(defn intervalLocation [interval]
  (.setTimeout js/window
               #(.then (.getCurrentPosition bg)
                       (fn [position]
                         (intervalLocation interval)
                         )
                       )
               interval
               ))


(defn bg-prepare!
  []
  (when (some? bg)
    (listenForEvents!)
    (bg-ready!)
    (intervalLocation 8000)

    #_(.addEventListener js/document "deviceready"
                       #(do
                          (listenForEvents!)
                          (bg-ready!)))))
