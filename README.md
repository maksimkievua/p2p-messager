# p2p-messenger
p2p-messenger is peer-to-peer instant messaging system.
Contains cross-platform desktop client and Discovery Service.

# Getting started for developers
1. Start Discovery Service.
```text
./gradlew :discovery:bootRun
```
1. Start as many desktop clients as you want.
```text
./gradlew :client:bootRun
```
**NOTE:** by default client looks for Discovery Service on 'localhost' so 
if you want to connect to remote discovery service you should define system property.
```text
./gradlew :client:bootRun -Dchat.client.discoveryServiceHost=example.com
```
## Available properties
### Discovery Service
* **discovery.server.port** - Discovery Service port. Default: 61000.
* **debug** - Turns on debug logs.
### Client
* **chat.client.discoveryServiceHost** - Discovery Service host. May be host name or IP address. Default: localhost.
* **chat.client.discoveryServicePort** - Discovery Service port. Default: 61000.
* **debug** - Turns on debug logs.
