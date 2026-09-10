<template>
  <main class="page">
    <header style="display:flex;justify-content:space-between;align-items:flex-end;gap:1rem">
      <div>
        <p class="eyebrow">Yue Lock</p>
        <h1>月锁</h1>
      </div>
      <div style="display:flex;flex-direction:column;align-items:flex-end;gap:0.4rem">
        <span class="chip">{{ statusLabel }}</span>
        <div style="display:flex;gap:0.75rem">
          <router-link class="linkish" to="/profile">资料</router-link>
          <router-link class="linkish" to="/keys">钥匙</router-link>
        </div>
      </div>
    </header>

    <p v-if="store.error" class="err" style="margin-top:1rem">{{ store.error }}</p>

    <CreateLock v-if="!store.lock" @created="onCreated" />
    <ActiveLock v-else :lock="store.lock" :now="now" @changed="onChanged" />
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useLockStore } from '../stores/lock'
import { integritySync } from '../api/lock'
import CreateLock from '../components/CreateLock.vue'
import ActiveLock from '../components/ActiveLock.vue'

const store = useLockStore()
const now = ref(Date.now())
let tick
let poll
let integrity

const statusLabel = computed(() => (store.lock ? '锁定中' : '空闲'))

onMounted(() => {
  store.hydrate()
  tick = setInterval(() => (now.value = Date.now()), 250)
  poll = setInterval(() => {
    if (store.lock) store.refresh().catch(() => {})
  }, 4000)
  integrity = setInterval(() => {
    const l = store.lock
    if (!l) return
    integritySync({
      token: l.wearerToken,
      clientNow: Date.now(),
      localEndsAt: l.endsAt,
      sessionNonce: l.sessionNonce,
    })
      .then((r) => store.persist(r.lock))
      .catch(() => {})
  }, 20000)
})

onUnmounted(() => {
  clearInterval(tick)
  clearInterval(poll)
  clearInterval(integrity)
})

function onCreated() {}
function onChanged(next) {
  store.persist(next)
}
</script>
