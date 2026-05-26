'use client'

import { useState, useOptimistic } from 'react'
import { TaskItem } from '@/types'

interface VibeTaskListProps {
  sessionId: string
  initialTasks: TaskItem[]
  color: string
}

export function VibeTaskList({ sessionId, initialTasks, color }: VibeTaskListProps) {
  const [tasks, setTasks] = useState<TaskItem[]>(initialTasks)
  const [input, setInput] = useState('')
  const [adding, setAdding] = useState(false)

  async function addTask(e: React.FormEvent) {
    e.preventDefault()
    if (!input.trim()) return
    setAdding(true)
    const res = await fetch('/api/tasks', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ vibeSessionId: sessionId, text: input.trim() }),
    })
    if (res.ok) {
      const task = await res.json()
      setTasks((prev) => [...prev, task])
      setInput('')
    }
    setAdding(false)
  }

  async function toggleTask(task: TaskItem) {
    setTasks((prev) => prev.map((t) => (t.id === task.id ? { ...t, completed: !t.completed } : t)))
    await fetch(`/api/tasks/${task.id}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ completed: !task.completed }),
    })
  }

  async function deleteTask(id: string) {
    setTasks((prev) => prev.filter((t) => t.id !== id))
    await fetch(`/api/tasks/${id}`, { method: 'DELETE' })
  }

  const done = tasks.filter((t) => t.completed).length

  return (
    <div className="rounded-2xl border border-border bg-card p-6">
      <div className="mb-4 flex items-center justify-between">
        <h3 className="font-semibold">Session tasks</h3>
        {tasks.length > 0 && (
          <span className="text-xs text-white/40">
            {done}/{tasks.length} done
          </span>
        )}
      </div>

      {tasks.length > 0 && (
        <ul className="mb-4 space-y-2">
          {tasks.map((task) => (
            <li key={task.id} className="group flex items-center gap-3">
              <button
                onClick={() => toggleTask(task)}
                className="relative flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full border transition-all"
                style={{
                  borderColor: task.completed ? color : '#333',
                  background: task.completed ? color : 'transparent',
                }}
              >
                {task.completed && (
                  <svg width="10" height="8" viewBox="0 0 10 8" fill="none">
                    <path d="M1 4l3 3 5-6" stroke="#000" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                )}
              </button>
              <span
                className={`flex-1 text-sm transition-all ${task.completed ? 'line-through text-white/25' : 'text-white/80'}`}
              >
                {task.text}
              </span>
              <button
                onClick={() => deleteTask(task.id)}
                className="hidden text-white/20 transition-colors hover:text-white/50 group-hover:flex"
              >
                ×
              </button>
            </li>
          ))}
        </ul>
      )}

      <form onSubmit={addTask} className="flex gap-2">
        <input
          type="text"
          className="input-field py-2 text-sm"
          placeholder="Add a task…"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          disabled={adding}
        />
        <button
          type="submit"
          disabled={!input.trim() || adding}
          className="rounded-xl px-4 py-2 text-sm font-medium transition-all active:scale-95 disabled:opacity-40"
          style={{ background: color, color: '#000' }}
        >
          +
        </button>
      </form>
    </div>
  )
}
