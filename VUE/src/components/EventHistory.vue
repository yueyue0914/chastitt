<template>
  <section class="card stack event-history">
    <div class="event-history__head">
      <p class="muted" style="margin:0">操作历史</p>
      <span v-if="events.length" class="muted" style="font-size:0.75rem">{{ events.length }} 条</span>
    </div>

    <p v-if="!events.length" class="muted" style="margin:0;font-size:0.85rem">暂无记录</p>

    <ul v-else class="event-list">
      <li
        v-for="e in events"
        :key="e.id"
        class="event-item"
        :class="toneClass(e.kind)"
      >
        <div class="event-item__top">
          <span class="event-badge">{{ meta(e.kind).label }}</span>
          <time class="event-time">{{ formatTime(e.createdAt) }}</time>
        </div>
        <p v-if="e.detail" class="event-detail">{{ e.detail }}</p>
        <p v-if="e.amountMs > 0" class="event-penalty">
          {{ meta(e.kind).penaltyPrefix || '加罚' }}
          <strong>{{ formatDuration(e.amountMs) }}</strong>
        </p>
      </li>
    </ul>
  </section>
</template>

<script setup>
import { formatDuration } from '../utils/time'

defineProps({
  events: { type: Array, default: () => [] },
})

const EVENT_META = {
  started: { label: '开始锁定', tone: 'neutral' },
  ended: { label: '到期结束', tone: 'ok' },
  emergency: { label: '紧急解锁', tone: 'warn' },
  emergency_penalty: { label: '紧急永久惩罚', tone: 'danger', penaltyPrefix: '永久记录' },
  phrase_fail: { label: '宣言输错', tone: 'warn' },
  phrase_fail_penalty: { label: '宣言输错加罚', tone: 'danger', penaltyPrefix: '加罚' },
  phrase_set: { label: '更新结束宣言', tone: 'info' },
  hygiene_penalty: { label: '清洁超时惩罚', tone: 'danger', penaltyPrefix: '加罚' },
  force_hygiene: { label: '强制清洁', tone: 'info' },
  keyholder_unlock: { label: '钥匙开锁', tone: 'ok' },
  keyholder_add_time: { label: '钥匙加时', tone: 'warn', penaltyPrefix: '加时' },
  keyholder_sub_time: { label: '钥匙减时', tone: 'ok', penaltyPrefix: '减时' },
  keyholder_claim: { label: '钥匙认领', tone: 'info' },
  freeze: { label: '冻结', tone: 'info' },
  unfreeze: { label: '解冻', tone: 'info', penaltyPrefix: '补偿' },
  min_lock_set: { label: '最低锁定', tone: 'info' },
  photo_request: { label: '要求拍照', tone: 'info' },
  photo_submit: { label: '提交拍照', tone: 'ok' },
  task_created: { label: '发布任务', tone: 'info' },
  task_done: { label: '完成任务', tone: 'ok', penaltyPrefix: '奖励' },
  integrity_penalty: { label: '防作弊惩罚', tone: 'danger', penaltyPrefix: '加罚' },
  obedience_open: { label: '服从开始', tone: 'info' },
  obedience_success: { label: '服从成功', tone: 'ok' },
  obedience_fail: { label: '服从失败', tone: 'danger', penaltyPrefix: '加罚' },
  obedience_set: { label: '更新服从规则', tone: 'info' },
}

function meta(kind) {
  return EVENT_META[kind] || { label: kind || '事件', tone: 'neutral' }
}

function toneClass(kind) {
  return `tone-${meta(kind).tone}`
}

function formatTime(ts) {
  if (!ts) return ''
  try {
    return new Date(ts).toLocaleString()
  } catch {
    return ''
  }
}
</script>

<style scoped>
.event-history__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.75rem;
}
.event-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.55rem;
}
.event-item {
  border-radius: 0.75rem;
  padding: 0.7rem 0.8rem;
  background: rgba(255, 255, 255, 0.03);
  box-shadow: inset 0 0 0 1px rgba(244, 239, 230, 0.08);
}
.event-item__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}
.event-badge {
  display: inline-flex;
  align-items: center;
  font-size: 0.78rem;
  font-weight: 600;
  letter-spacing: 0.02em;
}
.event-time {
  font-size: 0.72rem;
  color: var(--muted);
  white-space: nowrap;
}
.event-detail {
  margin: 0.35rem 0 0;
  font-size: 0.8rem;
  color: var(--muted);
  line-height: 1.4;
}
.event-penalty {
  margin: 0.4rem 0 0;
  font-size: 0.85rem;
  font-weight: 500;
}
.event-penalty strong {
  font-family: Fraunces, Georgia, serif;
  font-size: 1rem;
  margin-left: 0.25rem;
}

.tone-danger {
  background: rgba(224, 122, 95, 0.14);
  box-shadow: inset 0 0 0 1px rgba(224, 122, 95, 0.35);
}
.tone-danger .event-badge,
.tone-danger .event-penalty {
  color: #f0a090;
}
.tone-warn {
  background: rgba(196, 165, 116, 0.12);
  box-shadow: inset 0 0 0 1px rgba(196, 165, 116, 0.28);
}
.tone-warn .event-badge,
.tone-warn .event-penalty {
  color: #e0c08a;
}
.tone-ok {
  background: rgba(120, 170, 130, 0.1);
  box-shadow: inset 0 0 0 1px rgba(120, 170, 130, 0.25);
}
.tone-ok .event-badge,
.tone-ok .event-penalty {
  color: #9dcca5;
}
.tone-info .event-badge {
  color: var(--fg);
}
.tone-neutral .event-badge {
  color: var(--muted);
}
</style>
