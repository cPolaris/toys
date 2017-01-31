# Software Tools for Firewall Traversal

### SSH Tunneling

SSH: application layer protocol. Consists of three component protocols:

- [Transport Layer Protocol][25]: server authentication, confidentiality, integrity, and compression.
- [User Authentication Protocol][26]: authenticate client-side user to the server.
- [Connection Protocol][27]: multiplex encrypted tunnel into logical channels.

#### Local port forwarding

SSH client <-> SSH server <-> host. `ssh -L [bind_address:]port:host:hostport`. This works by allocating a socket to listen to **port** on the local side (optionally bound to the specified **bind_address**). Whenever a connection is made to this port, the connection is forwarded over the secure channel, and a connection is made to **host** port **hostport** from the remote machine.

e.g. `ssh -L 8080:www.ubuntuforums.org:80 <host>`: forwards `localhost:80`, via host that this SSH session is connected with, to `www.ubuntuforums.org:80`.

e.g. `ssh -L 8080:www.ubuntuforums.org:80 -L 12345:ubuntu.com:80 <host>`: forward multiple ports.

#### Remote port forwarding

SSH server <-> SSH client <-> host. `ssh -R [bind_address:]port:host:hostport`. This works by allocating a socket to listen to **port** on the remote side, and whenever a connection is made to this port, the connection is forwarded over the secure channel, and a connection is made to **host** port **hostport** from the local machine.

#### Dynamic port forwarding

`-D [bind_address:]port <host>` . Use SSH client as SOCKS server that listens on **port**. Connections are proxied by the **host**.


### VPN


OpenVPN: custom protocol based on SSL and TLS.


### Lantern

Is a big project that provides commercial proxy service.

- [Lantern: Open Internet For Everyone][1]
- [Lantern FAQ][28]
- [Lantern README for developers][29]
- [lantern on GitHub][4]
- [Flashlight: actual component for proxying][24]

### Psiphon

Psiphon is a "Swiss Army knife" tool for network proxy. It can switch between VPN, SOCKS, and HTTP proxy from a client program's perspective. Underneath it uses [L2TP](https://en.wikipedia.org/wiki/Layer_2_Tunneling_Protocol) over [IPsec](https://en.wikipedia.org/wiki/IPsec) for VPN networking. For SOCKS and HTTP proxy modes, it uses SSH for tunneling but with [obfuscated handshake protocol](https://github.com/brl/obfuscated-openssh/blob/master/README) to defend against adversary.

- [Psiphon: Beyond Borders][9]
- [Psiphon Circumvention System on Bitbucket][10]
- [Psiphon Tunnel Core on GitHub][11]


### Shadowsocks

Shadowsocks is a minimal tool for establishing a proxy. It accepts SOCKS protocol from the client side, and uses custom protocol to communicate between Shadowcoks client and Shadowsocks server. All packets run through TCP/IP with application layer encryption.

- [Shadowsocks: A secure SOCKS5 proxy][12]
- [Spec: the protocol][30]
- [Spec: one time auth][31]
- [shadowsocks on GitHub][13]
- [PyPI: shadowsocks 2.8.2][2]
- [PyPI: shadowsocks all releases][15]
- [Python doc: Socket programming howto][16]

