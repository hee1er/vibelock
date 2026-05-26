'use client'

import { useState, useEffect, useCallback } from 'react'
import { useRouter, useParams } from 'next/navigation'
import { VibeTimer } from '@/components/vibe/VibeTimer'
import { VibeTaskList } from '@/components/vibe/VibeTaskList'
import { VibeAmbience } from '@/components/vibe/VibeAmbience'
import { VIBE_CONFIGS } from '@/types'
import { useVibeStore } from '@/store/vibeStore'
import type { VibeSessionWithTasks } from '@/types'

export default function SessionPage() {
  const params = useParams<{ id: string }>()
  const router = useRouter()
  const endSession = useVibeStore((s) => s.endSession)
  const elapsed = useVibeStore((s) => s.elapsed)

  const [session, setSession] = useState<VibeSessionWithTasks | null>(null)
  const [loading, setLoading] = useState(true)
  const [ending, setEnding] = useState(false)
  const [showNotes, setShowNotes] = useState(false)
  const [notes, setNotes] = useState('')

  useEffect(() => {
    fetch(`/api/sessions/${params.id}`)
      .then((r) => r.json())
      .then((data) => {
        setSession(data)
        setNotes(data.notes ?? '')
        setLoading(false)
      })
      .catch(() => router.push('/dashboard'))
  }, [params.id]) // eslint-disable-line react-hooks/exhaustive-deps

  const handleComplete = useCallback(async () => {
    if (ending || !session) return
    setEnding(true)
    await fetch(`/api/sessions/${session.id}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: 'COMPLETED', elapsed, notes }),
    })
    endSession()
    router.push('/dashboard')
  }, [session, elapsed, notes, ending, endSession, router])

  async function handleAbandon() {
    if (!session) return
    await fetch(`/api/sessions/${session.id}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: 'ABANDONED', elapsed }),
    })
    endSession()
    router.push('/dashboard')
  }

  if (loading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center text-white/30">
        Loading session…
      </div>
    )
  }

  if (!session) return null
  const config = VIBE_CONFIGS[session.vibeType]

  return (
    <div
      className="animate-fade-in"
      style={{ '--vibe-color': config.color } as React.CSSProperties}
    >
      {/* Header */}
      <div className="mb-8 flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-2xl">{config.emoji}</span>
            <h1 className="text-xl font-bold">{config.label}</h1>
            <span
              className="rounded-full px-2 py-0.5 text-xs font-medium"
              style={{ background: `${config.color}20`, color: config.color }}
            >
              Active
            </span>
          </div>
          <p className="mt-1 text-sm text-white/30 italic">&ldquo;{config.quote}&rdquo;</p>
        </div>
        <button onClick={handleAbandon} className="btn-danger text-xs py-2 px-4">
          End session
        </button>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        {/* Timer column */}
        <div className="flex flex-col gap-6">
          <div className="flex justify-center rounded-2xl border border-border bg-card py-10">
            <VibeTimer session={session} onComplete={handleComplete} />
          </div>

          {/* Notes */}
          <div className="rounded-2xl border border-border bg-card p-6">
            <button
              onClick={() => setShowNotes(!showNotes)}
              className="flex w-full items-center justify-between text-left"
            >
              <h3 className="font-semibold">Session notes</h3>
              <span className="text-white/30 text-sm">{showNotes ? '−' : '+'}</span>
            </button>
            {showNotes && (
              <textarea
                className="mt-3 w-full resize-none rounded-xl border border-border bg-surface px-4 py-3 text-sm text-white/80 placeholder-white/20 outline-none focus:border-white/20"
                rows={4}
                placeholder="What are you working on? Any thoughts…"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              />
            )}
          </div>

          {/* Complete button */}
          <button
            onClick={handleComplete}
            disabled={ending}
            className="w-full rounded-xl py-4 text-base font-semibold transition-all active:scale-[0.99] disabled:opacity-50"
            style={{ background: config.color, color: '#000' }}
          >
            {ending ? 'Completing…' : '✓ Complete Session'}
          </button>
        </div>

        {/* Sidebar */}
        <div className="flex flex-col gap-4">
          <VibeTaskList
            sessionId={session.id}
            initialTasks={session.tasks}
            color={config.color}
          />
          <VibeAmbience vibeType={session.vibeType} />
        </div>
      </div>
    </div>
  )
}
