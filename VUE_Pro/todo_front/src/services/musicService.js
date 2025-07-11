import axios from 'axios'

// 创建axios实例
const api = axios.create({
  baseURL: 'http://localhost:8080/api/music',
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
    console.error('音乐API请求错误:', error)
    return Promise.reject(error)
  }
)

export const musicService = {
  /**
   * 获取音乐文件列表
   */
  async getMusicList() {
    try {
      const response = await api.get('/list')
      return response.data
    } catch (error) {
      console.error('获取音乐列表错误:', error)
      throw new Error(error.response?.data?.message || '获取音乐列表失败')
    }
  },

  /**
   * 获取音乐文件URL
   */
  getMusicUrl(fileName) {
    return `http://localhost:8080/api/music/file/${fileName}`
  }
}
