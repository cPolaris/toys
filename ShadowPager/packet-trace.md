## Version: 

#### shadowsocks

Same setup as last time. Encryption was on. This 

config:

```javascript
{
    "server":"172.22.154.142",
    "server_port":8388,
    "local_address": "127.0.0.1",
    "local_port":1080,
    "password":"knowledge",
    "timeout":300,
    "method":"aes-256-cfb",
    "fast_open": false
}
```

local:

```
2016-10-26 13:06:46 INFO     connecting ipecho.net:80 from 127.0.0.1:57150
2016-10-26 13:06:46 INFO     connecting ipecho.net:80 from 127.0.0.1:57151
```

server:

```
2016-10-26 13:06:45 INFO     connecting ipecho.net:80 from 10.192.176.136:57153
2016-10-26 13:06:45 INFO     connecting ipecho.net:80 from 10.192.176.136:57152
```

#### Google Chrome socket trace

```c
1042: SOCKET
socks5/ipecho.net:80
Start Time: 2016-10-26 13:06:46.397

t=559839 [st=  0] +SOCKET_ALIVE  [dt=?]
                   --> source_dependency = 1040 (CONNECT_JOB)
t=559839 [st=  0]   +TCP_CONNECT  [dt=1]
                     --> address_list = ["[::1]:1080","127.0.0.1:1080"]
t=559840 [st=  1]     +TCP_CONNECT_ATTEMPT  [dt=0]
                       --> address = "[::1]:1080"
t=559840 [st=  1]     -TCP_CONNECT_ATTEMPT
                       --> os_error = 61
t=559840 [st=  1]      TCP_CONNECT_ATTEMPT  [dt=0]
                       --> address = "127.0.0.1:1080"
t=559840 [st=  1]   -TCP_CONNECT
                     --> source_address = "127.0.0.1:57150"
t=559840 [st=  1]   +SOCKET_IN_USE  [dt=?]
                     --> source_dependency = 1039 (CONNECT_JOB)
t=559840 [st=  1]     +SOCKS5_CONNECT  [dt=1]
t=559840 [st=  1]       +SOCKS5_GREET_WRITE  [dt=0]
t=559840 [st=  1]          SOCKET_BYTES_SENT
                           --> byte_count = 3
t=559840 [st=  1]       -SOCKS5_GREET_WRITE
t=559840 [st=  1]       +SOCKS5_GREET_READ  [dt=0]
t=559840 [st=  1]          SOCKET_BYTES_RECEIVED
                           --> byte_count = 2
t=559840 [st=  1]       -SOCKS5_GREET_READ
t=559840 [st=  1]       +SOCKS5_HANDSHAKE_WRITE  [dt=0]
t=559840 [st=  1]          SOCKET_BYTES_SENT
                           --> byte_count = 17
t=559840 [st=  1]       -SOCKS5_HANDSHAKE_WRITE
t=559840 [st=  1]       +SOCKS5_HANDSHAKE_READ  [dt=1]
t=559841 [st=  2]          SOCKET_BYTES_RECEIVED
                           --> byte_count = 5
t=559841 [st=  2]       -SOCKS5_HANDSHAKE_READ
t=559841 [st=  2]       +SOCKS5_HANDSHAKE_READ  [dt=0]
t=559841 [st=  2]          SOCKET_BYTES_RECEIVED
                           --> byte_count = 5
t=559841 [st=  2]       -SOCKS5_HANDSHAKE_READ
t=559841 [st=  2]     -SOCKS5_CONNECT
t=559945 [st=106]     +SOCKET_IN_USE  [dt=138]
                       --> source_dependency = 1048 (HTTP_STREAM_JOB)
t=559945 [st=106]        SOCKET_BYTES_SENT
                         --> byte_count = 410
t=560082 [st=243]        SOCKET_BYTES_RECEIVED
                         --> byte_count = 337
t=560083 [st=244]     -SOCKET_IN_USE
t=560100 [st=261]     +SOCKET_IN_USE  [dt=120]
                       --> source_dependency = 1053 (HTTP_STREAM_JOB)
t=560100 [st=261]        SOCKET_BYTES_SENT
                         --> byte_count = 366
t=560218 [st=379]        SOCKET_BYTES_RECEIVED
                         --> byte_count = 1237
t=560220 [st=381]     -SOCKET_IN_USE
```

