(ns cup.app
  (:require [clojure.set :as cs]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def cities {
  "Paris" [2.352222 48.856614]
  "London" [-0.127758 51.507351]
  "Zurich" [8.541694 47.376887]
  "Bern" [7.447447 46.947974]
  "Berlin" [13.404954 52.520007]
  "Princeton" [-74.667223 40.357298]
  "Trieste" [13.776818 45.649526]
  "Rome" [12.496366 41.902784]
  "Vienna" [16.373819 48.208174]
  })

(def people-data '(
  {:name "Mahatma Gandhi"
   :link "https://en.wikipedia.org/wiki/Mahatma_Gandhi"
   :avatar "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d1/Portrait_Gandhi.jpg/400px-Portrait_Gandhi.jpg"
   :color "#FF9800"
   :locations {
    "1888-1891" "London"}}
  {:name "Ernest Hemingway"
   :link "https://en.wikipedia.org/wiki/Ernest_Hemingway"
   :avatar "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bb/Ernest_Hemingway_1923_passport_photo.jpg/440px-Ernest_Hemingway_1923_passport_photo.jpg"
   :color "#8BC34A"
   :locations {
    "1921-1928" "Paris"
    "1944-1945" "London"}}
  {:name "Albert Einstein"
   :link "https://en.wikipedia.org/wiki/Albert_Einstein"
   :avatar "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a0/Einstein_patentoffice.jpg/340px-Einstein_patentoffice.jpg"
   :color "#FF4081"
   :locations {
    "1896-1900" "Zurich"
    "1900-1904" "Bern"
    "1905-1914" "Zurich"
    "1914-1921" "Berlin"
    "1933-1955" "Princeton"
   }}
 {:name "James Joyce"
  :link "https://en.wikipedia.org/wiki/James_Joyce"
  :avatar "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1e/Revolutionary_Joyce_Better_Contrast.jpg/440px-Revolutionary_Joyce_Better_Contrast.jpg"
  :color "#FFC107"
  :locations {
   "1905-1906" "Trieste"
   "1906-1907" "Rome"
   "1907-1909" "Trieste"
   "1915-1920" "Zurich"
   "1920-1940" "Paris"
   "1931-1931" "London"
   "1941-1941" "Zurich"
  }}
  {:name "Salvador Dalí"
   :link "https://en.wikipedia.org/wiki/Salvador_Dal%C3%AD"
   :avatar "https://upload.wikimedia.org/wikipedia/commons/thumb/2/24/Salvador_Dal%C3%AD_1939.jpg/440px-Salvador_Dal%C3%AD_1939.jpg"
   :color "#00BCD4"
   :locations {
    "1925-1929" "Paris"
   }}
  {:name "Sigmund Freud"
   :link "https://en.wikipedia.org/wiki/Sigmund_Freud"
   :avatar "https://upload.wikimedia.org/wikipedia/commons/thumb/1/12/Sigmund_Freud_LIFE.jpg/400px-Sigmund_Freud_LIFE.jpg"
   :color "#4CAF50"
   :locations {
     "1881-1938" "Vienna"
     "1938-1939" "London"
     }}
  {:name "Pablo Picasso"
   :link "https://en.wikipedia.org/wiki/Pablo_Picasso"
   :avatar "https://upload.wikimedia.org/wikipedia/commons/thumb/1/12/Sigmund_Freud_LIFE.jpg/400px-Sigmund_Freud_LIFE.jpg"
   :color "#FFEB3B"
   :locations {
     "1881-1891" "Málaga"
     "1891-1895" "A Coruña"
     "1895-1897" "Barcelona"
     "1897-1900" "Madrid"
     "1900-1901" "Paris"
     "1901-1901" "Madrid"
     "1901-1914" "Paris"
     "1914-1918" "Avignon"
     "1918-1973" "Paris"
     }}
   ))

