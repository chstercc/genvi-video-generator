import axios from 'axios'

// 创建axios实例
const api = axios.create({
  baseURL: 'http://localhost:8080/api/auth',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 添加token到请求头
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 处理响应和错误
api.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      // Token过期或无效，清除本地存储并跳转到登录页
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export const authService = {
  // 用户注册
  async register(userData) {
    try {
      const response = await api.post('/register', userData)
      return {
        success: true,
        data: response.data
      }
    } catch (error) {
      return {
        success: false,
        message: error.response?.data?.message || '注册失败',
        errors: error.response?.data?.errors || []
      }
    }
  },

  // 用户登录
  async login(loginData) {
    try {
      const response = await api.post('/login', loginData)
      if (response.data.token) {
        // 保存token和用户信息到localStorage
        localStorage.setItem('token', response.data.token)
        localStorage.setItem('user', JSON.stringify({
          id: response.data.userId,
          username: response.data.username,
          email: response.data.email,
          role: response.data.role
        }))
      }
      return {
        success: true,
        data: response.data
      }
    } catch (error) {
      return {
        success: false,
        message: error.response?.data?.message || '登录失败'
      }
    }
  },

  // 检查用户名是否存在
  async checkUsername(username) {
    try {
      const response = await api.get(`/check-username/${username}`)
      return {
        success: true,
        exists: response.data
      }
    } catch {
      return {
        success: false,
        message: '检查用户名失败'
      }
    }
  },

  // 检查邮箱是否存在
  async checkEmail(email) {
    try {
      const response = await api.get(`/check-email/${email}`)
      return {
        success: true,
        exists: response.data
      }
    } catch {
      return {
        success: false,
        message: '检查邮箱失败'
      }
    }
  },

  // 登出
  logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  },

  // 获取当前用户信息
  getCurrentUser() {
    const userStr = localStorage.getItem('user')
    return userStr ? JSON.parse(userStr) : null
  },

  // 检查是否已登录
  isAuthenticated() {
    const token = localStorage.getItem('token')
    return !!token
  },

  // 获取token
  getToken() {
    return localStorage.getItem('token')
  },

  // 获取当前用户ID
  getCurrentUserId() {
    const user = this.getCurrentUser()
    return user ? user.id : null
  }
}

export default authService
