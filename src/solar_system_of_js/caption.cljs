(ns solar-system-of-js.caption
  (:require
    [solar-system-of-js.state :refer [state]]
    cljsjs.jquery
    [markdown.core :refer [md->html]]))

(def caption-element (.getElementById js/document "caption"))

(defn make-link-popout!
  []
  (this-as this
    (.attr (js/$ this) "target" "_blank")))

(defn set-caption!
  [caption]
  (aset caption-element "style" "opacity" "1")
  (let [html (md->html new-caption)]
    (aset caption-element "innerHTML" html))
  (-> (js/$ caption-element)
      (.find "a")
      (.each make-link-popout!)))

(defn on-caption-change
  [_key _atom {old-caption :caption} {new-caption :caption}]
  (when (not= old-caption new-caption)
    (set-caption! new-caption)))

(add-watch state :caption-watcher on-caption-change)