(defn expand-location [location-item]
  (let [years-str (first location-item)
        location (second location-item)
        years (clojure.string/split years-str #"-")
        start-year (int (first years))
        ; NOTE: we do inc here because of range implementation
        end-year (inc (int (last years)))
        years (range start-year end-year)]
    (doall
      (map
        (fn [year]
          [year location])
        years))))

(defn flatten-one-level [coll]
  (mapcat  #(if (sequential? %) % [%]) coll))

(defn expand-locations [locations]
  (flatten-one-level
    (map expand-location locations)))


(def expanded-people-data
  (doall
    (map
      (fn [person]
        (let [locations (:locations person)
              expanded (expand-locations locations)]
              (assoc person :locations expanded)))
      people-data)))

(defn find-location-for-year [year locations]
  (first
    (filter
      (fn [location]
        (= (int year) (-> location first int)))
      locations)))

(defn people-by-year [people-data year]
  (remove
    (fn [person]
      (nil? (:year person)))
    (map
      (fn [person]
        (let [locations (:locations person)
              ; TODO: for now support one location for one person per year
              location-for-year (find-location-for-year year locations)]
              (if (nil? location-for-year)
                (do
                  ;(println (:name person) "had no location in" year)
                  person)
                (do
                  ;(println (:name person) "was in" (second location-for-year) "in" year)
                  (assoc person :year (first location-for-year)
                                :location (second location-for-year))))))
      people-data)))


(defn create-person-popup [coordinates avatar]
  (let [person-popup (js/mapboxgl.Popup. (js-obj "closeOnClick" false "closeButton" false))
        html (str "<img width='40px' src='" avatar "'>")]
        (do
          (.setLngLat person-popup (clj->js coordinates))
          (.setHTML person-popup html)
          person-popup
          )))

(defn render-person [person]
  (let [coordinates (get cities (:location person))
        ui-control (create-person-popup coordinates (:avatar person))]
    {:name (:name person)
     :control ui-control}))

(defn widget [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/h1 nil (:text data)))))

(defn add-all-to-map [items app-map]
  (doall
    (map
      (fn [item]
        (.addTo (:control item) app-map))
      items)))

(defn remove-all-from-map [items]
  (doall
    (map
      (fn [item]
        (println "removing item" (:name item))
        (.remove (:control item)))
      items)))


(defonce state
  (atom {
      :map nil
      :curr-year 1890
      :prev-year nil
      :curr-people nil
      :prev-people nil
  }))


(defn create-people-controls [people]
  (doall (map render-person people)))

(defn render-for-year [year]
  (println "render for year" year)
  (let [app-map (:map @state)
        people (people-by-year expanded-people-data year)
        people-to-popups (create-people-controls people)]
      (do
        (println "people by year" year people)
        (add-all-to-map people-to-popups app-map)
        people-to-popups)))

(defn filter-people-by-names [people names]
  (filter
    (fn [people]
      (contains? names (:name people)))
    people))



(defn render-people [new-year]
  (println "render people on the map for" new-year)
  (if-let [prev-year (:prev-year @state)]
    ; do something when prev year exists
    (do
      (println "previous data found. more comple logic is coming")
      (let [app-map (:map @state)
            people (people-by-year expanded-people-data new-year)
            prev-people (:prev-people @state)
            prev-names (set (map :name prev-people))
            new-names (set (map :name people))
            names-to-delete (cs/difference prev-names new-names)
            names-to-create (cs/difference new-names prev-names)
            names-to-remain (cs/intersection new-names prev-names)
            people-to-popups-to-remove (filter-people-by-names prev-people names-to-delete)
            people-to-create (filter-people-by-names people names-to-create)
            created-people-to-popups (create-people-controls people-to-create)
            remained-people-to-popups (filter-people-by-names prev-people names-to-remain)
            new-curr-people (concat remained-people-to-popups created-people-to-popups)]
        (println "people for the prev year" prev-names prev-year)
        (println "people for the new year" new-names new-year)
        (println "names to delete" names-to-delete (count people-to-popups-to-remove))
        (println "names to remain" names-to-remain (count remained-people-to-popups))
        (println "names to create" names-to-create  (count created-people-to-popups))
        (println "new current people size" (count new-curr-people))
        (add-all-to-map created-people-to-popups app-map)
        (remove-all-from-map people-to-popups-to-remove)
        (swap! state assoc
                :curr-year new-year
                :curr-people new-curr-people)
      )
      )
    ; first-time render
    (let [people-to-popups (render-for-year new-year)]
      (swap! state assoc
              :curr-year new-year
              :curr-people people-to-popups))))

(defn year-change-handler [e]
  (println "attached listener")
  (let [new-year (dommy/value (sel1 :#years))
        curr-year (:curr-year @state)
        curr-people (:curr-people @state)]
    (println "year-changed from" curr-year "to" new-year state)
    (println "changed prev year and prev people" curr-people)
    (println "transaction. before" (:curr-year @state) (:prev-year @state) (map :name (:prev-people @state)))
    (swap! state assoc
            :curr-year new-year
            :prev-year curr-year
            :curr-people []
            :prev-people curr-people)
    (println "transaction. after" (:curr-year @state) (:prev-year @state) (map :name (:prev-people @state)))
    (render-people new-year)))

(defn create-map []
  (aset js/mapboxgl "accessToken" "pk.eyJ1IjoiaGFzaG9iamVjdCIsImEiOiJjaWh0ZWU4MjkwMTdsdGxtMWIzZ3hnbnVqIn0.RQjfkzc1hI2UuR0vzjMtJQ")
  (let [props (js-obj "container" "map"
                      "zoom" 1
                      "center" (clj->js [12.496366 41.902784])
                      "style" "mapbox://styles/mapbox/streets-v8")
        app-map (js/mapboxgl.Map. props)]
    ; save map into state atom
    (swap! state assoc :map app-map)
    ; save map into windown
    (aset js/window "appMap" app-map)))


(create-map)
(render-people (:curr-year @state))

;(dommy/unlisten! (sel1 :#years) :change year-change-handler)

(println "exec")
(defn init []
  (enable-console-print!)
  (println "init")
  (dommy/listen! (sel1 :#years) :change year-change-handler)

  (om/root widget
          {:text ""}
          {:target (. js/document (getElementById "container"))}))