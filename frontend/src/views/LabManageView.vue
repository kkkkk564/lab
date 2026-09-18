<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索实验室" clearable style="width: 200px" :prefix-icon="Search" />
      <div class="spacer"></div>
      <el-button type="primary" :icon="Plus" @click="openEdit(null)">新增实验室</el-button>
    </div>

    <el-table v-loading="loading" :data="filtered" border stripe>
      <el-table-column prop="code" label="编号" width="100">
        <template #default="{ row }">
          <span class="mono">{{ row.code }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
      <el-table-column label="位置" width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.building }} {{ row.room }}</template>
      </el-table-column>
      <el-table-column prop="category" label="类别" width="90" />
      <el-table-column prop="capacity" label="容量" width="70" />
      <el-table-column label="开放时间" width="110">
        <template #default="{ row }">
          <span class="mono">{{ row.openTime }}-{{ row.closeTime }}</span>
        </template>
      </el-table-column>
      <el-table-column label="开放状态" width="100">
        <template #default="{ row }">
          <el-tag :type="labTagType(row.status)">{{ labStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="实时状态" width="95">
        <template #default="{ row }">
          <el-tag :type="currentTagType(row.currentStatus)" effect="plain">{{ row.currentStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="deviceCount" label="设备" width="65" />
      <el-table-column label="操作" width="235" fixed="right">
        <template #default="{ row }">
          <div class="op-cell">
            <el-button size="small" plain type="primary" @click="openEdit(row)">编辑</el-button>
            <el-dropdown popper-class="status-pop" @command="(cmd) => onStatusCmd(cmd, row)">
              <el-button size="small" plain type="warning">
                状态<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="OPEN" :disabled="row.status === 'OPEN'">
                    <span class="st-dot" style="background: #2f7d5b"></span>开放
                  </el-dropdown-item>
                  <el-dropdown-item command="MAINTENANCE" :disabled="row.status === 'MAINTENANCE'">
                    <span class="st-dot" style="background: #b45309"></span>维护中
                  </el-dropdown-item>
                  <el-dropdown-item command="CLOSED" :disabled="row.status === 'CLOSED'">
                    <span class="st-dot" style="background: #64748b"></span>关闭
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button size="small" plain type="danger" @click="doDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="editVisible" :title="editForm.id ? '编辑实验室' : '新增实验室'" width="540px">
      <el-form :model="editForm" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="编号" required>
              <el-input v-model="editForm.code" maxlength="30" placeholder="如 LAB-103" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称" required>
              <el-input v-model="editForm.name" maxlength="60" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="楼栋">
              <el-input v-model="editForm.building" maxlength="30" placeholder="实验楼A" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="房间号">
              <el-input v-model="editForm.room" maxlength="30" placeholder="A-103" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类别">
              <el-input v-model="editForm.category" maxlength="20" placeholder="计算机 / 电子信息" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="容量(人)">
              <el-input-number v-model="editForm.capacity" :min="1" :max="500" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-input v-model="editForm.manager" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="editForm.phone" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开放时间">
              <el-time-select v-model="editForm.openTime" start="00:00" end="23:30" step="00:30" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关闭时间">
              <el-time-select v-model="editForm.closeTime" :start="editForm.openTime || '00:30'" end="23:59" step="00:30" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="开放状态">
          <el-radio-group v-model="editForm.status">
            <el-radio value="OPEN">开放</el-radio>
            <el-radio value="MAINTENANCE">维护中</el-radio>
            <el-radio value="CLOSED">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="editForm.description" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="主要设备">
          <el-input v-model="editForm.equipment" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Plus, Search } from '@element-plus/icons-vue'
import api from '../api'

const keyword = ref('')
const labs = ref([])
const loading = ref(false)

const editVisible = ref(false)
const submitting = ref(false)
const editForm = reactive({
  id: null, code: '', name: '', building: '', room: '', category: '', capacity: 40,
  manager: '', phone: '', openTime: '08:00', closeTime: '22:00', status: 'OPEN',
  description: '', equipment: ''
})

const filtered = computed(() => labs.value.filter((l) =>
  !keyword.value || [l.name, l.code, l.building, l.room].some((v) => (v || '').includes(keyword.value))
))

function labStatusText(s) {
  return { OPEN: '开放中', MAINTENANCE: '维护中', CLOSED: '已关闭' }[s] || s
}

function labTagType(s) {
  return { OPEN: 'success', MAINTENANCE: 'warning', CLOSED: 'info' }[s] || 'info'
}

function currentTagType(s) {
  return {
    使用中: 'danger',
    空闲: 'success',
    即将开始: 'warning',
    维护中: 'info',
    已关闭: 'info'
  }[s] || 'info'
}

async function loadLabs() {
  loading.value = true
  try {
    labs.value = await api.labs()
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

function openEdit(row) {
  if (row) {
    Object.assign(editForm, {
      id: row.id, code: row.code, name: row.name, building: row.building || '',
      room: row.room || '', category: row.category || '', capacity: row.capacity || 40,
      manager: row.manager || '', phone: row.phone || '',
      openTime: row.openTime || '08:00', closeTime: row.closeTime || '22:00',
      status: row.status, description: row.description || '', equipment: row.equipment || ''
    })
  } else {
    Object.assign(editForm, {
      id: null, code: '', name: '', building: '', room: '', category: '', capacity: 40,
      manager: '', phone: '', openTime: '08:00', closeTime: '22:00', status: 'OPEN',
      description: '', equipment: ''
    })
  }
  editVisible.value = true
}

async function submitEdit() {
  if (!editForm.code.trim()) return ElMessage.warning('请填写实验室编号')
  if (!editForm.name.trim()) return ElMessage.warning('请填写实验室名称')
  if (editForm.openTime >= editForm.closeTime) return ElMessage.warning('关闭时间必须晚于开放时间')
  submitting.value = true
  try {
    const payload = { ...editForm, code: editForm.code.trim(), name: editForm.name.trim() }
    if (editForm.id) {
      await api.updateLab(editForm.id, payload)
    } else {
      await api.createLab(payload)
    }
    ElMessage.success('保存成功')
    editVisible.value = false
    loadLabs()
  } catch (e) { /* 拦截器已提示 */ } finally {
    submitting.value = false
  }
}

async function onStatusCmd(cmd, row) {
  try {
    await ElMessageBox.confirm(
      `确定将「${row.name}」设置为「${labStatusText(cmd)}」吗？`,
      '变更开放状态',
      { type: 'warning' }
    )
    await api.updateLabStatus(row.id, cmd)
    ElMessage.success('状态已更新')
    loadLabs()
  } catch (e) { /* 取消或错误 */ }
}

async function doDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除实验室「${row.name}」吗？`, '删除实验室', { type: 'warning' })
    await api.deleteLab(row.id)
    ElMessage.success('已删除')
    loadLabs()
  } catch (e) { /* 取消或错误 */ }
}

onMounted(loadLabs)
</script>

<style scoped>
/* 操作列：主题按钮等距排列 */
.op-cell {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}

.op-cell :deep(.el-dropdown) {
  margin: 0;
}
</style>

<style>
/* 状态切换弹层：teleport 到 body，需全局样式 */
.status-pop.el-popper {
  border-radius: 12px;
  border: 1px solid rgba(35, 44, 90, 0.08);
  box-shadow: 0 14px 40px rgba(23, 32, 74, 0.16);
  padding: 6px;
  min-width: 148px;
}

.status-pop .el-popper__arrow {
  display: none;
}

.status-pop .el-dropdown-menu__item {
  border-radius: 6px;
  padding: 8px 12px;
  margin: 2px 3px;
  font-size: 13.5px;
  color: #45536b;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: background 0.15s ease, color 0.15s ease;
}

.status-pop .el-dropdown-menu__item:not(.is-disabled):hover {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.status-pop .el-dropdown-menu__item.is-disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.status-pop .st-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
