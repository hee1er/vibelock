'use client'

import { useEffect, useCallback } from 'react'
import { useVibeStore } from '@/store/vibeStore'
import { VIBE_CONFIGS, VibeSessionWithTasks } from '@/types'
import { formatDuration } from '@/lib/utils'

const CIRCUMFERENCE = 2 * Math.PI * 80 // r=80

interface VibeTimerProps {
  session: VibeSessionWithTasks
  onComplete: () => void
}

export function VibeTimer({ session, onComplete }: VibeTimerProps) {
  const { elapsed, running, targetSeconds, startSession, tick, pause, resume } = useVibeStore()
  const config = VIBE_CONFIGS[session.vibeType]

  useEffect(() => {
    startSession(session.id, session.vibeType, session.duration, session.elapsed)
  }, [session.id]) // eslint-disable-line react-hooks/exhaustive-deps

  // Tick every second
  useEffect(() => {
    if (!running) return
    const id = setInterval(() => {
      tick()
    }, 1000)
    return () => clearInterval(id)
  }, [running, tick])

  // Auto-save elapsed every 10s
  const saveElapsed = useCallback(
    async (elapsedVal: number) => {
      await fetch(`/api/sessions/${session.id}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ elapsed: elapsedVal }),
      })
    },
    [session.id]
  )

  useEffect(() => {
    if (elapsed > 0 && elapsed % 10 === 0) {
      saveElapsed(elapsed)
    }
    if (elapsed >= targetSeconds && targetSeconds > 0) {
      onComplete()
    }
  }, [elapsed, targetSeconds, saveElapsed, onComplete])

  const progress = targetSeconds > 0 ? elapsed / targetSeconds : 0
  const dashOffset = CIRCUMFERENCE * (1 - progress)
  const remaining = Math.max(targetSeconds - elapsed, 0)

  return (
    <div className="flex flex-col items-center">
      {/* SVG Ring Timer */}
      <div className="relative">
        <svg width="220" height="220" viewBox="0 0 220 220">
          {/* Background track */}
          <circle
            cx="110" cy="110" r="80"
            fill="none"
            stroke="#1a1a1a"
            strokeWidth="8"
          />
          {/* Progress ring */}
          <circle
            cx="110" cy="110" r="80"
            fill="none"
            stroke={config.color}
            strokeWidth="8"
            strokeLinecap="round"
            strokeDasharray={CIRCUMFERENCE}
            strokeDashoffset={dashOffset}
            className="timer-ring"
            style={{ transition: 'stroke-dashoffset 1s linear' }}
          />
        </svg>
        {/* Center content */}
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <div className="text-4xl font-mono font-bold tabular-nums">
            {formatDuration(remaining)}
          </div>
          <div className="mt-1 text-xs text-white/40">
            {running ? 'running' : 'paused'}
          </div>
          <div className="mt-2 text-xl">{config.emoji}</div>
        </div>
      </div>

      {/* Progress text */}
      <div className="mt-4 text-sm text-white/40">
        {formatDuration(elapsed)} elapsed · {Math.round(progress * 100)}% done
      </div>

      {/* Controls */}
      <div className="mt-6 flex gap-3">
        <button
          onClick={running ? pause : resume}
          className="rounded-xl px-6 py-3 text-sm font-medium transition-all active:scale-95"
          style={{
            background: running ? `${config.color}20` : config.color,
            color: running ? config.color : '#000',
            border: `1px solid ${config.color}40`,
          }}
        >
          {running ? '⏸ Pause' : '▶ Resume'}
        </button>
      </div>
    </div>
  )
}
