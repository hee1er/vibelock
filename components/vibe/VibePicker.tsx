'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { VIBE_CONFIGS, VibeType } from '@/types'
import { cn } from '@/lib/utils'

export function VibePicker() {
  const router = useRouter()
  const [selected, setSelected] = useState<VibeType | null>(null)
  const [duration, setDuration] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)

  const config = selected ? VIBE_CONFIGS[selected] : null

  async function handleStart() {
    if (!selected) return
    setLoading(true)
    const res = await fetch('/api/sessions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        vibeType: selected,
        duration: duration ?? VIBE_CONFIGS[selected].defaultDuration,
      }),
    })
    if (res.ok) {
      const data = await res.json()
      router.push(`/session/${data.id}`)
    }
    setLoading(false)
  }

  return (
    <div className="animate-fade-in">
      <div className="mb-8 text-center">
        <h1 className="text-3xl font-bold sm:text-4xl">What&apos;s your vibe?</h1>
        <p className="mt-2 text-white/40">Pick a mode to lock into</p>
      </div>

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
        {Object.values(VIBE_CONFIGS).map((vibe) => (
          <button
            key={vibe.id}
            onClick={() => {
              setSelected(vibe.id)
              setDuration(null)
            }}
            className={cn(
              'vibe-card text-left transition-all',
              selected === vibe.id
                ? 'ring-2 ring-offset-1 ring-offset-black scale-[1.02]'
                : 'hover:scale-[1.01]'
            )}
            style={{
              borderColor: selected === vibe.id ? vibe.color : `${vibe.color}25`,
              ...(selected === vibe.id ? { ringColor: vibe.color } : {}),
            }}
          >
            {selected === vibe.id && (
              <div
                className="absolute inset-0 opacity-20"
                style={{ background: `radial-gradient(circle at top left, ${vibe.color}, transparent 70%)` }}
              />
            )}
            <div className="relative">
              <div className="mb-2 text-3xl">{vibe.emoji}</div>
              <div className="mb-0.5 font-semibold text-sm">{vibe.label}</div>
              <div className="text-xs text-white/40 leading-snug">{vibe.description}</div>
              <div className="mt-3 text-xs font-medium" style={{ color: vibe.color }}>
                {vibe.defaultDuration}m
              </div>
            </div>
          </button>
        ))}
      </div>

      {config && (
        <div className="mt-6 animate-slide-up rounded-2xl border border-border bg-card p-6">
          <div className="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <div className="mb-3 text-sm text-white/50">Duration</div>
              <div className="flex flex-wrap gap-2">
                {[15, 25, 45, 60, 90].map((min) => (
                  <button
                    key={min}
                    onClick={() => setDuration(min)}
                    className={cn(
                      'rounded-lg border px-3 py-1.5 text-sm font-medium transition-all',
                      (duration ?? config.defaultDuration) === min
                        ? 'border-transparent text-black'
                        : 'border-border bg-transparent text-white/50 hover:text-white'
                    )}
                    style={
                      (duration ?? config.defaultDuration) === min
                        ? { backgroundColor: config.color }
                        : {}
                    }
                  >
                    {min}m
                  </button>
                ))}
              </div>
            </div>
            <button
              onClick={handleStart}
              disabled={loading}
              className="btn-primary min-w-[160px] justify-center"
              style={{ background: config.color, color: '#000' }}
            >
              {loading ? 'Starting…' : `Lock into ${config.label} →`}
            </button>
          </div>
          <div className="mt-4 border-t border-border pt-4">
            <p className="text-xs italic text-white/30">&ldquo;{config.quote}&rdquo;</p>
          </div>
        </div>
      )}
    </div>
  )
}
