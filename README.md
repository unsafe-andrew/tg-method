# tg-method

A library for building Telegram bots

Its name comes from just a single macro that defines API methods. The library is not full yet

Usage example:
```clj
(ns mybot.core
  (:require [tg-method.core :as t]))

(def token "TOKEN")

(defn handler [upd]
  (t/send-message token (t/sender-id (:message upd)) (:text msg))) ; Send back whatever message a user sent

(t/polling token handler 500) ; 500 is the polling interval
```

In bb.edn:
```
{:deps {unsafe-andrew/tg-method {:mvn/version "0.1"}}}
```
