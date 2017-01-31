# Overview

The ShadowPager software consists of a **client-side** and a **server-side** program. The client handles interfacing with programs that wish to use the proxy service, which includes protocol handling and negotiation in different modes of operation (e.g. SOCKS, HTTP, etc.). The server side accepts multiple client connections, and forwards request and response back and forth between clients and destination hosts.

In addition, servers will communicate between each other, optionally with the aid of a **central authority**, to obtain knowledge of other peers' existence. As a result, each client is able to obtain a list of servers providing ShadowPager service. This enables the client to select a **fall-back server** in case current server encounters trouble, or select a server with smoother connection. We call such a network a **ShadowPager Network** (SPN) in which a group of servers have the knowledge of the existence of any other peer in the same network. These servers may serve arbitrary number of ShadowPager clients.

The objective of the ShadowPager network is to provide secure and reliable proxy service.

# Design Considerations

### Complete knowledge of peers

The complete list of servers available on the SPN is granted to clients in order to provide maximum usability of the network from a client's perspective. The trade-off is security: an adversary could pretend to be a client and obtain the complete list of servers, then bring down the whole network easily with one shot, or at least obtain critical knowledge about the network. This calls for great care when sharing the network with other people. Only trusted servers and clients shall be accepted when growing a SPN.

SPN is organized in a decentralized manner. One small SPN with a dozen servers has a "chief" server acting as central authority to do book keeping for peer servers.

This decision also avoids extra complexities of trying to hide clients from extra knowledge of the network while striving to provide good usability.

# Protocols

### SP_PEER_COMM Peer Communication Protocol

`SP_PEER_COMM` is used for servers to communicate with each other about transactions concerned with status of the network. It is delivered over TCP/IP. A server should designate a special port dedicated to peer communication.

|Action|Usage|
|------|------|
|**Join**||
|**Leave**||
|**Update**|Update server information|
|**Register**||
|**Register**||
|**Register**||

### SP_CLIENT Client Protocol

