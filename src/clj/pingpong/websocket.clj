(ns pingpong.websocket
  (:require [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]))


;;; Sente channels --->
(let [{:keys [ch-recv send-fn connected-uids ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket-server! (get-sch-adapter) 
                                         {; :user-id-fn uid-to-client!
                                          :csrf-token-fn nil})]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv)         ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn)         ; ChannelSocket's send API fn
  (def connected-uids                connected-uids)) ; Watchable, read-only atom
