(ns metube.core
  (:require [robert.bruce :refer [try-try-again *try*]]
            [clojure.java.shell :as sh]
            [clojure.java.io :refer [copy reader]]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [immutant.cache :as cache]
            [immutant.messaging :as msg])
  (:import (java.io ByteArrayOutputStream)))

(def qn "queue.notifications")
(def youtube-dl-cmd "youtube-dl")
(def download-dir "/home/hinmanm/Downloads")

(def stats-cache (cache/cache "metube-stats"))

(defonce youtube-dl-enabled?
  (= 0 (:exit (sh/sh "which" "youtube-dl"))))

(defn download
  [url]
  (when youtube-dl-enabled?
    (let [resp (sh/with-sh-dir download-dir (sh/sh youtube-dl-cmd url "-t"))
          exit (:exit resp)]
      (when-not (zero? exit)
        (println "Non-zero exit downloading:" url)
        (println "Output:" resp)
        (throw (Exception. (str "error downloading " url (:err resp))))))
    true))

(defn download-youtube-url [url]
  (println "Received download request for:" url)
  (try
    (cache/put stats-cache
               :active
               (inc (get stats-cache :active 0)))
    ;; TODO retries
    (let [resp (download url)]
      (when resp
        (msg/publish qn (str "Successfully downloaded " url))))
    (catch Throwable e
      (msg/publish qn (str "Unable to download: " url ", reason: " e)))
    (finally
      (cache/put stats-cache
                 :active
                 (dec (get stats-cache :active))))))

(defn enqueue-handler
  "Handler for enqueuing youtube download requests"
  [request]
  (try
    (let [body (:body request)
          url (slurp body)]
      (println "URL:" url)
      (if (and (string? url) (not (empty? url)))
        (do
          (msg/publish qn (str "Queuing download of " url))
          (msg/publish "queue.metube" url)
          {:status 200
           :body (str {:success true} "\n")
           :headers {"Content-Type" "application/edn"}})
        {:status 500
         :body (str {:success false :exception "No URL specified"} "\n")
         :headers {"Content-Type" "application/edn"}}))
    (catch Throwable e
      {:status 500
       :body (str {:success false :exception (str e)} "\n")
       :headers {"Content-Type" "application/edn"}})))

(defn active-requests
  "Returns number of active downloads"
  []
  {:status 200
   :body (str {:active (get stats-cache :active 0)} "\n")
   :headers {"Content-Type" "application/edn"}})

(defroutes metube-routes
  (GET "/active" [] (active-requests))
  (ANY "/" request (enqueue-handler request)))

(def handler (-> #'metube-routes handler/api))
