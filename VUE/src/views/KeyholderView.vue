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
      <p v-if="banner" class="ok" style="text-align:center">{{ banner }}</p>
      <p v-if="error" class="err" style="text-align:center">{{ error }}</p>

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

        <!-- 结束宣言设置 -->
        <section class="card stack settings-block">
          <div>
            <p class="settings-title">结束宣言</p>
            <p class="muted" style="margin:0.25rem 0 0;font-size:0.8rem">
              佩戴者到期结束 / 紧急解锁时必须一字不差输入。
            </p>
          </div>
          <label class="field">
            宣言内容（至少 4 字）
            <textarea
              v-model="editPhrase"
              rows="3"
              maxlength="200"
              autocomplete="off"
              placeholder="例如：我是主人的无面锁屌latex性偶"
            />
            <span class="muted" style="font-size:0.75rem">{{ editPhrase.length }}/200</span>
          </label>
          <div class="row">
            <label class="field">
              最大错误次数
              <input v-model.number="editMaxFails" type="number" min="1" max="20" />
            </label>
            <label class="field">
              加罚时间（小时）
              <input v-model.number="editPenaltyHours" type="number" min="0.1" step="0.5" />
            </label>
          </div>
          <p class="muted" style="font-size:0.8rem;margin:0">
            当前生效：最多错 {{ lock.phraseMaxFails || 3 }} 次 → 加罚
            {{ formatDuration(lock.phraseFailPenaltyMs || 3600000) }}
            （已错 {{ lock.phraseFailCount || 0 }} 次）
          </p>
          <p v-if="phraseFeedback" class="ok">{{ phraseFeedback }}</p>
          <button
            class="btn"
            :disabled="busy || editPhrase.trim().length < 4"
            @click="savePhrase"
          >
            {{ busy && savingKind === 'phrase' ? '保存中…' : '保存宣言设置' }}
          </button>
        </section>

        <!-- 服从设置 -->
        <section class="card stack settings-block">
          <div>
            <p class="settings-title">服从确认</p>
            <p class="muted" style="margin:0.25rem 0 0;font-size:0.8rem">
              按间隔强制全屏弹出；超时未完成由服务端加罚。
            </p>
          </div>
          <label class="check">
            <input v-model="obEnabled" type="checkbox" />
            开启定期服从
          </label>
          <fieldset class="stack" :disabled="!obEnabled" style="border:0;padding:0;margin:0;min-inline-size:0">
            <label class="field">
              服从短句（至少 2 字）
              <input v-model="obPhrase" maxlength="80" autocomplete="off" placeholder="服从主人" />
              <span class="muted" style="font-size:0.75rem">{{ obPhrase.length }}/80</span>
            </label>
            <div class="row">
              <label class="field">
                间隔（分钟）
                <input v-model.number="obIntervalMin" type="number" min="1" max="1440" />
              </label>
              <label class="field">
                完成时限（秒）
                <input v-model.number="obTimeoutSec" type="number" min="30" max="1800" />
              </label>
            </div>
            <label class="field">
              超时惩罚（小时）
              <input v-model.number="obPenaltyHours" type="number" min="0.1" step="0.5" />
            </label>
          </fieldset>
          <p class="muted" style="font-size:0.8rem;margin:0">
            状态：{{ lock.obedienceEnabled ? '已开启' : '已关闭' }}
            · 成功 {{ lock.obedienceSuccessCount || 0 }}
            · 失败 {{ lock.obedienceFailCount || 0 }}
            <template v-if="lock.obedienceChallengeDueAt"> · 挑战进行中</template>
          </p>
          <p v-if="obedienceFeedback" class="ok">{{ obedienceFeedback }}</p>
          <button
            class="btn"
            :disabled="busy || (obEnabled && obPhrase.trim().length < 2)"
            @click="saveObedience"
          >
            {{ busy && savingKind === 'obedience' ? '保存中…' : '保存服从设置' }}
          </button>
        </section>

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

        <div v-if="openTasks.length" class="card stack">
          <p class="muted">进行中任务</p>
          <div v-for="t in openTasks" :key="t.id">{{ t.title }}</div>
        </div>

        <EventHistory :events="events" />

        <button class="btn warn" :disabled="busy" @click="doUnlock">钥匙开锁</button>
      </div>
    </template>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import * as api from '../api/lock'
import EventHistory from '../components/EventHistory.vue'
import { useAuthStore } from '../stores/auth'
import { formatDuration, remainingMs } from '../utils/time'

const route = useRoute()
const auth = useAuthStore()
const code = computed(() => route.params.code)
const lock = ref(null)
const tasks = ref([])
const events = ref([])
const loading = ref(true)
const busy = ref(false)
const savingKind = ref('')
const error = ref('')
const banner = ref('')
const phraseFeedback = ref('')
const obedienceFeedback = ref('')
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

/** Prevent 4s poll from wiping in-progress edits. */
const formHydrated = ref(false)

const remain = computed(() => remainingMs(lock.value, now.value))
const openTasks = computed(() => tasks.value.filter((t) => t.status === 'open'))

let poll
let tick
let bannerTimer

function flashBanner(text) {
  banner.value = text
  clearTimeout(bannerTimer)
  bannerTimer = setTimeout(() => {
    if (banner.value === text) banner.value = ''
  }, 4000)
}

