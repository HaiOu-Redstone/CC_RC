// QQ 播报守护脚本（qq_broadcast.mjs）
//
// 功能：
//  1. 阶段思考摘要：监听绑定会话的日志，聚合每步（step）的推理/文本增量，
//     在 step 边界把简短摘要推送到 QQ（节流防刷屏）；
//  2. 发问转发：识别 ask_user_question / approval/asked 事件，把问题原文
//     推送到 QQ（群聊需 @ 机器人才能回复）；
//  3. 意外错误通知：识别 turn/end 的 error/stopped/blocked 与工具失败，
//     推送错误摘要到 QQ。
//
// 设计说明：
//  - 不改动 dsh-im 插件本体（其运行加载的是打包产物 lib/index.js，改源码
//    易被 pnpm 重装覆盖），而是作为独立守护进程增量读取会话日志。
//  - 会话日志为 .jsonl.zstd：多帧 zstd 顺序追加，每帧一行 JSON。脚本记录
//    已读偏移，每次只解压新增帧（按魔数 28 b5 2f fd 定位帧边界）。
//  - QQ 发送走官方开放平台 REST API（与插件同源）：获取 access_token 后
//    POST /v2/users/{openid}/messages 或 /v2/groups/{group_openid}/messages。
//
// 用法：node qq_broadcast.mjs [--debug] [--hello]

import { zstdDecompressSync } from 'node:zlib';
import { readFileSync, statSync, openSync, readSync, closeSync } from 'node:fs';
import https from 'node:https';

// ---- 配置 ----
const SESSION_FILE = process.env.DSH_SESSION_LOG
  ?? 'E:\\Program Files\\DeepSeek_Harness_Desktop\\DeepSeek Harness\\config\\sessions\\--E-trae-program-CC_RC--\\session-f41f2f27-4495-418e-8349-0d60c64ad4c3\\session.jsonl.zstd';

const QQ_APP_ID = '1905586534';
const QQ_SECRET = process.env.DSH_QQBOT_APP_SECRET_9A003FEEE35F3E253B831B9E
  ?? 'T6jN1gM2jQ8qZI2mXI4qdRF4tjZQH91u';

// 播报目标：
//  - QQ 开放平台曾禁止机器人主动向**群聊**推送消息（40034105 无权限）；
//    2026-09-09 用户已在 QQ 开放平台重设机器人权限，群聊主动推送已恢复
//    （实测 HTTP 200 + 消息 ID 返回成功）。
//  - 默认同时播报到 owner 私聊与绑定群聊；可用 BROADCAST_C2C=0 关闭私聊。
const TARGETS = [
  { scope: 'c2c', targetId: 'A7AE7163CFBE07286DD8B886F2D15BC7', label: '私聊(owner)', enabled: process.env.BROADCAST_C2C !== '0' },
  { scope: 'group', targetId: '47BF966A42FE0B3C904771CEB48A60FA', label: '群聊', enabled: true },
];

// 发送节流（毫秒）：最短发送间隔，避免刷屏
const SEND_THROTTLE_MS = Number(process.env.SEND_THROTTLE_MS ?? 2000);
// 定时器驱动摘要推送的间隔（毫秒）：仅推送"助手"文本增量（不播报思考碎片）
const SUMMARY_TICK_MS = Number(process.env.SUMMARY_TICK_MS ?? 3000);
// 触发一次摘要推送的最小新增字符数（过小的增量跳过；文本摘要要求更完整，默认 30）
const SUMMARY_MIN_DELTA = Number(process.env.SUMMARY_MIN_DELTA ?? 30);
// 摘要最大长度（每次发送的增量片段长度）
const SUMMARY_MAX_LEN = Number(process.env.SUMMARY_MAX_LEN ?? 160);
// 轮询间隔
const LOOP_MS = Number(process.env.LOOP_MS ?? 1000);
// 看门狗：等待用户应答（提问/审批）的超时阈值（毫秒，默认 5 分钟）
const WATCHDOG_TIMEOUT_MS = Number(process.env.WATCHDOG_TIMEOUT_MS ?? 5 * 60 * 1000);
// 看门狗检查间隔（毫秒）
const WATCHDOG_TICK_MS = Number(process.env.WATCHDOG_TICK_MS ?? 10_000);

