import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import { db } from '@/lib/db'
import { VibeType } from '@/types'

const VALID_VIBES: VibeType[] = ['DEEP_WORK', 'CREATIVE', 'CHILL', 'SOCIAL', 'WORKOUT']

export async function GET(req: NextRequest) {
  const session = await getServerSession(authOptions)
  if (!session?.user?.id) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  const { searchParams } = new URL(req.url)
  const limit = Math.min(parseInt(searchParams.get('limit') ?? '20'), 100)
  const offset = parseInt(searchParams.get('offset') ?? '0')
  const status = searchParams.get('status')

  const where = {
    userId: session.user.id,
    ...(status ? { status } : {}),
  }

  const [sessions, total] = await Promise.all([
    db.vibeSession.findMany({
      where,
      include: { tasks: { orderBy: { order: 'asc' } } },
      orderBy: { startedAt: 'desc' },
      take: limit,
      skip: offset,
    }),
    db.vibeSession.count({ where }),
  ])

  return NextResponse.json({ sessions, total, limit, offset })
}

export async function POST(req: NextRequest) {
  const session = await getServerSession(authOptions)
  if (!session?.user?.id) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  const body = await req.json()
  const { vibeType, duration, isPublic } = body

  if (!VALID_VIBES.includes(vibeType)) {
    return NextResponse.json({ error: 'Invalid vibe type' }, { status: 400 })
  }

  // Abandon any currently active sessions
  await db.vibeSession.updateMany({
    where: { userId: session.user.id, status: 'ACTIVE' },
    data: { status: 'ABANDONED', endedAt: new Date() },
  })

  const vibeSession = await db.vibeSession.create({
    data: {
      userId: session.user.id,
      vibeType,
      duration: duration ?? 25,
      isPublic: isPublic ?? false,
    },
    include: { tasks: true },
  })

  return NextResponse.json(vibeSession, { status: 201 })
}
