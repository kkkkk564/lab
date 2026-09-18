<template>
  <div class="page-card">
    <div class="toolbar">
      <el-radio-group v-model="status" @change="filterList">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="PENDING">待审批</el-radio-button>
        <el-radio-button value="APPROVED">已生效</el-radio-button>
        <el-radio-button value="FINISHED">已完成</el-radio-button>
        <el-radio-button value="REJECTED">已驳回</el-radio-button>
        <el-radio-button value="CANCELLED">已取消</el-radio-button>
      </el-radio-group>
      <div class="spacer"></div>
      <el-button :icon="Refresh" @click="loadList">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="shown" border stripe>
      <el-table-column label="实验室" min-width="150">
        <template #default="{ row }">
          <div>{{ row.labName }}</div>
          <div class="sub mono">{{ row.room }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="用途标题" min-width="140" show-overflow-tooltip />
      <el-table-column label="日期" width="110">
        <template #default="{ row }">
          <span class="mono">{{ row.date }}</span>
        </template>
      </el-table-column>
      <el-table-column label="时段" width="120">
        <template #default="{ row }">
          <span class="mono">{{ row.startTime }}-{{ row.endTime }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="headcount" label="人数" width="70" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.displayStatus)">{{ statusText(row.displayStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审批意见" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.reviewComment">{{ row.reviewComment }}</span>
          <span v-else class="sub">—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="cancellable(row)"
            type="danger"
            link
            size="small"
            @click="doCancel(row)">
            取消
          </el-button>
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

const status = ref('')
const list = ref([])
const loading = ref(false)

const shown = computed(() => list.value)

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

function cancellable(row) {
  return (row.status === 'PENDING' || row.status === 'APPROVED') && row.displayStatus !== 'FINISHED'
}

async function loadList() {
  loading.value = true
  try {
    list.value = await api.reservations({ scope: 'mine' })
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

function filterList() {
  /* computed 自动响应 */
}

async function doCancel(row) {
  try {
    await ElMessageBox.confirm(
      `确定取消「${row.title}」（${row.date} ${row.startTime}-${row.endTime}）吗？`,
      '取消预约',
      { type: 'warning' }
    )
    await api.cancelReservation(row.id)
    ElMessage.success('预约已取消')
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
</style>
