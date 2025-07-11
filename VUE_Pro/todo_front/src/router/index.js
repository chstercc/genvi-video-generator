import { createRouter, createWebHistory } from 'vue-router'
import userStore from '@/stores/userStore'

// 路由组件
const LoginPage = () => import('@/components/LoginPage.vue')
const RegisterPage = () => import('@/components/RegisterPage.vue')
const VideoDiscoveryPage = () => import('@/components/VideoDiscoveryPage.vue')
const StoryPage = () => import('@/components/StoryPage.vue')
const StoryboardPage = () => import('@/components/StoryboardPage.vue')
const VideoGenerationPage = () => import('@/components/VideoGenerationPage.vue')
const FinalVideoPage = () => import('@/components/FinalVideoPage.vue')
const MyScriptsPage = () => import('@/components/MyScriptsPage.vue')
const MyWorksPage = () => import('@/components/MyWorksPage.vue')

// 路由配置
const routes = [
  {
    path: '/',
    name: 'Home',
    component: VideoDiscoveryPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginPage,
    meta: { requiresGuest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: RegisterPage,
    meta: { requiresGuest: true }
  },
  {
    path: '/stories',
    name: 'StoryPage',
    component: StoryPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/storyboard',
    name: 'StoryboardPage',
    component: StoryboardPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/video-generation',
    name: 'VideoGenerationPage',
    component: VideoGenerationPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/final-video',
    name: 'FinalVideoPage',
    component: FinalVideoPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/my-scripts',
    name: 'MyScriptsPage',
    component: MyScriptsPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/my-works',
    name: 'MyWorksPage',
    component: MyWorksPage,
    meta: { requiresAuth: true }
  },
  // 404 页面重定向
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  // 确保用户状态已初始化
  userStore.methods.init()

  const isAuthenticated = userStore.computed.isAuthenticated.value

  // 检查是否需要认证
  if (to.meta.requiresAuth && !isAuthenticated) {
    // 未认证用户访问需要认证的页面，重定向到登录页
    next({
      name: 'Login',
      query: { redirect: to.fullPath }
    })
  } else if (to.meta.requiresGuest && isAuthenticated) {
    // 已认证用户访问登录/注册页面，重定向到首页
    next({ name: 'Home' })
  } else {
    // 正常导航
    next()
  }
})

// 全局后置钩子
router.afterEach((to) => {
  // 更新页面标题
  const baseTitle = 'AI短片制作助手'
  let title = baseTitle

  switch (to.name) {
    case 'Login':
      title = '登录 - ' + baseTitle
      break
    case 'Register':
      title = '注册 - ' + baseTitle
      break
    case 'Home':
      title = '视频发现 - ' + baseTitle
      break
    case 'StoryPage':
      title = '故事梗概 - ' + baseTitle
      break
    case 'StoryboardPage':
      title = '分镜头脚本 - ' + baseTitle
      break
    case 'VideoGenerationPage':
      title = '图生视频 - ' + baseTitle
      break
    case 'FinalVideoPage':
      title = '最终作品 - ' + baseTitle
      break
    case 'MyScriptsPage':
      title = '我的脚本 - ' + baseTitle
      break
    case 'MyWorksPage':
      title = '我的作品 - ' + baseTitle
      break
  }

  document.title = title
})

export default router
