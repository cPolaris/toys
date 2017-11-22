const fs = require('fs');
const puppeteer = require('puppeteer');
const sqlite = require('sqlite');
const http = require('http');
const serveStatic = require('serve-static');
const winston = require('winston');

/*********************************************************************
 * configuration constants
 ********************************************************************/
const UA = '';
// @todo JWT might needs update after every visit
const COOKIE_JWT = {
  name: '',
  value: '',
};
const COOKIE_SENTINEL = {
  name: '',
  value: '',
};

const USERNAME = '';
const PASSWORD = '';
const COOKIE_PATH = '';
const SCREENSHOT_PATH = '';

const DB_PATH = '';
const FAKE_PORT = 8000;
const LOG_FILE = '';

const CATEGORIES = [];

/*********************************************************************
 * configure logger
 ********************************************************************/

winston.configure({
  level: 'info',
  transports: [
    new (winston.transports.Console)(),
    new (winston.transports.File)({
      json: false,
      filename: LOG_FILE,
      timestamp: () => new Date().toLocaleString(),
      formatter: options => {
        return `[${options.timestamp()}] ${options.message}`;
      }
    })
  ]
});

/*********************************************************************
 * spin up local server for image request redirection
 ********************************************************************/

const servePublic = serveStatic('./public');

const server = http.createServer((req, res) => {
  servePublic(req, res);
});

server.listen(FAKE_PORT);

/*********************************************************************
 * run jobs
 ********************************************************************/

(async () => {
  winston.info('\n----------------------------------------------------\n');
  const browser = await puppeteer.launch({
    headless: true
  });
  const page = await browser.newPage();
  await prologue(page);

  await page.waitFor(10000);

  // do something here...

  await epilogue(page);
  await browser.close();
  process.exit();
})().catch(e => {
  winston.info(e);
});


/*********************************************************************
 * common actions
 ********************************************************************/

async function gotoUrlDebug(page, url) {
  await page.goto(url, {waitUntil: 'networkidle'});
  await page.screenshot({
    path: `${SCREENSHOT_PATH}/last_visit.png`,
    fullPage: true
  });
  return page;
}

/**
 * Fetch all albums, album meta, and pics in album on given url,
 * also save all of them to DB
 * @param page
 * @param url
 * @returns {Promise.<void>}
 */
async function saveAlbsMetaPicsOnPage(page, url) {
  const db = await sqlite.open(DB_PATH);

  winston.info(`saveAlbsMetaPicsOnPage ${url}`);
  winston.info(`GOTO: ${url}`);
  await page.goto(url, {waitUntil: 'networkidle'});
  await page.waitFor(2000);

  // 1. fetch and save all albums
  const freshAlbums = await fetchAlbums(page);
  freshAlbums.reverse();  // reverse order is in-time order
  const oldAlbumUrls = await db.all('SELECT url FROM album');

  // remove duplicated albums
  // @todo make this an efficient subroutine. Maybe use hashing
  const albumsToAdd = freshAlbums.filter(alb => {
    return -1 === oldAlbumUrls.findIndex(oldUrl => oldUrl.url === alb.url);
  });

  // put new albums into DB
  winston.info(`${albumsToAdd.length} new albums to add`);

  if (albumsToAdd.length > 0) {
    for (let nalb of albumsToAdd) {
      winston.info(`[${nalb.url}] ${nalb.catName} ${nalb.count}p ${nalb.title}`);
    }

    await db.run('BEGIN TRANSACTION');

    for (let album of albumsToAdd) {
      await db.run('INSERT INTO album (title, url, catName, count) VALUES (?,?,?,?)',
        album.title, album.url, album.catName, album.count);
    }

    await db.run('END TRANSACTION');
    winston.info(`${albumsToAdd.length} new albums committed into DB`);
  }

  // 2. for each album, fetch pics and meta, then put in DB
  for (let alb of albumsToAdd) {
    await page.waitFor(10000);
    winston.info(`GOTO: ${alb.url}`);
    await page.goto(alb.url, {waitUntil: 'networkidle'});
    await page.waitFor(2000);

    const metaObj = await fetchMetaObj(page);
    const metaObjStr = JSON.stringify(metaObj);
    const picUrls = await fetchPics(page);
    const albumId = await db.get('SELECT id FROM album WHERE url = ?', alb.url);

    await db.run('BEGIN TRANSACTION');
    await db.run('UPDATE album SET metaJson = ? WHERE id = ?', metaObjStr, albumId.id);
    winston.info(`updated ${alb.url} meta: ${metaObjStr}`);
    for (let ind = 0; ind < picUrls.length; ind++) {
      await db.run('INSERT INTO pic (url, albumid, albumseq) VALUES (?,?,?)',
        picUrls[ind], albumId.id, ind + 1);
    }
    await db.run('END TRANSACTION');
    winston.info(`${picUrls.length} new pics committed into DB`);
  }

  await db.close();
}

