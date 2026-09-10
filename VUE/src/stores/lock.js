import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as api from '../api/lock'

const LOCK_KEY = 'yue-lock:v1'

export const useLockStore = defineStore('lock', () => {
  const lock = ref(null)
  const busy = ref(false)
  const error = ref('')

  function hydrate() {
    try {
      const raw = localStorage.getItem(LOCK_KEY)
      lock.value = raw ? JSON.parse(raw) : null
    } catch {
      lock.value = null
    }
  }

  function persist(next) {
    lock.value = next && next.status === 'active' ? next : null
    if (lock.value) localStorage.setItem(LOCK_KEY, JSON.stringify(lock.value))
    else localStorage.removeItem(LOCK_KEY)
  }

  async function create(payload) {
    busy.value = true
    error.value = ''
    try {
      persist(await api.createLock(payload))
    } catch (e) {
      error.value = e.message
      throw e
    } finally {
      busy.value = false
    }
  }

  async function refresh() {
    if (!lock.value?.wearerToken) return
    const remote = await api.getByWearer(lock.value.wearerToken)
    if (remote?.id) persist(remote)
    else persist(null)
  }

  async function run(fn) {
    busy.value = true
    error.value = ''
    try {
      const next = await fn()
      if (next?.id) persist(next)
      return next
    } catch (e) {
      error.value = e.message
      throw e
    } finally {
      busy.value = false
    }
  }

  return { lock, busy, error, hydrate, persist, create, refresh, run }
})
