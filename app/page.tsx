import Link from 'next/link'
import { VIBE_CONFIGS } from '@/types'

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-black text-white">
      {/* Nav */}
      <nav className="fixed top-0 z-50 w-full border-b border-border/30 bg-black/80 backdrop-blur-sm">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
          <span className="text-lg font-bold tracking-tight">VibeLock 🔒</span>
          <div className="flex items-center gap-3">
            <Link href="/login" className="btn-ghost text-sm py-2 px-4">Sign in</Link>
            <Link href="/register" className="btn-primary text-sm py-2 px-4">Get started</Link>
          </div>
        </div>
      </nav>

      {/* Hero */}
      <section className="relative flex min-h-screen flex-col items-center justify-center px-6 pt-20">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_rgba(168,85,247,0.08)_0%,_transparent_60%)]" />
        <div className="relative z-10 max-w-4xl text-center">
          <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-purple-500/30 bg-purple-500/10 px-4 py-2 text-sm text-purple-300">
            <span className="h-1.5 w-1.5 rounded-full bg-purple-400 animate-pulse-slow" />
            Focus. Flow. Repeat.
          </div>
          <h1 className="mb-6 text-6xl font-extrabold leading-tight tracking-tight md:text-7xl lg:text-8xl">
            Lock into
            <br />
            <span className="bg-gradient-to-r from-blue-400 via-purple-400 to-pink-400 bg-clip-text text-transparent">
              your vibe.
            </span>
          </h1>
          <p className="mb-10 text-xl text-white/50 md:text-2xl max-w-2xl mx-auto">
            Choose your mode. Set a timer. Track your focus sessions.
            Build the habit of deep work.
          </p>
          <div className="flex flex-col items-center gap-4 sm:flex-row sm:justify-center">
            <Link href="/register" className="btn-primary px-8 py-4 text-base">
              Start for free →
            </Link>
            <Link href="/login" className="btn-ghost px-8 py-4 text-base">
              Sign in
            </Link>
          </div>
        </div>

        {/* Scroll indicator */}
        <div className="absolute bottom-10 flex flex-col items-center gap-2 text-white/30">
          <span className="text-xs uppercase tracking-widest">Choose your vibe</span>
          <div className="h-6 w-0.5 bg-gradient-to-b from-white/30 to-transparent" />
        </div>
      </section>

      {/* Vibe Cards */}
      <section className="px-6 py-24">
        <div className="mx-auto max-w-6xl">
          <h2 className="mb-12 text-center text-3xl font-bold">Five vibes. Infinite focus.</h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
            {Object.values(VIBE_CONFIGS).map((vibe) => (
              <div
                key={vibe.id}
                className="vibe-card group"
                style={{ borderColor: `${vibe.color}30` }}
              >
                <div
                  className="absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-100"
                  style={{ background: `radial-gradient(circle at top left, ${vibe.color}15, transparent 60%)` }}
                />
                <div className="relative">
                  <div className="mb-3 text-3xl">{vibe.emoji}</div>
                  <div className="mb-1 font-semibold">{vibe.label}</div>
                  <div className="text-xs text-white/40">{vibe.description}</div>
                  <div
                    className="mt-4 text-xs font-medium"
                    style={{ color: vibe.color }}
                  >
                    {vibe.defaultDuration}min default
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="border-t border-border/30 px-6 py-24">
        <div className="mx-auto max-w-4xl">
          <h2 className="mb-16 text-center text-3xl font-bold">Built for flow state</h2>
          <div className="grid gap-8 sm:grid-cols-3">
            {[
              { icon: '⏱', title: 'Pomodoro Timer', desc: 'Custom durations per vibe type. Pause anytime, auto-save progress.' },
              { icon: '✅', title: 'Session Tasks', desc: 'Add tasks to each session. Track completion as you go.' },
              { icon: '📊', title: 'Streak Tracking', desc: 'Daily streaks, total minutes, favorite vibe analytics.' },
            ].map((f) => (
              <div key={f.title} className="rounded-2xl border border-border bg-card p-6">
                <div className="mb-3 text-3xl">{f.icon}</div>
                <div className="mb-2 font-semibold">{f.title}</div>
                <div className="text-sm text-white/40">{f.desc}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="px-6 py-24">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="mb-4 text-4xl font-extrabold">Ready to lock in?</h2>
          <p className="mb-8 text-white/40">Free forever. No credit card required.</p>
          <Link href="/register" className="btn-primary px-10 py-4 text-base">
            Create your account →
          </Link>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-border/30 px-6 py-8 text-center text-sm text-white/30">
        VibeLock — Lock into your vibe.
      </footer>
    </div>
  )
}
