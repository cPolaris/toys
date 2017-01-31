from __future__ import absolute_import, division, print_function, \
    with_statement
from shadowsocks import shell, daemon, eventloop, tcprelay, udprelay, asyncdns, encrypt
from db import ServersManager
import os
import random
import string
import sys
import json
import logging
import signal
import socket

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../'))

BUF_SIZE = 2048


class ClientPager:
    def __init__(self, addr, secret, config):
        self.crypto = encrypt.Encryptor(config['pager_secret'], config['method'])
        self.server_addr = addr
        self.server_secret = secret
        self.config = config
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)

    def send_crypt(self, data, action):
        data = json.dumps(data)
        self.sock.connect(self.server_addr)
        self.sock.send(self.crypto.encrypt(data))
        logging.info('Sent %s to [%s]: ' % (action, self.server_addr,))

    def recv_crypt(self):
        reply = self.sock.recv(BUF_SIZE)
        return self.crypto.decrypt(reply)

    def get_garbage(self):
        return ''.join(
            random.choice(string.ascii_uppercase + string.digits)
            for _ in range(random.randint(100, 200)))

    def log_reply(self, dict):
        pretty_str = json.dumps(dict, sort_keys=True, indent=4, separators=(',', ': '))
        logging.info('Reply from [%s]\n%s: ' % ((self.server_addr,), pretty_str))

    def exec_command(self, command):
        if command == 'poke':
            self.send_crypt({'act': 'POKE', 'discard': self.get_garbage()}, 'POKE')
            reply = self.recv_crypt()
            self.log_reply(json.loads(reply))
        elif command == 'add':
            logging.warning('Adding a new service port for %s' % (self.server_addr,))
            port = int(raw_input('Enter port:'))
            secret = raw_input('Enter secret:')
            self.send_crypt({'act': 'ADD_SERVICE', 'port': port, 'secret': secret}, 'ADD_SERVICE')
            reply = self.recv_crypt()
            self.log_reply(json.loads(reply))
        elif command == 'rm':
            logging.warning('Adding a new service port for %s' % (self.server_addr,))
            port = int(raw_input('Enter port:'))
            self.send_crypt({'act': 'RM_SERVICE', 'port': port}, 'RM_SERVICE')
            reply = self.recv_crypt()
            self.log_reply(json.loads(reply))
        elif command == 'ls':
            self.send_crypt({'act': 'LS_SERVICE', 'discard': self.get_garbage()}, 'LS_SERVICE')
            reply = self.recv_crypt()
            self.log_reply(json.loads(reply))
        # elif command == 'lsall':
        #     self.send_crypt({'act': 'LS_SERVICE_ALL', 'discard': self.get_garbage()},
        #                     'LS_SERVICE_ALL')
        #     reply = self.recv_crypt()
        #     self.log_reply(json.loads(reply))
        # elif command == 'join':
        #     logging.error('')
        #     ip = raw_input('Enter IP address:')
        #     port = raw_input('Enter port:')
        #     secret = raw_input('Enter secret:')
        #     self.send_crypt({'act': 'JOIN', 'ip': ip, 'port': port, 'secret': secret},
        #                     'JOIN')
        #     reply = self.recv_crypt()
        #     self.log_reply(json.loads(reply))
        else:
            logging.error('NOT SUPPORTED')


def start_service(config):
    # start local service
    logging.info("starting local at %s:%d" %
                 (config['local_address'], config['local_port']))

    dns_resolver = asyncdns.DNSResolver()
    tcp_server = tcprelay.TCPRelay(config, dns_resolver, True)
    udp_server = udprelay.UDPRelay(config, dns_resolver, True)

    loop = eventloop.EventLoop()

    dns_resolver.add_to_loop(loop)
    tcp_server.add_to_loop(loop)
    udp_server.add_to_loop(loop)

    def handler(signum, _):
        logging.warn('received SIGQUIT, doing graceful shutting down..')
        tcp_server.close(next_tick=True)
        udp_server.close(next_tick=True)

    signal.signal(getattr(signal, 'SIGQUIT', signal.SIGTERM), handler)

    def int_handler(signum, _):
        sys.exit(1)

    signal.signal(signal.SIGINT, int_handler)

    daemon.set_user(config.get('user', None))
    loop.run()


@shell.exception_handle(self_=False, exit_code=1)
def main():
    config = shell.get_config(True)
    command = config['daemon']

    if not command:
        shell.print_help(True)
        return

    sm = ServersManager(config['db-path'])
    addr = (config['server'], int(config['pager_port']))
    pager = ClientPager(addr, config['pager_secret'], config)

    if command in ['start', 'stop', 'restart']:
        daemon.daemon_exec(config)
    else:
        pager.exec_command(command)

    if config['daemon'] == 'start':
        start_service(config)


if __name__ == '__main__':
    main()