const ZSTD_MAGIC = Buffer.from([0x28, 0xb5, 0x2f, 0xfd]);
const args = process.argv.slice(2);
const DEBUG = args.includes('--debug');
const HELLO = args.includes('--hello');

// ---- 状态 ----
let cursor = 0n;               // 已读字节偏移
let pending = Buffer.alloc(0); // 跨读边界的未完成帧缓冲
const sentTimestamps = new Map();
let missedFrames = 0;

const active = {
  turn: null,
  step: null,
  reasoningBuf: '',  // 推理增量累积（仅记录，不播报）
  textBuf: '',       // 助手（assistant）文本增量累积（播报来源）
  finalText: '',     // 本 turn 最终回答文本（assistant/message 的 text 块）
  sentOffset: 0,     // 已发送到 QQ 的字符偏移（相对 textBuf 拼接流）
  lastSentAt: 0,     // 上次真正发送的时刻
};

// 看门狗状态：等待用户应答的交互记录
// key = approval id 或 ask_user_question 的 callId
const pendingInteractions = new Map(); // key -> { kind, startedAt, notified, askedText }

// ---- QQ REST 客户端（内置 https，绕过 schannel 沙箱限制）----
function reqJson(host, path, { method = 'GET', headers = {}, body, timeoutMs = 15000 } = {}) {
  return new Promise((resolve, reject) => {
    const payload = body ? JSON.stringify(body) : null;
    const req = https.request({
      host, path, method,
      headers: {
        ...(payload ? { 'content-type': 'application/json', 'content-length': Buffer.byteLength(payload) } : {}),
        ...headers,
      },
      timeout: timeoutMs,
    }, (res) => {
      let data = '';
      res.on('data', (c) => { data += c; });
      res.on('end', () => {
        let parsed = null;
        try { parsed = JSON.parse(data); } catch { /* keep raw */ }
        resolve({ status: res.statusCode, body: parsed ?? data.slice(0, 500) });
      });
    });
    req.on('timeout', () => { req.destroy(new Error('timeout')); });
    req.on('error', reject);
    if (payload) req.write(payload);
    req.end();
  });
}

let cachedToken = null;
let cachedTokenExpiry = 0;

async function getAccessToken() {
  const now = Date.now();
  if (cachedToken && now < cachedTokenExpiry) return cachedToken;
  const res = await reqJson('bots.qq.com', '/app/getAppAccessToken', {
    method: 'POST',
    body: { appId: QQ_APP_ID, clientSecret: QQ_SECRET },
  });
  if (!res.body?.access_token) throw new Error(`getAccessToken failed: ${JSON.stringify(res.body)}`);
  cachedToken = res.body.access_token;
  cachedTokenExpiry = now + (Number(res.body.expires_in ?? 3600) - 60) * 1000;
  return cachedToken;
}

async function sendC2CText(token, openid, content) {
  return reqJson('api.sgroup.qq.com', `/v2/users/${openid}/messages`, {
    method: 'POST',
    headers: { 'Authorization': `QQBot ${token}`, 'User-Agent': 'dsh-im-broadcast/1.0' },
    body: { content, msg_type: 0 },
  });
}

async function sendGroupText(token, groupOpenid, content) {
  return reqJson('api.sgroup.qq.com', `/v2/groups/${groupOpenid}/messages`, {
    method: 'POST',
    headers: { 'Authorization': `QQBot ${token}`, 'User-Agent': 'dsh-im-broadcast/1.0' },
    body: { content, msg_type: 0 },
  });
}

// 发送到所有启用的目标（带全局节流；force 绕过节流用于关键通知）
async function broadcast(text, { force = false } = {}) {
  const now = Date.now();
  if (!force) {
    const last = sentTimestamps.get('default') ?? 0;
    if (now - last < SEND_THROTTLE_MS) return { throttled: true };
  }
  sentTimestamps.set('default', now);
  const token = await getAccessToken();
  const results = [];
  for (const t of TARGETS) {
    if (!t.enabled) continue;
    try {
      const res = t.scope === 'group'
        ? await sendGroupText(token, t.targetId, text)
        : await sendC2CText(token, t.targetId, text);
      const ok = res.status === 200 || res.status === 201;
      results.push(ok
        ? { target: t.label, ok: true }
        : { target: t.label, ok: false, status: res.status, body: res.body });
    } catch (e) {
      results.push({ target: t.label, ok: false, error: e.message });
    }
  }
  return { results };
}

