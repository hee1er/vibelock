import { Metadata } from 'next'
import { getServerSession } from 'next-auth'
import { redirect } from 'next/navigation'
import { authOptions } from '@/lib/auth'
import { db } from '@/lib/db'
import { VIBE_CONFIGS, VibeType } from '@/types'
import { formatMinutes } from '@/lib/utils'
import { format } from 'date-fns'

export const metadata: Metadata = { title: 'History' }

export default async function HistoryPage() {
  const session = await getServerSession(authOptions)
  if (!session?.user?.id) redirect('/login')

  const [sessions, vibeCounts] = await Promise.all([
    db.vibeSession.findMany({
      where: { userId: session.user.id },
      include: { tasks: { select: { completed: true } } },
      orderBy: { startedAt: 'desc' },
      take: 50,
    }),
    db.vibeSession.groupBy({
      by: ['vibeType'],
      where: { userId: session.user.id, status: 'COMPLETED' },
      _count: true,
      _sum: { elapsed: true },
      orderBy: { _count: { vibeType: 'desc' } },
    }),
  ])

  const totalCompleted = sessions.filter((s) => s.status === 'COMPLETED').length
  const totalMinutes = Math.floor(
    sessions
      .filter((s) => s.status === 'COMPLETED')
      .reduce((sum, s) => sum + s.elapsed, 0) / 60
  )

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold">History</h1>
        <p className="mt-1 text-white/40">Your vibe journey</p>
      </div>

      {/* Summary stats */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        <div className="stat-card">
          <div className="text-3xl font-bold">{totalCompleted}</div>
          <div className="mt-1 text-xs text-white/40">Completed sessions</div>
        </div>
        <div className="stat-card">
          <div className="text-3xl font-bold">{formatMinutes(totalMinutes)}</div>
          <div className="mt-1 text-xs text-white/40">Total focus time</div>
        </div>
        <div className="stat-card col-span-2 sm:col-span-1">
          <div className="text-3xl font-bold">{sessions.length}</div>
          <div className="mt-1 text-xs text-white/40">All sessions</div>
        </div>
      </div>

      {/* Vibe breakdown */}
      {vibeCounts.length > 0 && (
        <div>
          <h2 className="mb-4 text-lg font-semibold">Vibe breakdown</h2>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {vibeCounts.map((v) => {
              const cfg = VIBE_CONFIGS[v.vibeType as VibeType]
              const mins = Math.floor((v._sum.elapsed ?? 0) / 60)
              return (
                <div
                  key={v.vibeType}
                  className="flex items-center gap-4 rounded-xl border bg-card px-4 py-3"
                  style={{ borderColor: `${cfg.color}30` }}
                >
                  <span className="text-2xl">{cfg.emoji}</span>
                  <div className="flex-1">
                    <div className="font-medium text-sm">{cfg.label}</div>
                    <div className="text-xs text-white/30">{v._count} sessions · {formatMinutes(mins)}</div>
                    {/* Mini bar */}
                    <div className="mt-2 h-1 rounded-full bg-white/10">
                      <div
                        className="h-1 rounded-full transition-all"
                        style={{
                          width: `${Math.min((v._count / Math.max(...vibeCounts.map((x) => x._count))) * 100, 100)}%`,
                          background: cfg.color,
                        }}
                      />
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* Session list */}
      <div>
        <h2 className="mb-4 text-lg font-semibold">All sessions</h2>
        {sessions.length === 0 ? (
          <div className="rounded-2xl border border-border bg-card py-16 text-center text-white/30">
            No sessions yet. Pick a vibe and get started!
          </div>
        ) : (
          <div className="space-y-2">
            {sessions.map((s) => {
              const cfg = VIBE_CONFIGS[s.vibeType as VibeType]
              const mins = Math.floor(s.elapsed / 60)
              const completedTasks = s.tasks.filter((t) => t.completed).length
              const statusColors = {
                COMPLETED: 'text-emerald-400',
                ACTIVE: 'text-blue-400',
                ABANDONED: 'text-white/30',
              }
              return (
                <div
                  key={s.id}
                  className="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3 gap-4"
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <span className="text-xl flex-shrink-0">{cfg.emoji}</span>
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-medium">{cfg.label}</span>
                        <span className={`text-xs ${statusColors[s.status as keyof typeof statusColors]}`}>
                          {s.status.toLowerCase()}
                        </span>
                      </div>
                      <div className="text-xs text-white/30">
                        {format(new Date(s.startedAt), 'MMM d, yyyy · h:mm a')}
                        {s.tasks.length > 0 && ` · ${completedTasks}/${s.tasks.length} tasks`}
                      </div>
                    </div>
                  </div>
                  <div className="flex-shrink-0 text-right">
                    <div className="text-sm font-medium" style={{ color: cfg.color }}>
                      {formatMinutes(mins)}
                    </div>
                    <div className="text-xs text-white/30">of {s.duration}m</div>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
