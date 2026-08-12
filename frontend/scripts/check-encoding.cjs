const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..', 'src');
const extensions = new Set(['.css', '.js', '.jsx', '.ts', '.tsx']);

const mojibakePatterns = [
  /\u00c3[\u0080-\u00bf]/,
  /\u00c4[\u0080-\u00bf]/,
  /\u00c6[\u0080-\u00bf]/,
  /\u00e1[\u00ba\u00bb][\u0080-\u00bf]/,
  /[\u00c3\u00c4\u00c6][\u0080-\u024f]/,
  /\u00e1[\u00ba\u00bb]/,
  /\u00c2[\u0080-\u00bf]/,
  /\ufffd/
];

function walk(directory) {
  const files = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const fullPath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      files.push(...walk(fullPath));
    } else if (extensions.has(path.extname(entry.name))) {
      files.push(fullPath);
    }
  }
  return files;
}

let hasError = false;

for (const file of walk(root)) {
  const text = fs.readFileSync(file, 'utf8');
  const lines = text.split(/\r?\n/);
  lines.forEach((line, index) => {
    if (mojibakePatterns.some((pattern) => pattern.test(line))) {
      hasError = true;
      console.error(`${path.relative(process.cwd(), file)}:${index + 1}: ${line}`);
    }
  });
}

if (hasError) {
  console.error('Detected likely UTF-8 mojibake. Fix the text before building.');
  process.exit(1);
}

console.log('Encoding check passed.');
