<template>
  <div>
    <h3 class="page-title">RCA 根因辅助分析</h3>

    <el-card style="margin-bottom:14px">
      <el-form :inline="true">
        <el-form-item label="选择事件">
          <el-select v-model="eventId" filterable placeholder="选择要分析的不良事件" style="width:420px" @change="loadAll">
            <el-option
              v-for="e in events"
              :key="e.eventId"
              :label="`${e.eventNo} | ${e.eventTypeName || e.eventType} | ${e.deptName}`"
              :value="e.eventId" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button :disabled="!eventId" @click="openAssist">辅助分析建议</el-button>
        </el-form-item>
        <el-form-item v-if="!canAnalyze">
          <el-tag type="info">只读：仅护士长/管理员可编辑分析</el-tag>
        </el-form-item>
      </el-form>
      <el-alert v-if="selectedEvent" type="info" :closable="false" show-icon>
        <template #title>
          {{ selectedEvent.eventTypeName }} · {{ selectedEvent.deptName }} · 损伤 {{ selectedEvent.injuryLevelName }} ·
          发生 {{ selectedEvent.occurTime }}
        </template>
        <div>{{ selectedEvent.eventDesc }}</div>
      </el-alert>
    </el-card>

    <el-empty v-if="!eventId" description="请先选择一个事件开始根因分析" />

    <el-row v-else :gutter="14">
      <!-- 左：五维诱因 -->
      <el-col :span="13">
        <el-card>
          <template #header>
            <div class="card-head">
              <span>五维诱因采集（人 · 机 · 料 · 法 · 环）</span>
              <el-button v-if="canAnalyze" type="primary" size="small" :loading="savingFactors" @click="saveFactorsData">
                保存诱因
              </el-button>
            </div>
          </template>

          <div v-for="dim in dimensions" :key="dim" class="dim-block">
            <div class="dim-title">
              <el-tag effect="dark" size="small">{{ dim }}</el-tag>
              <span class="dim-count">已填 {{ factorsByDim[dim].length }} 条</span>
            </div>
            <div v-for="(f, idx) in factorsByDim[dim]" :key="idx" class="factor-row">
              <el-select v-model="f.factorType" size="small" style="width:100px" :disabled="!canAnalyze">
                <el-option label="近端诱因" value="PROXIMAL" />
                <el-option label="根本原因" value="ROOT" />
              </el-select>
              <el-input v-model="f.factorContent" size="small" placeholder="诱因描述" :disabled="!canAnalyze" />
              <el-button v-if="canAnalyze" text type="danger" size="small" @click="removeFactor(dim, idx)">删</el-button>
            </div>
            <el-button v-if="canAnalyze" text type="primary" size="small" @click="addFactor(dim)">+ 添加{{ dim }}维度诱因</el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 右：根因与改进 -->
      <el-col :span="11">
        <el-card>
          <template #header>
            <div class="card-head">
              <span>根因与改进措施</span>
              <el-button v-if="canAnalyze" type="primary" size="small" :loading="savingRca" @click="saveRcaData">保存根因</el-button>
            </div>
          </template>
          <el-form label-position="top" size="small">
            <el-form-item label="时间序列表（还原事件经过）">
              <el-input v-model="rca.timeline" type="textarea" :rows="3" :disabled="!canAnalyze"
                placeholder="按时间顺序还原事件发生、发现、处置经过" />
            </el-form-item>
            <el-form-item v-for="m in dimRootMap" :key="m.field" :label="`${m.dim} · 根本原因`">
              <el-input v-model="rca[m.field]" type="textarea" :rows="2" :disabled="!canAnalyze" />
            </el-form-item>
            <el-form-item label="改进措施">
              <el-input v-model="rca.improvement" type="textarea" :rows="3" :disabled="!canAnalyze"
                placeholder="针对根本原因制定的改进/预防措施" />
            </el-form-item>
            <el-form-item label="分析状态">
              <el-select v-model="rca.analysisStatus" :disabled="!canAnalyze" style="width:100%">
                <el-option label="未开始" value="NOT_STARTED" />
                <el-option label="分析中" value="ANALYZING" />
                <el-option label="已完成" value="DONE" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <!-- 辅助分析抽屉 -->
    <el-drawer v-model="assistVisible" title="辅助分析建议" size="46%">
      <div v-if="assist" style="padding:0 6px">
        <div class="assist-head">
          <span>分析完成度（已填维度占比）</span>
          <el-progress :percentage="assist.completeness" :stroke-width="14" text-inside style="flex:1;margin-left:12px" />
        </div>
        <el-divider />
        <div v-for="dim in dimensions" :key="dim" class="assist-dim">
          <h4>{{ dim }} 维度建议</h4>
          <el-checkbox-group v-model="assistChecked[dim]">
            <el-checkbox v-for="s in (assist.suggestions[dim] || [])" :key="s" :label="s" :value="s" border class="assist-cb">
              {{ s }}
            </el-checkbox>
          </el-checkbox-group>
        </div>
        <div style="margin-top:16px">
          <el-button type="primary" :disabled="!canAnalyze" @click="applyAssist">将选中建议加入诱因</el-button>
        </div>
      </div>
      <el-empty v-else description="暂无建议" />
    </el-drawer>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listEvents } from '../api/event'
