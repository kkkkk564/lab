<template>
  <div class="page-card">
    <div class="toolbar">
      <el-radio-group v-model="status">
        <el-radio-button value="PENDING">待审批（{{ pendingCount }}）</el-radio-button>
        <el-radio-button value="">全部记录</el-radio-button>
      </el-radio-group>
      <div class="spacer"></div>
      <el-button :icon="Refresh" @click="loadList">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="shown" border stripe>
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="expand-box">
            <p><b>用途说明：</b>{{ row.purpose || '未填写' }}</p>
            <p><b>审批意见：</b>{{ row.reviewComment || '—' }}</p>
            <p><b>审批人：</b>{{ row.reviewedBy || '—' }}</p>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="申请人" width="150">
        <template #default="{ row }">
          <div>{{ row.name }}</div>
          <div class="sub mono">{{ row.username }}</div>
        </template>
      </el-table-column>
      <el-table-column label="实验室" min-width="140">
        <template #default="{ row }">
          <div>{{ row.labName }}</div>
          <div class="sub mono">{{ row.room }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="用途标题" min-width="130" show-overflow-tooltip />
      <el-table-column label="日期" width="105">
        <template #default="{ row }">
          <span class="mono">{{ row.date }}</span>
        </template>
      </el-table-column>
      <el-table-column label="时段" width="115">
        <template #default="{ row }">
          <span class="mono">{{ row.startTime }}-{{ row.endTime }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="headcount" label="人数" width="65" />
      <el-table-column label="状态" width="95">
        <template #default="{ row }">
          <el-tag :type="statusType(row.displayStatus)">{{ statusText(row.displayStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button type="success" size="small" @click="doReview(row, 'approve')">通过</el-button>
            <el-button type="danger" size="small" plain @click="doReview(row, 'reject')">驳回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import api from '../api'

const status = ref('PENDING')
const list = ref([])
const loading = ref(false)

const shown = computed(() => list.value.filter((r) => !status.value || r.status === status.value))
const pendingCount = computed(() => list.value.filter((r) => r.status === 'PENDING').length)

function statusText(s) {
  return {
    PENDING: '待审批',
    APPROVED: '已生效',
    FINISHED: '已完成',
    REJECTED: '已驳回',
    CANCELLED: '已取消'
  }[s] || s
}

function statusType(s) {
  return {
    PENDING: 'warning',
    APPROVED: 'success',
    FINISHED: 'info',
    REJECTED: 'danger',
    CANCELLED: 'info'
  }[s] || 'info'
}

async function loadList() {
  loading.value = true
  try {
    list.value = await api.reservations({ scope: 'all' })
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

async function doReview(row, action) {
  const verb = action === 'approve' ? '通过' : '驳回'
  try {
    const { value } = await ElMessageBox.prompt(
      `确定${verb}「${row.name}」的预约申请（${row.date} ${row.startTime}-${row.endTime}）吗？`,
      `${verb}预约`,
      {
        inputPlaceholder: action === 'approve' ? '审批意见（选填）' : '请填写驳回原因',
        inputValidator: (v) => (action === 'approve' || (v && v.trim()) ? true : '驳回时必须填写原因'),
        type: action === 'approve' ? 'success' : 'warning'
      }
    )
    await api.reviewReservation(row.id, { action, comment: (value || '').trim() })
    ElMessage.success(`已${verb}该预约`)
    loadList()
  } catch (e) { /* 取消或错误 */ }
}

onMounted(loadList)
</script>

<style scoped>
.sub {
  font-size: 12px;
  color: #909399;
}

.expand-box {
  padding: 6px 20px;
  color: #606266;
  font-size: 13px;
}

.expand-box p {
  margin: 6px 0;
}
</style>
