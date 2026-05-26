import { Metadata } from 'next'
import { getServerSession } from 'next-auth'
import { redirect } from 'next/navigation'
import { authOptions } from '@/lib/auth'
import { db } from '@/lib/db'
import { VibePicker } from '@/components/vibe/VibePicker'
import { VIBE_CONFIGS, VibeType } from '@/types'
import { formatMinutes } from '@/lib/utils'
import Link from 'next/link'

export const metadata: Metadata = { title: 'Dashboard' }

export default async function DashboardPage() {
  const session = await getServerSession(authOptions)
  if (!session?.user?.id) redirect('/login')

  // Check for active session
  const active = await db.vibeSession.findFirst({
    where: { userId: session.user.id, status: 'ACTIVE' },
    orderBy: { startedAt: 'desc' },
  })

  if (active) redirect(`/session/${active.id}`)

  // Fetch recent + stats
  const [recent, stats] = await Promise.all([
    db.vibeSession.findMany({
      where: { userId: session.user.id, status: 'COMPLETED' },
      orderBy: { startedAt: 'desc' },
      take: 3,
    }),
    db.vibeSession.groupBy({
      by: ['vibeType'],
      where: { userId: session.user.id, status: 'COMPLETED' },
      _count: true,
      _sum: { elapsed: true },
    }),
  ])

  const totalMinutes = Math.floor(
    stats.reduce((sum, s) => sum + (s._sum.elapsed ?? 0), 0) / 60
  )

  return (
    <div className="space-y-10">
      {/* Greeting */}
      <div>
        <h1 className="text-3xl font-bold">
          Hey{session.user.name ? `, ${session.user.name.split(' ')[0]}` : ''} 👋
        </h1>
        {totalMinutes > 0 && (
          <p className="mt-1 text-white/40">
            {formatMinutes(totalMinutes)} focused so far. Keep it up.
          </p>
        )}
      </div>

      {/* Stats */}
      {stats.length > 0 && (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <div className="stat-card">
            <div className="text-2xl font-bold">{stats.reduce((s, v) => s + v._count, 0)}</div>
            <div className="mt-1 text-xs text-white/40">Sessions</div>
          </div>
          <div className="stat-card">
            <div className="text-2xl font-bold">{formatMinutes(totalMinutes)}</div>
            <div className="mt-1 text-xs text-white/40">Total time</div>
          </div>
          {stats.length > 0 && (() => {
            const top = stats.sort((a, b) => b._count - a._count)[0]
            const cfg = VIBE_CONFIGS[top.vibeType as VibeType]
            return (
              <div className="stat-card">
                <div className="text-2xl">{cfg.emoji}</div>
                <div className="mt-1 text-xs text-white/40">Favorite vibe</div>
              </div>
            )
          })()}
          <div className="stat-card">
            <Link href="/history" className="block h-full">
              <div className="text-2xl font-bold text-white/30">→</div>
              <div className="mt-1 text-xs text-white/40">View history</div>
            </Link>
          </div>
        </div>
      )}

      {/* Vibe Picker */}
      <VibePicker />

      {/* Recent sessions */}
      {recent.length > 0 && (
        <div>
          <h2 className="mb-4 text-lg font-semibold">Recent sessions</h2>
          <div className="space-y-2">
            {recent.map((s) => {
              const cfg = VIBE_CONFIGS[s.vibeType as VibeType]
              const mins = Math.floor(s.elapsed / 60)
              return (
                <div
                  key={s.id}
                  className="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3"
                >
                  <div className="flex items-center gap-3">
                    <span>{cfg.emoji}</span>
                    <div>
                      <div className="text-sm font-medium">{cfg.label}</div>
                      <div className="text-xs text-white/30">
                        {new Date(s.startedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                      </div>
                    </div>
                  </div>
                  <div className="text-sm" style={{ color: cfg.color }}>
                    {formatMinutes(mins)}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}
