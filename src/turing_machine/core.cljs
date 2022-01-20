(ns turing-machine.core
    (:require [react]
              [react-dom]
              [cljsjs.create-react-class]
              [sablono.core :as sab :include-macros true]))

(enable-console-print!)

(def initial-state
  {:text "Hello world!"})

(defonce app-state (atom initial-state))

(defn hello-world [state]
  (sab/html [:div
             [:h1 (:text @state)]
             [:h3 "Edit this and watch it change!"]]))

(defn on-js-reload []
  (reset! app-state initial-state))

;; React interop and boostrapping
(def class
  (js/createReactClass
   #js {:render #(hello-world app-state)}))

(def component
  (js/ReactDOM.render
   (js/React.createElement class #js {})
   (. js/document (getElementById "app"))))

(add-watch app-state :force-update #(.forceUpdate component))