import { getFactors, saveFactors, getRca, saveRca, getAssist } from '../api/rca'
import { auth } from '../store/auth'

const dimensions = ['人', '机', '料', '法', '环']
const dimRootMap = [
  { dim: '人', field: 'rootHuman' },
  { dim: '机', field: 'rootMachine' },
  { dim: '料', field: 'rootMaterial' },
  { dim: '法', field: 'rootMethod' },
  { dim: '环', field: 'rootEnv' }
]

const route = useRoute()
const user = auth.getUser()
const canAnalyze = computed(() => user && (user.role === 'HEAD_NURSE' || user.role === 'ADMIN'))

const events = ref([])
const eventId = ref(null)
const selectedEvent = computed(() => events.value.find((e) => e.eventId === eventId.value) || null)

const factorsByDim = reactive({ 人: [], 机: [], 料: [], 法: [], 环: [] })
const rca = reactive({
  timeline: '',
  rootHuman: '',
  rootMachine: '',
  rootMaterial: '',
  rootMethod: '',
  rootEnv: '',
  improvement: '',
  analysisStatus: 'NOT_STARTED'
})

const assist = ref(null)
const assistVisible = ref(false)
const assistChecked = reactive({ 人: [], 机: [], 料: [], 法: [], 环: [] })

const savingFactors = ref(false)
const savingRca = ref(false)

onMounted(async () => {
  const res = await listEvents({ page: 1, size: 200 })
  events.value = res.data.records || []
  const qid = Number(route.query.eventId)
  if (qid && events.value.some((e) => e.eventId === qid)) {
    eventId.value = qid
    await loadAll()
  }
})

function resetFactors() {
  dimensions.forEach((d) => {
    factorsByDim[d] = []
  })
}

async function loadAll() {
  if (!eventId.value) return
  resetFactors()
  Object.assign(rca, {
    timeline: '', rootHuman: '', rootMachine: '', rootMaterial: '', rootMethod: '',
    rootEnv: '', improvement: '', analysisStatus: 'NOT_STARTED'
  })
  assist.value = null

  const [fRes, rRes] = await Promise.all([getFactors(eventId.value), getRca(eventId.value)])
  ;(fRes.data || []).forEach((f) => {
    if (!factorsByDim[f.dimension]) factorsByDim[f.dimension] = []
    factorsByDim[f.dimension].push({
      factorType: f.factorType || 'PROXIMAL',
      factorContent: f.factorContent
    })
  })
  if (rRes.data) {
    Object.assign(rca, rRes.data)
  }
}

function addFactor(dim) {
  factorsByDim[dim].push({ factorType: 'PROXIMAL', factorContent: '' })
}

function removeFactor(dim, idx) {
  factorsByDim[dim].splice(idx, 1)
}

async function saveFactorsData() {
  const factors = []
  dimensions.forEach((dim) => {
    factorsByDim[dim].forEach((f) => {
      if (f.factorContent && f.factorContent.trim()) {
        factors.push({ dimension: dim, factorType: f.factorType, factorContent: f.factorContent.trim() })
      }
    })
  })
  savingFactors.value = true
  try {
    await saveFactors({ eventId: eventId.value, factors })
    ElMessage.success(`已保存 ${factors.length} 条诱因`)
  } finally {
    savingFactors.value = false
  }
}

async function saveRcaData() {
  savingRca.value = true
  try {
    await saveRca({ eventId: eventId.value, ...rca })
    ElMessage.success('根因分析已保存')
  } finally {
    savingRca.value = false
  }
}

async function openAssist() {
  const res = await getAssist(eventId.value)
  assist.value = res.data
  dimensions.forEach((d) => {
    assistChecked[d] = []
  })
  assistVisible.value = true
}

function applyAssist() {
  let added = 0
  dimensions.forEach((dim) => {
    const existing = new Set(factorsByDim[dim].map((f) => f.factorContent))
    ;(assistChecked[dim] || []).forEach((s) => {
      if (!existing.has(s)) {
        factorsByDim[dim].push({ factorType: 'PROXIMAL', factorContent: s })
        added++
      }
    })
    assistChecked[dim] = []
  })
  ElMessage.success(`已加入 ${added} 条建议，记得点“保存诱因”`)
  assistVisible.value = false
}
</script>

<style scoped>
.card-head { display: flex; align-items: center; justify-content: space-between; }
.dim-block { margin-bottom: 16px; }
.dim-title { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.dim-count { color: #909399; font-size: 12px; }
.factor-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.assist-head { display: flex; align-items: center; }
.assist-dim { margin-bottom: 14px; }
.assist-cb { display: block; margin: 6px 0; }
</style>
