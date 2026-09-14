// ccrc-broadcast.mjs —— dsh web 启动插件（ESM，.mjs 强制 ESM 解析）
//
// 挂载方式：在 web profile 的 cordis.patch.yml 中 insert：
//   - insert:
//       - id: ccrc-broadcast
//         name: './ccrc-broadcast.mjs'
//
// 作用：dsh web（桌面软件 / Web GUI）启动时，自动以 --daemon 方式启动
// QQ 播报守护脚本（qq_broadcast.mjs），实现"打开软件 / 打开这个对话即启动"。
// 守护脚本自带单实例保护（锁文件），重复启动会被跳过。
//
// 注意：必须使用 .mjs 扩展名 —— web profile 的 package.json 无 "type": "module"，
// .js 会按 CommonJS 解析，ESM 语法（export/import）会直接抛错导致 dsh web 崩溃。
import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import { join } from 'node:path';

const name = 'ccrc-broadcast';

// 工作区中的守护脚本与运行环境
const CC_RC_DIR = 'E:/trae/program/CC_RC';
const BROADCAST_SCRIPT = join(CC_RC_DIR, 'qq_broadcast.mjs');
const NODE = 'E:/Program Files/DeepSeek_Harness_Desktop/DeepSeek Harness/resources/runtime/win32-x64/node.exe';

function startBroadcastDaemon(logger) {
  if (!existsSync(NODE) || !existsSync(BROADCAST_SCRIPT)) {
    logger?.warn?.('[ccrc-broadcast] 守护脚本或 Node 缺失，跳过启动', {
      node: NODE,
      script: BROADCAST_SCRIPT,
    });
    return null;
  }
  // detached：守护进程不随 dsh web 退出而终止；
  // 单实例锁由 qq_broadcast.mjs 内部机制保证（跨进程锁文件）。
  const child = spawn(NODE, [BROADCAST_SCRIPT, '--hello'], {
    cwd: CC_RC_DIR,
    env: { ...process.env },
    stdio: ['ignore', 'pipe', 'pipe'],
    detached: true,
  });
  child.unref();
  child.stdout?.on('data', (d) => logger?.info?.('[ccrc-broadcast]', String(d).trimEnd()));
  child.stderr?.on('data', (d) => logger?.warn?.('[ccrc-broadcast]', String(d).trimEnd()));
  logger?.info?.('[ccrc-broadcast] 已启动 QQ 播报守护（pid=%s）', child.pid);
  return child;
}

export function apply(ctx, config = {}) {
  const logger = ctx?.logger
    ? (typeof ctx.logger === 'function' ? ctx.logger(name) : ctx.logger)
    : console;
  // 延迟到 dsh web 完全启动后触发，避免启动竞态
  const delayMs = Number(config?.delayMs ?? 3000);
  setTimeout(() => {
    try {
      startBroadcastDaemon(logger);
    } catch (e) {
      logger?.error?.('[ccrc-broadcast] 启动守护失败:', e);
    }
  }, delayMs);
}

export default { apply, name };