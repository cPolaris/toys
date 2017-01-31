import errno
import json
import logging
import random
import socket
import string

from shadowsocks import eventloop, shell, encrypt, tcprelay, udprelay

BUF_SIZE = 1024
LESSONS = [
    'Computer Science is no more about computers than astronomy is about telescopes. - EWD',
    'There are two ways of constructing a software design: One way is to make it so simple that '
    'there are obviously no deficiencies, and the other way is to make it so complicated that '
    'there are no obvious deficiencies. The first method is far more difficult. - Hoare',
    'Source: thanks to: gist.github.com/ferhatelmas/4128924',
    'Nine people can\'t make a baby in a month. - Brooks',
    'C makes it easy to shoot yourself in the foot; C++ makes it harder, but when you do, '
    'it blows away your whole leg. - Stroustrup',
    'Source: thanks to: gist.github.com/ferhatelmas/4128924',
    'Most good programmers do programming not because they expect to get paid or get adulation by '
    'the public, but because it is fun to program. - Linus Torvalds',
    'Programming is like sex. One mistake and you have to support it for the rest of your life. - '
    'Michael Sinz',
    'If builders built buildings the way programmers wrote programs, then the first woodpecker '
    'that came along wound destroy civilization. - Gerald Weinberg',
    'Source: thanks to: gist.github.com/ferhatelmas/4128924',
    'One of my most productive days was throwing away 1000 lines of code. - Ken Thompson',
    'If builders built buildings the way programmers wrote programs, then the first woodpecker '
    'that came along wound destroy civilization. - Gerald Weinberg '
]


