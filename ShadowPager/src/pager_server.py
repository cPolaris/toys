import logging
import signal
from pagerhandler import PagerRequestHandler
from db import SocksManager, PeersManager
from shadowsocks import shell, daemon, eventloop, asyncdns, tcprelay, udprelay


def main():
    tcp_relays = []
    udp_relays = []
    loop = eventloop.EventLoop()

    logging.basicConfig(level=5,
                        format='%(asctime)s %(levelname)-8s %(message)s',
                        datefmt='%Y-%m-%d %H:%M:%S')

    config = shell.get_config(False)
    daemon.daemon_exec(config)

    logging.info('loading database from %s' % config['db-path'])
    sm = SocksManager(config['db-path'])
    pm = PeersManager(config['db-path'])

    socks = sm.list_all()
    peers = pm.list_all()

    logging.info('loaded %d service entries; %d peer entries' % (len(socks), len(peers)))

    dns_resolver = asyncdns.DNSResolver()

    # set up shadowsocks services
    port_secret = config['port_password']
    del config['port_password']

    for s in socks:
        port_secret[s['port']] = s['secret']

    for port, secret in port_secret.items():
        sm.add(port, secret)
        a_config = config.copy()
        a_config['server'] = '0.0.0.0'
        a_config['server_port'] = int(port)
        a_config['password'] = secret
        logging.info("starting shadowsocks at %s:%d" % (a_config['server'], int(port)))
        tcp_relays.append(tcprelay.TCPRelay(a_config, dns_resolver, False))
        udp_relays.append(udprelay.UDPRelay(a_config, dns_resolver, False))

    dns_resolver.add_to_loop(loop)
    list(map(lambda s: s.add_to_loop(loop), tcp_relays + udp_relays))

    # set up pager service
    PagerRequestHandler(config['pager_port'], loop, config, dns_resolver, sm, pm, tcp_relays,
                        udp_relays)

    def daemon_shutdown_handler(signum, _):
        logging.warn('received %s, doing graceful shutting down..' % signum)
        list(map(lambda s: s.close(next_tick=True), tcp_relays + udp_relays))
        loop.stop()

    signal.signal(signal.SIGINT, daemon_shutdown_handler)
    signal.signal(signal.SIGTERM, daemon_shutdown_handler)

    loop.run()


if __name__ == '__main__':
    main()
