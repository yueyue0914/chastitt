<template>
  <main class="page">
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1rem">
      <p class="eyebrow">钥匙端</p>
      <div style="display:flex;gap:0.75rem;align-items:center">
        <router-link v-if="auth.isLoggedIn" class="linkish" to="/keys">工作台</router-link>
        <router-link v-else class="linkish" to="/login">登录认领</router-link>
      </div>
    </div>

    <p v-if="loading" class="muted" style="text-align:center">加载中…</p>
    <template v-else-if="!lock || lock.status !== 'active'">
      <p class="font-display" style="text-align:center;font-size:1.8rem">无进行中锁定</p>
      <router-link class="btn secondary" style="margin-top:2rem;display:block;text-align:center" to="/">返回月锁</router-link>
    </template>
    <template v-else>
      <div class="countdown">{{ formatDuration(remain) }}</div>
      <p class="muted" style="text-align:center">
        {{ lock.frozenAt ? '已冻结' : '解锁 ' + new Date(lock.endsAt).toLocaleString() }}
      </p>
      <p v-if="message" class="muted" style="text-align:center">{{ message }}</p>
      <p v-if="error" class="err">{{ error }}</p>

      <div class="stack" style="margin-top:1.5rem">
        <p class="muted">加时</p>
        <div class="row">
          <button class="btn secondary" :disabled="busy" @click="add(15*60e3)">+15分</button>
          <button class="btn secondary" :disabled="busy" @click="add(3600e3)">+1时</button>
          <button class="btn secondary" :disabled="busy" @click="add(6*3600e3)">+6时</button>
        </div>
        <p class="muted">减时</p>
        <div class="row">
          <button class="btn secondary" :disabled="busy" @click="sub(15*60e3)">-15分</button>
          <button class="btn secondary" :disabled="busy" @click="sub(3600e3)">-1时</button>
          <button class="btn secondary" :disabled="busy" @click="sub(6*3600e3)">-6时</button>
        </div>
        <div class="row">
          <button class="btn secondary" :disabled="busy" @click="toggleFreeze">
            {{ lock.frozenAt ? '解冻' : '冻结' }}
          </button>
          <button class="btn secondary" :disabled="busy" @click="forceHygiene">强制清洁</button>
        </div>
        <button class="btn secondary" :disabled="busy" @click="reqPhoto">要求拍照</button>
        <div v-if="lock.photoThumb" class="card">
          <p class="muted">已提交照片</p>
          <img :src="lock.photoThumb" alt="" style="max-width:100%;border-radius:0.5rem" />
        </div>

        <div class="card stack">
          <p class="muted">服从确认规则</p>
          <label class="check">
            <input v-model="obEnabled" type="checkbox" /> 开启定期服从
          </label>
          <label class="field">
            间隔（分钟）
            <input v-model.number="obIntervalMin" type="number" min="1" max="1440" />
          </label>
          <label class="field">
            短句
            <input v-model="obPhrase" maxlength="80" />
          </label>
          <label class="field">
            应答时限（秒）
            <input v-model.number="obTimeoutSec" type="number" min="30" max="1800" />
          </label>
          <label class="field">
            超时加罚（小时）
            <input v-model.number="obPenaltyHours" type="number" min="0.1" step="0.5" />
          </label>
          <p class="muted" style="font-size:0.8rem">
            成功 {{ lock.obedienceSuccessCount || 0 }} · 失败 {{ lock.obedienceFailCount || 0 }}
          </p>
          <button class="btn secondary" :disabled="busy" @click="saveObedience">保存服从规则</button>
        </div>

        <div class="card stack">
          <p class="muted">结束宣言（佩戴者到期/紧急须完整输入）</p>
          <input v-model="editPhrase" maxlength="200" autocomplete="off" />
          <label class="field">
            输错几次加罚
            <input v-model.number="editMaxFails" type="number" min="1" max="20" />
          </label>
          <label class="field">
            加罚小时
            <input v-model.number="editPenaltyHours" type="number" min="0.1" step="0.5" />
          </label>
          <button class="btn secondary" :disabled="busy || editPhrase.trim().length < 4" @click="savePhrase">
            保存宣言规则
          </button>
        </div>

        <div class="card stack">
          <p class="muted">发布任务</p>
          <input v-model="taskTitle" placeholder="任务内容" />
          <select v-model="rewardType">
            <option value="reduce">减时奖励</option>
            <option value="unlock">完成即开锁</option>
          </select>
          <button class="btn secondary" :disabled="busy || taskTitle.trim().length < 2" @click="newTask">
            发布
          </button>
        </div>

        <div v-if="tasks.filter(t=>t.status==='open').length" class="card stack">
          <p class="muted">进行中任务</p>
          <div v-for="t in tasks.filter(t=>t.status==='open')" :key="t.id">{{ t.title }}</div>
        </div>

        <button class="btn warn" :disabled="busy" @click="doUnlock">钥匙开锁</button>
      </div>
    </template>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import * as api from '../api/lock'
