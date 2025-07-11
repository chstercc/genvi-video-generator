<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, SwitchButton, Document, VideoPlay } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/userStore'
import AIAssistant from './components/AIAssistant.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const { isAuthenticated, user } = userStore

// 计算是否应该显示用户信息
const shouldShowUserInfo = computed(() => {
  return isAuthenticated.value && !['/login', '/register'].includes(route.path)
})

// 处理查看我的作品
const handleMyWorks = () => {
  router.push('/my-works')
}

// 处理查看我的脚本
const handleMyScripts = () => {
  router.push('/my-scripts')
}

// 退出登录
const handleLogout = async () => {
  userStore.logout()
  ElMessage.success('退出登录成功')

  await nextTick()  // ⬅️ 确保状态更新完成后再跳转
  router.replace('/login') // ⬅️ replace 更安全，不留历史记录
}

// 导航菜单逻辑
const activeMenu = ref('discovery')

// 根据当前路由更新活跃菜单
watch(() => route.path, (newPath) => {
  if (newPath === '/') {
    activeMenu.value = 'discovery'
  } else if (newPath === '/stories') {
    activeMenu.value = 'stories'
  } else if (newPath === '/my-scripts') {
    activeMenu.value = 'my-scripts'
  } else if (newPath === '/my-works') {
    activeMenu.value = 'my-works'
  }
}, { immediate: true })

// 处理菜单选择
const handleMenuSelect = (index) => {
  if (index === 'discovery') {
    router.push('/')
  } else if (index === 'stories') {
    router.push('/stories')
  } else if (index === 'my-scripts') {
    router.push('/my-scripts')
  } else if (index === 'my-works') {
    router.push('/my-works')
  }
}
</script>

<template>
  <div id="app">
    <!-- Element Plus Header -->
    <el-header v-if="shouldShowUserInfo" class="app-header">
      <div class="header-content">
        <div class="logo">
          <h2>造影 Genvi</h2>
        </div>

        <!-- 导航菜单 -->
        <div class="nav-menu">
          <el-menu
            mode="horizontal"
            :default-active="activeMenu"
            @select="handleMenuSelect"
            class="header-menu"
          >
            <el-menu-item index="discovery">
              <el-icon><VideoPlay /></el-icon>
              <span class="menu-text">视频发现</span>
            </el-menu-item>
            <el-menu-item index="stories">
              <el-icon><Document /></el-icon>
              <span class="menu-text">故事梗概</span>
            </el-menu-item>
            <el-menu-item index="my-scripts">
              <el-icon><Document /></el-icon>
              <span class="menu-text">我的脚本</span>
            </el-menu-item>
            <el-menu-item index="my-works">
              <el-icon><VideoPlay /></el-icon>
              <span class="menu-text">我的作品</span>
            </el-menu-item>
          </el-menu>
        </div>

        <div class="user-info">
          <el-dropdown trigger="click">
            <span class="user-avatar">
              <el-avatar :size="32" style="background-color: green; color: white;">
                {{ user?.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <span class="username">{{ user?.username }}</span>
              <el-icon class="el-icon--right">
                <arrow-down />
              </el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <strong>{{ user?.username }}</strong>
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleMyScripts">
                  <el-icon><Document /></el-icon>
                  我的脚本
                </el-dropdown-item>
                <el-dropdown-item @click="handleMyWorks">
                  <el-icon><VideoPlay /></el-icon>
                  我的作品
                </el-dropdown-item>
                <el-dropdown-item @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <!-- 主要内容区域 -->
    <div class="main-content" :class="{ 'with-header': shouldShowUserInfo }">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
      <AIAssistant />
    </div>
  </div>
</template>

<style scoped>
#app {
  min-height: 100vh;
  background-color: #f5f7fa;
  position: relative;
  overflow-x: hidden; /* 防止水平滚动条 */
}
.el-image-viewer__wrapper {
  z-index: 9999 !important;
  position: fixed !important;
}
.el-image-viewer__canvas {
  z-index: 10000 !important;
  position: relative !important;
}
/* Element Plus Header样式 */
.app-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-bottom: none;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.15);
  height: 100px; /* 增加Header高度 */
  padding: 0;
  overflow: hidden; /* 防止滚动条出现 */
  backdrop-filter: blur(10px);
  position: relative;
}

.app-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%);
  pointer-events: none;
}

.app-header::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.3) 20%,
    rgba(255, 255, 255, 0.8) 50%,
    rgba(255, 255, 255, 0.3) 80%,
    transparent 100%
  );
  animation: shimmer 3s ease-in-out infinite;
}

@keyframes shimmer {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 30px;
  max-width: 1400px;
  margin: 0 auto;
  min-width: 0;
  position: relative;
  z-index: 2;
  overflow: hidden; /* 防止内容溢出 */
}

.logo {
  flex-shrink: 0;
  width: 160px; /* 增加Logo宽度 */
  cursor: pointer;
  transition: all 0.3s ease;
}

.logo:hover {
  transform: scale(1.05);
}