Believed to be the author... [Botao Zhu](https://www.linkedin.com/in/botaozhu) Currently working at Apple.


## Tools Roundup

|Tool | Internet | Transport | Application | Technology | Notes |
|--------|--------|--------|--------|--------|--------|
| SSH | IP | TCP | SSH-TRANS, SSH-USERAUTH, SSH-CONNECT | C | Very distinct handshake. Standardized, robust and widely accepted |
| VPN | | | | C |  |
| Lantern | | | | Go | Commercial. receives funding from Google and US government??? |
| Psiphon | | | | ? | |
| Shadowsocks| | | Python | | Pre-configured shared secret and encryption algorithm, i.e. no handshake |


# Networking Notes

### TCP

Basic TCP Header: 20 bytes

```text
16 bits  Source port
16 bits  Destination port

32 bits  Sequence number

32 bits  [ Acknowledgment number ]

4  bits  Header length
4  bits  Reserved

1  bit   CWR
1  bit   [ ECE ]
1  bit   URG
1  bit   [ ACK ]
1  bit   PSH
1  bit   RST
1  bit   SYN
1  bit   FIN

16 bits  [ Window size ]

16 bits  TCP Checksum
16 bits  Urgent Pointer
(variable) Options
```

### SOCKS5

CONNECT command phases: greet (establish connection, method-specific sub-negotiation), handshake (SOCKS requests), relay.

In other words: first the client connects to the SOCKS service port of the proxy server, negotiate the connection to resolve issues such as authentication, then sends request for connection with the target host. The server takes the request and try to establish connection with target host. The proxy server then returns to the client which address and port the server used to connect to target host (why giving this information to the client?). At this point, the proxy bridge has been established. The client can send and receive data through the proxy server, as if communicating directly with the target host. And the target host sends and receives data through the connection that the proxy server opened, the same way as dealing with any client.

- client initiate: `version(1) num_methods(1) methods(1-255)`. Version should be `0x05` for SOCKS 5.
- server initial response: `version(1) method(1)`. Method is the method that the server chooses. Predefined methods:
    + `0x00` No authentication
    + `0x01` [GSSAPI][19]
    + `0x02` Username/password
    + `0x03 - 0x7F` [Internet Assigned Numbers Authority][18] assigned
    + `0x80 - 0xFE` reserved for private usage
    + `0xFF` No acceptable methods. Client must close connection upon receiving this from server.
- client subsequent SOCKS request: `version(1) command(1) reserved(1) address_type(1) dest_address(variable) dest_port(2)`.
    + Commands:
        * `0x01` CONNECT
        * `0x02` BIND
        * `0x03` UDP associate
    + Address types:
        * `0x01` IPv4
        * `0x03` Domain name
        * `0x04` IPv6
- server reply to request: `version(1) reply_field(1) reserved(1) address_type(1) bound_address(variable) bound_port(2)`. Replies:
    + `0x00` succeeded
    + `0x01` general SOCKS server failure
    + `0x02` connection not allowed
    + `0x03` network unreachable
    + `0x04` host unreachable
    + `0x05` connection refused
    + `0x06` TTL expired
    + `0x07` command not supported
    + `0x08` address type not supported
    + `0x09 - 0xff` unassigned
- If last step succeeded, the client can now send and receive data to proxy server as if communicating with the target server.

### Socket Programming

> The Python interface is a straightforward transliteration of the Unix system call and library interface for sockets to Python’s object-oriented style

- [An Introductory 4.4BSD Interprocess Communication Tutorial](https://docs.freebsd.org/44doc/psd/20.ipctut/paper.pdf)
- [An Advanced 4.4BSD Interprocess Communication Tutorial](https://docs.freebsd.org/44doc/psd/21.ipc/paper.pdf)

### IPv6 Quirks

**flowinfo**

**scopeid**

# References

- [SOCKS5: RFC 1928][17]
- [SSH Connection Protocol: RFC 4254][21]
- [Beej's Guide to Network Programming: Using Internet Sockets][20]
- [Wikipedia: Internet censorship circumvention][3]
- [Wikipedia: Tunneling Protocol][8]
- [Ubuntu Doc: SSH Configuring][5]
- [Ubuntu Doc: SSH PortForwarding][6]
- [Wikipedia: SSH][7]
- [Network Programming using the Go Programming Language by Jan Newmarch][22]


[1]: https://getlantern.org
[2]: https://pypi.python.org/pypi/shadowsocks/2.8.2
[3]: https://en.wikipedia.org/wiki/Internet_censorship_circumvention
[4]: https://github.com/getlantern/lantern
[5]: https://help.ubuntu.com/community/SSH/OpenSSH/Configuring
[6]: https://help.ubuntu.com/community/SSH/OpenSSH/PortForwarding
[7]: https://en.wikipedia.org/wiki/Secure_Shell
[8]: https://en.wikipedia.org/wiki/Tunneling_protocol
[9]: https://www.psiphon3.com/en/index.html
[10]: https://bitbucket.org/psiphon/psiphon-circumvention-system
[11]: https://github.com/Psiphon-Labs/psiphon-tunnel-core
[12]: https://shadowsocks.org/en/index.html
[13]: https://github.com/shadowsocks/shadowsocks
[14]: https://crypto.stanford.edu/flashproxy/
[15]: https://pypi.python.org/simple/shadowsocks/
[16]: https://docs.python.org/2.7/howto/sockets.html
[17]: http://www.rfc-editor.org/rfc/rfc1928.txt
[18]: https://en.wikipedia.org/wiki/Internet_Assigned_Numbers_Authority
[19]: https://en.wikipedia.org/wiki/Generic_Security_Services_Application_Program_Interface
[20]: http://www.beej.us/guide/bgnet/output/html/singlepage/bgnet.html
[21]: https://tools.ietf.org/html/rfc4254
[22]: https://jan.newmarch.name/golang/
[23]: https://github.com/getlantern/forum/issues/361
[24]: https://github.com/getlantern/flashlight-build/tree/valencia/src/github.com/getlantern/flashlight
[25]: https://tools.ietf.org/html/rfc4253
[26]: https://tools.ietf.org/html/rfc4252
[27]: https://tools.ietf.org/html/rfc4254
[28]: https://github.com/getlantern/lantern/wiki/Questions-and-Answers
[29]: https://github.com/getlantern/lantern/blob/valencia/README-dev.md
[30]: https://shadowsocks.org/en/spec/protocol.html
[31]: https://shadowsocks.org/en/spec/one-time-auth.html
