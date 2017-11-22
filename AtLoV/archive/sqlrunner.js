const sqlite = require('sqlite');
const DB_PATH = '';


async function allAlbums() {
  const db = await sqlite.open(DB_PATH);
  const res = await db.all('select * from album');
  await db.close();
  return res;
}

async function latestAlbums(count) {
  const db = await sqlite.open(DB_PATH);
  const res = await db.all('select * from album ORDER BY id DESC LIMIT ?', count);
  await db.close();
  return res;
}

async function picsInAlbumId(id) {
  const db = await sqlite.open(DB_PATH);
  const pics = await db.all('select * from pic WHERE albumid = ? ORDER BY albumseq ASC', id);
  await db.close();
  return pics;
}

async function picsInAlbumUrl(url) {
  const db = await sqlite.open(DB_PATH);
  const albumId = await db.get('select id from album WHERE url = ?', url);
  console.log(albumId);
  const pics = await db.all('select * from pic WHERE albumid = ? ORDER BY albumseq ASC', albumId.id);
  await db.close();
  return pics;
}

async function addAlbums(albums) {
  const db = await sqlite.open(DB_PATH);
  for (let album of albums) {
    await db.run('INSERT OR IGNORE INTO album (title, url, catName, count) VALUES (?,?,?,?)', album.title, album.url, album.catName, album.count);
  }
  await db.close();
  console.log(`added ${albums.length} albums!`);
}

async function addPics(albumUrl, picUrls) {
  const db = await sqlite.open(DB_PATH);
  const albumId = await db.get('SELECT id FROM album WHERE url = ?', albumUrl);
  for (let ind = 0; ind < picUrls.length; ind++) {
    await db.run('INSERT OR IGNORE INTO pic (url, albumid, albumseq) VALUES (?,?,?)', picUrls[ind], albumId.id, ind+1);
  }
  await db.close();
  console.log(`added ${picUrls.length} pics to ${albumUrl} (id: ${albumId.id})`);
}

module.exports = {latestAlbums, picsInAlbumUrl, addAlbums, addPics, allAlbums, picsInAlbumId};
