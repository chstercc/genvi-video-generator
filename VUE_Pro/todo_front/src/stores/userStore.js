import { reactive, computed } from 'vue'
import authService from '@/services/authService'

// 用户状态
const state = reactive({
  user: null,
  isAuthenticated: false,
  isLoading: false
})

// 用户store
export const userStore = {
  // 状态
  state,

  // 计算属性
  computed: {
    isAuthenticated: computed(() => state.isAuthenticated),
    user: computed(() => state.user),
    isLoading: computed(() => state.isLoading)
  },

  // 方法
  methods: {
    // 设置用户信息
    setUser(user) {
      state.user = user
      state.isAuthenticated = !!user
    },

    // 清除用户信息
    clearUser() {
      state.user = null
      state.isAuthenticated = false
    },

    // 设置加载状态
    setLoading(loading) {
      state.isLoading = loading
    },

    // 初始化用户状态（从localStorage恢复）
    init() {
      const user = authService.getCurrentUser()
      const isAuthenticated = authService.isAuthenticated()

      if (user && isAuthenticated) {
        this.setUser(user)
      } else {
        this.clearUser()
      }
    },

    // 登录
    async login(loginData) {
      this.setLoading(true)
      try {
        const result = await authService.login(loginData)
        if (result.success) {
          this.setUser({
            id: result.data.userId,
            username: result.data.username,
            email: result.data.email,
            role: result.data.role
          })
        }
        return result
      } finally {
        this.setLoading(false)
      }
    },

    // 注册
    async register(registerData) {
      this.setLoading(true)
      try {
        const result = await authService.register(registerData)
        if (result.success) {
          this.setUser({
            id: result.data.userId,
            username: result.data.username,
            email: result.data.email,
            role: result.data.role
          })
        }
        return result
      } finally {
        this.setLoading(false)
      }
    },

    // 登出
    logout() {
      authService.logout()
      this.clearUser()
    },

    // 检查用户名是否存在
    async checkUsername(username) {
      return await authService.checkUsername(username)
    },

    // 检查邮箱是否存在
    async checkEmail(email) {
      return await authService.checkEmail(email)
    },

    // 获取当前用户ID
    getCurrentUserId() {
      return state.user ? state.user.id : null
    }
  }
}

// 导出使用store的组合式函数
export function useUserStore() {
  return {
    // 状态
    user: userStore.computed.user,
    isAuthenticated: userStore.computed.isAuthenticated,
    isLoading: userStore.computed.isLoading,

    // 方法
    setUser: userStore.methods.setUser,
    clearUser: userStore.methods.clearUser,
    setLoading: userStore.methods.setLoading,
    init: userStore.methods.init,
    login: userStore.methods.login,
    register: userStore.methods.register,
    logout: userStore.methods.logout,
    checkUsername: userStore.methods.checkUsername,
    checkEmail: userStore.methods.checkEmail,
    getCurrentUserId: userStore.methods.getCurrentUserId
  }
}

export default userStore
