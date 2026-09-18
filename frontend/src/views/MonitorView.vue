<template>
  <div class="monitor">
    <el-row :gutter="12">
      <el-col v-for="s in statCards" :key="s.label" :xs="12" :sm="8" :md="6" :lg="3">
        <div class="stat-card">
          <div class="stat-head">
            <span class="stat-label">{{ s.label }}</span>
            <span class="stat-icon" :style="{ background: s.color + '14', color: s.color }">
              <el-icon><component :is="s.icon" /></el-icon>
            </span>
          </div>
          <div class="stat-value num">{{ s.value }}<span v-if="s.unit" class="stat-unit">{{ s.unit }}</span></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="mt">
      <el-col :xs="24" :lg="16">
        <div class="panel">
          <div class="panel-title">近 14 天预约趋势</div>
          <div ref="trendRef" class="chart chart-lg"></div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="8">
        <div class="panel">
          <div class="panel-title">预约状态分布</div>
          <div ref="pieRef" class="chart chart-lg"></div>
        </div>
      </el-col>
    </el-row>

    <div class="panel mt">
      <div class="panel-title">实验室使用时长排行（近 14 天 · 小时）</div>
      <div ref="usageRef" class="chart chart-md"></div>
    </div>

    <div class="panel mt">
      <div class="panel-title">
        实验室实时状态
        <span class="refresh-hint">{{ countdown }}s 后自动刷新</span>
      </div>
      <el-row :gutter="12">
        <el-col v-for="lab in labs" :key="lab.id" :xs="24" :sm="12" :md="8" :lg="4">
          <div class="lab-card" :class="statusClass(lab.currentStatus)">
            <div class="lab-name">{{ lab.name }}</div>
            <div class="lab-room mono">{{ lab.building }} {{ lab.room }}</div>
            <div class="lab-status">
              <span class="dot"></span>{{ lab.currentStatus }}
            </div>
            <div class="lab-meta">容量 {{ lab.capacity }} 人 · 今日 {{ lab.todayReservations }} 单</div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import api from '../api'

const overview = ref({})
const labs = ref([])
const countdown = ref(30)
let timer = null

const trendRef = ref(null)
const pieRef = ref(null)
const usageRef = ref(null)
let trendChart = null
let pieChart = null
let usageChart = null

const statCards = computed(() => {
  const o = overview.value || {}
  /* 图标仅用语义色点缀真实状态，数值统一深色 */
  return [
    { label: '实验室总数', value: fmtNum(o.totalLabs), unit: '间', color: '#1e3a5f', icon: 'OfficeBuilding' },
    { label: '开放中', value: fmtNum(o.openLabs), unit: '间', color: '#2f7d5b', icon: 'CircleCheck' },
    { label: '使用中', value: fmtNum(o.inUseLabs), unit: '间', color: '#b4232a', icon: 'VideoPlay' },
    { label: '待审批', value: fmtNum(o.pendingApprovals), unit: '单', color: '#b45309', icon: 'Clock' },
    { label: '今日预约', value: fmtNum(o.todayTotal), unit: '单', color: '#1e3a5f', icon: 'Calendar' },
    { label: '设备总数', value: fmtNum(o.totalDevices), unit: '台', color: '#1e3a5f', icon: 'Cpu' },
    { label: '故障/维修', value: fmtNum((o.faultyDevices || 0) + (o.repairingDevices || 0)), unit: '台', color: '#b4232a', icon: 'WarningFilled' },
    { label: '活跃用户', value: fmtNum(o.activeUsers), unit: '人', color: '#1e3a5f', icon: 'User' }
  ]
})

function fmtNum(v) {
  return v === undefined || v === null ? '-' : v
}

function statusClass(s) {
  return {
    使用中: 'st-busy',
    空闲: 'st-free',
    即将开始: 'st-soon',
    维护中: 'st-maint',
    已关闭: 'st-closed'
  }[s] || 'st-free'
}

async function loadOverview() {
  try {
    overview.value = await api.statsOverview()
  } catch (e) { /* 忽略轮询错误 */ }
}

async function loadLabs() {
  try {
    labs.value = await api.labs()
  } catch (e) { /* 忽略轮询错误 */ }
}

async function loadTrends() {
  try {
    const data = await api.statsTrends(14)
    const dates = data.map((d) => d.date.slice(5))
    trendChart && trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['预约总数', '已批准'], top: 0 },
      grid: { left: 40, right: 20, top: 34, bottom: 26 },
      xAxis: { type: 'category', data: dates },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        { name: '预约总数', type: 'bar', data: data.map((d) => d.total), barMaxWidth: 18, itemStyle: { color: '#1e3a5f', borderRadius: [3, 3, 0, 0] } },
        { name: '已批准', type: 'line', data: data.map((d) => d.approved), smooth: true, itemStyle: { color: '#2f7d5b' }, lineStyle: { width: 2 } }
      ]
    })
  } catch (e) { /* 忽略轮询错误 */ }
}

