const fs = require('fs');
const path = require('path');

// 最小有效 PNG 文件 (1x1 像素，灰色)
const createMinimalPNG = (r, g, b) => {
  // PNG 文件头
  const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);

  // IHDR chunk
  const ihdrData = Buffer.alloc(13);
  ihdrData.writeUInt32BE(1, 0);  // width
  ihdrData.writeUInt32BE(1, 4);  // height
  ihdrData[8] = 8;               // bit depth
  ihdrData[9] = 2;               // color type (RGB)
  ihdrData[10] = 0;              // compression
  ihdrData[11] = 0;              // filter
  ihdrData[12] = 0;              // interlace

  const ihdrCrc = crc32(Buffer.concat([Buffer.from('IHDR'), ihdrData]));
  const ihdrChunk = Buffer.alloc(25);
  ihdrChunk.writeUInt32BE(13, 0);
  ihdrChunk.write('IHDR', 4);
  ihdrData.copy(ihdrChunk, 8);
  ihdrChunk.writeUInt32BE(ihdrCrc, 21);

  // IDAT chunk (compressed image data)
  const rawData = Buffer.from([0, r, g, b]); // filter byte + RGB
  const zlib = require('zlib');
  const compressed = zlib.deflateSync(rawData);

  const idatCrc = crc32(Buffer.concat([Buffer.from('IDAT'), compressed]));
  const idatChunk = Buffer.alloc(compressed.length + 12);
  idatChunk.writeUInt32BE(compressed.length, 0);
  idatChunk.write('IDAT', 4);
  compressed.copy(idatChunk, 8);
  idatChunk.writeUInt32BE(idatCrc, compressed.length + 8);

  // IEND chunk
  const iendCrc = crc32(Buffer.from('IEND'));
  const iendChunk = Buffer.alloc(12);
  iendChunk.writeUInt32BE(0, 0);
  iendChunk.write('IEND', 4);
  iendChunk.writeUInt32BE(iendCrc, 8);

  return Buffer.concat([signature, ihdrChunk, idatChunk, iendChunk]);
};

// CRC32 计算
function crc32(buf) {
  let crc = -1;
  for (let i = 0; i < buf.length; i++) {
    crc = (crc >>> 8) ^ crc32Table[(crc ^ buf[i]) & 0xff];
  }
  return (crc ^ -1) >>> 0;
}

const crc32Table = new Int32Array(256);
for (let i = 0; i < 256; i++) {
  let c = i;
  for (let j = 0; j < 8; j++) {
    c = (c & 1) ? (0xedb88320 ^ (c >>> 1)) : (c >>> 1);
  }
  crc32Table[i] = c;
}

const outputDir = path.join(__dirname, '..', 'src', 'static', 'images');

if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

const icons = [
  { name: 'tab-home', color: [153, 153, 153] },
  { name: 'tab-home-active', color: [74, 144, 217] },
  { name: 'tab-university', color: [153, 153, 153] },
  { name: 'tab-university-active', color: [74, 144, 217] },
  { name: 'tab-qa', color: [153, 153, 153] },
  { name: 'tab-qa-active', color: [74, 144, 217] },
  { name: 'tab-profile', color: [153, 153, 153] },
  { name: 'tab-profile-active', color: [74, 144, 217] },
];

icons.forEach(icon => {
  const png = createMinimalPNG(...icon.color);
  const filePath = path.join(outputDir, `${icon.name}.png`);
  fs.writeFileSync(filePath, png);
  console.log(`Created: ${filePath}`);
});

console.log('\n所有图标已创建完成！');
