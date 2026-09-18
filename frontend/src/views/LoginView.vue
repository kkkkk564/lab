<template>
  <div class="login-page">
    <canvas ref="bgCanvas" class="bg-canvas"></canvas>
    <div class="grid-layer"></div>
    <div class="scanline"></div>

    <div class="login-card">
      <div class="card-glowline"></div>

      <div class="brand">
        <div class="brand-badge">Lab</div>
        <h1>校园实验室资源预约与状态监控</h1>
        <p class="sub">CAMPUS LAB SYSTEM</p>
      </div>

      <el-form :model="form" size="large" @submit.prevent="doLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
            @keyup.enter="doLogin" />
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="doLogin">
          登录
        </el-button>
      </el-form>

      <div class="demo-tip"><span>演示账号 · 密码均为 123456</span></div>
      <div class="quick">
        <el-button plain size="small" @click="fill('admin')">管理员 admin</el-button>
        <el-button plain size="small" @click="fill('teacher1')">教师 teacher1</el-button>
        <el-button plain size="small" @click="fill('student1')">学生 student1</el-button>
      </div>
    </div>

    <div class="footer">© 2026 实验教学与设备管理中心</div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

function fill(username) {
  form.username = username
  form.password = '123456'
}

async function doLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const user = await auth.login({ ...form })
    ElMessage.success(`欢迎回来，${user.name}`)
    router.push('/monitor')
  } catch (e) {
    /* 错误已由拦截器提示 */
  } finally {
    loading.value = false
  }
}

/* ========== 背景粒子网络（Canvas 2D，零依赖） ==========
   交互：鼠标移动吸附连线 / 点击产生扩散波纹
   可访问性：prefers-reduced-motion 时只渲染静态一帧 */
const bgCanvas = ref(null)
let cleanupBg = null

function setupBg() {
  const el = bgCanvas.value
  if (!el) return
  const ctx = el.getContext('2d')
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches

  let W = 0
  let H = 0
  let rafId = 0
  let particles = []
  const ripples = []
  const mouse = { x: -9999, y: -9999 }

  function resize() {
    const dpr = Math.min(window.devicePixelRatio || 1, 2)
    W = el.clientWidth
    H = el.clientHeight
    el.width = Math.round(W * dpr)
    el.height = Math.round(H * dpr)
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    const n = Math.min(110, Math.max(40, Math.round((W * H) / 14000)))
    particles = Array.from({ length: n }, () => ({
      x: Math.random() * W,
      y: Math.random() * H,
      vx: (Math.random() - 0.5) * 0.35,
      vy: (Math.random() - 0.5) * 0.35,
      r: Math.random() * 1.5 + 0.6
    }))
  }

  function drawFrame() {
    ctx.clearRect(0, 0, W, H)
    const LINK = 130
    for (let i = 0; i < particles.length; i++) {
      const a = particles[i]
      for (let j = i + 1; j < particles.length; j++) {
        const b = particles[j]
        const dx = a.x - b.x
        const dy = a.y - b.y
        const d2 = dx * dx + dy * dy
        if (d2 < LINK * LINK) {
          const t = 1 - Math.sqrt(d2) / LINK
          ctx.strokeStyle = `rgba(96, 168, 224, ${0.13 * t})`
          ctx.lineWidth = 1
          ctx.beginPath()
          ctx.moveTo(a.x, a.y)
          ctx.lineTo(b.x, b.y)
          ctx.stroke()
        }
      }
    }
    const MR = 170
    for (const p of particles) {
      const dx = p.x - mouse.x
      const dy = p.y - mouse.y
      const d2 = dx * dx + dy * dy
      if (d2 < MR * MR) {
        const t = 1 - Math.sqrt(d2) / MR
        ctx.strokeStyle = `rgba(130, 205, 255, ${0.38 * t})`
        ctx.lineWidth = 1
        ctx.beginPath()
        ctx.moveTo(p.x, p.y)
        ctx.lineTo(mouse.x, mouse.y)
        ctx.stroke()
      }
    }
    ctx.fillStyle = 'rgba(140, 196, 240, 0.8)'
    for (const p of particles) {
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fill()
    }
    for (let k = ripples.length - 1; k >= 0; k--) {
      const rp = ripples[k]
      rp.r += 2.8
      rp.a *= 0.945
      if (rp.a < 0.02) {
        ripples.splice(k, 1)
        continue
      }
      ctx.strokeStyle = `rgba(130, 205, 255, ${rp.a})`
      ctx.lineWidth = 1.2
      ctx.beginPath()
      ctx.arc(rp.x, rp.y, rp.r, 0, Math.PI * 2)
      ctx.stroke()
    }
  }

  function tick() {
    for (const p of particles) {
      p.x += p.vx
      p.y += p.vy
      if (p.x < -24) p.x = W + 24
      if (p.x > W + 24) p.x = -24
      if (p.y < -24) p.y = H + 24
      if (p.y > H + 24) p.y = -24
    }
    drawFrame()
    rafId = requestAnimationFrame(tick)
  }

  function onMove(e) {
    const rect = el.getBoundingClientRect()
    mouse.x = e.clientX - rect.left
    mouse.y = e.clientY - rect.top
  }

  function onLeave() {
    mouse.x = -9999
    mouse.y = -9999
  }

  function onClick(e) {
    const rect = el.getBoundingClientRect()
    ripples.push({ x: e.clientX - rect.left, y: e.clientY - rect.top, r: 4, a: 0.5 })
    if (ripples.length > 6) ripples.shift()
  }

  resize()
  drawFrame()
  if (!reduced) rafId = requestAnimationFrame(tick)

  const onResize = () => {
    resize()
    if (reduced) drawFrame()
  }
  window.addEventListener('resize', onResize)

  const page = el.parentElement
  if (!reduced) {
    page.addEventListener('mousemove', onMove)
    page.addEventListener('mouseleave', onLeave)
    page.addEventListener('click', onClick)
  }

  cleanupBg = () => {
    cancelAnimationFrame(rafId)
    window.removeEventListener('resize', onResize)
    if (!reduced) {
      page.removeEventListener('mousemove', onMove)
      page.removeEventListener('mouseleave', onLeave)
      page.removeEventListener('click', onClick)
    }
  }
}

