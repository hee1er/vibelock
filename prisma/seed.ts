import { PrismaClient } from '@prisma/client'
import bcrypt from 'bcryptjs'

const prisma = new PrismaClient()

async function main() {
  const passwordHash = await bcrypt.hash('password123', 12)

  const demo = await prisma.user.upsert({
    where: { email: 'demo@vibelock.app' },
    update: {},
    create: {
      email: 'demo@vibelock.app',
      name: 'Demo User',
      passwordHash,
    },
  })

  const session = await prisma.vibeSession.create({
    data: {
      userId: demo.id,
      vibeType: 'DEEP_WORK',
      status: 'COMPLETED',
      duration: 25,
      elapsed: 1500,
      notes: 'Great focus session!',
      startedAt: new Date(Date.now() - 2 * 60 * 60 * 1000),
      endedAt: new Date(Date.now() - 95 * 60 * 1000),
    },
  })

  await prisma.task.createMany({
    data: [
      { vibeSessionId: session.id, text: 'Review PR comments', completed: true, order: 0 },
      { vibeSessionId: session.id, text: 'Write unit tests', completed: true, order: 1 },
      { vibeSessionId: session.id, text: 'Update documentation', completed: false, order: 2 },
    ],
  })

  await prisma.vibeSession.create({
    data: {
      userId: demo.id,
      vibeType: 'CREATIVE',
      status: 'COMPLETED',
      duration: 45,
      elapsed: 2700,
      startedAt: new Date(Date.now() - 24 * 60 * 60 * 1000),
      endedAt: new Date(Date.now() - 23 * 60 * 60 * 1000),
    },
  })

  console.log('Seed complete. Demo user: demo@vibelock.app / password123')
}

main()
  .catch(console.error)
  .finally(() => prisma.$disconnect())
