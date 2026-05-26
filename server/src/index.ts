import express from 'express'
import { WebSocketServer, WebSocket } from 'ws'
import { createServer } from 'http'
import { v4 as uuid } from 'uuid'
import { getRandomWord } from './words'

const PORT = parseInt(process.env.PORT ?? '3001')

const app = express()
app.use(express.json())
app.get('/health', (_, res) => res.json({
  ok: true,
  connections: clients.size,
  queue: queue.length,
  premiumQueue: premiumQueue.length,
  rooms: rooms.size,
}))

const server = createServer(app)
const wss = new WebSocketServer({ server, path: '/ws' })

// ── 상태 ────────────────────────────────────────────────────────────────────

interface Client {
  ws: WebSocket
  socketId: string
  userId: string
  displayName: string
  isPremium: boolean
  roomId: string | null
  connectedAt: Date
}

const clients = new Map<string, Client>()
const premiumQueue: string[] = []   // 프리미엄 우선 대기열
const queue: string[] = []          // 일반 대기열

interface Room {
  id: string
  player1: string
  player2: string
  word: string
  drawerSocketId: string
  round: number
  createdAt: Date
}

const rooms = new Map<string, Room>()

// ── 헬퍼 ─────────────────────────────────────────────────────────────────────

function send(ws: WebSocket, payload: object) {
  if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(payload))
}

function getPartner(socketId: string): Client | null {
  const client = clients.get(socketId)
  if (!client?.roomId) return null
  const room = rooms.get(client.roomId)
  if (!room) return null
  const partnerId = room.player1 === socketId ? room.player2 : room.player1
  return clients.get(partnerId) ?? null
}

function removeFromQueues(socketId: string) {
  const pi = premiumQueue.indexOf(socketId)
  if (pi !== -1) premiumQueue.splice(pi, 1)
  const qi = queue.indexOf(socketId)
  if (qi !== -1) queue.splice(qi, 1)
}

function broadcastQueueUpdate() {
  const total = premiumQueue.length + queue.length
  ;[...premiumQueue, ...queue].forEach((sid) => {
    const c = clients.get(sid)
    if (c) send(c.ws, { type: 'queue_update', queueSize: total })
  })
}

/**
 * 매칭 시도:
 * 1) 프리미엄 ↔ 프리미엄
 * 2) 프리미엄 ↔ 일반
 * 3) 일반 ↔ 일반
 */
function tryMatch() {
  // 두 큐를 합쳐서 처리 (프리미엄 우선)
  const combined = [...premiumQueue, ...queue]

  while (combined.length >= 2) {
    const id1 = combined.shift()!
    const id2 = combined.shift()!
    const c1 = clients.get(id1)
    const c2 = clients.get(id2)

    if (!c1 || !c2) continue

    // 실제 큐에서 제거
    removeFromQueues(id1)
    removeFromQueues(id2)

    const roomId = uuid()
    const word = getRandomWord()
    const room: Room = {
      id: roomId, player1: id1, player2: id2,
      word, drawerSocketId: id1, round: 1,
      createdAt: new Date(),
    }
    rooms.set(roomId, room)
    c1.roomId = roomId
    c2.roomId = roomId

    send(c1.ws, { type: 'matched', roomId, role: 'drawer', word, partnerName: c2.displayName, isPremiumMatch: c1.isPremium && c2.isPremium })
    send(c2.ws, { type: 'matched', roomId, role: 'guesser', partnerName: c1.displayName, isPremiumMatch: c1.isPremium && c2.isPremium })

    const tag = `[${c1.isPremium ? '⭐' : ''}${c1.displayName}] ↔ [${c2.isPremium ? '⭐' : ''}${c2.displayName}]`
    console.log(`[match] Room ${roomId.slice(0, 8)} — ${tag} — 단어: "${word}"`)
  }

  broadcastQueueUpdate()
}

function leaveRoom(socketId: string) {
  const client = clients.get(socketId)
  if (!client?.roomId) return
  const room = rooms.get(client.roomId)
  const partner = getPartner(socketId)

  if (partner) {
    send(partner.ws, { type: 'partner_disconnected' })
    partner.roomId = null
    // 파트너를 적절한 대기열로 재진입
    if (partner.isPremium) premiumQueue.push(partner.socketId)
    else queue.push(partner.socketId)
  }

  if (room) rooms.delete(room.id)
  client.roomId = null
}

// ── WebSocket 핸들러 ──────────────────────────────────────────────────────────

