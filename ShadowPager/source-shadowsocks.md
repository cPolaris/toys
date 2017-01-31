
- [Python Learnings](#python-learnings)
- [Source Code Annotation](#source-code-annotations)

## Python Learnings

### `gevent.monkey`

> gevent is a coroutine -based Python networking library that uses greenlet to provide a high-level synchronous API on top of the libev event loop.

And `gevent.monkey` can easily patch/replace standard library code to be cooperative, not interfering with the behavior (including socket, thread etc.):

```python
from gevent import monkey
monkey.patch_all()
```

Introduced in `12173a66d19cbba2`.

More to read about cooperative/coroutine: [Coroutine](https://en.wikipedia.org/wiki/Coroutine). Gevent: [gevent introduction](http://www.gevent.org/intro.html)


### `SocketServer.allow_resuse_address = True`

`65444c5b066cd1a64f3` To be studied...


### `socket.send()/recv()` vs `socket.wfile.write()/rfile.read()`

`send(data_string)` and `recv(buffer_size)` belong to the socket interface while `rfile` and `wfile` are attributes added by `StreamRequestHandler`, and are created with `socket.makefile`, which provides a file/stream style interface to do IO with the socket. `makefile()` interprets options exactly the same as built-in function `open()`.

standard library snippet:

```python

class StreamRequestHandler(BaseRequestHandler):
    rbufsize = -1   # Use system default buffering. Maybe full buffering
    wbufsize = 0    # No buffering. Also can be 1 for line buffering

    # A timeout to apply to the request socket, if not None.
    timeout = None

    # Disable nagle algorithm for this socket, if True.
    # Use only when wbufsize != 0, to avoid small packets.
    disable_nagle_algorithm = False

    def setup(self):
        self.connection = self.request
        if self.timeout is not None:
            self.connection.settimeout(self.timeout)
        if self.disable_nagle_algorithm:
            self.connection.setsockopt(socket.IPPROTO_TCP,
                                       socket.TCP_NODELAY, True)
                                       # 'b' mode: binary mode
        self.rfile = self.connection.makefile('rb', self.rbufsize)
        self.wfile = self.connection.makefile('wb', self.wbufsize)
```

This issue was first addressed in commit `c681ce101dbfad809`. But later clowwindy rolled back to use socket interface `send` and `recv` in `dd8ccf6f6eee0114e79018`.

More to read about [Nagle's Algorithm](https://en.wikipedia.org/wiki/Nagle%27s_algorithm).


### `socket.send(data)` vs `socket.sendall(data)`

`send` requires the application/receiver to check that all the data has been sent, and returns the number of byte sent. While `sendall` continues to send data until all given data has been sent or error occurs, and returns None on success or throw exception on error.

This issue was addressed in commit `3960e6495ee95d`.

clowwindy then uses self-implemented `sendall` later in `004e9292f4ee2fdadd2` for unknown reasons (educational? efficiency?).

```python
def send_all(sock, data):
    bytes_sent = 0
    while True:
        r = sock.send(data[bytes_sent:])
        if r < 0:
            return r
        bytes_sent += r
        if bytes_sent == len(data):
            return bytes_sent
```


## Source Code Annotations

### 

### `June 5 2014, commit aab163f067ba`

Can't believe two years just passed. The author must be developing in a really really casual pace and learning Python and networking in the mean time.

Version number 2.0.0. Major changes: 

- Isolated event loop: `eventloop.py`. Prefers `select.epoll` to `select.kqueue` to `select.select`. Why? For performance improvements: [The C10K Problem](http://www.kegel.com/c10k.html)
- Isolated TCP and UDP relay: both handle SOCKS5 protocol stages and relay packets back and forth
- Complete error checking and handling

Manifest:

`utils.py`: utilities to help find and load config file.

`encrypt.py`: wraps [OpenSSL EVP](https://wiki.openssl.org/index.php/EVP) style encryption implemented by package [M2Crypto]((https://gitlab.com/m2crypto/m2crypto). [What does EVP mean ?](http://stackoverflow.com/questions/3055454)

`local.py`, `server.py`: now can read user specified config file. Python standard module `log` used for logging, `getopt` used for parsing program arguments, `simplejson` package used for older Python versions to parse JSON formatted config file and `json` module for newer. Still using hybrid `socket.send/recv` and `rfile/wfile.read/write`.

### `May 10, 2012, commit 174d50bc6b1e330314`

Back then the project only consisted of: `local.py` and `server.py`, and was still at the state of proof-of-concept, which makes it a good starting point to understand everything else built upon it. Python 2 was the chosen language.

With the current setup, `local.py` essentially only function as an encryption middleware. With encryption taken off, `server.py` can already take SOCKS requests from client software.

```text
 Copyright (c) 2012 clowwindy

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
```

### `local.py`

```python
SERVER = '::1'       # Proxy servr IP
REMOTE_PORT = 8499   # Proxy server port to connect to
PORT = 1080          # Local port for SOCKS client programs
KEY = "foobar!"      # Encryption key

import socket        # low-level networking. Berkeley socket interface
import select        # IO multiplexing. Including select() poll(), epoll() kqueue() etc
import string        # string operations
import struct        # pack/unpack binary data to/from strings
import hashlib       # secure hash functions. including md5 and sha
import threading     # high level threading interface. on top of ``thread'' module
import time          # Time access and conversions
import SocketServer  # framework for writing server programs


# Exact copy from standard library code
# What is the purpose? For reference?
def socket_create_connection(address, timeout=socket._GLOBAL_DEFAULT_TIMEOUT,
                      source_address=None):
    """python 2.7 socket.create_connection
    Connect to *address* and return the socket object.
    Convenience function.  Connect to *address* (a 2-tuple ``(host,
    port)``) and return the socket object.  Passing the optional
    *timeout* parameter will set the timeout on the socket instance
    before attempting to connect.  If no *timeout* is supplied, the
    global default timeout setting returned by :func:`getdefaulttimeout`
    is used.  If *source_address* is set it must be a tuple of (host, port)
    for the socket to bind as a source address before making the connection.
    An host of '' or port 0 tells the OS to use the default.

    """
    host, port = address    # address is a 2-tuple
    err = None
    for res in socket.getaddrinfo(host, port, 0, socket.SOCK_STREAM):
        af, socktype, proto, canonname, sa = res
        sock = None
        try:
            sock = socket.socket(af, socktype, proto)
            if timeout is not socket._GLOBAL_DEFAULT_TIMEOUT:
                sock.settimeout(timeout)
            if source_address:
                sock.bind(source_address)
            sock.connect(sa)
            return sock

        except socket.error as _:
            err = _
            if sock is not None:
                sock.close()
    if err is not None:
        raise err
    else:
        raise error("getaddrinfo returns an empty list")


# Encryption and decryption are done by mapping bytes according to a
# translation table calculated from a shared key between client and server
def get_table(key):
    m = hashlib.md5()
    m.update(key)    # generate md5 digest of the encryption key
    s = m.digest()   # using default digest length: 16 bytes
    (a, b) = struct.unpack('<QQ', s)  # unpack(interpret) digest as two 8-byte numbers
                                      # but only ``a'' is actually used
                                      # ``<QQ'': little endian, unsigned long long

    # maketrans('','') returns a string of all 256 possible bytes
    # thus ``table'' starts off as a list of bytes starting from 0x00 to 0xff
    table = [c for c in string.maketrans('', '')]

    # doing some crazy stuff
    # No sure which encryption scheme is this
    for i in xrange(1, 1024):
        table.sort(lambda x, y: int(a % (ord(x) + i) - a % (ord(y) + i)))
    return table

# decryption table just maps from encryption table to normal byte order
# get_table returns a list; string.transform takes a string
encrypt_table = ''.join(get_table(KEY))
decrypt_table = string.maketrans(encrypt_table, string.maketrans('', ''))

# Python 2 print is not thread-safe
# We need a thread-safe version for our threading server
# Synchronize it by using a mutex and wrap it as a custom function
my_lock = threading.Lock()  # create new primitive lock object

def lock_print(msg):
    my_lock.acquire()
    try:
        print "[%s] %s" % (time.ctime(), msg)
    finally:
        my_lock.release()


# Using Mixin to declare a threading TCP socket server. This is the standard way
# of doing this (by the documentation).
# A SocketServer requires a request handler class (in this case, Socks5Server) to work.
# And it works synchronously (blocking request). Hence the threading mixin
class ThreadingTCPServer(SocketServer.ThreadingMixIn, SocketServer.TCPServer):
    pass

# SocketServer.StreamRequestHandler subclasses SocketServer.BaseRequestHandler,
# overrding setup() and finish(). Base class has three methods: setup() handle()
# and finish(). handle() is the core method to service each request.
class Socks5Server(SocketServer.StreamRequestHandler):
    def encrypt(self, data):
        return data.translate(encrypt_table)

    def decrypt(self, data):
        return data.translate(decrypt_table)

    def handle_tcp(self, sock, remote):
        try:
            fdset = [sock, remote]  # file descriptor set, convention from C IO programming
            counter = 0     # number of times gone through main loop

            # Main server loop here!!! Watch how we play with two socks!!!
            while True:     
                # we use `select' to multiplex the sockets
                # `select' accepts three lists of sockets: [try read] [try write] [try check error]
                # returns three lists: r: readable; w: writable; e: in error
                # here we only care about the readable list
                r, w, e = select.select(fdset, [], [])
                if sock in r:  # got something something from SOCKS client software
                    r_data = sock.recv(4096) # 4096 is just buffer size
                    if counter == 1:  
                        # will print some info at the second time reading from the local sock
                        # and execute only once
                        try:
                            lock_print("Connecting " + r_data[5:5 + ord(r_data[4])])
                        except Exception:
                            pass
                    if counter < 2:
                        counter += 1
                    if remote.send(self.encrypt(r_data)) <= 0:
                        # quit when cannot send any bytes to remote proxy
                        break
                if remote in r:  # got something back from remote proxy
                    # quit when cannot send any bytes to local client
                    if sock.send(self.decrypt(remote.recv(4096))) <= 0:
                        break
        finally:
            remote.close()

    def handle(self):
        try:
            sock = self.connection     # socket connection for local client software
            remote = socket_create_connection((SERVER, REMOTE_PORT))  # socket connection to proxy
            self.handle_tcp(sock, remote)
        except socket.error:
            lock_print('socket error')


def main():
    print 'Starting proxy at port %d' % PORT
    # Server init: (server_address, request_handler_class)
    # For the address, empty string is for INADDR_ANY, which binds the socket to
    # all available interfaces
    server = ThreadingTCPServer(('', PORT), Socks5Server)
    server.serve_forever()

if __name__ == '__main__':
    main()
```

--------

### `server.py`

```python
PORT = 8499
KEY = "foobar!"

# Code for next version? Currently no use for gevent
try:
    import gevent, gevent.monkey
    gevent.monkey.patch_all(dns=gevent.version_info[0]>=1)
except ImportError:
    gevent = None

import socket
import select
import SocketServer
import struct
import string
import hashlib
import sys

# same as library code
def socket_create_connection(address, timeout=socket._GLOBAL_DEFAULT_TIMEOUT,
                      source_address=None):
    """python 2.7 socket.create_connection"""
    host, port = address
    err = None
    for res in socket.getaddrinfo(host, port, 0, socket.SOCK_STREAM):
        af, socktype, proto, canonname, sa = res
        sock = None
        try:
            sock = socket.socket(af, socktype, proto)
            if timeout is not socket._GLOBAL_DEFAULT_TIMEOUT:
                sock.settimeout(timeout)
            if source_address:
                sock.bind(source_address)
            sock.connect(sa)
            return sock

        except error as _:
            err = _
            if sock is not None:
                sock.close()
    if err is not None:
        raise err
    else:
        raise error("getaddrinfo returns an empty list")

# same as client, myterious encryption method
def get_table(key):
    m = hashlib.md5()
    m.update(key)
    s = m.digest()
    (a, b) = struct.unpack('<QQ', s)
    table = [c for c in string.maketrans('', '')]
    for i in xrange(1, 1024):
        table.sort(lambda x, y: int(a % (ord(x) + i) - a % (ord(y) + i)))
    return table


class ThreadingTCPServer(SocketServer.ThreadingMixIn, SocketServer.TCPServer):
    pass


class Socks5Server(SocketServer.StreamRequestHandler):
    # At the proxy sever, `sock' is the socket connection with shadowsocks client,
    # and `remote' is the socket connection to some other website
    # Using some attributes inherited from base request handler:
    #   client_address: 2-tuple (host, port)
    #   rfile: input stream starting from optional input data
    def handle_tcp(self, sock, remote):
        try:
            fdset = [sock, remote]
            while True:
                # Relay client requests to remote, relay remote responses to
                # shadowsocks client
                r, w, e = select.select(fdset, [], [])
                if sock in r:                           # 4096 is just buffer size
                    if remote.send(self.decrypt(sock.recv(4096))) <= 0:
                        break
                if remote in r:
                    if sock.send(self.encrypt(remote.recv(4096))) <= 0:
                        break
        finally:
            remote.close()

    def encrypt(self, data):
        return data.translate(encrypt_table)

    def decrypt(self, data):
        return data.translate(decrypt_table)

    def send_encrpyt(self, sock, data):
        sock.send(self.encrypt(data))

    def handle(self):  # Here is a complete implementation of a SOCKS5 server
        try:
            print 'socks connection from ', self.client_address
            sock = self.connection
            sock.recv(262)      # 262 is just buffer size. Looks too magical

            # Step 1: SOCKS initiation response: choose ``no auth'' method
            self.send_encrpyt(sock, "\x05\x00")

            # Step 2: evaluate SOCKS request
            # data: first 4 bytes of SOCKS request
            data = self.decrypt(self.rfile.read(4))
            mode = ord(data[1])       # command field
            addrtype = ord(data[3])   # address_type field

            # obtain requesting remote address
            if addrtype == 1:         # addr_type = IPv4, length 4 bytes
                addr = socket.inet_ntoa(self.decrypt(self.rfile.read(4)))
            elif addrtype == 3:       # addr_type = domain name
                addr = self.decrypt(  # then next byte indicates domain name length
                    self.rfile.read(ord(self.decrypt(sock.recv(1)))))
            else:
                # not supported
                return

            # obtain requesting remote port
            # `>H': Big endian unsigned short. Port takes 2 bytes
            # unpack() returns tuple
            port = struct.unpack('>H', self.decrypt(self.rfile.read(2)))

            # Step 3: send reply to previous SOCKS request
            # 0x00: Reqeust granted.  the rest of the reply should be info about bounded socket
            #              v
            reply = "\x05\x00\x00\x01"
            try:
                if mode == 1:  # accept CONNECT command
                    remote = socket_create_connection((addr, port[0]))
                    local = remote.getsockname()  # 2-tuple (address, port)
                    reply += socket.inet_aton(local[0]) + struct.pack(">H", local[1])
                    print 'Tcp connect to', addr, port[0]
                else:
                    reply = "\x05\x07\x00\x01" # Command not supported
                    print 'command not supported'
            except socket.error:
                # 0x05: Connection refused by destination host
                #              v
                reply = '\x05\x05\x00\x01\x00\x00\x00\x00\x00\x00'

            self.send_encrpyt(sock, reply)
            if reply[1] == '\x00':
                if mode == 1:
                    self.handle_tcp(sock, remote)
        except socket.error as e:
            print 'socket error'


def main():
    if '-6' in sys.argv[1:]:
        ThreadingTCPServer.address_family = socket.AF_INET6
    server = ThreadingTCPServer(('', PORT), Socks5Server)
    server.allow_reuse_address = True
    print "starting server at port %d ..." % PORT
    server.serve_forever()

if __name__ == '__main__':
    encrypt_table = ''.join(get_table(KEY))
    decrypt_table = string.maketrans(encrypt_table, string.maketrans('', ''))
    main()
```
