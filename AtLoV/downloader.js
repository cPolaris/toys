const fs = require('fs');
const https = require('https');
const sqlite = require('sqlite');
const ProgressBar = require('progress');
const winston = require('winston');

const DB_PATH = '';
const DOWNLOAD_FOLDER_PATH = '';
const LOG_FILE = '';

const HOST_DICT = {
};

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
 * run jobs
 ********************************************************************/

(async () => {
  winston.info('\n----------------------------------------------------\n');
  const db = await sqlite.open(DB_PATH);
  await downloadAllMissingAlbums(db, 2000);
  await db.close();
})();

/*********************************************************************
 * utilities
 ********************************************************************/

// /**
//  * For each album exists in download folder:
//  *   check the pictures under the directory, download missing pictures
//  * @param db
//  * @returns {Promise.<void>}
//  */
// async function fixDownloadedAlbums(db) {
// }

/**
 * Make sure the category folder and album folder are in place
 * for the given album object.
 * @param {{ id, title, url, catName, count }} album
 */
async function mkdirForAlbum(album) {
  const catFolderPath = `${DOWNLOAD_FOLDER_PATH}/${album.catName}`;
  const albumFolderPath = `${catFolderPath}/${album.title}`;

  if (!fs.existsSync(catFolderPath)) {
    fs.mkdirSync(catFolderPath);
    winston.info(`directory created for ${album.catName}`);
  }

  if (!fs.existsSync(albumFolderPath)) {
    fs.mkdirSync(albumFolderPath);
    winston.info(`directory created for ${album.catName}/${album.title}`);
  }

  return albumFolderPath;
}


async function albumExitsInDownload(album) {
  return fs.existsSync(`${DOWNLOAD_FOLDER_PATH}/${album.catName}/${album.title}`);
}

/**
 * Look through all albums in database,
 * download all albums that does not exist in download folder.
 * An album is considered exist if a folder named the album title
 * exists in corresponding category folder
 * limit: limit number of pictures to download for this run
 * @returns {Promise.<void>}
 */
async function downloadAllMissingAlbums(db, limit = 800) {
  winston.info(`Begin downloadAllMissingAlbums. Limit at ${limit} pictures for current run`);
  let allAlbums = await db.all('SELECT * FROM album');
  allAlbums = allAlbums.reverse();
  const dbAlbumSize = allAlbums.length;
  const thisRunList = [];
  let downloadedSize = 0;
  let thisRunPicCount = 0;

  for (let alb of allAlbums) {
    if (!await albumExitsInDownload(alb)) {
      thisRunPicCount += alb.count;
      if (thisRunPicCount > limit) {
        thisRunPicCount -= alb.count;
        break;
      }
      thisRunList.push(alb);
    } else {
      downloadedSize++;
    }
  }

  const thisRunCount = thisRunList.length;
  winston.info(`This run ${thisRunCount} albums ${thisRunPicCount} pics`);
  for (let i = 0; i < thisRunCount; i++) {
    winston.info(`${i + 1} of ${thisRunCount}`);
    await downloadAlbum(db, thisRunList[i]);
    if (i < thisRunCount - 1) await waitAbout(30000, 0);
  }
}


/**
 * Download the given album object
 * @param db a db connection
 * @param {{ id, title, url, catName, count }} album
 * @returns {Promise.<void>}
 */
async function downloadAlbum(db, album) {
  winston.info(`Downloading ${album.title}, ${album.count}p`);

  const picUrls = await db.all('SELECT url FROM pic WHERE albumid = ? ORDER BY albumseq ASC', album.id);

  // this sanity check is kind of redundant...
  if (picUrls.length !== album.count) {
    winston.info(`!!!!!WARNING!!!!! Not downloading!`);
    winston.info(`!!!!!WARNING!!!!! album.count:${album.count} but only ${picUrls.length} pics in DB`);
    winston.info(`!!!!!WARNING!!!!! run metafetcher first to fix the DB`);
    return;
  }

  const albumPath = await mkdirForAlbum(album);
  const bar = new ProgressBar('[:bar] :current/:total elapsed: :elapseds', {total: album.count});

  for (let ind = 0; ind < picUrls.length; ind++) {
    await waitAbout(800);
    const pUrl = picUrls[ind].url;
    const fileSaveName = pUrl.replace(/\//g, '_');
    const file = fs.createWriteStream(`${albumPath}/${ind + 1}-${fileSaveName}`);

    getPic(pUrl, album.url, resp => {
      resp.pipe(file);
      file.on('finish', () => {
        bar.tick();
      });
    });
  }
}

async function waitAbout(millis, fluctuationPercentage = 0.7) {
  const waitT = Math.floor(millis + (Math.random() - 0.5) * fluctuationPercentage * millis);
  return new Promise(resolve => {
    setTimeout(() => resolve(waitT), waitT);
  });
}


/*********************************************************************
 * request related
 ********************************************************************/

function getPic(pUrl, albumUrl, cb, hostKey='US') {
  const req = https.get({
    hostname: HOST_DICT[hostKey],
    path: `/${pUrl}`,
    headers: {
      'Accept': 'image/webp,image/apng,image/*,*/*;q=0.8',
      // 'Accept-Encoding': 'gzip, deflate, br',  @todo accept compression
      'Accept-Language': 'en-US,en;q=0.8,zh-CN;q=0.6,zh;q=0.4',
      'Cache-Control': 'no-cache',
      'Pragma': 'no-cache',
      'Referer': albumUrl,  // oh geez it checks referer url
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36',
    }
  }, cb);
  req.on('error', (e) => {
    winston.info(`Problem with request ${HOST_DICT[hostKey]} ${pUrl} ${albumUrl}`);
    winston.info(e.message);
  });
}
