import { NextResponse } from 'next/server'
import { getServerSession } from 'next-auth'
import { authOptions } from '@/lib/auth'
import { db } from '@/lib/db'
import { VibeType } from '@/types'

export async function GET() {
  const session = await getServerSession(authOptions)
  if (!session?.user?.id) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  const sessions = await db.vibeSession.findMany({
    where: { userId: session.user.id },
    orderBy: { startedAt: 'asc' },
    select: { status: true, elapsed: true, vibeType: true, startedAt: true },
  })

  const completed = sessions.filter((s) => s.status === 'COMPLETED')
  const totalMinutes = Math.floor(completed.reduce((sum, s) => sum + s.elapsed, 0) / 60)

  const vibeCounts = completed.reduce<Record<string, number>>((acc, s) => {
    acc[s.vibeType] = (acc[s.vibeType] ?? 0) + 1
    return acc
  }, {})
  const favoriteVibe = Object.entries(vibeCounts).sort((a, b) => b[1] - a[1])[0]?.[0] as VibeType | undefined

  // Calculate streaks based on unique active days
  const activeDays = new Set(
    completed.map((s) => new Date(s.startedAt).toISOString().slice(0, 10))
  )
  const sortedDays = Array.from(activeDays).sort()

  let currentStreak = 0
  let longestStreak = 0
  let tempStreak = 0

  const today = new Date().toISOString().slice(0, 10)
  const yesterday = new Date(Date.now() - 86400000).toISOString().slice(0, 10)

  for (let i = 0; i < sortedDays.length; i++) {
    if (i === 0) {
      tempStreak = 1
    } else {
      const prev = new Date(sortedDays[i - 1])
      const curr = new Date(sortedDays[i])
      const diff = (curr.getTime() - prev.getTime()) / 86400000
      tempStreak = diff === 1 ? tempStreak + 1 : 1
    }
    if (tempStreak > longestStreak) longestStreak = tempStreak
    if (sortedDays[i] === today || sortedDays[i] === yesterday) {
      currentStreak = tempStreak
    }
  }
  if (!activeDays.has(today) && !activeDays.has(yesterday)) currentStreak = 0

  return NextResponse.json({
    totalSessions: sessions.length,
    completedSessions: completed.length,
    totalMinutes,
    currentStreak,
    longestStreak,
    favoriteVibe: favoriteVibe ?? null,
  })
}
