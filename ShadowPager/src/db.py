import logging
import sqlite3


def get_table_names(cursor):
    return [row[0] for row in cursor.execute("SELECT name FROM sqlite_master")]


def dict_factory(cursor, row):
    d = {}
    for idx, col in enumerate(cursor.description):
        d[col[0]] = row[idx]
    return d


class SocksManager:
    def __init__(self, path_to_db):
        self.db_path = path_to_db
        connection = sqlite3.connect(path_to_db)
        if 'socks' not in get_table_names(connection.cursor()):
            logging.warn('socks table not found')
            connection.execute(
                'CREATE TABLE socks (port TEXT PRIMARY KEY, secret TEXT, UNIQUE (port))')
            connection.commit()
            connection.close()
            logging.warn('new table created: socks')

    def list_all(self):
        connection = sqlite3.connect(self.db_path)
        connection.row_factory = dict_factory
        result = list(connection.cursor().execute('SELECT * FROM socks').fetchall())
        connection.close()
        return result

    def add(self, port, secret):
        connection = sqlite3.connect(self.db_path)
        try:
            connection.cursor().execute('INSERT INTO socks VALUES (?, ?)', (port, secret))
        except sqlite3.IntegrityError:
            logging.warning('skipping duplicate entry')
            connection.close()
            return
        connection.commit()
        connection.close()

    def remove(self, port):
        connection = sqlite3.connect(self.db_path)
        connection.cursor().execute('DELETE FROM socks WHERE port=?', (port,))
        connection.commit()
        connection.close()


class PeersManager:
    def __init__(self, path_to_db):
        self.db_path = path_to_db
        connection = sqlite3.connect(path_to_db)
        if 'peers' not in get_table_names(connection.cursor()):
            logging.warn('peers table not found')
            connection.execute(
                'CREATE TABLE peers (ip TEXT PRIMARY KEY, port TEXT, secret TEXT, UNIQUE (ip))')
            connection.commit()
            connection.close()
            logging.warn('new table created: peers')

    def list_all(self):
        connection = sqlite3.connect(self.db_path)
        connection.row_factory = dict_factory
        result = list(connection.cursor().execute('SELECT * FROM peers').fetchall())
        connection.close()
        return result

    def add(self, ip, port, secret):
        connection = sqlite3.connect(self.db_path)
        try:
            connection.cursor().execute('INSERT INTO peers VALUES (?, ?, ?)', (ip, port, secret))
        except sqlite3.IntegrityError:
            logging.warning('skipping duplicate entry')
            connection.close()
            return
        connection.commit()
        connection.close()

    def remove(self, ip):
        connection = sqlite3.connect(self.db_path)
        connection.cursor().execute('DELETE FROM peers WHERE ip=?', (ip,))
        connection.commit()
        connection.close()


class ServersManager:
    def __init__(self, path_to_db):
        self.db_path = path_to_db
        connection = sqlite3.connect(path_to_db)
        if 'servers' not in get_table_names(connection.cursor()):
            logging.warn('servers table not found')
            connection.execute(
                'CREATE TABLE servers (ip TEXT PRIMARY KEY , port TEXT, secret TEXT, UNIQUE (ip))')
            connection.commit()
            connection.close()
            logging.warn('new table created: servers')

    def list_all(self):
        connection = sqlite3.connect(self.db_path)
        connection.row_factory = dict_factory
        result = list(connection.cursor().execute('SELECT * FROM servers').fetchall())
        connection.close()
        return result

    def add(self, ip, port, secret):
        connection = sqlite3.connect(self.db_path)
        try:
            connection.cursor().execute('INSERT INTO servers VALUES (?, ?, ?)', (ip, port, secret))
        except sqlite3.IntegrityError:
            logging.warning('skipping duplicate entry')
            connection.close()
            return
        connection.commit()
        connection.close()

    def remove(self, ip):
        connection = sqlite3.connect(self.db_path)
        connection.cursor().execute('DELETE FROM servers WHERE ip=?', (ip,))
        connection.commit()
        connection.close()


def test():
    logging.basicConfig(level=5,
                        format='%(asctime)s %(levelname)-8s %(message)s',
                        datefmt='%Y-%m-%d %H:%M:%S')
    import os

    db_path = 'temp_jizi.db'

    logging.info('socks test')

    new_items = [{'port': '1', 'secret': '1 no secrets'},
                 {'port': '2', 'secret': '2 no secrets'},
                 {'port': '3', 'secret': '3 no secrets'},
                 {'port': '4', 'secret': '4 no secrets'}]

    sm = SocksManager(db_path)
    for item in sm.list_all():
        print(item)

    logging.info('add items')
    for item in new_items:
        sm.add(item['port'], item['secret'])
    for item in sm.list_all():
        print(item)

    logging.info('remove items')
    for item in new_items:
        sm.remove(item['port'])
    assert (len(sm.list_all()) == 0)

    logging.info('peers test')

    new_items = [{'ip': '1foobarr', 'port': '1', 'secret': '1 no secrets'},
                 {'ip': '2foobarr', 'port': '2', 'secret': '1 no secrets'},
                 {'ip': '3foobarr', 'port': '3', 'secret': '1 no secrets'},
                 {'ip': '4foobarr', 'port': '4', 'secret': '1 no secrets'}]

    sm = PeersManager(db_path)
    for item in sm.list_all():
        print(item)

    logging.info('add items')
    for item in new_items:
        sm.add(item['ip'], item['port'], item['secret'])
    for item in sm.list_all():
        print(item)

    logging.info('remove items')
    for item in new_items:
        sm.remove(item['ip'])
    assert (len(sm.list_all()) == 0)

    logging.info('servers test')
    new_items = [{'port': '1', 'secret': '1 no secrets', 'ip': '1ffoobar'},
                 {'port': '2', 'secret': '2 no secrets', 'ip': '2ffoobar'},
                 {'port': '3', 'secret': '3 no secrets', 'ip': '3ffoobar'},
                 {'port': '4', 'secret': '4 no secrets', 'ip': '4ffoobar'}]
    sm = ServersManager(db_path)
    for item in sm.list_all():
        print(item)

    logging.info('add items')
    for item in new_items:
        sm.add(item['ip'], item['port'], item['secret'])
    for item in sm.list_all():
        print(item)

    logging.info('remove items')
    for item in new_items:
        sm.remove(item['ip'])
    assert (len(sm.list_all()) == 0)

    os.unlink(db_path)


if __name__ == '__main__':
    test()
