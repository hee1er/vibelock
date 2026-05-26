export type VibeType = 'DEEP_WORK' | 'CREATIVE' | 'CHILL' | 'SOCIAL' | 'WORKOUT'
export type SessionStatus = 'ACTIVE' | 'COMPLETED' | 'ABANDONED'

export interface VibeConfig {
  id: VibeType
  label: string
  emoji: string
  description: string
  color: string
  bgClass: string
  textClass: string
  borderClass: string
  ringClass: string
  defaultDuration: number // minutes
  sounds: string[]
  quote: string
}

export const VIBE_CONFIGS: Record<VibeType, VibeConfig> = {
  DEEP_WORK: {
    id: 'DEEP_WORK',
    label: 'Deep Work',
    emoji: '🎯',
    description: 'Full focus. Zero distractions. Ship it.',
    color: '#3b82f6',
    bgClass: 'bg-blue-500',
    textClass: 'text-blue-400',
    borderClass: 'border-blue-500/40',
    ringClass: 'ring-blue-500',
    defaultDuration: 25,
    sounds: ['Rain on window', 'White noise', 'Café ambience'],
    quote: 'The ability to concentrate is the rare and crucial skill that makes work valuable.',
  },
  CREATIVE: {
    id: 'CREATIVE',
    label: 'Creative Flow',
    emoji: '🎨',
    description: 'Let ideas run wild. Make something.',
    color: '#a855f7',
    bgClass: 'bg-purple-500',
    textClass: 'text-purple-400',
    borderClass: 'border-purple-500/40',
    ringClass: 'ring-purple-500',
    defaultDuration: 45,
    sounds: ['Lo-fi beats', 'Forest sounds', 'Soft jazz'],
    quote: 'Creativity is intelligence having fun.',
  },
  CHILL: {
    id: 'CHILL',
    label: 'Chill Mode',
    emoji: '😌',
    description: 'Slow down. Breathe. Restore.',
    color: '#10b981',
    bgClass: 'bg-emerald-500',
    textClass: 'text-emerald-400',
    borderClass: 'border-emerald-500/40',
    ringClass: 'ring-emerald-500',
    defaultDuration: 20,
    sounds: ['Ocean waves', 'Birdsong', 'Fireplace'],
    quote: 'Almost everything will work again if you unplug it for a few minutes.',
  },
  SOCIAL: {
    id: 'SOCIAL',
    label: 'Social Energy',
    emoji: '💬',
    description: 'Connect. Collaborate. Energize.',
    color: '#f59e0b',
    bgClass: 'bg-amber-500',
    textClass: 'text-amber-400',
    borderClass: 'border-amber-500/40',
    ringClass: 'ring-amber-500',
    defaultDuration: 30,
    sounds: ['Upbeat café', 'City sounds', 'Light music'],
    quote: 'Alone we can do so little; together we can do so much.',
  },
  WORKOUT: {
    id: 'WORKOUT',
    label: 'Workout',
    emoji: '💪',
    description: 'Push hard. Go beyond. Break limits.',
    color: '#ef4444',
    bgClass: 'bg-red-500',
    textClass: 'text-red-400',
    borderClass: 'border-red-500/40',
    ringClass: 'ring-red-500',
    defaultDuration: 30,
    sounds: ['Power metal', 'EDM', 'Hip-hop beats'],
    quote: 'Pain is temporary. Glory lasts forever.',
  },
}

export interface TaskItem {
  id: string
  text: string
  completed: boolean
  order: number
}

export interface VibeSessionWithTasks {
  id: string
  vibeType: VibeType
  status: SessionStatus
  duration: number
  elapsed: number
  notes: string | null
  isPublic: boolean
  startedAt: string
  endedAt: string | null
  tasks: TaskItem[]
}

export interface UserStats {
  totalSessions: number
  completedSessions: number
  totalMinutes: number
  currentStreak: number
  longestStreak: number
  favoriteVibe: VibeType | null
}

declare module 'next-auth' {
  interface Session {
    user: {
      id: string
      name?: string | null
      email?: string | null
      image?: string | null
    }
  }
}
