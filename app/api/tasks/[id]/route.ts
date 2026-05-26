import { NextRequest, NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import { db } from '@/lib/db'

async function getOwnedTask(taskId: string, userId: string) {
  return db.task.findFirst({
    where: { id: taskId, vibeSession: { userId } },
  })
}

export async function PATCH(req: NextRequest, { params }: { params: { id: string } }) {
  const session = await getServerSession(authOptions)
  if (!session?.user?.id) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  const task = await getOwnedTask(params.id, session.user.id)
  if (!task) return NextResponse.json({ error: 'Not found' }, { status: 404 })

  const { completed, text } = await req.json()
  const updated = await db.task.update({
    where: { id: params.id },
    data: {
      ...(completed !== undefined ? { completed } : {}),
      ...(text !== undefined ? { text: text.trim() } : {}),
    },
  })
  return NextResponse.json(updated)
}

export async function DELETE(_req: NextRequest, { params }: { params: { id: string } }) {
  const session = await getServerSession(authOptions)
  if (!session?.user?.id) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  const task = await getOwnedTask(params.id, session.user.id)
  if (!task) return NextResponse.json({ error: 'Not found' }, { status: 404 })

  await db.task.delete({ where: { id: params.id } })
  return new NextResponse(null, { status: 204 })
}
