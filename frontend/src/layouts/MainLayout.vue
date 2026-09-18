<template>
  <el-container class="layout">
    <el-aside width="230px" class="aside">
      <div class="logo">
        <div class="logo-badge">Lab</div>
        <div class="logo-text">
          <div class="t1">实验室预约与监控</div>
          <div class="t2">CAMPUS LAB SYSTEM</div>
        </div>
      </div>
      <el-menu
        :default-active="activePath"
        router
        background-color="transparent"
        text-color="#45536b"
        active-text-color="#1e3a5f"
        class="menu">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-foot">
        <div class="aside-foot-line"></div>
        <span>v1.0 · 前后端分离</span>
      </div>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <div class="header-title">{{ ($route.meta && $route.meta.title) || '' }}</div>
          <span class="header-clock mono">{{ clock }}</span>
        </div>
        <el-dropdown @command="onCommand" popper-class="user-pop">
          <span class="user-chip">
            <el-avatar :size="32" class="user-avatar">{{ initials }}</el-avatar>
            <span class="user-name">{{ user.name || '' }}</span>
            <el-tag size="small" :type="roleTagType" effect="light" round>{{ roleText }}</el-tag>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <li class="user-pop-head">
                <el-avatar :size="40" class="up-avatar">{{ initials }}</el-avatar>
                <div class="up-info">
                  <div class="up-name">
                    {{ user.name }}
                    <el-tag size="small" :type="roleTagType" effect="light" round>{{ roleText }}</el-tag>
                  </div>
                  <div class="up-sub mono">@{{ user.username || '—' }}</div>
                </div>
              </li>
              <el-dropdown-item command="profile">
                <el-icon><UserFilled /></el-icon>个人中心
              </el-dropdown-item>
              <el-dropdown-item command="logout" class="danger" divided>
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { SwitchButton, UserFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const user = computed(() => auth.user || {})
const roleText = computed(() => auth.roleText)
const activePath = computed(() => route.path)
const roleTagType = computed(() =>
  user.value.role === 'ADMIN' ? 'danger' : user.value.role === 'TEACHER' ? 'warning' : 'success'
)
const initials = computed(() => (user.value.name || '?').slice(-2))
const role = computed(() => user.value.role)

/* 顶栏实时时钟：监控系统的仪式感细节 */
const clock = ref('')
let clockTimer = null
function tick() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  clock.value = `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} 周${week} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const allMenus = [
  { path: '/monitor', title: '实时监控', icon: 'Monitor' },
  { path: '/labs', title: '实验室预约', icon: 'OfficeBuilding' },
  { path: '/my-reservations', title: '我的预约', icon: 'Calendar' },
  { path: '/approvals', title: '预约审批', icon: 'Stamp', roles: ['ADMIN'] },
  { path: '/devices', title: '设备管理', icon: 'Cpu' },
  { path: '/lab-manage', title: '实验室管理', icon: 'Setting', roles: ['ADMIN'] },
  { path: '/announcements', title: '公告通知', icon: 'Bell' },
  { path: '/users', title: '用户管理', icon: 'User', roles: ['ADMIN'] },
  { path: '/profile', title: '个人中心', icon: 'UserFilled' }
]
const menus = computed(() => allMenus.filter((m) => !m.roles || m.roles.includes(role.value)))

function onCommand(cmd) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
      .then(() => {
        auth.logout()
        router.push('/login')
      })
      .catch(() => {})
  } else if (cmd === 'profile') {
    router.push('/profile')
  }
}

onMounted(() => {
  tick()
  clockTimer = setInterval(tick, 1000)
})
onBeforeUnmount(() => {
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<style scoped>
.layout {
  height: 100%;
}

/* 侧栏：浅灰白 + 细分隔线，接近真实校内业务系统 */
.aside {
  background: #fbfcfd;
  border-right: 1px solid var(--ink-line);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.logo {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 20px 18px 18px;
  border-bottom: 1px solid var(--ink-line);
}

/* 品牌徽章：藏青实色，机构感 */
.logo-badge {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #1e3a5f;
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  flex-shrink: 0;
}

.logo-text .t1 {
  color: #1c2430;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.logo-text .t2 {
  color: #8592a3;
  font-size: 10px;
  letter-spacing: 1.4px;
  margin-top: 3px;
}

.menu {
  border-right: none;
  flex: 1;
  padding: 10px 0;
  overflow-y: auto;
}

/* 菜单项：选中态 = 主色竖条 + 浅藏青底 */
.menu :deep(.el-menu-item) {
  margin: 2px 10px;
  border-radius: 6px;
  height: 44px;
  line-height: 44px;
  position: relative;
  transition: background 0.15s ease, color 0.15s ease;
}

.menu :deep(.el-menu-item:hover) {
  background: #f0f3f7 !important;
}

.menu :deep(.el-menu-item.is-active) {
  background: var(--el-color-primary-light-9) !important;
  font-weight: 600;
}

.menu :deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: -10px;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--el-color-primary);
}

.aside-foot {
  padding: 14px 0 16px;
  text-align: center;
  color: #98a1ae;
  font-size: 11px;
}

.aside-foot-line {
  width: 36px;
  height: 2px;
  background: #d2d5df;
  margin: 0 auto 10px;
  border-radius: 2px;
}

/* 顶栏：毛玻璃 + 实时时钟 */
.header {
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: saturate(160%) blur(10px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--ink-line);
  position: relative;
  z-index: 5;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 14px;
}

.header-title {
  font-size: 16.5px;
  font-weight: 600;
  color: var(--text-1);
}

.header-clock {
  font-size: 12px;
  color: var(--text-2);
  letter-spacing: 0.5px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 9px;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 10px;
  transition: background 0.2s ease;
  outline: none;
}

.user-chip:hover { background: #f0f3f7; }

.user-avatar {
  background: #1e3a5f;
  font-size: 13px;
  flex-shrink: 0;
}

.user-name {
  font-size: 14px;
  color: var(--text-1);
  font-weight: 500;
}

.main {
  background: var(--app-bg);
  padding: 18px 20px;
  overflow-y: auto;
}
</style>

<style>
/* 用户下拉弹层：teleport 到 body 下，需用全局样式定制 */
.user-pop.el-popper {
  border-radius: 10px;
  border: 1px solid rgba(28, 36, 48, 0.08);
  box-shadow: 0 10px 30px rgba(28, 36, 48, 0.14);
  padding: 6px;
  min-width: 216px;
}

.user-pop .el-popper__arrow {
  display: none;
}

/* 弹层头部：当前登录用户信息卡 */
.user-pop .user-pop-head {
  list-style: none;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  margin: 0 0 4px;
  border-radius: 8px;
  background: #f6f7f9;
  border-bottom: 1px solid rgba(28, 36, 48, 0.06);
}

.user-pop .up-avatar {
  background: #1e3a5f;
  font-size: 14px;
  flex-shrink: 0;
}

.user-pop .up-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #1c2430;
}

.user-pop .up-sub {
  font-size: 11.5px;
  color: #5d6875;
  margin-top: 3px;
}

/* 菜单项：圆角胶囊 + 主题色悬停 */
.user-pop .el-dropdown-menu__item {
  border-radius: 6px;
  padding: 9px 12px;
  margin: 2px 3px;
  font-size: 13.5px;
  color: #45536b;
  transition: background 0.15s ease, color 0.15s ease;
}

.user-pop .el-dropdown-menu__item .el-icon {
  margin-right: 8px;
  color: #64748b;
  transition: color 0.18s ease;
}

.user-pop .el-dropdown-menu__item:not(.is-disabled):hover,
.user-pop .el-dropdown-menu__item:not(.is-disabled):focus {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.user-pop .el-dropdown-menu__item:not(.is-disabled):hover .el-icon,
.user-pop .el-dropdown-menu__item:not(.is-disabled):focus .el-icon {
  color: var(--el-color-primary);
}

/* 退出登录：红色警示态 */
.user-pop .el-dropdown-menu__item.danger:not(.is-disabled):hover,
.user-pop .el-dropdown-menu__item.danger:not(.is-disabled):focus {
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
}

.user-pop .el-dropdown-menu__item.danger:not(.is-disabled):hover .el-icon,
.user-pop .el-dropdown-menu__item.danger:not(.is-disabled):focus .el-icon {
  color: var(--el-color-danger);
}

/* 分隔线：替代 EP 默认伪元素细线 */
.user-pop .el-dropdown-menu__item--divided {
  margin-top: 4px;
  border-top: 1px solid rgba(28, 36, 48, 0.07);
}

.user-pop .el-dropdown-menu__item--divided::before {
  display: none;
}
</style>