function dbg(...parts) {
  if (DEBUG) console.log(new Date().toISOString(), ...parts);
}

// ---- 事件处理 ----
function handleEvent(evt) {
  if (!evt || typeof evt !== 'object') return;
  const type = evt.type;
  const data = evt.data || {};
  const turn = data.turn;
  const step = data.step;

  switch (type) {
    case 'reasoning-chunks':
    case 'text-chunks': {
      // 增量累积：同一 turn 内追加到流，sentOffset 保持（只推进已发送位置），
      // 换 turn/step 时重置流与偏移。
      if (turn !== undefined && step !== undefined) {
        if (active.turn !== turn || active.step !== step) {
          flushStepSummary();
          active.turn = turn;
          active.step = step;
          active.reasoningBuf = '';
          active.textBuf = '';
          active.finalText = '';
          active.sentOffset = 0;
        }
      }
      const texts = Array.isArray(data.texts) ? data.texts.join('') : (data.text || '');
      if (!texts) return;
      if (type === 'reasoning-chunks') active.reasoningBuf += texts;
      else active.textBuf += texts;
      dbg('[chunk]', type, turn, step, texts.slice(0, 40));
      break;
    }

    case 'step/end': {
      // step 边界：立即推送未发送增量（不等下一个 tick）
      flushStepSummary();
      break;
    }

    case 'assistant/message': {
      // 记录最终回答文本（text 块），用于完成标记；工具调用消息忽略。
      const msg = data.message || {};
      const textBlocks = (Array.isArray(msg.content) ? msg.content : [])
        .filter((b) => b && b.type === 'text')
        .map((b) => String(b.text ?? ''));
      if (textBlocks.length > 0) {
        active.finalText = textBlocks.join('\n').trim();
        if (active.turn === null) active.turn = turn ?? null;
      }
      break;
    }

    case 'turn/start': {
      // 新任务开始：发送开始标记（区分于思考过程与完成标记）
      void broadcast(`🚀 开始处理新任务（turn ${turn}）`, { force: true })
        .then((r) => dbg('[sent] turn-start', JSON.stringify(r)));
      break;
    }

    case 'turn/end': {
      flushStepSummary();
      const reason = data.reason;
      const kind = reason && typeof reason === 'object' ? reason.kind : null;
      // 意外打断/错误通知：completed 正常结束走完成标记，其余（error/aborted/stopped/
      // blocked/interrupted/max-tokens 等）推送错误通知；force 确保必达。
      if (kind && kind !== 'completed') {
        const msg = summarizeTurnFailure(reason, turn);
        if (msg) {
          void broadcast(`⚠️ 回合中断/出错（turn ${turn}，${kind}）：${msg}`, { force: true })
            .then((r) => dbg('[sent] turn-failure', JSON.stringify(r)));
        }
        // 回合异常中断：未决的提问/审批无法继续，清空看门狗避免误报
        if (pendingInteractions.size > 0) {
          pendingInteractions.clear();
          dbg('[watchdog] cleared pending interactions after turn failure');
        }
      } else if (kind === 'completed') {
        // 任务完成标记：优先用最终回答文本（assistant/message），其次回退 textBuf，
        // 与思考过程（🧠）明确区分。
        const finalText = active.finalText.trim() || active.textBuf.trim() || '(无文本回复)';
        const oneLine = finalText.replace(/\n+/g, ' ').slice(0, SUMMARY_MAX_LEN);
        void broadcast(`✅ 任务完成（turn ${turn}）：${oneLine}`, { force: true })
          .then((r) => dbg('[sent] turn-complete', JSON.stringify(r)));
      }
      active.turn = null;
      active.step = null;
      active.reasoningBuf = '';
      active.textBuf = '';
      active.finalText = '';
      active.sentOffset = 0;
      break;
    }

    // 工具结果失败：单独通知（补充 turn/end 之外的错误，如单工具失败但回合继续）
    case 'tool/result': {
      const isError = data.isError === true
        || (data.message && typeof data.message === 'object' && data.message.isError === true)
        || (data.message && Array.isArray(data.message.content)
          && data.message.content.some((c) => c && c.type === 'tool-result' && c.isError === true));
      if (isError) {
        const toolName = typeof data.name === 'string'
          ? data.name
          : (data.message?.source?.callId ? '工具' : '工具');
        void broadcast(`⚠️ 工具执行失败（${toolName}）：回合仍在继续，若影响结果请留意最终回复。`, { force: true })
          .then((r) => dbg('[sent] tool-failure', JSON.stringify(r)));
      }
      // 若该 tool/result 对应 ask_user_question 的 callId，则交互已解决，清除看门狗
      const sourceCallId = data.message?.source?.callId;
      if (typeof sourceCallId === 'string' && pendingInteractions.has(sourceCallId)) {
        pendingInteractions.delete(sourceCallId);
        dbg('[watchdog] question resolved by tool/result', sourceCallId);
      }
      break;
    }

    case 'approval/asked': {
      const reason = typeof data.reason === 'string' ? data.reason : '';
      const toolName = typeof data.toolName === 'string' ? data.toolName : '工具';
      const approvalId = typeof data.id === 'string' ? data.id : null;
      if (reason) {
        // 登记看门狗：等待用户批准
        if (approvalId) {
          pendingInteractions.set(approvalId, {
            kind: 'approval',
            startedAt: Date.now(),
            notified: false,
            askedText: reason.slice(0, 160),
          });
        }
        void broadcast(`❓ 需要你的确认（${toolName}）：\n${reason.slice(0, 300)}\n\n> 群聊中请 @机器人 后回复答案。`, { force: true })
          .then((r) => dbg('[sent] approval', JSON.stringify(r)));
      }
      break;
    }

    case 'approval/decided': {
      const approvalId = typeof data.id === 'string' ? data.id : null;
      if (approvalId) {
        pendingInteractions.delete(approvalId);
        dbg('[watchdog] approval resolved:', approvalId, data.outcome ?? '');
      }
      break;
    }

    case 'tool/call': {
      if (data.name !== 'ask_user_question') break;
      const argsText = typeof data.arguments === 'string'
        ? data.arguments
        : (typeof data.arguments === 'object' ? JSON.stringify(data.arguments) : '');
      const questionText = extractQuestions(argsText);
      const callId = typeof data.callId === 'string' ? data.callId : null;
      if (questionText) {
        // 登记看门狗：等待用户回答问题
        if (callId) {
          pendingInteractions.set(callId, {
            kind: 'question',
            startedAt: Date.now(),
            notified: false,
            askedText: questionText.slice(0, 160),
          });
        }
        void broadcast(`❓ 需要你补充信息：\n${questionText}\n\n> 群聊中请 @机器人 后回复答案。`, { force: true })
          .then((r) => dbg('[sent] question', JSON.stringify(r)));
      }
      break;
    }

    default:
      break;
  }
}

