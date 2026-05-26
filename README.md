# VibeLock

Lock into your vibe. Focus deeper. Flow longer.

A mood-based focus timer app — pick a vibe (Deep Work, Creative, Chill, Social, Workout), set a Pomodoro-style timer, track tasks, and build a habit of intentional sessions.

## Architecture

```
Next.js 14 (App Router) + TypeScript
├── Frontend: Tailwind CSS, Zustand (timer state)
├── Backend: Next.js API Routes
├── Auth: NextAuth.js (credentials + JWT)
└── DB: Prisma + SQLite (swap to PostgreSQL for production)
```

## File Structure

```
app/
├── (auth)/login|register     # Auth pages
├── (app)/dashboard           # Vibe picker + stats
├── (app)/session/[id]        # Active session (timer + tasks)
├── (app)/history             # Past sessions + analytics
└── api/                      # REST API routes

components/
├── vibe/                     # VibePicker, VibeTimer, VibeTaskList, VibeAmbience
├── layout/                   # Navbar
└── providers/                # NextAuth session provider

lib/           # db.ts, auth.ts, utils.ts
store/         # Zustand vibe/timer store
types/         # Shared types + VIBE_CONFIGS
prisma/        # Schema + seed
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/register` | Register user |
| POST/GET | `/api/auth/[...nextauth]` | NextAuth |
| GET/POST | `/api/sessions` | List / create sessions |
| GET/PATCH/DELETE | `/api/sessions/:id` | Session CRUD |
| GET/POST | `/api/tasks` | List / create tasks |
| PATCH/DELETE | `/api/tasks/:id` | Task CRUD |
| GET | `/api/stats` | User statistics |

## Quick Start

```bash
npm install
cp .env.example .env    # edit DATABASE_URL + NEXTAUTH_SECRET
npm run db:push
npm run db:seed         # creates demo@vibelock.app / password123
npm run dev
```

## Production

Swap `DATABASE_URL` to a PostgreSQL URL (Neon, Supabase, Railway).
Set a real `NEXTAUTH_SECRET` (`openssl rand -base64 32`).
Deploy to Vercel.
