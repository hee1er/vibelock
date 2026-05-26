'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { signOut } from 'next-auth/react'
import { cn } from '@/lib/utils'

interface NavbarProps {
  user: { name?: string | null; email?: string | null }
}

const links = [
  { href: '/dashboard', label: 'Dashboard' },
  { href: '/history', label: 'History' },
]

export function Navbar({ user }: NavbarProps) {
  const pathname = usePathname()

  return (
    <nav className="fixed top-0 z-50 w-full border-b border-border/30 bg-black/80 backdrop-blur-sm">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4 sm:px-6">
        <div className="flex items-center gap-6">
          <Link href="/dashboard" className="text-base font-bold">
            VibeLock 🔒
          </Link>
          <div className="hidden gap-1 sm:flex">
            {links.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className={cn(
                  'rounded-lg px-3 py-1.5 text-sm transition-colors',
                  pathname === link.href
                    ? 'bg-white/10 text-white'
                    : 'text-white/50 hover:text-white/80'
                )}
              >
                {link.label}
              </Link>
            ))}
          </div>
        </div>
        <div className="flex items-center gap-3">
          <span className="hidden text-sm text-white/40 sm:block">
            {user.name ?? user.email}
          </span>
          <button
            onClick={() => signOut({ callbackUrl: '/' })}
            className="rounded-lg px-3 py-1.5 text-sm text-white/40 transition-colors hover:text-white/70"
          >
            Sign out
          </button>
        </div>
      </div>
    </nav>
  )
}
