(ns turing-machine.core
    (:require [react]
              [react-dom]
              [cljsjs.create-react-class]
              [sablono.core :as sab :include-macros true]))

(enable-console-print!)

(defprotocol Humanize
  (humanize [self]))

(defrecord Step [tape direction state]
  Humanize
  (humanize [_]
    (str tape ({:left "L" :right "R"} direction) state)))

(defrecord Halt []
  Humanize
  (humanize [_] "HALT"))

;; https://en.wikipedia.org/wiki/Turing_machine
;; Using strings here in anticipation of human user input
(def initial-state
  {:states ["0" "1" "2" "3"]
   :tape-symbols ["0", "1"]
   :blank-symbol "0"
   :initial-state ["0"]
   :state-table
   ;; {state {tape [tape, direction, state]}}
   {"0" {"0" (Step. "1" :left "1")
         "1" (Step. "1" :right "0")}
    "1" {"0" (Step. "1" :right "2")
         "1" (Step. "0" :left "2")}
    "2" {"0" (Step. "1" :right "3")
         "1" (Step. "0" :left "0")}
    "3" {"0" (Halt.)
         "1" (Halt.)}}})

(defonce app-state (atom initial-state))

(defn hello-world [state]
  (let [snapshot @state
        table (:state-table snapshot)]
    (sab/html
     [:div
      [:h1 "Turing machine"]
      [:table
       [:thead
        [:tr
         [:th " "]
         (for [symbol (:tape-symbols snapshot)]
           [:th {:key symbol} symbol])]]
       [:tbody
        (for [state (:states snapshot)]
          [:tr {:key state}
           [:td state]
           (for [symbol (:tape-symbols snapshot)]
             (let [cell (get-in table [state symbol])]
               [:td {:key (str state "-" symbol)}
                (humanize cell)]))])]]])))

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
