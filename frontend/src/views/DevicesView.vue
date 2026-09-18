<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索设备 / 型号 / 实验室" clearable style="width: 220px" :prefix-icon="Search" />
      <el-select v-model="query.labId" placeholder="所属实验室" clearable filterable style="width: 180px">
        <el-option v-for="l in labs" :key="l.id" :label="l.name" :value="l.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="设备状态" clearable style="width: 130px">
        <el-option label="正常" value="NORMAL" />
        <el-option label="故障" value="FAULTY" />
        <el-option label="维修中" value="REPAIRING" />
        <el-option label="已报废" value="SCRAPPED" />
      </el-select>
      <el-button :icon="Refresh" @click="loadDevices">刷新</el-button>
      <div class="spacer"></div>
      <el-button v-if="auth.isAdmin" type="primary" :icon="Plus" @click="openEdit(null)">新增设备</el-button>
    </div>

    <el-table v-loading="loading" :data="filtered" border stripe>
      <el-table-column prop="name" label="设备名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="model" label="型号" min-width="120" show-overflow-tooltip />
      <el-table-column prop="labName" label="所属实验室" min-width="130" show-overflow-tooltip />
      <el-table-column label="状态" width="95">
        <template #default="{ row }">
          <el-tag :type="deviceTagType(row.status)">{{ deviceStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="purchaseDate" label="购入" width="95">
        <template #default="{ row }">
          <span class="mono sub">{{ row.purchaseDate || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="价格" width="100" align="right">
        <template #default="{ row }">
          <span class="mono">{{ row.price ? '¥' + Number(row.price).toLocaleString() : '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="265" fixed="right">
        <template #default="{ row }">
          <div class="op-cell">
            <el-button v-if="row.status === 'NORMAL'" size="small" plain type="warning" @click="openReport(row)">报修</el-button>
            <el-button v-if="row.faultDesc" size="small" plain type="info" @click="openFault(row)">故障详情</el-button>
            <template v-if="auth.isAdmin">
              <el-button
                v-if="row.status === 'FAULTY' || row.status === 'REPAIRING'"
                size="small" plain type="success"
                @click="doRepair(row)">标记修复</el-button>
              <el-button size="small" plain type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" plain type="danger" @click="doDelete(row)">删除</el-button>
            </template>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="editVisible" :title="editForm.id ? '编辑设备' : '新增设备'" width="460px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="所属实验室" required>
          <el-select v-model="editForm.labId" filterable style="width: 100%">
            <el-option v-for="l in labs" :key="l.id" :label="l.name" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备名称" required>
          <el-input v-model="editForm.name" maxlength="60" />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="editForm.model" maxlength="60" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option label="正常" value="NORMAL" />
            <el-option label="故障" value="FAULTY" />
            <el-option label="维修中" value="REPAIRING" />
            <el-option label="已报废" value="SCRAPPED" />
          </el-select>
        </el-form-item>
        <el-form-item label="购入日期">
          <el-input v-model="editForm.purchaseDate" placeholder="如 2024-03" />
        </el-form-item>
        <el-form-item label="价格(元)">
          <el-input-number v-model="editForm.price" :min="0" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportVisible" title="设备报修" width="440px">
      <div class="report-target">
        <b>{{ reportDevice.name }}</b>（{{ reportDevice.labName }}）
      </div>
      <el-input v-model="reportDesc" type="textarea" :rows="4" maxlength="300" placeholder="请描述故障现象，如：屏幕无显示，无法开机…" />
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="warning" :loading="submitting" @click="submitReport">提交报修</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="faultVisible" title="故障详情" width="480px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="设备名称">
          {{ faultDevice.name }}<span v-if="faultDevice.model" class="sub">（{{ faultDevice.model }}）</span>
        </el-descriptions-item>
        <el-descriptions-item label="所属实验室">{{ faultDevice.labName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag size="small" :type="deviceTagType(faultDevice.status)">{{ deviceStatusText(faultDevice.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="故障描述">
          <div class="fault-text">{{ faultDevice.faultDesc || '—' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="报修人">{{ faultDevice.reportBy || '—' }}</el-descriptions-item>
        <el-descriptions-item label="报修时间">
          <span class="mono sub">{{ formatTime(faultDevice.reportAt) }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button
          v-if="auth.isAdmin && (faultDevice.status === 'FAULTY' || faultDevice.status === 'REPAIRING')"
          type="success"
          plain
          @click="faultVisible = false; doRepair(faultDevice)">
          标记修复
        </el-button>
        <el-button @click="faultVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import api from '../api'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const query = reactive({ keyword: '', labId: null, status: '' })
const labs = ref([])
const devices = ref([])
const loading = ref(false)

const editVisible = ref(false)
const reportVisible = ref(false)
const faultVisible = ref(false)
const faultDevice = ref({})
const submitting = ref(false)
const reportDevice = ref({})
const reportDesc = ref('')

function formatTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

function openFault(row) {
  faultDevice.value = row
  faultVisible.value = true
}

const editForm = reactive({
  id: null, labId: null, name: '', model: '', status: 'NORMAL', purchaseDate: '', price: 0, remark: ''
})

const filtered = computed(() => devices.value.filter((d) => {
  if (query.keyword) {
    const k = query.keyword.toLowerCase()
    if (![d.name, d.model, d.labName].some((v) => (v || '').toLowerCase().includes(k))) return false
  }
  if (query.labId && d.labId !== query.labId) return false
  if (query.status && d.status !== query.status) return false
  return true
}))

function deviceStatusText(s) {
  return { NORMAL: '正常', FAULTY: '故障', REPAIRING: '维修中', SCRAPPED: '已报废' }[s] || s
}

function deviceTagType(s) {
  return { NORMAL: 'success', FAULTY: 'danger', REPAIRING: 'warning', SCRAPPED: 'info' }[s] || 'info'
}

async function loadLabs() {
  try {
    labs.value = await api.labs()
  } catch (e) { /* 拦截器已提示 */ }
}

async function loadDevices() {
  loading.value = true
  try {
    devices.value = await api.devices({})
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

function openEdit(row) {
  if (row) {
    Object.assign(editForm, {
      id: row.id, labId: row.labId, name: row.name, model: row.model || '',
      status: row.status, purchaseDate: row.purchaseDate || '',
      price: row.price || 0, remark: row.remark || ''
    })
  } else {
    Object.assign(editForm, {
      id: null, labId: labs.value.length ? labs.value[0].id : null,
      name: '', model: '', status: 'NORMAL', purchaseDate: '', price: 0, remark: ''
    })
  }
  editVisible.value = true
}

async function submitEdit() {
  if (!editForm.labId) return ElMessage.warning('请选择所属实验室')
  if (!editForm.name.trim()) return ElMessage.warning('请填写设备名称')
  submitting.value = true
  try {
    const payload = {
      labId: editForm.labId,
      name: editForm.name.trim(),
      model: editForm.model.trim(),
      status: editForm.status,
      purchaseDate: editForm.purchaseDate.trim(),
      price: editForm.price,
      remark: editForm.remark.trim()
    }
    if (editForm.id) {
      await api.updateDevice(editForm.id, payload)
    } else {
      await api.createDevice(payload)
    }
    ElMessage.success('保存成功')
    editVisible.value = false
    loadDevices()
  } catch (e) { /* 拦截器已提示 */ } finally {
    submitting.value = false
  }
}

function openReport(row) {
  reportDevice.value = row
  reportDesc.value = ''
  reportVisible.value = true
}

async function submitReport() {
  if (!reportDesc.value.trim()) return ElMessage.warning('请填写故障描述')
  submitting.value = true
  try {
    await api.reportDevice(reportDevice.value.id, { description: reportDesc.value.trim() })
    ElMessage.success('报修已提交，等待管理员处理')
    reportVisible.value = false
    loadDevices()
  } catch (e) { /* 拦截器已提示 */ } finally {
    submitting.value = false
  }
}

async function doRepair(row) {
  try {
    await ElMessageBox.confirm(`确认「${row.name}」已完成维修并恢复正常吗？`, '标记修复', { type: 'success' })
    await api.repairDevice(row.id)
    ElMessage.success('设备已恢复正常')
    loadDevices()
  } catch (e) { /* 取消或错误 */ }
}

async function doDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除设备「${row.name}」吗？`, '删除设备', { type: 'warning' })
    await api.deleteDevice(row.id)
    ElMessage.success('已删除')
    loadDevices()
  } catch (e) { /* 取消或错误 */ }
}

onMounted(() => {
  loadLabs()
  loadDevices()
})
</script>

<style scoped>
.sub {
  font-size: 12px;
  color: #909399;
}

.report-target {
  margin-bottom: 12px;
  color: #606266;
}

/* 操作列：弹性容器统一间距，按钮多时自动两行居中排列 */
.op-cell {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 6px 8px;
}

/* 抵消相邻按钮的默认 margin-left，间距全部交给 gap 控制 */
.op-cell .el-button {
  margin-left: 0;
}

.fault-text {
  white-space: pre-wrap;
  line-height: 1.7;
  color: #303133;
}
</style>