function extractQuestions(argsText) {
  try {
    const parsed = JSON.parse(argsText);
    const questions = Array.isArray(parsed.questions) ? parsed.questions : [];
    if (questions.length === 0) return null;
    return questions.map((q) => {
      const lines = [];
      if (q.header) lines.push(`【${String(q.header)}】`);
      lines.push(String(q.question ?? ''));
      if (Array.isArray(q.options) && q.options.length > 0) {
        q.options.forEach((opt, j) => {
          const label = typeof opt?.label === 'string' ? opt.label : '';
          const desc = typeof opt?.description === 'string' ? ` — ${opt.description}` : '';
          lines.push(`  ${j + 1}. ${label}${desc}`);
        });
      }
      if (q.multi_select === true) lines.push('（可多选）');
      return lines.filter(Boolean).join('\n');
    }).join('\n\n');
  } catch {
    return null;
  }
}

function flushStepSummary() {
  // step 边界：把已累积但尚未发送的增量立即推送（不等下一个 tick）
  sendIncrementalSummary({ force: true });
}

// 增量摘要发送：仅推送"助手"文本增量（textBuf，即对话轨迹中 assistant 正式输出的文字），
// 不播报思考碎片（reasoningBuf 仅记录、不推送），降低播报频率。
// 定时器每 SUMMARY_TICK_MS 调用一次（不依赖 step/end）。
function sendIncrementalSummary({ force = false } = {}) {
  const stream = active.textBuf;
  const total = stream.length;
  if (total === 0) return;
  const delta = stream.slice(active.sentOffset);
  const now = Date.now();
  if (!force) {
    // 节流：距上次发送不足 SEND_THROTTLE_MS，或新增内容过少，则等待下一次 tick
    if (now - active.lastSentAt < SEND_THROTTLE_MS || delta.length < SUMMARY_MIN_DELTA) return;
  }
  if (!delta.trim()) return;
  const text = (delta.trim().split('\n')[0] ?? '').slice(0, SUMMARY_MAX_LEN);
  if (!text) return;
  active.sentOffset = total; // 已发送到当前流末尾
  active.lastSentAt = now;
  void broadcast(`📝 助手：${text}`)
    .then((r) => dbg('[sent] summary', JSON.stringify(r)));
}

