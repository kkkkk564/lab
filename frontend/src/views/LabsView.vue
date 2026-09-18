<template>
  <div>
    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索名称 / 编号 / 楼栋" clearable style="width: 220px" :prefix-icon="Search" />
        <el-select v-model="query.category" placeholder="类别" clearable style="width: 130px">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="query.status" placeholder="开放状态" clearable style="width: 130px">
          <el-option label="开放中" value="OPEN" />
          <el-option label="维护中" value="MAINTENANCE" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
        <el-button :icon="Refresh" @click="loadLabs">刷新</el-button>
        <div class="spacer"></div>
        <span class="hint">实时状态每 30 秒自动刷新</span>
      </div>

      <el-row v-loading="loading" :gutter="12">
        <el-col v-for="lab in filtered" :key="lab.id" :xs="24" :sm="12" :md="8" :xl="6">
          <div class="lab-card">
            <div class="card-head">
              <span class="lab-code mono">{{ lab.code }}</span>
              <el-tag size="small" :type="currentTagType(lab.currentStatus)">{{ lab.currentStatus }}</el-tag>
            </div>
            <div class="lab-name">{{ lab.name }}</div>
            <div class="lab-info">
              <el-icon><Location /></el-icon>
              {{ lab.building }} {{ lab.room }} · 容量 {{ lab.capacity }} 人
            </div>
            <div class="lab-info">
              <el-icon><Clock /></el-icon>
              开放 {{ lab.openTime }} - {{ lab.closeTime }}
            </div>
            <div class="lab-info">
              <el-icon><Cpu /></el-icon>
              设备 {{ lab.deviceCount }} 台
              <el-tag v-if="lab.faultyCount > 0" size="small" type="danger" style="margin-left: 6px">故障 {{ lab.faultyCount }}</el-tag>
            </div>
            <div class="card-foot">
              <el-button text type="primary" @click="openDetail(lab)">详情 / 时刻表</el-button>
              <el-button type="primary" size="small" :disabled="lab.status !== 'OPEN'" @click="openBook(lab)">
                预约
              </el-button>
            </div>
          </div>
        </el-col>
      </el-row>
      <el-empty v-if="filtered.length === 0" description="没有符合条件的实验室" />
    </div>

    <el-drawer v-model="detailVisible" :title="detailLab.name || '实验室详情'" size="46%">
      <template v-if="detailLab.id">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="编号">{{ detailLab.code }}</el-descriptions-item>
          <el-descriptions-item label="类别">{{ detailLab.category }}</el-descriptions-item>
          <el-descriptions-item label="位置">{{ detailLab.building }} {{ detailLab.room }}</el-descriptions-item>
          <el-descriptions-item label="容量">{{ detailLab.capacity }} 人</el-descriptions-item>
          <el-descriptions-item label="负责人">{{ detailLab.manager }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detailLab.phone }}</el-descriptions-item>
          <el-descriptions-item label="开放时间" :span="2">{{ detailLab.openTime }} - {{ detailLab.closeTime }}</el-descriptions-item>
          <el-descriptions-item label="当前状态" :span="2">
            <el-tag :type="currentTagType(detailLab.currentStatus)">{{ detailLab.currentStatus }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="简介" :span="2">{{ detailLab.description || '暂无' }}</el-descriptions-item>
          <el-descriptions-item label="主要设备" :span="2">{{ detailLab.equipment || '暂无' }}</el-descriptions-item>
        </el-descriptions>

        <div class="drawer-section">
          <div class="section-title">
            当日时刻表
            <el-date-picker v-model="timetableDate" size="small" style="width: 140px" value-format="YYYY-MM-DD" :disabled-date="disablePast" @change="loadTimetable" />
          </div>
          <el-table :data="timetable" size="small" border>
            <el-table-column label="时段" width="130">
              <template #default="{ row }">
                <span class="mono">{{ row.startTime }} - {{ row.endTime }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="用途" min-width="140" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'APPROVED' ? 'success' : 'warning'">
                  {{ row.status === 'APPROVED' ? '已批准' : '待审批' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="headcount" label="人数" width="70" />
          </el-table>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="bookVisible" title="预约实验室" width="480px">
      <div class="book-target">
        <b>{{ bookForm.labName }}</b>
        <span class="mono">（开放 {{ bookForm.openRange }}）</span>
      </div>
      <el-form :model="bookForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="bookForm.title" maxlength="50" placeholder="如：数据结构课程实验" />
        </el-form-item>
        <el-form-item label="用途">
          <el-input v-model="bookForm.purpose" type="textarea" :rows="2" maxlength="200" placeholder="补充说明（选填）" />
        </el-form-item>
        <el-form-item label="日期" required>
          <el-date-picker v-model="bookForm.date" style="width: 100%" value-format="YYYY-MM-DD" :disabled-date="disablePast" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-time-select v-model="bookForm.startTime" start="00:00" end="23:30" step="00:30" placeholder="开始" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-time-select v-model="bookForm.endTime" :start="bookForm.startTime || '00:30'" end="23:59" step="00:30" placeholder="结束" style="width: 100%" />
        </el-form-item>
        <el-form-item label="使用人数" required>
          <el-input-number v-model="bookForm.headcount" :min="1" :max="bookForm.capacity || 99" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bookVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBook">提交预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Clock, Cpu, Location, Refresh, Search } from '@element-plus/icons-vue'
import api from '../api'

const query = reactive({ keyword: '', category: '', status: '' })
const labs = ref([])
const loading = ref(false)
const categories = computed(() => [...new Set(labs.value.map((l) => l.category).filter(Boolean))])

const detailVisible = ref(false)
const detailLab = ref({})
const timetableDate = ref(todayStr())
const timetable = ref([])

const bookVisible = ref(false)
const submitting = ref(false)
const bookForm = reactive({
  labId: null, labName: '', openRange: '', capacity: 0,
  title: '', purpose: '', date: '', startTime: '', endTime: '', headcount: 1
})

let pollTimer = null

function todayStr() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

function disablePast(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

const filtered = computed(() => labs.value.filter((l) => {
  if (query.keyword) {
    const k = query.keyword.toLowerCase()
    if (![l.name, l.code, l.building, l.room].some((v) => (v || '').toLowerCase().includes(k))) return false
  }
  if (query.category && l.category !== query.category) return false
  if (query.status && l.status !== query.status) return false
  return true
}))

function currentTagType(s) {
  return {
    使用中: 'danger',
    空闲: 'success',
    即将开始: 'warning',
    维护中: 'info',
    已关闭: 'info'
  }[s] || 'info'
}

async function loadLabs(silent) {
  if (!silent) loading.value = true
  try {
    labs.value = await api.labs()
  } catch (e) { /* 拦截器已提示 */ } finally {
    if (!silent) loading.value = false
  }
}

async function openDetail(lab) {
  detailLab.value = lab
  timetableDate.value = todayStr()
  detailVisible.value = true
  loadTimetable()
}

async function loadTimetable() {
  if (!detailLab.value.id || !timetableDate.value) return
  try {
    timetable.value = await api.labTimetable(detailLab.value.id, timetableDate.value)
  } catch (e) { /* 拦截器已提示 */ }
}

function openBook(lab) {
  Object.assign(bookForm, {
    labId: lab.id,
    labName: `${lab.name}（${lab.building} ${lab.room}）`,
    openRange: `${lab.openTime} - ${lab.closeTime}`,
    capacity: lab.capacity,
    title: '', purpose: '', date: todayStr(), startTime: '', endTime: '', headcount: 1
  })
  bookVisible.value = true
}

async function submitBook() {
  if (!bookForm.title.trim()) return ElMessage.warning('请填写预约标题')
  if (!bookForm.date) return ElMessage.warning('请选择日期')
  if (!bookForm.startTime || !bookForm.endTime) return ElMessage.warning('请选择开始与结束时间')
  if (bookForm.startTime >= bookForm.endTime) return ElMessage.warning('结束时间必须晚于开始时间')
  submitting.value = true
  try {
    await api.createReservation({
      labId: bookForm.labId,
      title: bookForm.title.trim(),
      purpose: bookForm.purpose.trim(),
      date: bookForm.date,
      startTime: bookForm.startTime,
      endTime: bookForm.endTime,
      headcount: bookForm.headcount
    })
    ElMessage.success('预约已提交，等待管理员审批')
    bookVisible.value = false
    loadLabs()
  } catch (e) { /* 拦截器已提示 */ } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadLabs()
  pollTimer = setInterval(() => loadLabs(true), 30000)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.lab-card {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 12px;
  background: #fff;
  position: relative;
  overflow: hidden;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.lab-card:hover {
  border-color: #d2d5df;
  box-shadow: 0 2px 8px rgba(28, 36, 48, 0.08);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.lab-code {
  font-size: 12px;
  color: #909399;
}

.lab-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.lab-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
  margin: 5px 0;
}

.lab-info .el-icon {
  color: #909399;
}

.card-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
}

.hint {
  font-size: 12px;
  color: #909399;
}

.drawer-section {
  margin-top: 18px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.book-target {
  margin-bottom: 16px;
  color: #606266;
  font-size: 14px;
}
</style>
