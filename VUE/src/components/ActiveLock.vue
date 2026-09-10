<template>
  <section class="stack" style="margin-top:1.5rem">
    <div class="countdown">{{ formatDuration(remain) }}</div>
    <p class="muted" style="text-align:center">
      {{ lock.frozenAt ? '已冻结 · 倒计时暂停' : '解锁时间 ' + new Date(lock.endsAt).toLocaleString() }}
    </p>

    <div class="card stack">
      <p class="muted">钥匙链接（发给管理者）</p>
      <input :value="keyUrl" readonly @focus="$event.target.select()" />
      <button class="btn secondary" type="button" @click="copy">复制链接</button>
    </div>

    <div v-if="lock.allowHygiene" class="row">
      <button
        v-if="!lock.hygieneStartedAt"
        class="btn secondary"
        :disabled="busy || lock.frozenAt"
        @click="startHygiene"
      >
        开始清洁
      </button>
      <button v-else class="btn secondary" :disabled="busy" @click="endHygiene">结束清洁</button>
    </div>

    <button
      v-if="lock.photoRequestActive"
      class="btn secondary"
      :disabled="busy"
      @click="submitPhoto"
    >
      提交拍照验证
    </button>

    <div v-if="openTasks.length" class="card stack">
      <p class="muted">待完成任务</p>
      <div v-for="t in openTasks" :key="t.id" class="row" style="align-items:center">
        <span style="flex:2">{{ t.title }}</span>
        <button class="btn secondary" style="flex:1" :disabled="busy" @click="doTask(t.id)">完成</button>
      </div>
    </div>

    <button
      v-if="canEnd"
      class="btn"
      :disabled="busy"
      @click="openEnd = true"
    >
      输入宣言结束
    </button>
    <button
      v-if="lock.allowEmergency"
      class="btn warn"
      :disabled="busy"
      @click="openEmergency = true"
    >
      紧急解锁
    </button>

    <div v-if="events.length" class="card stack">
      <p class="muted">历史</p>
      <div v-for="e in events" :key="e.id" class="muted" style="font-size:0.8rem">
        {{ e.kind }} · {{ e.detail }}
      </div>
    </div>

    <div v-if="openEnd || openEmergency" class="modal" @click.self="closeModals">
      <div class="panel stack">
        <h2 class="font-display" style="margin:0;font-size:1.4rem">
          {{ openEmergency ? '紧急解锁' : '结束锁定' }}
        </h2>
        <p class="muted">请完整输入结束宣言</p>
        <input v-model="phrase" />
        <p v-if="localErr" class="err">{{ localErr }}</p>
        <button class="btn" :disabled="busy" @click="confirmUnlock">确认</button>
        <button class="btn secondary" @click="closeModals">取消</button>
      </div>
    </div>

    <div v-if="obedienceOpen" class="modal">
      <div class="panel stack">
        <h2 class="font-display" style="margin:0;font-size:1.4rem">服从确认</h2>
        <p class="muted">请输入：{{ lock.obediencePhrase }}</p>
        <input v-model="obedienceInput" />
        <button class="btn" @click="confirmObedience">确认</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import * as api from '../api/lock'
import { canWearerEnd, formatDuration, remainingMs } from '../utils/time'

const props = defineProps({ lock: Object, now: Number })
const emit = defineEmits(['changed'])

const busy = ref(false)
const localErr = ref('')
const phrase = ref('')
const openEnd = ref(false)
const openEmergency = ref(false)
const events = ref([])
const tasks = ref([])
const obedienceOpen = ref(false)
const obedienceInput = ref('')
const keyUrl = computed(() => `${location.origin}/key/${props.lock.keyholderToken}`)
const remain = computed(() => remainingMs(props.lock, props.now))
const canEnd = computed(() => canWearerEnd(props.lock, props.now))
const openTasks = computed(() => tasks.value.filter((t) => t.status === 'open'))

let obedienceTimer

async function loadMeta() {
  events.value = await api.listEvents(props.lock.wearerToken, 'wearer')
  tasks.value = await api.listTasks(props.lock.wearerToken, 'wearer')
}

onMounted(() => {
  loadMeta().catch(() => {})
  if (props.lock.obedienceEnabled) {
    obedienceTimer = setInterval(() => {
      const key = `yue-ob:${props.lock.id}`
      const last = Number(localStorage.getItem(key) || 0)
      if (Date.now() - last >= (props.lock.obedienceIntervalMs || 1800000)) {
        obedienceOpen.value = true
      }
    }, 5000)
  }
})
onUnmounted(() => clearInterval(obedienceTimer))
watch(() => props.lock.id, () => loadMeta().catch(() => {}))

function closeModals() {
  openEnd.value = false
  openEmergency.value = false
  phrase.value = ''
  localErr.value = ''
}

async function run(fn) {
  busy.value = true
  localErr.value = ''
  try {
    const next = await fn()
    emit('changed', next)
    await loadMeta()
    return next
  } catch (e) {
    localErr.value = e.message
    throw e
  } finally {
    busy.value = false
  }
}

function copy() {
  navigator.clipboard?.writeText(keyUrl.value)
}

async function startHygiene() {
  await run(() => api.hygieneStart({ token: props.lock.wearerToken, role: 'wearer' }))
}
async function endHygiene() {
  await run(() => api.hygieneEnd({ token: props.lock.wearerToken, role: 'wearer' }))
}
async function doTask(taskId) {
  const res = await run(() =>
    api.completeTask({ token: props.lock.wearerToken, role: 'wearer', taskId }),
  )
  emit('changed', res.lock)
}
async function confirmUnlock() {
  await run(() =>
    api.unlock({
      token: props.lock.wearerToken,
      mode: openEmergency.value ? 'emergency' : 'expiry',
      phrase: phrase.value,
    }),
  )
  closeModals()
}
async function submitPhoto() {
  const thumbDataUrl =
    'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=='
  await run(() => api.photoSubmit({ token: props.lock.wearerToken, thumbDataUrl }))
}
function confirmObedience() {
  if (obedienceInput.value.trim() !== props.lock.obediencePhrase) return
  localStorage.setItem(`yue-ob:${props.lock.id}`, String(Date.now()))
  obedienceOpen.value = false
  obedienceInput.value = ''
}
</script>
