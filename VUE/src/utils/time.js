export function formatDuration(ms) {
  const n = Math.max(0, Math.floor(ms / 1000))
  const d = Math.floor(n / 86400)
  const h = Math.floor((n % 86400) / 3600)
  const m = Math.floor((n % 3600) / 60)
  const s = n % 60
  if (d > 0) return `${d}天 ${h}时 ${m}分`
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  return `${m}:${String(s).padStart(2, '0')}`
}

export function remainingMs(lock, now = Date.now()) {
  if (!lock) return 0
  const t = lock.frozenAt != null ? lock.frozenAt : now
  return Math.max(0, lock.endsAt - t)
}

export function canWearerEnd(lock, now = Date.now()) {
  if (!lock || lock.frozenAt != null) return false
  if (now < lock.endsAt) return false
  return now >= lock.startedAt + (lock.minLockMs || 0)
}

export function hoursToMs(h) {
  return Math.round(Number(h) * 3600_000)
}
