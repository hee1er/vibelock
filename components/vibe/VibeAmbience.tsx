'use client'

import { useVibeStore } from '@/store/vibeStore'
import { VIBE_CONFIGS, VibeType } from '@/types'

interface VibeAmbienceProps {
  vibeType: VibeType
}

export function VibeAmbience({ vibeType }: VibeAmbienceProps) {
  const { soundEnabled, selectedSound, toggleSound, setSound } = useVibeStore()
  const config = VIBE_CONFIGS[vibeType]

  return (
    <div className="rounded-2xl border border-border bg-card p-6">
      <div className="mb-4 flex items-center justify-between">
        <h3 className="font-semibold">Ambience</h3>
        <button
          onClick={toggleSound}
          className="flex items-center gap-2 rounded-lg px-3 py-1.5 text-xs font-medium transition-all"
          style={{
            background: soundEnabled ? `${config.color}20` : '#1a1a1a',
            color: soundEnabled ? config.color : '#666',
            border: `1px solid ${soundEnabled ? config.color + '40' : '#2a2a2a'}`,
          }}
        >
          {soundEnabled ? '🔊 On' : '🔇 Off'}
        </button>
      </div>
      <div className="flex flex-wrap gap-2">
        {config.sounds.map((sound) => (
          <button
            key={sound}
            onClick={() => setSound(selectedSound === sound ? null : sound)}
            disabled={!soundEnabled}
            className="rounded-lg border px-3 py-1.5 text-xs transition-all disabled:opacity-30"
            style={{
              borderColor: selectedSound === sound ? config.color : '#2a2a2a',
              background: selectedSound === sound ? `${config.color}15` : 'transparent',
              color: selectedSound === sound ? config.color : '#666',
            }}
          >
            {selectedSound === sound && soundEnabled ? '♪ ' : ''}{sound}
          </button>
        ))}
      </div>
      {soundEnabled && selectedSound && (
        <p className="mt-3 text-xs text-white/25">
          ♪ Playing: {selectedSound} — use your preferred music app for best results
        </p>
      )}
    </div>
  )
}
