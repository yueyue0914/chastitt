<template>
  <section class="stack" style="margin-top:1.5rem">
    <label class="field">
      锁定时长（小时）
      <input v-model.number="hours" type="number" min="0.02" step="0.5" />
    </label>

    <label class="check"><input v-model="allowEmergency" type="checkbox" /> 允许紧急解锁</label>
    <label v-if="allowEmergency" class="field">
      紧急限制
      <select v-model="emergencyLimitMode">
        <option value="cooldown_24h">24 小时冷却</option>
        <option value="once_penalty">仅一次（永久惩罚记录）</option>
        <option value="unlimited">不限制</option>
      </select>
    </label>

    <label class="check"><input v-model="allowHygiene" type="checkbox" /> 允许卫生清洁</label>
    <template v-if="allowHygiene">
      <label class="field">
        最长清洁时间（分钟）
        <input v-model.number="hygieneMaxMin" type="number" min="1" max="120" />
      </label>
      <label class="field">
        超时惩罚
        <select v-model="hygienePenaltyMode">
          <option value="multiplier">超时 × 倍数</option>
          <option value="fixed">固定加时</option>
        </select>
      </label>
    </template>

    <label class="field">
      结束宣言（到期/紧急须完整输入，一字不差）
      <input v-model="endPhrase" maxlength="200" autocomplete="off" />
    </label>
    <label class="field">
      宣言输错几次后加罚
      <input v-model.number="phraseMaxFails" type="number" min="1" max="20" />
    </label>
    <label class="field">
      每次触发加罚（小时）
      <input v-model.number="phraseFailPenaltyHours" type="number" min="0.1" step="0.5" />
    </label>

    <label class="check"><input v-model="obedienceEnabled" type="checkbox" /> 定期服从确认</label>
    <template v-if="obedienceEnabled">
      <label class="field">
        服从间隔（分钟）
        <input v-model.number="obedienceIntervalMin" type="number" min="1" max="1440" />
      </label>
      <label class="field">
        服从短句
        <input v-model="obediencePhrase" maxlength="80" />
      </label>
      <label class="field">
        应答时限（秒）
        <input v-model.number="obedienceTimeoutSec" type="number" min="30" max="1800" />
      </label>
      <label class="field">
        超时加罚（小时）
        <input v-model.number="obediencePenaltyHours" type="number" min="0.1" step="0.5" />
      </label>
    </template>
    <label class="check"><input v-model="notifyExpiry" type="checkbox" /> 到期本地通知</label>

    <p v-if="error" class="err">{{ error }}</p>
    <button class="btn" :disabled="busy" @click="submit">{{ busy ? '创建中…' : '开始锁定' }}</button>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useLockStore } from '../stores/lock'
import { hoursToMs } from '../utils/time'

const store = useLockStore()
const emit = defineEmits(['created'])

const hours = ref(6)
const allowEmergency = ref(true)
const emergencyLimitMode = ref('cooldown_24h')
const allowHygiene = ref(false)
const hygieneMaxMin = ref(15)
const hygienePenaltyMode = ref('multiplier')
const endPhrase = ref('我是主人的无面锁屌latex性偶')
const phraseMaxFails = ref(3)
const phraseFailPenaltyHours = ref(1)
const obedienceEnabled = ref(true)
const obedienceIntervalMin = ref(30)
const obediencePhrase = ref('服从主人')
const obedienceTimeoutSec = ref(120)
const obediencePenaltyHours = ref(1)
const notifyExpiry = ref(true)
const busy = ref(false)
const error = ref('')

async function submit() {
  busy.value = true
  error.value = ''
  try {
    const durationMs = hoursToMs(hours.value)
    await store.create({
      durationMs,
      allowEmergency: allowEmergency.value,
      emergencyLimitMode: emergencyLimitMode.value,
      emergencyPenaltyMs: 24 * 3600_000,
      allowHygiene: allowHygiene.value,
      hygieneMaxMs: hygieneMaxMin.value * 60_000,
      hygienePenaltyMode: hygienePenaltyMode.value,
      hygienePenaltyFixedMs: 3600_000,
      hygienePenaltyMultiplier: 2,
      endPhrase: endPhrase.value,
      phraseMaxFails: phraseMaxFails.value,
      phraseFailPenaltyMs: hoursToMs(phraseFailPenaltyHours.value),
      notifyExpiry: notifyExpiry.value,
      minLockMs: durationMs,
      obedienceEnabled: obedienceEnabled.value,
      obedienceIntervalMs: obedienceIntervalMin.value * 60_000,
      obediencePhrase: obediencePhrase.value,
      obedienceTimeoutMs: obedienceTimeoutSec.value * 1000,
      obediencePenaltyMs: hoursToMs(obediencePenaltyHours.value),
    })
    emit('created')
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
</script>
