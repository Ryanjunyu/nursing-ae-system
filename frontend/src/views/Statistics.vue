<template>
  <div>
    <h3 class="page-title">统计分析</h3>

    <el-row :gutter="14" style="margin-bottom:14px">
      <el-col :span="6">
        <el-card><div class="metric"><div class="num">{{ summary.total }}</div><div class="label">事件总数</div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="metric"><div class="num">{{ summary.reported }}</div><div class="label">待分析（已上报）</div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="metric"><div class="num">{{ summary.analyzed }}</div><div class="label">已分析</div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card><div class="metric"><div class="num">{{ summary.anonymous }}</div><div class="label">匿名上报</div></div></el-card>
      </el-col>
    </el-row>

    <el-alert
      v-if="summary.total > 0"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom:14px"
      :title="`高频安全问题：${summary.topType || '—'}（${summary.topTypeCount} 起）；高发科室：${summary.topDept || '—'}（${summary.topDeptCount} 起）`" />

    <el-row :gutter="14">
      <el-col :span="12">
        <el-card header="各科室事件数量"><div ref="deptRef" class="chart"></div></el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="各事件类型分布"><div ref="typeRef" class="chart"></div></el-card>
      </el-col>
    </el-row>
    <el-row :gutter="14" style="margin-top:14px">
      <el-col :span="12">
        <el-card header="损伤分级分布"><div ref="levelRef" class="chart"></div></el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="近 6 个月发生趋势"><div ref="trendRef" class="chart"></div></el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { getSummary, getByDept, getByType, getByLevel, getTrend } from '../api/statistics'

const summary = reactive({ total: 0, reported: 0, analyzed: 0, anonymous: 0, topType: '', topTypeCount: 0, topDept: '', topDeptCount: 0 })
const deptRef = ref()
const typeRef = ref()
const levelRef = ref()
const trendRef = ref()
let charts = []

function bar(el, data) {
  const c = echarts.init(el)
  c.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 20, top: 24, bottom: 40 },
    xAxis: { type: 'category', data: data.map((d) => d.name), axisLabel: { interval: 0, rotate: data.length > 5 ? 20 : 0 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'bar', barMaxWidth: 42, data: data.map((d) => d.value), itemStyle: { color: '#1d6fb8' }, label: { show: true, position: 'top' } }]
  })
  charts.push(c)
}

function pie(el, data) {
  const c = echarts.init(el)
  c.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 起 ({d}%)' },
    legend: { bottom: 0 },
    series: [{ type: 'pie', radius: ['40%', '66%'], center: ['50%', '46%'], data: data.map((d) => ({ name: d.name, value: d.value })), label: { formatter: '{b}\n{c}' } }]
  })
  charts.push(c)
}

function line(el, data) {
  const c = echarts.init(el)
  c.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 20, top: 24, bottom: 30 },
    xAxis: { type: 'category', data: data.map((d) => d.name) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'line', smooth: true, symbolSize: 7, data: data.map((d) => d.value), areaStyle: { opacity: 0.15 }, itemStyle: { color: '#2a8c5a' } }]
  })
  charts.push(c)
}

function resize() {
  charts.forEach((c) => c.resize())
}

onMounted(async () => {
  const [s, dept, type, level, trend] = await Promise.all([
    getSummary(), getByDept(), getByType(), getByLevel(), getTrend(6)
  ])
  Object.assign(summary, s.data)
  bar(deptRef.value, dept.data)
  pie(typeRef.value, type.data)
  pie(levelRef.value, level.data)
  line(trendRef.value, trend.data)
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  charts.forEach((c) => c.dispose())
  charts = []
})
</script>

<style scoped>
.metric { text-align: center; padding: 6px 0; }
.metric .num { font-size: 28px; font-weight: 700; color: #1d6fb8; }
.metric .label { font-size: 13px; color: #909399; margin-top: 4px; }
.chart { height: 320px; width: 100%; }
</style>
