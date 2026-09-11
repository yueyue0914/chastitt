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
      :disabled="busy || obedience.required || obedienceBootstrapping"
      @click="openEnd = true"
    >
      输入宣言结束
    </button>
    <button
      v-if="lock.allowEmergency"
      class="btn warn"
      :disabled="busy || obedience.required || obedienceBootstrapping"
      @click="openEmergency = true"
    >
      紧急解锁
    </button>

    <EventHistory :events="events" />

    <div v-if="openEnd || openEmergency" class="modal">
      <div class="panel stack">
        <h2 class="font-display" style="margin:0;font-size:1.4rem">
          {{ openEmergency ? '紧急解锁' : '结束锁定' }}
        </h2>
        <p class="muted">
          请完整输入结束宣言（须一字不差，共 {{ phraseLen }} 字）。
          当前输错 {{ lock.phraseFailCount || 0 }}/{{ lock.phraseMaxFails || 3 }}，
          满额将加罚 {{ formatDuration(lock.phraseFailPenaltyMs || 3600000) }}。
        </p>
        <input
          v-model="phrase"
          autocomplete="off"
          autocorrect="off"
          spellcheck="false"
          placeholder="在此完整输入宣言…"
          @paste.prevent
        />
        <p v-if="localErr" class="err">{{ localErr }}</p>
        <button class="btn" :disabled="busy || !phrase.trim()" @click="confirmUnlock">确认</button>
        <button class="btn secondary" :disabled="busy" @click="closeModals">取消</button>
      </div>
    </div>

    <!-- Forced obedience: no dismiss, no cancel, blocks entire viewport -->
    <Teleport to="body">
      <div
        v-if="obedience.required || obedienceBootstrapping"
        class="obedience-force"
        @keydown.esc.prevent
        @keydown.tab.prevent
      >
        <div class="obedience-force__panel stack">
          <p class="eyebrow" style="text-align:center">Obedience</p>
          <h2 class="font-display" style="margin:0;font-size:1.75rem;text-align:center">服从确认</h2>
          <template v-if="obedienceBootstrapping && !obedience.required">
            <p class="muted" style="text-align:center">正在同步服从状态…</p>
          </template>
          <template v-else>
            <p class="muted" style="text-align:center">
              必须完整输入短句才能继续。剩余
              <strong style="color:var(--fg)">{{ formatDuration(obedienceRemain) }}</strong>
              ，超时将加罚 {{ formatDuration(obedience.penaltyMs || 3600000) }}。
            </p>
            <p v-if="obedienceNotice" class="err" style="text-align:center">{{ obedienceNotice }}</p>
            <p style="text-align:center;font-size:1.15rem;letter-spacing:0.04em">
              {{ obedience.phrase }}
            </p>
            <input
              ref="obedienceInputEl"
              v-model="obedienceInput"
              autocomplete="off"
              autocorrect="off"
              spellcheck="false"
              placeholder="在此输入短句…"
              @paste.prevent
              @keydown.enter.prevent="submitObedience"
            />
            <p v-if="obedienceErr" class="err" style="text-align:center">{{ obedienceErr }}</p>
            <button
              class="btn"
              :disabled="obedienceBusy || !obedienceInput.trim()"
              @click="submitObedience"
            >
              {{ obedienceBusy ? '提交中…' : '确认服从' }}
            </button>
            <p class="muted" style="text-align:center;font-size:0.75rem">
              成功 {{ obedience.successCount || 0 }} · 失败 {{ obedience.failCount || 0 }}
            </p>
          </template>
        </div>
      </div>
    </Teleport>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as api from '../api/lock'
import EventHistory from './EventHistory.vue'
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
const obedience = ref({ required: false })
const obedienceInput = ref('')
const obedienceErr = ref('')
const obedienceNotice = ref('')
const obedienceBusy = ref(false)
const obedienceBootstrapping = ref(false)
const obedienceInputEl = ref(null)
const keyUrl = computed(() => `${location.origin}/key/${props.lock.keyholderToken}`)
const remain = computed(() => remainingMs(props.lock, props.now))
const canEnd = computed(() => canWearerEnd(props.lock, props.now))
const openTasks = computed(() => tasks.value.filter((t) => t.status === 'open'))
const phraseLen = computed(
  () => props.lock.endPhraseLength || (props.lock.endPhrase || '').length,
)
const obedienceRemain = computed(() => {
  if (!obedience.value?.required || !obedience.value.dueAt) return 0
  return Math.max(0, obedience.value.dueAt - props.now)
})

