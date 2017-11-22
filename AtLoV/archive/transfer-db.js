/*
WTF my database contains duplicate records. I don't know which program did this
but NOT COOL. I want to re-insert all the records sans the duplicates.
 */

const sqlite = require('sqlite');
const OLD_DB_PATH = '';
const NEW_DB_PATH = '';  // tables must exist in new DB

(async () => {
  const oldDB = await sqlite.open(OLD_DB_PATH);
  const newDB = await sqlite.open(NEW_DB_PATH);

  const oldAlbums = await oldDB.all('SELECT * FROM album ORDER BY id');
  const oldIdDict = {};

  await newDB.run('BEGIN TRANSACTION');

  for (let oa of oldAlbums) {
    const picsInAlbum = await oldDB.all('SELECT * FROM pic WHERE albumid = ?', oa.id);

    if (picsInAlbum.length !== oa.count) {
      console.log(`inconsistency for album ID ${oa.id}`);
      continue;
    }

    oldIdDict[oa.url] = oa.id;

    // transfer album records
    await newDB.run('INSERT INTO album (title, url, catName, count, rating) VALUES (?, ?, ?, ?, ?)',
      canonializedTitle(oa.title), oa.url, oa.catName, oa.count, oa.rating);
  }


  // now the album IDs have changed
  // we'll have to re-label them for all pictures
  const newAlbums = await newDB.all('SELECT * FROM album ORDER BY id');
  for (let newAlb of newAlbums) {
    const pics = await oldDB.all('SELECT * FROM pic WHERE albumid = ?', oldIdDict[newAlb.url]);

    if (pics.length !== newAlb.count) {
      console.log(`inconsistency for album ID ${newAlb.id}`);
      continue;
    }

    for (let pic of pics) {
      await newDB.run('INSERT INTO pic (url, albumid, albumseq) VALUES (?, ?, ?)',
        trim_pic_url(pic.url), newAlb.id, pic.albumseq);
    }
  }

  await newDB.run('END TRANSACTION');

  await newDB.close();
  await oldDB.close();
})();


/**
 * Trims the URL. So we can later switch the host part of the URL
 * @param url
 * @returns {*}
 */
function trim_pic_url(url) {
  let slash_count = 0;
  let res;
  for (let i = 0; i < url.length; i++) {
    if (url[i] === '/' && ++slash_count === 3) {
      res = url.substring(i + 1);
      break;
    }
  }
  return res;
}

function canonializedTitle(str) {
  return str.replace(/[\\/<>:"|?*]/g, ' ');
}

// INSERT INTO album (title, url, catName, count) VALUES ();