wss.on('connection', (ws) => {
  const socketId = uuid()
  const client: Client = {
    ws, socketId, userId: '', displayName: 'Anonymous',
    isPremium: false, roomId: null, connectedAt: new Date(),
  }
  clients.set(socketId, client)
  send(ws, { type: 'connected', socketId })
  console.log(`[connect] ${socketId.slice(0, 8)} (총: ${clients.size}명)`)

  ws.on('message', (raw) => {
    let msg: any
    try { msg = JSON.parse(raw.toString()) } catch { return }

    switch (msg.type) {

      // ── 매칭 대기열 진입 ────────────────────────────────────────
      case 'join_queue': {
        client.userId = msg.userId ?? uuid()
        client.displayName = (msg.displayName ?? 'Anonymous').slice(0, 24)
        client.isPremium = msg.isPremium === true

        // 이미 방에 있으면 먼저 나가기
        if (client.roomId) leaveRoom(socketId)
        removeFromQueues(socketId)

        // 프리미엄은 앞쪽 대기열
        if (client.isPremium) premiumQueue.unshift(socketId)
        else queue.push(socketId)

        console.log(`[queue] ${client.displayName}${client.isPremium ? ' ⭐' : ''} 대기 (P:${premiumQueue.length}/N:${queue.length})`)
        tryMatch()
        break
      }

      // ── 그림 이벤트 릴레이 ──────────────────────────────────────
      case 'draw_start':
      case 'draw_move':
      case 'draw_end': {
        const partner = getPartner(socketId)
        if (!partner) break
        send(partner.ws, {
          type: `partner_${msg.type}`,
          x: msg.x, y: msg.y, color: msg.color, strokeWidth: msg.strokeWidth,
        })
        break
      }

      case 'clear': {
        getPartner(socketId)?.let(p => send(p.ws, { type: 'partner_clear' }))
        break
      }

      // ── 정답 체크 ─────────────────────────────────────────────
      case 'guess': {
        const room = rooms.get(client.roomId ?? '')
        if (!room) break
        const answer = (msg.word ?? '').trim()
        const correct = answer.toLowerCase() === room.word.toLowerCase()

        send(ws, { type: correct ? 'correct_guess' : 'wrong_guess', word: msg.word, correct })

        if (correct) {
          const partner = getPartner(socketId)
          if (partner) send(partner.ws, { type: 'correct_guess', word: msg.word, correct: true })

          // 2초 후 새 라운드: 역할 교체 + 새 단어
          const newWord = getRandomWord()
          room.word = newWord
          room.round++
          room.drawerSocketId = room.drawerSocketId === room.player1 ? room.player2 : room.player1

          setTimeout(() => {
            const c1 = clients.get(room.player1)
            const c2 = clients.get(room.player2)
            if (c1 && c2) {
              const drawer = room.drawerSocketId
              send(c1.ws, { type: 'new_round', round: room.round, role: room.player1 === drawer ? 'drawer' : 'guesser', word: room.player1 === drawer ? newWord : undefined })
              send(c2.ws, { type: 'new_round', round: room.round, role: room.player2 === drawer ? 'drawer' : 'guesser', word: room.player2 === drawer ? newWord : undefined })
            }
          }, 2000)
        }
        break
      }

      // ── 스킵 ──────────────────────────────────────────────────
      case 'skip': {
        const partner = getPartner(socketId)
        if (partner) {
          send(partner.ws, { type: 'partner_skipped' })
          partner.roomId = null
          if (partner.isPremium) premiumQueue.unshift(partner.socketId)
          else queue.push(partner.socketId)
        }
        leaveRoom(socketId)
        if (client.isPremium) premiumQueue.unshift(socketId)
        else queue.push(socketId)
        tryMatch()
        break
      }

      case 'heartbeat':
        send(ws, { type: 'heartbeat_ack' })
        break
    }
  })

  ws.on('close', () => {
    leaveRoom(socketId)
    removeFromQueues(socketId)
    clients.delete(socketId)
    console.log(`[disconnect] ${socketId.slice(0, 8)} (총: ${clients.size}명)`)
    broadcastQueueUpdate()
  })

  ws.on('error', (err) => console.error(`[error] ${socketId.slice(0, 8)}:`, err.message))
})

// TypeScript 헬퍼
declare global {
  interface Object { let<T, R>(this: T, fn: (v: T) => R): R }
}
Object.prototype.let = function<T, R>(this: T, fn: (v: T) => R) { return fn(this) }

server.listen(PORT, () => {
  console.log(`
  ╔══════════════════════════════════════╗
  ║   VibeLock Server v1.0               ║
  ║   ws://localhost:${PORT}/ws           ║
  ║   http://localhost:${PORT}/health    ║
  ╚══════════════════════════════════════╝
  `)
})
