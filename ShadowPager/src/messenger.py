import socket
from shadowsocks import encrypt

BUFFER_SIZE = 2048
TIMEOUT = 10

class Messenger:
    def __init__(self, ip, port, secret, encrypt_method):
        self.crypto = encrypt.Encryptor(secret, encrypt_method)
        self.sock = socket.socket(family=socket.AF_INET, type=socket.SOCK_DGRAM)
        self.sock.settimeout(TIMEOUT)
        self.sock.connect((ip, int(port)))

    def ack(self, success, data):
        pass

    def poke(self):
        pass