let pollObTimer

async function loadMeta() {
  events.value = await api.listEvents(props.lock.wearerToken, 'wearer')
  tasks.value = await api.listTasks(props.lock.wearerToken, 'wearer')
}

async function tickObedience() {
  if (!props.lock?.wearerToken || props.lock.status !== 'active') return
  try {
    const status = await api.pollObedience(props.lock.wearerToken)
    const wasRequired = obedience.value.required
    obedience.value = status
    obedienceBootstrapping.value = false
    if (status.lock?.id) emit('changed', status.lock)
    if (status.justPenalized) {
      obedienceNotice.value = `服从超时，已加罚 ${formatDuration(status.justPenalizedMs || status.penaltyMs || 0)}`
      await loadMeta()
    }
    if (status.required && !wasRequired) {
      obedienceInput.value = ''
      obedienceErr.value = ''
      if (!status.justPenalized) obedienceNotice.value = ''
      await nextTick()
      obedienceInputEl.value?.focus()
    }
    if (!status.required && wasRequired) {
      await loadMeta()
    }
  } catch {
    /* keep last status; leave bootstrap overlay if still syncing */
  }
}

function onKeydown(e) {
  if (!obedience.value.required && !obedienceBootstrapping.value) return
  if (e.key === 'Escape' || e.key === 'F5') {
    e.preventDefault()
    e.stopPropagation()
  }
}

function onVisibility() {
  if (document.visibilityState === 'visible') {
    tickObedience()
  }
}

function onPageShow() {
  tickObedience()
}

function onBeforeUnload(e) {
  if (!obedience.value.required) return
  e.preventDefault()
  e.returnValue = '当前有未完成的服从确认，离开可能导致超时加罚'
}

onMounted(async () => {
  // Refresh / reopen: if server already has an open challenge, block UI immediately.
  if (props.lock.obedienceEnabled && props.lock.obedienceChallengeDueAt) {
    obedienceBootstrapping.value = true
  }
  loadMeta().catch(() => {})
  await tickObedience()
  pollObTimer = setInterval(tickObedience, 2000)
  window.addEventListener('keydown', onKeydown, true)
  document.addEventListener('visibilitychange', onVisibility)
  window.addEventListener('pageshow', onPageShow)
  window.addEventListener('focus', onPageShow)
  window.addEventListener('beforeunload', onBeforeUnload)
})
onUnmounted(() => {
  clearInterval(pollObTimer)
  window.removeEventListener('keydown', onKeydown, true)
  document.removeEventListener('visibilitychange', onVisibility)
  window.removeEventListener('pageshow', onPageShow)
  window.removeEventListener('focus', onPageShow)
  window.removeEventListener('beforeunload', onBeforeUnload)
})
watch(() => props.lock.id, async () => {
  if (props.lock.obedienceEnabled && props.lock.obedienceChallengeDueAt) {
    obedienceBootstrapping.value = true
  }
  loadMeta().catch(() => {})
  await tickObedience()
})

function closeModals() {
  if (obedience.value.required) return
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
  busy.value = true
  localErr.value = ''
  try {
    const next = await api.unlock({
      token: props.lock.wearerToken,
      mode: openEmergency.value ? 'emergency' : 'expiry',
      phrase: phrase.value,
    })
    emit('changed', next)
    await loadMeta()
    closeModals()
  } catch (e) {
    localErr.value = e.message
    try {
      const remote = await api.getByWearer(props.lock.wearerToken)
      if (remote?.id) {
        emit('changed', remote)
        await loadMeta()
      }
    } catch {
      /* ignore */
    }
  } finally {
    busy.value = false
  }
}
async function submitPhoto() {
  const thumbDataUrl =
    'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=='
  await run(() => api.photoSubmit({ token: props.lock.wearerToken, thumbDataUrl }))
}

async function submitObedience() {
  if (!obedience.value.required || obedienceBusy.value) return
  obedienceBusy.value = true
  obedienceErr.value = ''
  try {
    const status = await api.completeObedience({
      token: props.lock.wearerToken,
      phrase: obedienceInput.value,
    })
    obedience.value = status
    if (status.lock?.id) emit('changed', status.lock)
    obedienceInput.value = ''
    await loadMeta()
  } catch (e) {
    obedienceErr.value = e.message
    await tickObedience()
  } finally {
    obedienceBusy.value = false
  }
}
</script>
