(defproject metube "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [robert/bruce "0.7.1"]
                 [org.immutant/immutant "1.0.0.beta1"]
                 [compojure "1.1.5"]
                 [org.clojure/tools.logging "0.2.6"]]
  :immutant {:nrepl-port "10000"})
