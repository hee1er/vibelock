'use client'

import { create } from 'zustand'
import { VibeType } from '@/types'

interface TimerState {
  sessionId: string | null
  vibeType: VibeType | null
  targetSeconds: number
  elapsed: number
  running: boolean
  soundEnabled: boolean
  selectedSound: string | null
}

interface TimerActions {
  startSession: (sessionId: string, vibeType: VibeType, durationMinutes: number, elapsedSeconds?: number) => void
  tick: () => void
  pause: () => void
  resume: () => void
  endSession: () => void
  toggleSound: () => void
  setSound: (sound: string | null) => void
}

export const useVibeStore = create<TimerState & TimerActions>((set) => ({
  sessionId: null,
  vibeType: null,
  targetSeconds: 0,
  elapsed: 0,
  running: false,
  soundEnabled: false,
  selectedSound: null,

  startSession: (sessionId, vibeType, durationMinutes, elapsedSeconds = 0) =>
    set({
      sessionId,
      vibeType,
      targetSeconds: durationMinutes * 60,
      elapsed: elapsedSeconds,
      running: true,
    }),

  tick: () =>
    set((state) => ({
      elapsed: Math.min(state.elapsed + 1, state.targetSeconds),
    })),

  pause: () => set({ running: false }),
  resume: () => set({ running: true }),

  endSession: () =>
    set({
      sessionId: null,
      vibeType: null,
      targetSeconds: 0,
      elapsed: 0,
      running: false,
    }),

  toggleSound: () => set((state) => ({ soundEnabled: !state.soundEnabled })),
  setSound: (sound) => set({ selectedSound: sound }),
}))