async function loadPie() {
  try {
    const data = await api.statsStatusDistribution()
    pieChart && pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} 单 ({d}%)' },
      legend: { orient: 'vertical', right: 8, top: 'middle', itemWidth: 10 },
      series: [
        {
          type: 'pie',
          radius: ['42%', '68%'],
          center: ['38%', '52%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          data: data.map((d) => ({
            name: d.status,
            value: d.count,
            itemStyle: { color: pieColor(d.status) }
          }))
        }
      ]
    })
  } catch (e) { /* 忽略轮询错误 */ }
}

function pieColor(name) {
  return {
    已完成: '#94a3b8',
    已生效: '#2f7d5b',
    待审批: '#b45309',
    已驳回: '#b4232a',
    已取消: '#d2d5df'
  }[name] || '#1e3a5f'
}

async function loadUsage() {
  try {
    const data = await api.statsLabUsage(14)
    usageChart && usageChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: (ps) => {
        const p = ps[0]
        const item = data[p.dataIndex]
        return `${p.name}<br/>使用时长：${p.value} 小时<br/>预约单数：${item.reservations} 单<br/>利用率：${item.utilization}%`
      } },
      grid: { left: 130, right: 40, top: 10, bottom: 26 },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: data.map((d) => d.name).reverse(), axisLabel: { width: 120 } },
      series: [
        {
          type: 'bar',
          data: data.map((d) => d.hours).reverse(),
          barMaxWidth: 16,
          itemStyle: { color: '#1e3a5f', borderRadius: [0, 3, 3, 0] },
          label: { show: true, position: 'right', formatter: '{c}h' }
        }
      ]
    })
  } catch (e) { /* 忽略轮询错误 */ }
}

function resizeAll() {
  trendChart && trendChart.resize()
  pieChart && pieChart.resize()
  usageChart && usageChart.resize()
}

function refreshAll() {
  loadOverview()
  loadLabs()
  loadTrends()
  loadPie()
  loadUsage()
}

onMounted(() => {
  trendChart = echarts.init(trendRef.value)
  pieChart = echarts.init(pieRef.value)
  usageChart = echarts.init(usageRef.value)
  window.addEventListener('resize', resizeAll)
  refreshAll()
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      countdown.value = 30
      refreshAll()
    }
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('resize', resizeAll)
  ;[trendChart, pieChart, usageChart].forEach((c) => c && c.dispose())
})
</script>

<style scoped>
.stat-card {
  background: #fff;
  border: 1px solid var(--ink-line);
  border-radius: 10px;
  padding: 16px 18px 14px;
  box-shadow: var(--card-shadow);
  transition: box-shadow 0.2s ease;
}

.stat-card:hover {
  box-shadow: var(--card-shadow-hover);
}

.stat-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-label {
  font-size: 12.5px;
  color: var(--text-2);
}

.stat-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  flex-shrink: 0;
}

.stat-value {
  font-size: 34px;
  font-weight: 700;
  line-height: 1.1;
  margin-top: 10px;
  letter-spacing: -0.5px;
  color: var(--text-1);
}

.stat-unit {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-2);
  margin-left: 4px;
  letter-spacing: 0;
}

.panel {
  background: #fff;
  border-radius: 10px;
  padding: 14px 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.refresh-hint {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}

.chart-lg {
  height: 280px;
}

.chart-md {
  height: 240px;
}

.lab-card {
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 10px;
  border: 1px solid #ebeef5;
  background: #fafbfc;
  position: relative;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.lab-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(16, 24, 40, 0.08);
}

.lab-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.lab-room {
  font-size: 12px;
  color: #909399;
  margin: 4px 0 10px;
}

.lab-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: currentColor;
}

.lab-meta {
  font-size: 12px;
  color: #909399;
}

.st-free .lab-status { color: #2f7d5b; }
.st-busy .lab-status { color: #b4232a; }
.st-soon .lab-status { color: #b45309; }
.st-maint .lab-status { color: #64748b; }
.st-closed .lab-status { color: #303133; }

.st-busy { border-color: #f0d3da; background: #f9f0f1; }
.st-busy .dot { animation: dot-pulse 1.6s infinite; }
.st-soon { border-color: #f0ddba; background: #f9f4ea; }
.st-maint { opacity: 0.75; }
.st-closed { opacity: 0.6; }
</style>
