<template>
  <div>
    <h3 class="page-title">事件查询</h3>
    <el-card>
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item v-if="isAdmin" label="科室">
          <el-select v-model="query.deptId" clearable placeholder="全部" style="width:150px">
            <el-option v-for="d in departments" :key="d.deptId" :label="d.deptName" :value="d.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="数据范围">
          <el-tag type="info">仅本科室</el-tag>
        </el-form-item>
        <el-form-item label="事件类型">
          <el-select v-model="query.eventType" clearable placeholder="全部" style="width:150px">
            <el-option v-for="t in eventTypes" :key="t.typeCode" :label="t.typeName" :value="t.typeCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="损伤分级">
          <el-select v-model="query.injuryLevel" clearable placeholder="全部" style="width:170px">
            <el-option v-for="l in injuryLevels" :key="l.levelCode" :label="l.levelName" :value="l.levelCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间区间">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
            start-placeholder="开始日期" end-placeholder="结束日期" style="width:250px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load(1)">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="rows" stripe border v-loading="loading" style="width:100%">
        <el-table-column prop="eventNo" label="事件编号" width="180" />
        <el-table-column prop="deptName" label="科室" width="120" />
        <el-table-column prop="eventTypeName" label="事件类型" width="120" />
        <el-table-column prop="injuryLevelName" label="损伤分级" width="160" />
        <el-table-column prop="occurTime" label="发生时间" width="170" />
        <el-table-column prop="eventDesc" label="事件描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="reporterDisplay" label="上报人" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'REPORTED' ? 'warning' : 'success'">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row.eventId)">明细</el-button>
            <el-button text type="warning" @click="goRca(row.eventId)">RCA分析</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top:14px; justify-content:flex-end"
        layout="total, prev, pager, next"
        :total="total"
        :current-page="query.page"
        :page-size="query.size"
        @current-change="(p) => load(p)" />
    </el-card>

    <el-dialog v-model="detailVisible" title="事件明细" width="560px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="事件编号">{{ detail.eventNo }}</el-descriptions-item>
        <el-descriptions-item label="科室">{{ detail.deptName }}</el-descriptions-item>
        <el-descriptions-item label="事件类型">{{ detail.eventTypeName }}</el-descriptions-item>
        <el-descriptions-item label="损伤分级">{{ detail.injuryLevelName }}</el-descriptions-item>
        <el-descriptions-item label="发生时间">{{ detail.occurTime }}</el-descriptions-item>
        <el-descriptions-item label="上报人">{{ detail.reporterDisplay }}</el-descriptions-item>
        <el-descriptions-item label="事件描述" :span="2">{{ detail.eventDesc }}</el-descriptions-item>
        <el-descriptions-item label="现场情况" :span="2">{{ detail.sceneDesc || '—' }}</el-descriptions-item>
        <el-descriptions-item label="患者（脱敏）" :span="2">{{ detail.patientMaskedName || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDepartments, getEventTypes, getInjuryLevels } from '../api/dict'
import { listEvents, getEvent } from '../api/event'
import { auth } from '../store/auth'

const router = useRouter()
const user = auth.getUser()
const isAdmin = user && user.role === 'ADMIN'
const dateRange = ref([])

const departments = ref([])
const eventTypes = ref([])
const injuryLevels = ref([])
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref(null)

const query = reactive({ deptId: null, eventType: '', injuryLevel: '', page: 1, size: 10 })

const statusMap = { REPORTED: '已上报', ANALYZED: '已分析' }
const statusText = (s) => statusMap[s] || s

onMounted(async () => {
  const [d, t, l] = await Promise.all([getDepartments(), getEventTypes(), getInjuryLevels()])
  departments.value = d.data
  eventTypes.value = t.data
  injuryLevels.value = l.data
  load(1)
})

async function load(page) {
  query.page = page || query.page
  query.startTime = dateRange.value && dateRange.value[0] ? dateRange.value[0] + ' 00:00:00' : ''
  query.endTime = dateRange.value && dateRange.value[1] ? dateRange.value[1] + ' 23:59:59' : ''
  loading.value = true
  try {
    const res = await listEvents(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.deptId = null
  query.eventType = ''
  query.injuryLevel = ''
  dateRange.value = []
  load(1)
}

async function openDetail(id) {
  const res = await getEvent(id)
  detail.value = res.data
  detailVisible.value = true
}

function goRca(id) {
  router.push({ name: 'rca', query: { eventId: id } })
}
</script>
