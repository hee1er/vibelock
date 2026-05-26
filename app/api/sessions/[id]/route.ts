import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import { db } from '@/lib/db'
import { SessionStatus } from '@/types'

const VALID_STATUSES: SessionStatus[] = ['ACTIVE', 'COMPLETED', 'ABANDONED']

async function getSession(id: string, userId: string) {
  return db.vibeSession.findFirst({ where: { id, userId } })
}

export async function GET(_req: NextRequest, { params }: { params: { id: string } }) {
  const session = await getServerSession(authOptions)
  if (!session?.user?.id) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  const vibeSession = await db.vibeSession.findFirst({
    where: { id: params.id, userId: session.user.id },
    include: { tasks: { orderBy: { order: 'asc' } } },
  })
  if (!vibeSession) return NextResponse.json({ error: 'Not found' }, { status: 404 })

  return NextResponse.json(vibeSession)
}

export async function PATCH(req: NextRequest, { params }: { params: { id: string } }) {
  const session = await getServerSession(authOptions)
  if (!session?.user?.id) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  const existing = await getSession(params.id, session.user.id)
  if (!existing) return NextResponse.json({ error: 'Not found' }, { status: 404 })

  const body = await req.json()
  const { status, elapsed, notes } = body

  if (status && !VALID_STATUSES.includes(status)) {
    return NextResponse.json({ error: 'Invalid status' }, { status: 400 })
  }

  const updated = await db.vibeSession.update({
    where: { id: params.id },
    data: {
      ...(status !== undefined ? { status } : {}),
      ...(elapsed !== undefined ? { elapsed } : {}),
      ...(notes !== undefined ? { notes } : {}),
      ...(status === 'COMPLETED' || status === 'ABANDONED' ? { endedAt: new Date() } : {}),
    },
    include: { tasks: { orderBy: { order: 'asc' } } },
  })

  return NextResponse.json(updated)
}

export async function DELETE(_req: NextRequest, { params }: { params: { id: string } }) {
  const session = await getServerSession(authOptions)
  if (!session?.user?.id) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  const existing = await getSession(params.id, session.user.id)
  if (!existing) return NextResponse.json({ error: 'Not found' }, { status: 404 })

  await db.vibeSession.delete({ where: { id: params.id } })
  return new NextResponse(null, { status: 204 })
}