onMounted(setupBg)
onBeforeUnmount(() => cleanupBg && cleanupBg())
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(1100px 620px at 72% 16%, rgba(46, 110, 180, 0.16), transparent 60%),
    radial-gradient(900px 560px at 16% 84%, rgba(30, 90, 150, 0.12), transparent 60%),
    #0a1526;
}

/* 粒子画布：CSS 尺寸必须锁定 100%，否则高 DPI 屏上画布会放大 dpr 倍导致"铺不满" */
.bg-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

/* 细网格：向边缘淡出，营造空间进深 */
.grid-layer {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(110, 183, 232, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(110, 183, 232, 0.05) 1px, transparent 1px);
  background-size: 44px 44px;
  -webkit-mask-image: radial-gradient(ellipse at center, #000 28%, transparent 76%);
  mask-image: radial-gradient(ellipse at center, #000 28%, transparent 76%);
}

/* 扫描光带：缓慢下移的 HUD 氛围层 */
.scanline {
  position: absolute;
  left: 0;
  right: 0;
  top: -140px;
  height: 120px;
  z-index: 1;
  pointer-events: none;
  background: linear-gradient(180deg, transparent, rgba(120, 190, 255, 0.05), transparent);
  animation: scan 12s linear infinite;
}

@keyframes scan {
  from { top: -140px; }
  to { top: 110%; }
}

/* 白色透明玻璃卡片：高透白 + 强模糊，粒子网络从卡片后清晰透出 */
.login-card {
  width: 404px;
  background: rgba(255, 255, 255, 0.09);
  backdrop-filter: blur(30px) saturate(1.6) brightness(1.05);
  -webkit-backdrop-filter: blur(30px) saturate(1.6) brightness(1.05);
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 26px;
  padding: 38px 38px 26px;
  box-shadow:
    0 20px 60px rgba(2, 8, 20, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.4),
    inset 0 -1px 0 rgba(255, 255, 255, 0.08);
  position: relative;
  z-index: 2;
}

/* 卡顶引导线 */
.card-glowline {
  position: absolute;
  top: -1px;
  left: 10%;
  right: 10%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.65), transparent);
  pointer-events: none;
}

.brand {
  text-align: center;
  margin-bottom: 28px;
}

.brand-badge {
  width: 56px;
  height: 56px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.35);
  color: #fff;
  font-family: Consolas, Monaco, monospace;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.25);
}

h1 {
  font-size: 19px;
  color: #fff;
  margin: 0 0 8px;
  letter-spacing: 0.5px;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.35);
}

.sub {
  font-family: Consolas, Monaco, monospace;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
  letter-spacing: 3px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
}

/* 表单输入框：白色玻璃质感 */
:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.1) !important;
  border: 1px solid rgba(255, 255, 255, 0.22) !important;
  border-radius: 12px !important;
  box-shadow: none !important;
  transition: border-color 0.25s, background 0.25s;
}
:deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.38) !important;
}
:deep(.el-input__wrapper.is-focus) {
  border-color: rgba(255, 255, 255, 0.6) !important;
  background: rgba(255, 255, 255, 0.15) !important;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.12) !important;
}
:deep(.el-input__inner) {
  color: #fff !important;
  height: 42px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
}
:deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.45);
}
:deep(.el-input__prefix-inner) {
  color: rgba(255, 255, 255, 0.55);
}
:deep(.el-input__suffix-inner) {
  color: rgba(255, 255, 255, 0.45);
}
:deep(.el-input__suffix-inner:hover) {
  color: rgba(255, 255, 255, 0.75);
}

.login-btn {
  width: 100%;
  margin-top: 4px;
  height: 44px;
  font-size: 15px;
  letter-spacing: 1px;
}

/* 演示账号分隔 */
.demo-tip {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  margin: 20px 0 12px;
}
.demo-tip::before,
.demo-tip::after {
  content: '';
  flex: 1;
  height: 1px;
  background: rgba(255, 255, 255, 0.18);
}

.quick {
  display: flex;
  justify-content: center;
  gap: 8px;
}

/* 快捷按钮白色玻璃 */
.quick :deep(.el-button) {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.22);
  color: rgba(255, 255, 255, 0.75);
}
.quick :deep(.el-button:hover) {
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.4);
  color: #fff;
}

.footer {
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;
  margin-top: 24px;
  position: relative;
  z-index: 2;
}

/* 减弱动态效果：冻结扫描带（粒子由脚本控制为静态一帧） */
@media (prefers-reduced-motion: reduce) {
  .scanline { animation: none; opacity: 0; }
}
</style>