function applyFormFromLock(remote) {
  editPhrase.value = remote.endPhrase || ''
  editMaxFails.value = remote.phraseMaxFails || 3
  editPenaltyHours.value = Math.max(0.1, (remote.phraseFailPenaltyMs || 3600000) / 3600000)
  obEnabled.value = !!remote.obedienceEnabled
  obIntervalMin.value = Math.max(1, Math.round((remote.obedienceIntervalMs || 1800000) / 60000))
  obPhrase.value = remote.obediencePhrase || '服从主人'
  obTimeoutSec.value = Math.max(30, Math.round((remote.obedienceTimeoutMs || 120000) / 1000))
  obPenaltyHours.value = Math.max(0.1, (remote.obediencePenaltyMs || 3600000) / 3600000)
  formHydrated.value = true
}

async function load({ syncForm = false } = {}) {
  try {
    const remote = await api.getByKeyholder(code.value)
    lock.value = remote?.id ? remote : null
    if (lock.value) {
      tasks.value = await api.listTasks(code.value, 'keyholder')
      events.value = await api.listEvents(code.value, 'keyholder')
      if (syncForm || !formHydrated.value) applyFormFromLock(lock.value)
    } else {
      events.value = []
    }
    error.value = ''
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await load({ syncForm: true })
  if (auth.isLoggedIn) {
    try {
      await api.claimKeyholder(code.value)
      flashBanner('已绑定到当前账号')
      await load({ syncForm: true })
    } catch (e) {
      if (String(e.message).includes('其他')) error.value = e.message
    }
  }
  // Live countdown / photo / tasks — do NOT overwrite settings inputs.
  poll = setInterval(() => load({ syncForm: false }), 4000)
  tick = setInterval(() => (now.value = Date.now()), 250)
})
onUnmounted(() => {
  clearInterval(poll)
  clearInterval(tick)
  clearTimeout(bannerTimer)
})

async function run(fn, { ok, kind } = {}) {
  busy.value = true
  savingKind.value = kind || ''
  error.value = ''
  try {
    const next = await fn()
    if (next?.status === 'active') lock.value = next
    else if (next?.lock) lock.value = next.lock.status === 'active' ? next.lock : null
    else if (kind) {
      /* settings save always returns LockView */
    } else {
      lock.value = null
    }
    if (ok) flashBanner(ok)
    await load({ syncForm: Boolean(kind) })
    return next
  } catch (e) {
    error.value = e.message
    throw e
  } finally {
    busy.value = false
    savingKind.value = ''
  }
}

const add = (ms) => run(() => api.addTime({ token: code.value, ms }), { ok: '已加时' })
const sub = (ms) => run(() => api.subTime({ token: code.value, ms }), { ok: '已减时' })
const toggleFreeze = () =>
  run(() => api.setFreeze({ token: code.value, frozen: !lock.value.frozenAt }), {
    ok: '已更新冻结',
  })
const forceHygiene = () =>
  run(() => api.hygieneStart({ token: code.value, role: 'keyholder' }), { ok: '已强制清洁' })
const reqPhoto = () => run(() => api.photoRequest({ token: code.value }), { ok: '已请求拍照' })

async function savePhrase() {
  phraseFeedback.value = ''
  try {
    await run(
      () =>
        api.setEndPhrase({
          token: code.value,
          endPhrase: editPhrase.value.trim(),
          phraseMaxFails: Number(editMaxFails.value) || 3,
          phraseFailPenaltyMs: Math.round(Number(editPenaltyHours.value) * 3600_000),
        }),
      { ok: '结束宣言设置已保存', kind: 'phrase' },
    )
    phraseFeedback.value = `已保存：${editPhrase.value.trim().length} 字 · 错 ${editMaxFails.value} 次加罚 ${editPenaltyHours.value} 小时`
  } catch {
    /* error already set */
  }
}

async function saveObedience() {
  obedienceFeedback.value = ''
  try {
    await run(
      () =>
        api.setObedience({
          token: code.value,
          enabled: !!obEnabled.value,
          intervalMs: Math.max(1, Number(obIntervalMin.value) || 30) * 60_000,
          phrase: obPhrase.value.trim(),
          timeoutMs: Math.max(30, Number(obTimeoutSec.value) || 120) * 1000,
          penaltyMs: Math.round(Number(obPenaltyHours.value) * 3600_000),
        }),
      { ok: obEnabled.value ? '服从设置已保存并开启' : '服从已关闭', kind: 'obedience' },
    )
    obedienceFeedback.value = obEnabled.value
      ? `已开启：每 ${obIntervalMin.value} 分钟 · 时限 ${obTimeoutSec.value} 秒 · 超时 +${obPenaltyHours.value} 小时`
      : '已关闭定期服从'
  } catch {
    /* error already set */
  }
}

const newTask = () =>
  run(
    () =>
      api.createTask({
        token: code.value,
        title: taskTitle.value,
        rewardType: rewardType.value,
        rewardMs: 30 * 60_000,
      }),
    { ok: '任务已发布' },
  ).then(() => {
    taskTitle.value = ''
  })

const doUnlock = () => {
  if (!confirm('确认钥匙开锁？')) return
  run(() => api.unlock({ token: code.value, mode: 'keyholder' }), { ok: '已开锁' })
}
</script>

<style scoped>
.settings-title {
  margin: 0;
  font-family: Fraunces, Georgia, serif;
  font-size: 1.15rem;
  letter-spacing: -0.02em;
}
.settings-block {
  gap: 0.85rem;
}
.ok {
  color: #8fbf8f;
  font-size: 0.875rem;
  margin: 0;
}
textarea {
  width: 100%;
  border: 0;
  border-radius: 0.65rem;
  background: var(--bg);
  color: var(--fg);
  padding: 0.7rem 0.85rem;
  box-shadow: var(--shadow-border, var(--border));
  outline: none;
  font: inherit;
  resize: vertical;
  min-height: 4.5rem;
}
fieldset:disabled {
  opacity: 0.45;
}
</style>