function summarizeTurnFailure(reason, turn) {
  const kind = reason?.kind;
  const err = reason?.error ?? reason?.failure ?? reason;
  if (kind === 'stopped') return '任务被停止';
  if (kind === 'aborted') {
    const sub = reason?.reason;
    const subKind = sub && typeof sub === 'object' ? sub.kind : sub;
    if (subKind === 'user') return '任务被用户主动停止';
    if (subKind === 'max-tokens') return '超出最大 token 限制';
    return typeof subKind === 'string' ? `任务被中断（${subKind}）` : '任务被中断';
  }
  if (kind === 'interrupted') return '任务被意外打断';
  if (kind === 'blocked') return '任务被阻止';
  if (kind === 'max-tokens') return '超出最大 token 限制';
  if (kind === 'error') {
    const msg = err && typeof err === 'object' ? [err.message ?? err.code, err.code ?? ''].filter(Boolean).join(' ') : String(err ?? '');
    return msg.slice(0, 250) || '回合执行出错';
  }
  if (kind === 'completed') return '';
  if (err && typeof err === 'object') {
    return [err.message ?? err.code ?? kind, err.code ?? ''].filter(Boolean).join(' ').slice(0, 250);
  }
  if (typeof err === 'string') return err.slice(0, 250);
  return String(kind ?? '未知原因').slice(0, 250);
}

// ---- zstd 增量读取 ----
// 帧独立解压：生成器产出完整行；遇到不完整帧（跨读边界）返回 {__rest}
function* decodeFrames(buffer) {
  let start = 0;
  while (start < buffer.length) {
    const idx = buffer.indexOf(ZSTD_MAGIC, start);
    if (idx === -1) break;
    const next = buffer.indexOf(ZSTD_MAGIC, idx + 4);
    const frameEnd = next === -1 ? buffer.length : next;
    try {
      const out = zstdDecompressSync(buffer.subarray(idx, frameEnd));
      // 一帧可能包含多条 JSON（按行分隔）
      const text = out.toString('utf8');
      if (text.trim()) {
        for (const one of text.split('\n')) {
          const line = one.trim();
          if (line) yield line;
        }
      }
      start = frameEnd;
    } catch {
      yield { __rest: buffer.slice(idx) };
      return;
    }
  }
}

// ---- 看门狗：等待用户应答超过阈值时提示并可恢复 ----
// 工作原理：
//  - 提问（ask_user_question）与审批（approval/asked）发生时在 pendingInteractions 登记；
//  - 回答/批准后经 approval/decided 或 ask_user_question 的 tool/result 清除；
//  - 若超过 WATCHDOG_TIMEOUT_MS 仍未被清除，则向 QQ 发送提示并标记 notified
//    （每条只提示一次），提醒"对话可能已卡住，可直接发送新消息继续"。
//  - 若此时回合仍在运行（说明回答了但未生效），强制发送"请重新发送/重开"提示。
function watchdogTick() {
  const now = Date.now();
  for (const [key, entry] of pendingInteractions.entries()) {
    if (entry.notified) continue;
    const elapsed = now - entry.startedAt;
    if (elapsed < WATCHDOG_TIMEOUT_MS) continue;
    entry.notified = true;
    const kindLabel = entry.kind === 'approval' ? '等待权限确认' : '等待问题回答';
    void broadcast(
      `⏰ 看门狗：${kindLabel}已超过 ${Math.round(WATCHDOG_TIMEOUT_MS / 60000)} 分钟未获回应。\n` +
      `若你认为已回复：可能是群聊中未 @ 机器人导致未生效。\n请 @机器人 重新发送你的答案，或发送 /stop 停止当前任务、/new 开启新会话。`,
      { force: true },
    ).then((r) => dbg('[watchdog] timeout notice', key, JSON.stringify(r)));
  }
}

