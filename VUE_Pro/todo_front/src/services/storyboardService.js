import axios from 'axios'

// 创建axios实例
const api = axios.create({
  baseURL: 'http://localhost:8080/api/storyboards',
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
    console.error('API请求错误:', error)
    console.error('错误响应:', error.response)
    console.error('错误状态码:', error.response?.status)
    console.error('错误信息:', error.response?.data)

    // 暂时注释掉401跳转，方便调试
    // if (error.response?.status === 401) {
    //   console.error('收到401错误，即将跳转到登录页面')
    //   localStorage.removeItem('token')
    //   localStorage.removeItem('user')
    //   window.location.href = '/login'
    // }
    return Promise.reject(error)
  }
)

export const storyboardService = {
  /**
   * 获取故事的分镜头脚本
   */
  async getStoryboard(storyId) {
    try {
      const response = await api.get(`/story/${storyId}`)
      return response.data
    } catch (error) {
      console.error('获取分镜头脚本错误:', error)
      throw new Error(error.response?.data?.message || '获取分镜头脚本失败')
    }
  },

  /**
   * 生成分镜头脚本
   */
  async generateStoryboard(storyId) {
    try {
      const response = await api.post(`/generate/${storyId}`)
      return response.data
    } catch (error) {
      console.error('生成分镜头脚本错误:', error)
      throw new Error(error.response?.data?.message || '生成分镜头脚本失败')
    }
  },

  /**
   * 创建分镜头脚本
   */
  async createStoryboard(storyboard) {
    try {
      const response = await api.post('/', storyboard)
      return {
        success: true,
        data: response.data
      }
    } catch (error) {
      console.error('创建分镜头脚本错误:', error)
      return {
        success: false,
        message: error.response?.data?.message || '创建分镜头脚本失败',
        error: error.response?.data || error.message
      }
    }
  },

  /**
   * 更新分镜头脚本
   */
  async updateStoryboard(id, storyboard) {
    try {
      const response = await api.put(`/${id}`, storyboard)
      return response.data
    } catch (error) {
      console.error('更新分镜头脚本错误:', error)
      throw new Error(error.response?.data?.message || '更新分镜头脚本失败')
    }
  },

  /**
   * 删除分镜头脚本
   */
  async deleteStoryboard(id) {
    try {
      await api.delete(`/${id}`)
      return true
    } catch (error) {
      console.error('删除分镜头脚本错误:', error)
      throw new Error(error.response?.data?.message || '删除分镜头脚本失败')
    }
  },

  /**
   * 更新分镜头脚本内容
   */
  async updateScript(storyboardId, script) {
    try {
      const response = await api.put(`/${storyboardId}/script`, { script })
      return response.data
    } catch (error) {
      console.error('更新分镜头脚本错误:', error)
      throw new Error(error.response?.data?.message || '更新分镜头脚本失败')
    }
  },

  /**
   * 更新图像提示词
   */
  async updateImagePrompt(storyboardId, imagePrompt) {
    try {
      const response = await api.put(`/${storyboardId}/image-prompt`, { imagePrompt })
      return response.data
    } catch (error) {
      console.error('更新图像提示词错误:', error)
      throw new Error(error.response?.data?.message || '更新图像提示词失败')
    }
  },

  /**
   * 更新视频提示词
   */
  async updateVideoPrompt(storyboardId, videoPrompt) {
    try {
      const response = await api.put(`/${storyboardId}/video-prompt`, { videoPrompt })
      return response.data
    } catch (error) {
      console.error('更新视频提示词错误:', error)
      throw new Error(error.response?.data?.message || '更新视频提示词失败')
    }
  },

  /**
   * 更新概念图
   */
  async updateConceptImage(storyboardId, conceptImage) {
    try {
      const response = await api.put(`/${storyboardId}/concept-image`, { conceptImage })
      return response.data
    } catch (error) {
      console.error('更新概念图错误:', error)
      throw new Error(error.response?.data?.message || '更新概念图失败')
    }
  },

  /**
   * 重新生成提示词
   */
  async regeneratePrompts(storyboardId) {
    try {
      const response = await api.post(`/${storyboardId}/regenerate-prompts`)
      return response.data
    } catch (error) {
      console.error('重新生成提示词错误:', error)
      throw new Error(error.response?.data?.message || '重新生成提示词失败')
    }
  },

  /**
   * 检查故事是否存在分镜头脚本
   */
  async existsStoryboard(storyId) {
    try {
      const response = await api.get(`/exists/${storyId}`)
      return response.data.exists
    } catch (error) {
      console.error('检查分镜头脚本错误:', error)
      throw new Error(error.response?.data?.message || '检查分镜头脚本失败')
    }
  },

  /**
   * 重新排序场景编号
   */
  async reorderScenes(storyId) {
    try {
      const response = await api.put(`/story/${storyId}/reorder`)
      return {
        success: true,
        data: response.data
      }
    } catch (error) {
      console.error('重新排序场景错误:', error)
      return {
        success: false,
        message: error.response?.data?.message || '重新排序场景失败',
        error: error.response?.data || error.message
      }
    }
  },

  /**
   * 更新视频信息
   */
  async updateVideoInfo(storyboardId, videoUrl, status) {
    try {
      const response = await api.put(`/${storyboardId}/video`, {
        videoUrl: videoUrl,
        status: status
      })
      return response.data
    } catch (error) {
      console.error('更新视频信息错误:', error)
      throw new Error(error.response?.data?.message || '更新视频信息失败')
    }
  }
}
