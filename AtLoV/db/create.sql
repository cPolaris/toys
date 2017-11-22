/*
  an album contains many pic
  id: the unique id
  url: url to the album page
  title: of the album
 */
CREATE TABLE album (
  id    INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT,
  url   TEXT,
  catName TEXT,
  metaJson TEXT,
  count INTEGER,
  rating INTEGER,
  UNIQUE (url)
);

/*
  id: the unique id
  url: to retrieve the pic
  albumid: id of the album it belongs to
  albumseq: sequence number within the album, starts from 1
 */
CREATE TABLE pic (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  url      TEXT,
  albumid  INTEGER,
  albumseq INTEGER,
  FOREIGN KEY (albumid) REFERENCES album (id),
  UNIQUE (albumid, albumseq)
);

CREATE TABLE category (
  id   INTEGER PRIMARY KEY,
  name TEXT
);

-- INSERT INTO category (id, name) VALUES


CREATE TABLE urlprefix (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  prefix TEXT UNIQUE,
  comment TEXT
);

-- INSERT INTO urlprefix (prefix, comment) VALUES