/**
 * Fetch all albums, album meta, and pics in album on given
 * category and page number.
 * Also save all of them to DB
 * @param page
 * @param catName
 * @param pageNum
 * @return {Promise.<void>}
 */
async function saveAlbsMetaPicsOnCategoryPage(page, catName, pageNum) {
  const catNum = CATEGORIES.find(entry => entry.name === catName).id;
  const builtUrl = ``;
  await saveAlbsMetaPicsOnPage(page, builtUrl);
}

/**
 * Scan through the DB, if any album metaJson is missing,
 * go to the URL and fetch it
 * @param page initialized empty page
 * @returns {Promise.<void>}
 */
async function fixAlbumMetas(page) {
  const db = await sqlite.open(DB_PATH);
  let albums = await db.all('SELECT * FROM album WHERE metaJson ISNULL');
  let ind = 0;
  for (let alb of albums) {
    await page.waitFor(10000);
    await page.goto(alb.url, {waitUntil: 'networkidle'});
    await page.waitFor(2000);
    const metaObj = await fetchMetaObj(page);
    await db.run('UPDATE album SET metaJson = ? WHERE id = ?', JSON.stringify(metaObj), alb.id);
    console.log(`Done: ${alb.url} ${JSON.stringify(metaObj)}`);
  }
  await db.close();
}


/*********************************************************************
 * utilities
 ********************************************************************/

/**
 * Use the page to go to home page, enter credentials and login
 * @param {*} page
 * @return array of cookies after login
 */
async function login(page) {
  await page.goto('');
  await page.waitFor(1000);
  await page.click('');
  await page.type(USERNAME);
  await page.click('');
  await page.type(PASSWORD);
  await page.click('');
  await page.waitForSelector('');
  return await page.cookies();
}

/**
 * Setup UA, blocking, and cookie.
 * @param {*} page a brand new page. After this, this page may
 * be used to navigate website URLs
 */
async function prologue(page) {
  // don't tell'em we're using headless chrome!!
  await page.setUserAgent(UA);
  // don't actually load the images!!
  await page.setRequestInterceptionEnabled(true);

  // set up image request blocker
  page.on('request', request => {
    if (request.resourceType === 'Image') {
      request.continue({url: `http://localhost:${FAKE_PORT}/huaji.png`});
    } else {
      request.continue();
    }
  });

  winston.info('visiting home page to set cookie');
  await page.goto('');

  let cookies = fs.readFileSync(COOKIE_PATH);
  cookies = JSON.parse(cookies);
  await page.setCookie(...cookies);
}

/**
 * Save current cookies
 * @param page
 * @returns {Promise.<void>}
 */
async function epilogue(page) {
  const cookies = await page.cookies();
  const cookedCookies = [];

  // setting anything other than name and value seems to give us trouble
  // so we only save those two fields
  for (let ck of cookies) {
    if (ck.name === '' || ck.name === '') {
      cookedCookies.push((({name, value}) => ({name, value}))(ck));
    }
  }

  fs.writeFileSync(COOKIE_PATH, JSON.stringify(cookedCookies));
}

/**
 * List all albums on the given URL with the given browser page
 * @param page must already on the website page from which you wish
 * to fetch information
 * @param explicitOnly
 * @returns {Promise.<Array>}
 */