```c
1046: SOCKET
socks5/ipecho.net:80
Start Time: 2016-10-26 13:06:46.398

t=559840 [st=0] +SOCKET_ALIVE  [dt=?]
                 --> source_dependency = 1045 (CONNECT_JOB)
t=559840 [st=0]   +TCP_CONNECT  [dt=0]
                   --> address_list = ["[::1]:1080","127.0.0.1:1080"]
t=559840 [st=0]     +TCP_CONNECT_ATTEMPT  [dt=0]
                     --> address = "[::1]:1080"
t=559840 [st=0]     -TCP_CONNECT_ATTEMPT
                     --> os_error = 61
t=559840 [st=0]      TCP_CONNECT_ATTEMPT  [dt=0]
                     --> address = "127.0.0.1:1080"
t=559840 [st=0]   -TCP_CONNECT
                   --> source_address = "127.0.0.1:57151"
t=559840 [st=0]   +SOCKET_IN_USE  [dt=?]
                   --> source_dependency = 1044 (CONNECT_JOB)
t=559840 [st=0]     +SOCKS5_CONNECT  [dt=1]
t=559840 [st=0]       +SOCKS5_GREET_WRITE  [dt=0]
t=559840 [st=0]          SOCKET_BYTES_SENT
                         --> byte_count = 3
t=559840 [st=0]       -SOCKS5_GREET_WRITE
t=559840 [st=0]       +SOCKS5_GREET_READ  [dt=1]
t=559841 [st=1]          SOCKET_BYTES_RECEIVED
                         --> byte_count = 2
t=559841 [st=1]       -SOCKS5_GREET_READ
t=559841 [st=1]       +SOCKS5_HANDSHAKE_WRITE  [dt=0]
t=559841 [st=1]          SOCKET_BYTES_SENT
                         --> byte_count = 17
t=559841 [st=1]       -SOCKS5_HANDSHAKE_WRITE
t=559841 [st=1]       +SOCKS5_HANDSHAKE_READ  [dt=0]
t=559841 [st=1]          SOCKET_BYTES_RECEIVED
                         --> byte_count = 5
t=559841 [st=1]       -SOCKS5_HANDSHAKE_READ
t=559841 [st=1]       +SOCKS5_HANDSHAKE_READ  [dt=0]
t=559841 [st=1]          SOCKET_BYTES_RECEIVED
                         --> byte_count = 5
t=559841 [st=1]       -SOCKS5_HANDSHAKE_READ
t=559841 [st=1]     -SOCKS5_CONNECT
```

## Version: June 5 2014, commit aab163f067ba

Encryption was turned off. Packets captured with Wireshark. cs241 VM was used as proxy.

localhost 62912, 62915, 62916 were used by Chrome to connect with SOCKS local server. localhost 62918, 62917, 62919 were used by `local.py` to connect with proxy. However, only connection with port 62918 received HTTP response data sent back from proxy (which was also the first connection established). Chrome connection 62915 sent the request. The duplicated request might be caused by Google Chrome.

[Wireshark capture file](wireshark-captures/ip_echo_http_log.pcap)



local.py console output:

```
[Sun Oct  9 00:20:24 2016] Connecting ipecho.net
[Sun Oct  9 00:20:24 2016] Connecting ipecho.net
[Sun Oct  9 00:20:24 2016] Connecting ipecho.net
```

server.py console output:

```
socks connection from  ('10.192.176.136', 62918)
socks connection from  ('10.192.176.136', 62917)
socks connection from  ('10.192.176.136', 62919)
Tcp connect to ipecho.net 80
Tcp connect to ipecho.net 80
Tcp connect to ipecho.net 80
```

