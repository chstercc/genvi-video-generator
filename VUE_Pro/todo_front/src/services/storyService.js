import axios from 'axios'

// 创建axios实例
const api = axios.create({
  baseURL: 'http://localhost:8080/api/stories',
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

export const storyService = {
  // 获取当前用户的所有故事
  async getAllStories() {
    try {
      const userId = JSON.parse(localStorage.getItem('user'))?.id
      if (!userId) {
        throw new Error('用户未登录')
      }
      const response = await api.get(`/user/${userId}`)
      return {
        success: true,
        data: response.data
      }
    } catch (error) {
      return {
        success: false,
        message: error.response?.data?.message || '获取故事列表失败',
        error: error.response?.data || error.message
      }
    }
  },

  // 根据ID获取故事
  async getStoryById(id) {
    try {
      const userId = JSON.parse(localStorage.getItem('user'))?.id
      if (!userId) {
        throw new Error('用户未登录')
      }
      const response = await api.get(`/${id}`)
      // 验证故事是否属于当前用户
      if (response.data.userId !== userId) {
        throw new Error('无权访问该故事')
      }
      return {
        success: true,
        data: response.data
      }
    } catch (error) {
      return {
        success: false,
        message: error.response?.data?.message || '获取故事详情失败',
        error: error.response?.data || error.message
      }
    }
  },

  // 创建新故事
  async createStory(storyData) {
    try {
      const userId = JSON.parse(localStorage.getItem('user'))?.id
      if (!userId) {
        throw new Error('用户未登录')
      }
      // 确保设置了用户ID
      storyData.userId = userId
      const response = await api.post('/', storyData)
      return {
        success: true,
        data: response.data
      }
    } catch (error) {
      return {
        success: false,
        message: error.response?.data?.message || '创建故事失败',
        error: error.response?.data || error.message
      }
    }
  },

  // 更新故事
  async updateStory(id, storyData) {
    try {
      const userId = JSON.parse(localStorage.getItem('user'))?.id
      if (!userId) {
        throw new Error('用户未登录')
      }
      // 先检查故事是否属于当前用户
      const checkResponse = await api.get(`/${id}`)
      if (checkResponse.data.userId !== userId) {
        throw new Error('无权修改该故事')
      }
      const response = await api.put(`/${id}`, storyData)
      return {
        success: true,
        data: response.data
      }
    } catch (error) {
      return {
        success: false,
        message: error.response?.data?.message || '更新故事失败',
        error: error.response?.data || error.message
      }
    }
  },

  // 删除故事
  async deleteStory(id) {
    try {
      const userId = JSON.parse(localStorage.getItem('user'))?.id
      if (!userId) {
        throw new Error('用户未登录')
      }
      // 先检查故事是否属于当前用户
      const checkResponse = await api.get(`/${id}`)
      if (checkResponse.data.userId !== userId) {
        throw new Error('无权删除该故事')
      }
      await api.delete(`/${id}`)
      return {
        success: true,
        message: '故事删除成功'
      }
    } catch (error) {
      return {
        success: false,
        message: error.response?.data?.message || '删除故事失败',
        error: error.response?.data || error.message
      }
    }
  },

  // 搜索故事
  async searchStories(title) {
    try {
      const userId = JSON.parse(localStorage.getItem('user'))?.id
      if (!userId) {
        throw new Error('用户未登录')
      }
      const response = await api.get('/search', {
        params: { title, userId }
      })
      return {
        success: true,
        data: response.data
      }
    } catch (error) {
      return {
        success: false,
        message: error.response?.data?.message || '搜索故事失败',
        error: error.response?.data || error.message
      }
    }
  },

  // 获取故事数量
  async getStoryCount() {
    try {
      const response = await api.get('/count')
      return {
        success: true,
        data: response.data
      }
    } catch (error) {
      return {
        success: false,
        message: error.response?.data?.message || '获取故事数量失败',
        error: error.response?.data || error.message
      }
    }
  }
}

export default storyService
