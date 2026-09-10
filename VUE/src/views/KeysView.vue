<template>
  <main class="page">
    <div style="display:flex;justify-content:space-between;align-items:flex-end">
      <div>
        <p class="eyebrow">钥匙管理者</p>
        <h1>工作台</h1>
      </div>
      <router-link class="linkish" to="/profile">资料</router-link>
    </div>
    <p class="muted" style="margin-top:0.75rem">已绑定账号的锁定会出现在这里，也可粘贴钥匙链接认领。</p>

    <div class="card stack" style="margin-top:1.5rem">
      <p class="muted">认领钥匙链接</p>
      <input v-model="invite" placeholder="粘贴 /key/xxxx 或令牌" />
      <button class="btn" :disabled="!invite.trim()" @click="openInvite">打开并认领</button>
    </div>

    <p v-if="error" class="err" style="margin-top:1rem">{{ error }}</p>

    <ul class="stack" style="margin-top:1.5rem;list-style:none;padding:0">
      <li v-if="!locks.length" class="muted">暂无绑定的锁定</li>
      <li v-for="l in locks" :key="l.id" class="card stack">
        <div style="display:flex;justify-content:space-between">
          <span>{{ l.status === 'active' ? '进行中' : '已结束' }}</span>
          <span class="muted">{{ formatDuration(l.durationMs) }}</span>
        </div>
        <p class="muted">解锁 {{ new Date(l.endsAt).toLocaleString() }}</p>
        <router-link
          v-if="l.status === 'active'"
          class="btn secondary"
          :to="`/key/${l.keyholderToken}`"
        >
          进入管锁
        </router-link>
      </li>
    </ul>

    <div class="row" style="margin-top:2rem">
      <router-link class="btn secondary" to="/">佩戴端</router-link>
      <router-link class="btn secondary" to="/profile">个人资料</router-link>
    </div>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { myKeyholderLocks } from '../api/lock'
import { formatDuration } from '../utils/time'

const router = useRouter()
const locks = ref([])
const invite = ref('')
const error = ref('')

onMounted(async () => {
  try {
    locks.value = await myKeyholderLocks()
  } catch (e) {
    error.value = e.message
  }
})

function openInvite() {
  const raw = invite.value.trim()
  const match = raw.match(/\/key\/([a-f0-9]+)/i)
  const code = match?.[1] || raw.replace(/[^a-f0-9]/gi, '')
  if (code.length >= 8) router.push(`/key/${code}`)
}
</script>