Google Chrome event trace (commented and can cross-reference with the packet log):

```c
1091: SOCKET
socks5/ipecho.net:80
Start Time: 2016-10-09 00:20:24.771

t=574930 [st=  0] +SOCKET_ALIVE  [dt=?]
                   --> source_dependency = 1090 (CONNECT_JOB)
t=574930 [st=  0]   +TCP_CONNECT  [dt=0]
                     --> address_list = ["[::1]:10800","127.0.0.1:10800"]
t=574930 [st=  0]     +TCP_CONNECT_ATTEMPT  [dt=0]
                       --> address = "[::1]:10800"
t=574930 [st=  0]     -TCP_CONNECT_ATTEMPT
                       --> os_error = 61
t=574930 [st=  0]      TCP_CONNECT_ATTEMPT  [dt=0]
                       --> address = "127.0.0.1:10800"
t=574930 [st=  0]   -TCP_CONNECT
                     --> source_address = "127.0.0.1:62915"

t=574931 [st=  1]   +SOCKET_IN_USE  [dt=?]
                     --> source_dependency = 1089 (CONNECT_JOB)

t=574931 [st=  1]     +SOCKS5_CONNECT  [dt=121]
t=574931 [st=  1]       +SOCKS5_GREET_WRITE  [dt=0]
t=574931 [st=  1]          SOCKET_BYTES_SENT
                           --> byte_count = 3     # SOCKS Initiation
t=574931 [st=  1]       -SOCKS5_GREET_WRITE

t=574931 [st=  1]       +SOCKS5_GREET_READ  [dt=5]
t=574936 [st=  6]          SOCKET_BYTES_RECEIVED
                           --> byte_count = 2     # SOCKS server chose no auth method
t=574936 [st=  6]       -SOCKS5_GREET_READ        # method negotiation ended

t=574936 [st=  6]       +SOCKS5_HANDSHAKE_WRITE  [dt=0]
t=574936 [st=  6]          SOCKET_BYTES_SENT
                           --> byte_count = 17    # start SOCKS request for ipecho.net:80
t=574936 [st=  6]       -SOCKS5_HANDSHAKE_WRITE

t=574936 [st=  6]       +SOCKS5_HANDSHAKE_READ  [dt=116]
t=575052 [st=122]          SOCKET_BYTES_RECEIVED
                           --> byte_count = 5     # SOCKS response for the previous request
t=575052 [st=122]       -SOCKS5_HANDSHAKE_READ    # 05 00 00 01 ac 16 9a 8e 8e 60

t=575052 [st=122]       +SOCKS5_HANDSHAKE_READ  [dt=0]
t=575052 [st=122]          SOCKET_BYTES_RECEIVED
                           --> byte_count = 5     # 10 byte response continued
t=575052 [st=122]       -SOCKS5_HANDSHAKE_READ
t=575052 [st=122]     -SOCKS5_CONNECT

t=575052 [st=122]     +SOCKET_IN_USE  [dt=120]
                       --> source_dependency = 1096 (HTTP_STREAM_JOB)
t=575052 [st=122]        SOCKET_BYTES_SENT
                         --> byte_count = 432    # HTTP request for index page
t=575171 [st=241]        SOCKET_BYTES_RECEIVED
                         --> byte_count = 337    # HTTP response for index page
t=575172 [st=242]     -SOCKET_IN_USE

t=575189 [st=259]     +SOCKET_IN_USE  [dt=110]
                       --> source_dependency = 1101 (HTTP_STREAM_JOB)
t=575189 [st=259]        SOCKET_BYTES_SENT
                         --> byte_count = 388    # HTTP request for favicon
t=575298 [st=368]        SOCKET_BYTES_RECEIVED
                         --> byte_count = 1237   # HTTP response for favicon
t=575299 [st=369]     -SOCKET_IN_USE
```