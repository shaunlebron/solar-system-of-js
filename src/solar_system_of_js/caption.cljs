(ns solar-system-of-js.caption
  (:require
    [solar-system-of-js.state :refer [state]]
    [markdown.core :refer [md->html]]))

(def caption-element (.getElementById js/document "caption"))

(defn set-caption!
  [caption]
  (aset caption-element "style" "opacity" "1")
  (let [html (md->html caption)]
    (aset caption-element "innerHTML" html))

  ;; make all links open in new tab
  (let [elements (.querySelectorAll caption-element "a")]
    (dotimes [i (aget elements "length")]
      (let [el (aget elements i)]
        (.setAttribute el "target" "_blank")))))

(defn on-caption-change
  [_key _atom {old-caption :caption} {new-caption :caption}]
  (when (not= old-caption new-caption)
    (set-caption! new-caption)))

(add-watch state :caption-watcher on-caption-change)
