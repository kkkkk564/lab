<template>
  <div class="profile-page">
    <div class="page-card profile-card">
      <div class="profile-head">
        <el-avatar :size="76" style="background: #1e3a5f; font-size: 24px; flex-shrink: 0">
          {{ (user.name || '?').slice(-2) }}
        </el-avatar>
        <div class="profile-main">
          <div class="profile-name">
            <h3>{{ user.name }}</h3>
            <el-tag :type="roleTagType" size="small">{{ roleText }}</el-tag>
          </div>
          <div class="profile-meta">
            <span class="meta-item">
              <el-icon><User /></el-icon>{{ user.username }}
            </span>
            <span class="meta-item" v-if="user.department">
              <el-icon><OfficeBuilding /></el-icon>{{ user.department }}
            </span>
            <span class="meta-item" v-if="user.phone">
              <el-icon><Iphone /></el-icon>{{ user.phone }}
            </span>
            <span class="meta-item" v-if="user.email">
              <el-icon><Message /></el-icon>{{ user.email }}
            </span>
          </div>
        </div>
      </div>

      <el-divider />

      <div class="pwd-section">
        <div class="section-title">修改密码</div>
        <el-form :model="pwdForm" label-width="90px" class="pwd-form">
          <el-form-item label="原密码" required>
            <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
          </el-form-item>
          <el-form-item label="新密码" required>
            <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-20 位" />
          </el-form-item>
          <el-form-item label="确认新密码" required>
            <el-input v-model="pwdForm.confirm" type="password" show-password placeholder="再次输入新密码" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="submit">确认修改</el-button>
          </el-form-item>
        </el-form>
        <el-alert type="info" :closable="false" title="密码修改成功后无需重新登录，下次登录请使用新密码。" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Iphone, Message, OfficeBuilding, User } from '@element-plus/icons-vue'
import api from '../api'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const user = computed(() => auth.user || {})
const roleText = computed(() => auth.roleText)
const roleTagType = computed(() =>
  user.value.role === 'ADMIN' ? 'danger' : user.value.role === 'TEACHER' ? 'warning' : 'success'
)

const submitting = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

async function submit() {
  if (!pwdForm.oldPassword) return ElMessage.warning('请输入原密码')
  if (!pwdForm.newPassword || pwdForm.newPassword.length < 6) return ElMessage.warning('新密码长度需为 6-20 位')
  if (pwdForm.newPassword !== pwdForm.confirm) return ElMessage.warning('两次输入的新密码不一致')
  submitting.value = true
  try {
    await api.changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirm = ''
  } catch (e) { /* 拦截器已提示 */ } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 860px;
}

.profile-card {
  padding: 26px 30px;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 6px 4px;
}

.profile-name {
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile-name h3 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.profile-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
  margin-top: 12px;
  color: #606266;
  font-size: 13px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.meta-item .el-icon {
  color: #909399;
}

.pwd-section {
  max-width: 560px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 18px;
}
</style>
