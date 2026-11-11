// Copies the guide sources (docs/guide, Markdown + SVG) into src/generated/guide, which angular.json serves as
// assets under /guide. Angular only accepts asset inputs inside the workspace, hence the copy (git-ignored).
import { cpSync, existsSync, mkdirSync, readdirSync, rmSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const source = resolve(root, '../docs/guide');
const target = join(root, 'src/generated/guide');

if (!existsSync(source)) {
  console.error(`copy-guide: ${source} does not exist`);
  process.exit(1);
}
rmSync(target, { recursive: true, force: true });
mkdirSync(target, { recursive: true });
for (const name of readdirSync(source)) {
  if (/\.(md|svg)$/.test(name)) {
    cpSync(join(source, name), join(target, name));
  }
}
console.log(`copy-guide: ${source} -> ${target}`);
