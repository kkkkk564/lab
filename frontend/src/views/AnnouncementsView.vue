<template>
  <div v-loading="loading">
    <div class="toolbar" style="margin-bottom: 14px">
      <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      <div class="spacer"></div>
      <el-button v-if="auth.isAdmin" type="primary" :icon="Plus" @click="openEdit(null)">发布公告</el-button>
    </div>

    <div v-for="a in list" :key="a.id" class="ann-card" :class="{ pinned: a.pinned }" @click="openDetail(a)">
      <div class="ann-head">
        <div class="ann-title">
          <el-tag v-if="a.pinned" type="danger" size="small" effect="dark">置顶</el-tag>
          <b>{{ a.title }}</b>
        </div>
        <div class="ann-actions" v-if="auth.isAdmin" @click.stop>
          <el-button size="small" plain type="primary" @click="openEdit(a)">编辑</el-button>
          <el-button size="small" plain type="danger" @click="doDelete(a)">删除</el-button>
        </div>
      </div>
      <div class="ann-content">{{ a.content }}</div>
      <div class="ann-foot">
        <span>{{ a.publisher }}</span>
        <span class="mono">{{ formatTime(a.createdAt) }}</span>
      </div>
    </div>
    <el-empty v-if="list.length === 0" description="暂无公告" />

    <el-dialog v-model="editVisible" :title="editForm.id ? '编辑公告' : '发布公告'" width="560px">
      <el-form :model="editForm" label-width="70px">
        <el-form-item label="标题" required>
          <el-input v-model="editForm.title" maxlength="150" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="editForm.content" type="textarea" :rows="7" maxlength="2000" />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="editForm.pinned" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" width="640px">
      <template #header>
        <div class="detail-title">
          <el-tag v-if="detail.pinned" type="danger" size="small" effect="dark">置顶</el-tag>
          <b>{{ detail.title }}</b>
        </div>
      </template>
      <div class="detail-meta">
        <span>{{ detail.publisher }}</span>
        <span class="mono">{{ formatTime(detail.createdAt) }}</span>
      </div>
      <div class="detail-content">{{ detail.content || '（无正文）' }}</div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import api from '../api'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const list = ref([])
const loading = ref(false)

const editVisible = ref(false)
const submitting = ref(false)
const editForm = reactive({ id: null, title: '', content: '', pinned: false })

function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

const detailVisible = ref(false)
const detail = ref({})

function openDetail(a) {
  detail.value = a
  detailVisible.value = true
}

async function loadList() {
  loading.value = true
  try {
    list.value = await api.announcements()
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

function openEdit(row) {
  if (row) {
    Object.assign(editForm, {
      id: row.id, title: row.title, content: row.content || '', pinned: !!row.pinned
    })
  } else {
    Object.assign(editForm, { id: null, title: '', content: '', pinned: false })
  }
  editVisible.value = true
}

async function submitEdit() {
  if (!editForm.title.trim()) return ElMessage.warning('请填写公告标题')
  submitting.value = true
  try {
    const payload = { title: editForm.title.trim(), content: editForm.content.trim(), pinned: editForm.pinned }
    if (editForm.id) {
      await api.updateAnnouncement(editForm.id, payload)
    } else {
      await api.createAnnouncement(payload)
    }
    ElMessage.success('保存成功')
    editVisible.value = false
    loadList()
  } catch (e) { /* 拦截器已提示 */ } finally {
    submitting.value = false
  }
}

async function doDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '删除公告', { type: 'warning' })
    await api.deleteAnnouncement(row.id)
    ElMessage.success('已删除')
    loadList()
  } catch (e) { /* 取消或错误 */ }
}

onMounted(loadList)
</script>

<style scoped>
.ann-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px 18px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.ann-card:hover {
  border-color: #d2d5df;
  box-shadow: 0 2px 8px rgba(28, 36, 48, 0.08);
}

/* 置顶公告：琥珀左侧标识条，与普通公告形成层级 */
.ann-card.pinned {
  border-left: 3px solid var(--accent);
}

.ann-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.ann-title {
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.ann-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.ann-actions .el-button {
  margin-left: 0;
}

.ann-content {
  color: #606266;
  font-size: 13px;
  line-height: 1.8;
  margin: 10px 0;
  white-space: pre-wrap;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ann-foot {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 12px;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
}

.detail-meta {
  display: flex;
  gap: 14px;
  color: #909399;
  font-size: 12px;
  margin-bottom: 14px;
}

.detail-content {
  color: #303133;
  font-size: 14px;
  line-height: 1.9;
  white-space: pre-wrap;
  max-height: 55vh;
  overflow-y: auto;
}
</style>
