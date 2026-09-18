<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索姓名 / 用户名 / 院系" clearable style="width: 220px" :prefix-icon="Search" />
      <el-select v-model="query.role" placeholder="角色" clearable style="width: 120px">
        <el-option label="管理员" value="ADMIN" />
        <el-option label="教师" value="TEACHER" />
        <el-option label="学生" value="STUDENT" />
      </el-select>
      <el-button :icon="Refresh" @click="loadUsers">刷新</el-button>
      <div class="spacer"></div>
      <el-button type="primary" :icon="Plus" @click="openEdit(null)">新增用户</el-button>
    </div>

    <el-table v-loading="loading" :data="filtered" border stripe>
      <el-table-column label="用户名" width="120">
        <template #default="{ row }">
          <span class="mono">{{ row.username }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column label="角色" width="90">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'TEACHER' ? 'warning' : 'success'">
            {{ roleText(row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="department" label="院系 / 班级" min-width="130" show-overflow-tooltip />
      <el-table-column prop="phone" label="电话" width="125">
        <template #default="{ row }">
          <span class="mono sub">{{ row.phone || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="170" show-overflow-tooltip />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.active ? 'success' : 'info'" size="small">{{ row.active ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="235" fixed="right">
        <template #default="{ row }">
          <div class="op-cell">
            <el-button size="small" plain type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" plain type="warning" @click="doReset(row)">重置密码</el-button>
            <el-button size="small" plain type="danger" :disabled="row.id === auth.user.id" @click="doDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="editVisible" :title="editForm.id ? '编辑用户' : '新增用户'" width="480px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="用户名" required>
          <el-input v-model="editForm.username" :disabled="!!editForm.id" maxlength="50" placeholder="登录账号" />
        </el-form-item>
        <el-form-item v-if="!editForm.id" label="初始密码">
          <el-input v-model="editForm.password" maxlength="20" placeholder="默认 123456" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="editForm.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="editForm.role" style="width: 100%">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="院系/班级">
          <el-input v-model="editForm.department" maxlength="50" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="editForm.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" maxlength="50" />
        </el-form-item>
        <el-form-item label="账号状态">
          <el-switch v-model="editForm.active" active-text="正常" inactive-text="停用" />
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
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import api from '../api'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const query = reactive({ keyword: '', role: '' })
const users = ref([])
const loading = ref(false)

const editVisible = ref(false)
const submitting = ref(false)
const editForm = reactive({
  id: null, username: '', password: '', name: '', role: 'STUDENT',
  department: '', phone: '', email: '', active: true
})

const filtered = computed(() => users.value.filter((u) => {
  if (query.keyword) {
    const k = query.keyword.toLowerCase()
    if (![u.name, u.username, u.department].some((v) => (v || '').toLowerCase().includes(k))) return false
  }
  if (query.role && u.role !== query.role) return false
  return true
}))

function roleText(r) {
  return { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }[r] || r
}

async function loadUsers() {
  loading.value = true
  try {
    users.value = await api.users({})
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

function openEdit(row) {
  if (row) {
    Object.assign(editForm, {
      id: row.id, username: row.username, password: '', name: row.name, role: row.role,
      department: row.department || '', phone: row.phone || '', email: row.email || '',
      active: row.active !== false
    })
  } else {
    Object.assign(editForm, {
      id: null, username: '', password: '', name: '', role: 'STUDENT',
      department: '', phone: '', email: '', active: true
    })
  }
  editVisible.value = true
}

async function submitEdit() {
  if (!editForm.username.trim()) return ElMessage.warning('请填写用户名')
  if (!editForm.name.trim()) return ElMessage.warning('请填写姓名')
  submitting.value = true
  try {
    const payload = {
      username: editForm.username.trim(),
      password: editForm.password || null,
      name: editForm.name.trim(),
      role: editForm.role,
      department: editForm.department.trim(),
      phone: editForm.phone.trim(),
      email: editForm.email.trim(),
      active: editForm.active
    }
    if (editForm.id) {
      await api.updateUser(editForm.id, payload)
    } else {
      await api.createUser(payload)
    }
    ElMessage.success('保存成功')
    editVisible.value = false
    loadUsers()
  } catch (e) { /* 拦截器已提示 */ } finally {
    submitting.value = false
  }
}

async function doReset(row) {
  try {
    await ElMessageBox.confirm(`确定将「${row.name}」的密码重置为 123456 吗？`, '重置密码', { type: 'warning' })
    await api.resetUserPassword(row.id)
    ElMessage.success('密码已重置为 123456')
  } catch (e) { /* 取消或错误 */ }
}

async function doDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.name}（${row.username}）」吗？`, '删除用户', { type: 'warning' })
    await api.deleteUser(row.id)
    ElMessage.success('已删除')
    loadUsers()
  } catch (e) { /* 取消或错误 */ }
}

onMounted(loadUsers)
</script>

<style scoped>
.sub {
  font-size: 12px;
  color: #909399;
}

/* 操作列：主题按钮等距排列 */
.op-cell {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 4px 8px;
}

.op-cell .el-button {
  margin-left: 0;
}
</style>
