(ns app.ui.countdown-many
  (:require
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.wsscode.pathom.connect :as pc]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SERVER:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; CLIENT:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsc Countdown [this {:keys   [counter-label]
                        :ui/keys [count]}]
  {:ident     [:counter-id :counter-id]
   :query     [:counter-id :counter-label :ui/count]
   :pre-merge (fn [{:keys [current-normalized data-tree]}]
                (merge
                  {:ui/count 5}
                  current-normalized
                  data-tree))}
  (dom/div
    (dom/h4 counter-label)
    (let [done? (zero? count)]
      (dom/button {:disabled done?
                   :onClick  #(m/set-value! this :ui/count (dec count))}
        (if done? "Done!" (str count))))))

(def ui-countdown (comp/factory Countdown {:keyfn :counter-id}))

(defsc Root [this {:keys [all-counters]}]
  {:initial-state (fn [_] {})
   :query         [{:all-counters (comp/get-query Countdown)}]}
  (dom/div
    (dom/h3 "Counters")
    (if (seq all-counters)
      (dom/div {:style {:display "flex" :alignItems "center" :justifyContent "space-between"}}
        (mapv ui-countdown all-counters))
      (dom/button {:onClick #(df/load! this :all-counters Countdown)}
        "Load many counters"))))