.logo h2 {
  color: white;
  font-weight: 700;
  margin: 0;
  font-size: 28px; /* 增加Logo字体大小 */
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  background: linear-gradient(45deg, #fff, #f0f8ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-menu {
  flex: 1; /* 调整flex权重 */
  display: flex;
  justify-content: center;
  margin: 0 20px;
  min-width: 200px; /* 减少最小宽度 */
  overflow: hidden; /* 防止溢出 */
}

.user-info {
  flex-shrink: 0; /* 防止用户信息被压缩 */
  min-width: 100px; /* 进一步减少用户信息的最小宽度 */
  overflow: hidden; /* 防止溢出 */
}

.header-menu {
  border-bottom: none;
  background: transparent;
  width: 100%;
}

.header-menu .el-menu-item {
  height: 70px; /* 增加按钮高度 */
  line-height: 70px;
  color: rgba(255, 255, 255, 0.85);
  border-bottom: none;
  margin: 0 12px; /* 增加按钮间距 */
  padding: 0 24px; /* 增加内边距 */
  border-radius: 16px; /* 增加圆角 */
  transition: all 0.3s ease;
  white-space: nowrap;
  min-width: auto;
  backdrop-filter: blur(5px);
  position: relative;
  overflow: hidden;
  font-size: 16px; /* 增加字体大小 */
  font-weight: 500;
}

.header-menu .el-menu-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);
  transition: left 0.5s;
}

.header-menu .el-menu-item:hover::before {
  left: 100%;
}

.header-menu .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.15);
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(255, 255, 255, 0.1);
}

.header-menu .el-menu-item.is-active {
  color: #4a5568;
  background: rgba(255, 255, 255, 0.9);
  border-bottom: 3px solid #4a5568;
  font-weight: 600;
  box-shadow: 0 2px 10px rgba(255, 255, 255, 0.3);
}

.header-menu .el-menu-item .el-icon {
  margin-right: 5px;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-avatar {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 16px 20px; /* 增加用户头像区域的内边距 */
  border-radius: 16px; /* 增加圆角 */
  transition: all 0.3s ease;
  backdrop-filter: blur(5px);
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.user-avatar:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(255, 255, 255, 0.1);
}

.username {
  margin-left: 10px;
  margin-right: 6px;
  font-weight: 600;
  color: white;
  font-size: 18px; /* 增加用户名字体大小 */
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

/* 主要内容区域 */
.main-content {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

/* 当有Header时，主要内容区域需要留出Header的高度 */
.main-content.with-header {
  min-height: calc(100vh - 100px);
  padding-top: 100px;
  margin-top: -100px; /* 让内容背景延伸到Header下方 */
}

/* 强制菜单显示所有项目 */
:deep(.el-menu--horizontal) {
  display: flex !important;
  width: 100% !important;
}

:deep(.el-menu--horizontal .el-menu-item) {
  flex-shrink: 0 !important; /* 防止菜单项被压缩 */
  white-space: nowrap !important; /* 防止文字换行 */
}

/* 下拉菜单样式 */
:deep(.el-dropdown-menu) {
  min-width: 280px !important;
  padding: 12px 0;
}

:deep(.el-dropdown-menu__item) {
  padding: 12px 20px;
  font-size: 15px;
}

:deep(.el-dropdown-menu__item:first-child) {
  color: #409eff;
  font-weight: 600;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 8px;
}

:deep(.el-dropdown-menu__item .el-icon) {
  margin-right: 10px;
}

/* 页面切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式设计 */

/* 中等屏幕：优化空间分配 */
@media (max-width: 1024px) {
  .header-content {
    padding: 0 20px;
  }

  .logo {
    width: 100px;
  }

  .logo h2 {
    font-size: 20px;
  }

  .nav-menu {
    margin: 0 15px;
    max-width: 300px;
  }

  .username {
    font-size: 14px;
  }
}

/* 小屏幕：隐藏用户名 */
@media (max-width: 900px) {
  .username {
    display: none;
  }

  .user-info {
    min-width: 60px;
  }
}

/* 更小屏幕：缩短菜单文字 */
@media (max-width: 768px) {
  .app-header {
    height: 70px;
  }

  .header-content {
    padding: 0 15px;
  }

  .logo {
    width: 80px;
  }

  .logo h2 {
    font-size: 18px;
  }

  .nav-menu {
    margin: 0 10px;
    max-width: 250px;
  }

  .header-menu .el-menu-item {
    height: 50px;
    line-height: 50px;
    font-size: 14px;
    margin: 0 3px;
    padding: 0 8px;
  }

  .main-content.with-header {
    min-height: calc(100vh - 70px);
    padding-top: 70px;
  }
}

/* 手机屏幕：只显示图标 */
@media (max-width: 480px) {
  .header-content {
    padding: 0 10px;
  }

  .logo {
    width: 70px;
  }

  .logo h2 {
    font-size: 16px;
  }

  .nav-menu {
    margin: 0 5px;
  }

  .header-menu .el-menu-item {
    margin: 0 2px;
    padding: 0 6px;
    min-width: 44px; /* 保证触摸目标足够大 */
  }

  .header-menu .el-menu-item .menu-text {
    display: none; /* 只显示图标 */
  }

  .user-info {
    min-width: 44px;
  }
}

</style>
