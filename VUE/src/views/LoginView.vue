<template>
  <main class="page">
    <p class="eyebrow">Yue Lock</p>
    <h1>登录月锁</h1>
    <p class="muted" style="margin-top: 0.5rem">邮箱注册后，可在资料里选择佩戴者或钥匙管理者。</p>

    <form class="stack" style="margin-top: 2rem" @submit.prevent="submit">
      <label v-if="mode === 'signup'" class="field">
        显示名称
        <input v-model="name" placeholder="怎么称呼你" />
      </label>
      <label class="field">
        邮箱
        <input v-model="email" type="email" required placeholder="you@example.com" />
      </label>
      <label class="field">
        密码（至少 8 位）
        <input v-model="password" type="password" required minlength="8" />
      </label>
      <p v-if="error" class="err">{{ error }}</p>
      <button class="btn" :disabled="busy">{{ busy ? '请稍候…' : mode === 'signup' ? '注册并登录' : '登录' }}</button>
    </form>

    <button class="linkish" style="margin-top: 1rem; background: none; border: 0; width: 100%" @click="toggle">
      {{ mode === 'signin' ? '没有账号？注册' : '已有账号？登录' }}
    </button>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const mode = ref('signin')
const email = ref('')
const password = ref('')
const name = ref('')
const busy = ref(false)
const error = ref('')

function toggle() {
  mode.value = mode.value === 'signin' ? 'signup' : 'signin'
  error.value = ''
}

async function submit() {
  busy.value = true
  error.value = ''
  try {
    if (mode.value === 'signup') {
      await auth.register({ email: email.value, password: password.value, name: name.value })
    } else {
      await auth.login({ email: email.value, password: password.value })
    }
    router.push('/')
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
</script>
