<template>
  <main class="page">
    <div style="display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem">
      <div>
        <p class="eyebrow">账号</p>
        <h1>个人资料</h1>
      </div>
      <button class="linkish" style="background:none;border:0" @click="logout">退出</button>
    </div>

    <div class="stack" style="margin-top: 2rem">
      <label class="field">
        显示名称
        <input v-model="displayName" maxlength="40" />
      </label>
      <div class="stack">
        <p class="muted">我的身份</p>
        <button
          v-for="r in roles"
          :key="r.id"
          type="button"
          class="card"
          :style="role === r.id ? 'background: var(--accent); color: var(--accent-fg)' : ''"
          @click="role = r.id"
        >
          <strong>{{ r.label }}</strong>
          <div class="muted" :style="role === r.id ? 'opacity:.8;color:inherit' : ''">{{ r.desc }}</div>
        </button>
      </div>
      <p v-if="error" class="err">{{ error }}</p>
      <p v-if="msg" class="muted">{{ msg }}</p>
      <button class="btn" :disabled="busy" @click="save">{{ busy ? '保存中…' : '保存资料' }}</button>
      <div class="row">
        <router-link class="btn secondary" to="/">佩戴端</router-link>
        <router-link class="btn secondary" to="/keys">钥匙台</router-link>
      </div>
    </div>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const displayName = ref('')
const role = ref('both')
const busy = ref(false)
const error = ref('')
const msg = ref('')

const roles = [
  { id: 'wearer', label: '佩戴者', desc: '创建并执行锁定' },
  { id: 'keyholder', label: '钥匙管理者', desc: '远程加时 / 减时 / 任务 / 开锁' },
  { id: 'both', label: '两者皆可', desc: '同一账号可切换场景' },
]

onMounted(async () => {
  try {
    const p = await auth.loadProfile()
    displayName.value = p.displayName || ''
    role.value = p.role || 'both'
  } catch (e) {
    error.value = e.message
  }
})

async function save() {
  busy.value = true
  error.value = ''
  msg.value = ''
  try {
    await auth.saveProfile({ displayName: displayName.value, role: role.value })
    msg.value = '已保存'
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}

function logout() {
  auth.logout()
  router.push('/login')
}
</script>
