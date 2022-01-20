(ns turing-machine.core
  (:require [react]
            [react-dom]
            [cljsjs.create-react-class]
            [clojure.string :as string]
            [sablono.core :as sab :include-macros true]))

(enable-console-print!)

(defprotocol Humanize
  (humanize [self]))

(defprotocol Execute
  (execute [action machine]))

(defrecord Running [state tape cursor])

(defn humanize-machine [app-state]
  (let [{:keys [machine blank-symbol]} app-state
        {:keys [state tape cursor]}    machine
        cells                          (sort (keys tape))
        min                            (first cells)
        max                            (last cells)
        htape                          (->> (range min (inc max))
                                            (map #(if (= % cursor)
                                                    (str "[" (get tape % blank-symbol) "]")
                                                    (get tape % blank-symbol)))
                                            (string/join ""))]
    (str state ": ...0" htape "0...")))

(defrecord Stopped [])

;; Direction could be it's own type
(defrecord Step [symbol direction state]
  Humanize
  (humanize [_]
    (str symbol ({:left "L" :right "R"} direction) state))

  Execute
  (execute [_ machine]
    (let [new-tape   (assoc (:tape machine) (:cursor machine) symbol)
          old-cursor (:cursor machine)
          new-cursor (case direction
                       :left  (dec old-cursor)
                       :right (inc old-cursor))]
      (Running. state new-tape new-cursor))))

(defrecord Halt []
  Humanize
  (humanize [_] "HALT")

  Execute
  (execute [_ machine]
    (Stopped.)))

;; https://en.wikipedia.org/wiki/Turing_machine
;; Using strings here in anticipation of human user input
(def initial-state
  {:states       ["0" "1" "2" "3"]
   :tape-symbols ["0", "1"]
   :blank-symbol "0"

   ;; {state {tape [symbol, direction, state]}}
   :state-table
   {"0" {"0" (Step. "1" :left  "1")
         "1" (Step. "1" :right "0")}
    "1" {"0" (Step. "1" :right "2")
         "1" (Step. "0" :left  "2")}
    "2" {"0" (Step. "1" :right "3")
         "1" (Step. "0" :left  "0")}
    "3" {"0" (Halt.)
         "1" (Halt.)}}

   :machine (map->Running
             {:state  "0"
              :tape   {}
              :cursor 0})})

(defn step [app-state]
  (let [{:keys [machine state-table blank-symbol]} app-state
        {:keys [state tape cursor]}                machine
        symbol                                     (get tape cursor blank-symbol)
        action                                     (get-in state-table [state symbol])
        new-machine                                (execute action machine)]
    (assoc app-state :machine new-machine)))

(defn step-until-halt [initial-state]
  (take-while (fn [{:keys [machine]}] (instance? Running machine))
              (iterate step initial-state)))

(assert (= (map->Running
            {:state  "1"
             :tape   {0 "1"}
             :cursor -1})
           (execute (Step. "1" :left  "1")
                    (map->Running
                     {:state  "0"
                      :tape   {}
                      :cursor 0}))))

;; Take 25 to avoid infinite loop if we never halt
(assert (= 8
           (->> (step-until-halt initial-state)
                (take 25)
                count)))

(defonce app-state (atom initial-state))

(defn hello-world [state]
  (let [steps    (step-until-halt @state)
        snapshot (last steps)
        table    (:state-table snapshot)]
    (sab/html
     [:div
      [:h1 "Turing machine"]
      [:p [:a {:href "https://github.com/logaan/turing-machine"} "Source on Github"]]
      [:pre
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
                 (humanize cell)]))])]]]
      [:h2 "Run log"]
      [:ol
       (for [[i step] (map list (range) steps)]
         [:li {:key i} [:pre (humanize-machine step)]])]])))

;; React interop and boostrapping
(def class
  (js/createReactClass
   #js {:render #(hello-world app-state)}))

(def component
  (js/ReactDOM.render
   (js/React.createElement class #js {})
   (. js/document (getElementById "app"))))

(add-watch app-state :force-update #(.forceUpdate component))
