// Render SVG previews for visual QA. Pass the folder containing the sharp module.
const fs = require('fs');
const path = require('path');
const sharp = require(process.argv[2] ? path.join(process.argv[2], 'sharp') : 'sharp');
const figureRoot = path.resolve(__dirname, '..');
const qaRoot = path.resolve(figureRoot, '../evidence/visual-qa');

(async () => {
  fs.mkdirSync(qaRoot, {recursive: true});
  const files = fs.readdirSync(figureRoot).filter(f => f.endsWith('.svg')).sort();
  const tiles = [];
  for (let i = 0; i < files.length; i++) {
    const input = path.join(figureRoot, files[i]);
    const preview = path.join(qaRoot, files[i].replace(/\.svg$/, '.png'));
    await sharp(input).png().toFile(preview);
    const tile = await sharp(input).resize(460, 360, {fit: 'contain', background: '#ffffff'}).png().toBuffer();
    tiles.push({input: tile, left: (i % 3) * 460, top: Math.floor(i / 3) * 360});
  }
  await sharp({create: {width: 1380, height: Math.ceil(files.length / 3) * 360, channels: 4, background: '#ffffff'}})
    .composite(tiles).png().toFile(path.join(qaRoot, 'contact-sheet.png'));
  console.log(`Rendered ${files.length} full previews and one contact sheet`);
})().catch(err => { console.error(err); process.exit(1); });