import { useAuthStore } from '../stores/auth'
import { formatDuration, remainingMs } from '../utils/time'

const route = useRoute()
const auth = useAuthStore()
const code = computed(() => route.params.code)
const lock = ref(null)
const tasks = ref([])
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const message = ref('')
const now = ref(Date.now())
const taskTitle = ref('')
const rewardType = ref('reduce')
const editPhrase = ref('')
const editMaxFails = ref(3)
const editPenaltyHours = ref(1)
const obEnabled = ref(true)
const obIntervalMin = ref(30)
const obPhrase = ref('服从主人')
const obTimeoutSec = ref(120)
const obPenaltyHours = ref(1)
const remain = computed(() => remainingMs(lock.value, now.value))
let poll
let tick

async function load() {
  try {
    const remote = await api.getByKeyholder(code.value)
    lock.value = remote?.id ? remote : null
    if (lock.value) {
      tasks.value = await api.listTasks(code.value, 'keyholder')
      editPhrase.value = lock.value.endPhrase || ''
      editMaxFails.value = lock.value.phraseMaxFails || 3
      editPenaltyHours.value = Math.max(0.1, (lock.value.phraseFailPenaltyMs || 3600000) / 3600000)
      obEnabled.value = !!lock.value.obedienceEnabled
      obIntervalMin.value = Math.max(1, Math.round((lock.value.obedienceIntervalMs || 1800000) / 60000))
      obPhrase.value = lock.value.obediencePhrase || '服从主人'
      obTimeoutSec.value = Math.max(30, Math.round((lock.value.obedienceTimeoutMs || 120000) / 1000))
      obPenaltyHours.value = Math.max(0.1, (lock.value.obediencePenaltyMs || 3600000) / 3600000)
    }
    error.value = ''
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await load()
  if (auth.isLoggedIn) {
    try {
      await api.claimKeyholder(code.value)
      message.value = '已绑定到当前账号'
      await load()
    } catch (e) {
      if (String(e.message).includes('其他')) error.value = e.message
    }
  }
  poll = setInterval(load, 4000)
  tick = setInterval(() => (now.value = Date.now()), 250)
})
onUnmounted(() => {
  clearInterval(poll)
  clearInterval(tick)
})

async function run(fn, ok) {
  busy.value = true
  error.value = ''
  try {
    const next = await fn()
    if (next?.status === 'active') lock.value = next
    else if (next?.lock) lock.value = next.lock.status === 'active' ? next.lock : null
    else lock.value = null
    if (ok) message.value = ok
    await load()
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}

const add = (ms) => run(() => api.addTime({ token: code.value, ms }), '已加时')
const sub = (ms) => run(() => api.subTime({ token: code.value, ms }), '已减时')
const toggleFreeze = () =>
  run(() => api.setFreeze({ token: code.value, frozen: !lock.value.frozenAt }), '已更新冻结')
const forceHygiene = () =>
  run(() => api.hygieneStart({ token: code.value, role: 'keyholder' }), '已强制清洁')
const reqPhoto = () => run(() => api.photoRequest({ token: code.value }), '已请求拍照')
const savePhrase = () =>
  run(
    () =>
      api.setEndPhrase({
        token: code.value,
        endPhrase: editPhrase.value,
        phraseMaxFails: editMaxFails.value,
        phraseFailPenaltyMs: Math.round(editPenaltyHours.value * 3600_000),
      }),
    '宣言规则已更新',
  )
const saveObedience = () =>
  run(
    () =>
      api.setObedience({
        token: code.value,
        enabled: obEnabled.value,
        intervalMs: obIntervalMin.value * 60_000,
        phrase: obPhrase.value,
        timeoutMs: obTimeoutSec.value * 1000,
        penaltyMs: Math.round(obPenaltyHours.value * 3600_000),
      }),
    '服从规则已更新',
  )
const newTask = () =>
  run(
    () =>
      api.createTask({
        token: code.value,
        title: taskTitle.value,
        rewardType: rewardType.value,
        rewardMs: 30 * 60_000,
      }),
    '任务已发布',
  ).then(() => (taskTitle.value = ''))
const doUnlock = () => {
  if (!confirm('确认钥匙开锁？')) return
  run(() => api.unlock({ token: code.value, mode: 'keyholder' }), '已开锁')
}
</script>
