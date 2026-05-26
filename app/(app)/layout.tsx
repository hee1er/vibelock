import { getServerSession } from 'next-auth'
import { redirect } from 'next/navigation'
import { authOptions } from '@/lib/auth'
import { Navbar } from '@/components/layout/Navbar'

export default async function AppLayout({ children }: { children: React.ReactNode }) {
  const session = await getServerSession(authOptions)
  if (!session) redirect('/login')

  return (
    <div className="min-h-screen bg-black">
      <Navbar user={session.user} />
      <main className="mx-auto max-w-6xl px-4 pt-20 pb-12 sm:px-6">
        {children}
      </main>
    </div>
  )
}
