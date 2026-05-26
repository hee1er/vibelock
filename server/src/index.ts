import express from 'express'
import { WebSocketServer, WebSocket } from 'ws'
import { createServer } from 'http'
import { v4 as uuid } from 'uuid'
import { getRandomWord } from './words'

const PORT = parseInt(process.env.PORT ?? '3001')

const app = express()
app.get('/health', (_, res) => res.json({ ok: true, connections: clients.size }))

const server = createServer(app)
const wss = new WebSocketServer({ server, path: '/ws' })

// ── State ─────────────────────────────────────────────────────────────────────

interface Client {
  ws: WebSocket
  userId: string
  displayName: string
  roomId: string | null
}

const clients = new Map<string, Client>()   // socketId → Client
const queue: string[] = []                   // socketIds waiting for match

interface Room {
  id: string
  player1: string  // socketId
  player2: string  // socketId
  word: string
  drawerSocketId: string
}

const rooms = new Map<string, Room>()

// ── Helpers ───────────────────────────────────────────────────────────────────

function send(ws: WebSocket, payload: object) {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(payload))
  }
}

function getPartner(socketId: string): Client | null {
  const client = clients.get(socketId)
  if (!client?.roomId) return null
  const room = rooms.get(client.roomId)
  if (!room) return null
  const partnerId = room.player1 === socketId ? room.player2 : room.player1
  return clients.get(partnerId) ?? null
}

function removeFromQueue(socketId: string) {
  const idx = queue.indexOf(socketId)
  if (idx !== -1) queue.splice(idx, 1)
}

function tryMatch() {
  while (queue.length >= 2) {
    const id1 = queue.shift()!
    const id2 = queue.shift()!
    const c1 = clients.get(id1)
    const c2 = clients.get(id2)
    if (!c1 || !c2) continue  // stale — try next pair

    const roomId = uuid()
    const word = getRandomWord()
    const room: Room = { id: roomId, player1: id1, player2: id2, word, drawerSocketId: id1 }
    rooms.set(roomId, room)
    c1.roomId = roomId
    c2.roomId = roomId

    send(c1.ws, {
      type: 'matched',
      roomId,
      role: 'drawer',
      word,
      partnerName: c2.displayName,
    })
    send(c2.ws, {
      type: 'matched',
      roomId,
      role: 'guesser',
      partnerName: c1.displayName,
    })

    console.log(`[room] ${roomId}: "${c1.displayName}" (drawer) + "${c2.displayName}" (guesser) — word: "${word}"`)
  }

  // Notify remaining waiter of queue size
  if (queue.length > 0) {
    const waiter = clients.get(queue[0])
    if (waiter) send(waiter.ws, { type: 'queue_update', queueSize: queue.length })
  }
}

function leaveRoom(socketId: string) {
  const client = clients.get(socketId)
  if (!client?.roomId) return
  const partner = getPartner(socketId)
  if (partner) {
    send(partner.ws, { type: 'partner_disconnected' })
    partner.roomId = null
    // Re-queue partner
    queue.push(partner.userId ? socketId : '')
  }
  rooms.delete(client.roomId)
  client.roomId = null
}

// ── WebSocket handler ─────────────────────────────────────────────────────────

wss.on('connection', (ws) => {
  const socketId = uuid()
  const client: Client = { ws, userId: '', displayName: 'Anonymous', roomId: null }
  clients.set(socketId, client)
  console.log(`[connect] ${socketId} (total: ${clients.size})`)

  ws.on('message', (raw) => {
    let msg: any
    try { msg = JSON.parse(raw.toString()) } catch { return }

    switch (msg.type) {
      case 'join_queue': {
        client.userId = msg.userId ?? uuid()
        client.displayName = (msg.displayName ?? 'Anonymous').slice(0, 24)
        removeFromQueue(socketId)
        if (!client.roomId) {
          queue.push(socketId)
          tryMatch()
        }
        break
      }

      case 'draw_start':
      case 'draw_move':
      case 'draw_end': {
        const partner = getPartner(socketId)
        if (!partner) break
        const partnerType = msg.type.replace('draw_', 'partner_draw_')
        send(partner.ws, { type: partnerType, x: msg.x, y: msg.y, color: msg.color, strokeWidth: msg.strokeWidth })
        break
      }

      case 'clear': {
        const partner = getPartner(socketId)
        if (partner) send(partner.ws, { type: 'partner_clear' })
        break
      }

      case 'guess': {
        const room = rooms.get(client.roomId ?? '')
        if (!room) break
        const answer = (msg.word ?? '').trim().toLowerCase()
        const correct = answer === room.word.toLowerCase()
        send(ws, { type: correct ? 'correct_guess' : 'wrong_guess', word: msg.word, correct })
        if (correct) {
          const partner = getPartner(socketId)
          if (partner) send(partner.ws, { type: 'correct_guess', word: msg.word, correct: true })
          // Start new round
          const newWord = getRandomWord()
          room.word = newWord
          // Swap roles
          const newDrawer = room.drawerSocketId === room.player1 ? room.player2 : room.player1
          room.drawerSocketId = newDrawer
          const c1 = clients.get(room.player1)
          const c2 = clients.get(room.player2)
          if (c1 && c2) {
            setTimeout(() => {
              send(c1.ws, {
                type: 'new_round',
                role: room.player1 === newDrawer ? 'drawer' : 'guesser',
                word: room.player1 === newDrawer ? newWord : undefined,
              })
              send(c2.ws, {
                type: 'new_round',
                role: room.player2 === newDrawer ? 'drawer' : 'guesser',
                word: room.player2 === newDrawer ? newWord : undefined,
              })
            }, 2000)
          }
        }
        break
      }

      case 'skip': {
        const partner = getPartner(socketId)
        if (partner) {
          send(partner.ws, { type: 'partner_skipped' })
          partner.roomId = null
          // Re-queue partner
          queue.push([...clients.entries()].find(([, c]) => c === partner)?.[0] ?? '')
        }
        leaveRoom(socketId)
        queue.push(socketId)
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
    removeFromQueue(socketId)
    clients.delete(socketId)
    console.log(`[disconnect] ${socketId} (total: ${clients.size})`)
  })

  ws.on('error', (err) => console.error(`[error] ${socketId}:`, err.message))

  send(ws, { type: 'connected', socketId })
})

server.listen(PORT, () => {
  console.log(`VibeLock server running on ws://localhost:${PORT}/ws`)
})