async function fetchAlbums(page, explicitOnly = true) {
  let albumHandles = await page.$$('.');
  let albums = [];
  winston.info(`Counting ${albumHandles.length} albumHandles`);

  for (let hdl of albumHandles) {
    const converted = await convertHandleToAlbum(page, hdl, explicitOnly);
    if (converted) {
      albums.push(converted);
    }
    hdl.dispose();
  }

  return albums;
}


/**
 * Visit the album URL, fetch meta attributes and all pictures within the album
 * @param page must already on the album page from which you wish
 * to fetch information
 * @returns {Promise.<Array>} [metaObj, pictureUrls]
 */
async function fetchMetaAndPics(page) {
  const metaObj = await fetchMetaObj(page);
  const pictureUrls = await fetchPics(page);
  return [metaObj, pictureUrls];
}


/**
 * Given page that is already on an album page,
 * return the meta object for that album
 * @param page must already on the album page from which you wish
 * to fetch information
 * @returns {Promise.<{}>}
 */
async function fetchMetaObj(page) {
  const metaObj = {};
  const tagHandles = await page.$$('.');

  for (let tagHdl of tagHandles) {
    const kvs = await page.evaluate(el => el.querySelectorAll('li').length, tagHdl);
    const keyStr = await page.evaluate(el => el.querySelectorAll('li')[0].innerText, tagHdl);
    const valStrs = [];
    for (let ind = 1; ind < kvs; ind++) {
      valStrs.push(await page.evaluate((el, i) => el.querySelectorAll('li')[i].innerText, tagHdl, ind));
    }
    metaObj[keyStr] = valStrs;
  }

  return metaObj;
}

/**
 * Given page that is already on an album page,
 * return the list of pictures in the album
 * @param page must already on the album page from which you wish
 * to fetch information
 * @returns {Promise.<Array>}
 */
async function fetchPics(page) {
  // click first picture to open overlay
  await page.click('.');
  await page.waitFor(1000);

  let pictureHandles = await page.$$('.');
  winston.info(`Hovering through ${pictureHandles.length} pictures in album`);
  for (let hdl of pictureHandles) {
    await hdl.hover();
    await page.waitFor(200);
  }

  const pictureUrls = [];
  for (let hdl of pictureHandles) {
    let rawUrl = await page.evaluate(els => els.src, hdl);
    pictureUrls.push(trim_pic_url(rawUrl));
    hdl.dispose();
  }

  return pictureUrls;
}

/**
 * Use given page to
 * convert an element handle to object representation of an album
 * @param {*} page
 * @param {*} handle
 * @param {*} explicitOnly
 * @return null if not explicit and explicit is preferred
 * {
 *   title, catName, count, url
 * }
 */
async function convertHandleToAlbum(page, handle, explicitOnly) {
  const explicit = await page.evaluate(el => el.querySelectorAll('').length, handle);
  if (explicitOnly && !explicit) return null;

  let url = await page.evaluate(el => el.href, handle);
  // might be useful?...
  // if (url[url.length-1] === '/') url = url.substring(0, url.length-1);

  const albumText = await page.evaluate(el => el.innerText, handle);
  const albumTextSplit = albumText.split('\n');
  let catName = null;
  let title = null;
  let rawCount = null;
  let catAndCount = null;

  if (albumTextSplit.length === 2) {
    [catAndCount, title] = albumTextSplit;
    [catName, rawCount] = catAndCount.split(' ');
  } else {
    // No pictures in this album
    return null;
  }

  const count = parseInt(rawCount.match(/^\((\d+)p\)$/)[1]);
  if (isNaN(count)) throw Error(`parseInt error on ${url} raw: ${rawCount}`);

  title = canonializedTitle(title);

  return {title, catName, count, url};
}


function loadCookies(path) {

}

function saveCookies(path, cookies) {

}

/**
 * https://msdn.microsoft.com/en-us/library/aa365247
 * Remove reserved characters from album title
 * @param str
 * @returns {string}
 */
function canonializedTitle(str) {
  return str.replace(/[\\/<>:"|?*]/g, ' ');
}

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
