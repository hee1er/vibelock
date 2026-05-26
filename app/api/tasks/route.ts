import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import { db } from '@/lib/db'

export async function GET(req: NextRequest) {
  const session = await getServerSession(authOptions)
  if (!session?.user?.id) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  const { searchParams } = new URL(req.url)
  const sessionId = searchParams.get('sessionId')
  if (!sessionId) return NextResponse.json({ error: 'sessionId required' }, { status: 400 })

  const vibeSession = await db.vibeSession.findFirst({
    where: { id: sessionId, userId: session.user.id },
  })
  if (!vibeSession) return NextResponse.json({ error: 'Not found' }, { status: 404 })

  const tasks = await db.task.findMany({
    where: { vibeSessionId: sessionId },
    orderBy: { order: 'asc' },
  })
  return NextResponse.json(tasks)
}

export async function POST(req: NextRequest) {
  const session = await getServerSession(authOptions)
  if (!session?.user?.id) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  const { vibeSessionId, text } = await req.json()
  if (!vibeSessionId || !text?.trim()) {
    return NextResponse.json({ error: 'vibeSessionId and text required' }, { status: 400 })
  }

  const vibeSession = await db.vibeSession.findFirst({
    where: { id: vibeSessionId, userId: session.user.id },
  })
  if (!vibeSession) return NextResponse.json({ error: 'Not found' }, { status: 404 })

  const count = await db.task.count({ where: { vibeSessionId } })
  const task = await db.task.create({
    data: { vibeSessionId, text: text.trim(), order: count },
  })
  return NextResponse.json(task, { status: 201 })
}
