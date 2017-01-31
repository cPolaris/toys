import socket, os

sock_name = 'temp.sock'
s = socket.socket(socket.AF_UNIX, socket.SOCK_DGRAM)

if os.path.exists(sock_name):
    os.unlink(sock_name)

s.bind(sock_name)
s.connect(sock_name)
s.send(b'dajizi')
print(s.recv(64).encode('UTF-8'))
s.send(b'dajizi')
print(s.recv(64).encode('UTF-8'))