function readIncremental() {
  let size = 0;
  try { size = statSync(SESSION_FILE).size; } catch { return; }
  if (size <= Number(cursor)) return;
  const fd = openSync(SESSION_FILE, 'r');
  const need = size - Number(cursor);
  let data;
  try {
    const buf = Buffer.alloc(need);
    let read = 0;
    while (read < need) {
      const n = readSync(fd, buf, read, need - read, Number(cursor) + read);
      if (n <= 0) break;
      read += n;
    }
    data = buf.subarray(0, read);
    cursor += BigInt(read);
  } finally {
    closeSync(fd);
  }
  if (!data.length) return;
  const combined = pending.length > 0 ? Buffer.concat([pending, data]) : data;
  const rest = [];
  for (const item of decodeFrames(combined)) {
    if (item && typeof item === 'object' && '__rest' in item) {
      rest.push(item.__rest);
    } else {
      try {
        handleEvent(JSON.parse(item));
      } catch (e) {
        missedFrames += 1;
        dbg('[parse-error]', e.message, item.slice(0, 80));
      }
    }
  }
  pending = rest.length > 0 ? Buffer.concat(rest) : Buffer.alloc(0);
}

// ---- 启动 ----
async function main() {
  console.log('[qq_broadcast] 启动，监控:', SESSION_FILE);
  console.log('[qq_broadcast] 目标:', TARGETS.map((t) => `${t.label}(${t.scope}:${t.targetId})`).join(', '));
  console.log('[qq_broadcast] 参数: summaryTick=%dms throttle=%dms summaryMax=%d loop=%dms watchdog=%dms targets=%s',
    SUMMARY_TICK_MS, SEND_THROTTLE_MS, SUMMARY_MAX_LEN, LOOP_MS, WATCHDOG_TIMEOUT_MS,
    TARGETS.map((t) => `${t.label}:${t.enabled ? 'on' : 'off'}`).join(' '));

  // 偏移初始化为当前文件大小（只播报新事件，不回放历史）
  try { cursor = BigInt(statSync(SESSION_FILE).size); } catch { cursor = 0n; }
  dbg('[init] cursor =', cursor.toString());

  if (HELLO) {
    try {
      const r = await broadcast('🤖 QQ 播报守护已上线（思考摘要 / 发问 / 错误通知将自动推送）', { force: true });
      console.log('[hello] 发送结果:', JSON.stringify(r));
    } catch (e) {
      console.error('[hello]', e.message);
    }
  }

  // 增量读取日志
  setInterval(() => {
    try {
      readIncremental();
    } catch (e) {
      console.error('[loop-error]', e.message);
    }
  }, LOOP_MS);

  // 阶段思考摘要定时推送：每 SUMMARY_TICK_MS 把累积的新增量发到 QQ，
  // 不等待 step 完成——实现"每一阶段的思考摘要实时发送"。
  setInterval(() => {
    try {
      sendIncrementalSummary();
    } catch (e) {
      console.error('[summary-error]', e.message);
    }
  }, SUMMARY_TICK_MS);

  // 看门狗：周期检查未应答的提问/审批，超时则提示
  setInterval(() => {
    try {
      watchdogTick();
    } catch (e) {
      console.error('[watchdog-error]', e.message);
    }
  }, WATCHDOG_TICK_MS);

  process.on('SIGINT', () => { console.log('[qq_broadcast] 退出'); process.exit(0); });
}

main().catch((e) => {
  console.error('[fatal]', e);
  process.exit(1);
});