// 解析 class 常量池，列出 Block.class 中所有方法名（含 isRedstone 相关）
import { readFileSync } from 'node:fs';

const d = readFileSync('E:/trae/program/CC_RC/.tools/Block.class');
let pos = 8;
const count = d.readUInt16BE(pos); pos += 2;
const cp = new Array(count);
let i = 1;
while (i < count) {
  const tag = d[pos]; pos += 1;
  if (tag === 1) { // UTF8
    const ln = d.readUInt16BE(pos); pos += 2;
    cp[i] = d.toString('utf8', pos, pos + ln); pos += ln;
  } else if (tag === 7 || tag === 8 || tag === 16 || tag === 19 || tag === 20) { pos += 2; }
  else if (tag === 15) { pos += 3; }
  else if (tag === 3 || tag === 4 || tag === 9 || tag === 10 || tag === 11 || tag === 12 || tag === 17 || tag === 18) { pos += 4; }
  else if (tag === 5 || tag === 6) { pos += 8; i += 1; } // long/double
  else { console.log('unknown tag', tag, 'at', i); break; }
  i += 1;
}
const strs = cp.filter((s) => typeof s === 'string');
console.log('=== isRedstone / conductor 相关 ===');
for (const s of strs) {
  if (/redstone|Redstone|conductor|Conductor|signal|Signal/i.test(s)) console.log(s);
}
console.log('\n=== 全部方法名（含 Redstone 字样的）===');
// 方法名在 Methodref 中通过 NameAndType 引用，这里简单打印含 Redstone 的常量池字符串
const redstone = strs.filter((s) => /redstone|Redstone/i.test(s));
console.log(redstone.join('\n'));