class PagerRequestHandler:
    def __init__(self, port, loop, config, dns, db_sock, db_peer, tcps, udps):
        address_info = socket.getaddrinfo('0.0.0.0', port, 0,
                                          socket.SOCK_DGRAM, socket.SOL_UDP)
        af, socktype, proto, canonname, sa = address_info[0]
        self.tcps = tcps
        self.udps = udps
        self.crypto = None
        self.socket = socket.socket(af, socktype, proto)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.socket.setblocking(False)
        self.socket.bind(sa)
        self.loop = loop
        self.loop.add(self.socket, eventloop.POLL_IN | eventloop.POLL_ERR, self)
        self.config = config
        self.secret = config['pager_secret']
        self.dns = dns
        self.sock_db = db_sock
        self.peer_db = db_peer
        logging.info('pager server listening at 0.0.0.0:%s' % port)

    def get_garbage(self):
        return ''.join(
            random.choice(string.ascii_uppercase + string.digits)
            for _ in range(random.randint(100, 200)))

    def parse_data(self, data):
        return shell.parse_json_in_str(data)

    def ack(self, status, addr, addnl_dict):
        reply = {'quote': random.choice(LESSONS), 'status': status, 'act': 'ACK'}
        reply.update(addnl_dict)
        self.send_crypt(addr, json.dumps(reply))

    def send_crypt(self, addr, data):
        self.socket.sendto(self.crypto.encrypt(data), addr)

    def send_crypt_with_secret(self, addr, data, secret):
        crypt = encrypt.Encryptor(secret, self.config['method'])
        self.socket.connect(addr)
        self.socket.send(crypt.encrypt(data))
        return crypt.decrypt(self.socket.recv(BUF_SIZE))

    def gather_peer_services(self):
        services = []
        peers = self.peer_db.list_all()
        for peer in peers:
            logging.warning('contacting peer %s' % peer)
            reply = self.send_crypt_with_secret((peer['ip'], int(peer['port'])),
                                                json.dumps(
                                                    {'act': 'LS_SERVICE',
                                                     'discard': self.get_garbage()}),
                                                peer['secret'])
            reply = shell.parse_json_in_str(reply)
            services.append({peer['ip']: reply['services']})
        return services

    def notify_all_peers(self, other_peers):
        peers = self.peer_db.list_all()
        for peer in peers:
            logging.warning('updating peer list to %s' % peer)
            reply = self.send_crypt_with_secret((peer['ip'], int(peer['port'])),
                                                json.dumps(
                                                    {'act': 'GREET',
                                                     'reply': False,
                                                     'peers': other_peers}),
                                                peer['secret'])
            reply = shell.parse_json_in_str(reply)
            if reply['status']:
                logging.warning('last update success')
            else:
                logging.warning('last update failed')

    def handle_command(self, dict, sender):
        try:
            if dict['act'] == 'POKE':
                logging.warning('POKE from %s' % (sender,))
                self.ack(True, sender, {})

            elif dict['act'] == 'ADD_SERVICE':
                logging.warning('ADD_SERVICE from %s' % (sender,))
                port = int(dict['port'])
                secret = dict['secret']
                a_config = self.config.copy()
                a_config['server'] = '0.0.0.0'
                a_config['server_port'] = port
                a_config['password'] = secret
                logging.info("starting shadowsocks at %s:%d" % ('0.0.0.0', int(port)))
                new_tcp = tcprelay.TCPRelay(a_config, self.dns, False)
                new_udp = udprelay.UDPRelay(a_config, self.dns, False)
                new_tcp.add_to_loop(self.loop)
                new_udp.add_to_loop(self.loop)
                self.tcps.append(new_tcp)
                self.udps.append(new_udp)
                self.ack(True, sender, {})
                self.sock_db.add(port, secret)

            elif dict['act'] == 'RM_SERVICE':
                port = int(dict['port'])
                logging.warning('RM_SERVICE port %d pending' % port)
                rmvd = False
                for relay in self.tcps:
                    if relay._listen_port == port:
                        relay.close()
                        rmvd = True
                        logging.warning('RM_SERVICE successful')
                        self.ack(True, sender, {})
                        self.sock_db.remove(port)
                        break
                if not rmvd:
                    self.ack(False, sender, {'msg': 'no service'})
                    logging.warning('RM_SERVICE failed')
                else:
                    for relay in self.udps:
                        if relay._listen_port == port:
                            relay.close()
                            break
            elif dict['act'] == 'LS_SERVICE':
                logging.warning('LS_SERVICE from %s' % (sender,))
                self.ack(True, sender, {'services': self.sock_db.list_all()})

            elif dict['act'] == 'LS_SERVICE_ALL':
                logging.warning('LS_SERVICE_ALL from %s' % (sender,))
                all_service = self.gather_peer_services()
                all_service.append(self.sock_db.list_all())
                self.ack(True, sender, {'services': all_service})

            elif dict['act'] == 'JOIN':
                ip = dict['ip']
                port = int(dict['port'])
                secret = dict['secret']
                reply = self.send_crypt_with_secret((ip, port),
                                                    json.dumps(
                                                        {'act': 'GREET', 'secret': self.secret,
                                                         'peers': self.peer_db.list_all,
                                                         'reply': True}),
                                                    secret)
                reply = shell.parse_json_in_str(reply)
                new_peers = reply['peers']
                for peer in new_peers:
                    self.peer_db.add(peer['ip'], peer['port'], peer['secret'])
                self.notify_all_peers(new_peers)
            elif dict['act'] == 'GREET':
                new_peer_secret = dict['secret']
                other_peers = dict['peers']
                if dict['reply']:
                    self.ack(True, sender, {'peers': self.peer_db.list_all()})
        except KeyError as e:
            logging.error('message does not contain expected key')
            shell.print_exception(e)
        except (OSError, IOError) as e:
            error_no = eventloop.errno_from_exception(e)
            if error_no in (errno.EAGAIN, errno.EINPROGRESS, errno.EWOULDBLOCK):
                return
            else:
                shell.print_exception(e)

    def handle_event(self, sock, fd, event):
        if sock == self.socket:
            self.crypto = encrypt.Encryptor(self.secret, self.config['method'])
            if event & eventloop.POLL_ERR:
                raise Exception('PagerHandler event error')
            try:
                data, sender = self.socket.recvfrom(BUF_SIZE)
                logging.info('PagerHandler from %s' % (sender,))
                dict = shell.parse_json_in_str(self.crypto.decrypt(data))
                self.handle_command(dict, sender)
            except (OSError, IOError, ValueError) as e:
                error_no = eventloop.errno_from_exception(e)
                if error_no in (errno.EAGAIN, errno.EINPROGRESS, errno.EWOULDBLOCK):
                    return
                else:
                    shell.print_exception(e)
