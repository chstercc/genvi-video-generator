<script setup>
import { onMounted, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, VideoCamera, Edit, Plus } from '@element-plus/icons-vue'
import axios from 'axios'

const axiosInstance = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 60000, // 增加到60秒，适应AI生成的处理时间
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 自动添加token
axiosInstance.interceptors.request.use(
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

// 响应式数据
const stories = ref([])
const storyboards = ref([])
const loading = ref(false)

// 搜索
const searchTitle = ref('')

// 计算属性
const scriptStats = computed(() => {
  // 只统计已制作分镜头的故事
  const storiesWithStoryboard = stories.value.filter(story =>
    hasStoryboard(story.id)
  )

  const stats = {
    total: storiesWithStoryboard.length,
    totalStoryboards: 0,
    recent: 0,
    avgStoryboards: 0
  }

  // 计算最近一周创建的故事
  const weekAgo = new Date()
  weekAgo.setDate(weekAgo.getDate() - 7)

  storiesWithStoryboard.forEach(story => {
    // 统计分镜头总数
    stats.totalStoryboards += getStoryboardCount(story.id)

    // 统计最近创建的脚本
    if (new Date(story.createdAt) > weekAgo) {
      stats.recent++
    }
  })

  // 计算平均分镜头数
  if (stats.total > 0) {
    stats.avgStoryboards = Math.round(stats.totalStoryboards / stats.total * 10) / 10
  }

  return stats
})

// 过滤后的故事列表 - 只显示已制作分镜头的故事
const filteredStories = computed(() => {
  // 首先过滤出已制作分镜头的故事
  const storiesWithStoryboard = stories.value.filter(story =>
    hasStoryboard(story.id)
  )

  // 然后根据搜索条件进一步过滤
  if (!searchTitle.value.trim()) {
    return storiesWithStoryboard
  }
  return storiesWithStoryboard.filter(story =>
    story.title.toLowerCase().includes(searchTitle.value.toLowerCase())
  )
})

// 获取故事列表
const fetchStories = async () => {
  loading.value = true
  try {
    const response = await axiosInstance.get('/api/stories')
    stories.value = response.data
  } catch (error) {
    ElMessage.error('获取故事列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 获取分镜头列表
const fetchStoryboards = async () => {
  try {
    // 获取所有故事的分镜头信息
    const storyboardPromises = stories.value.map(async (story) => {
      try {
        const response = await axiosInstance.get(`/api/storyboards/story/${story.id}`)
        return response.data.length > 0 ? { storyId: story.id, count: response.data.length } : null
      } catch {
        return null
      }
    })

    const results = await Promise.all(storyboardPromises)
    storyboards.value = results.filter(result => result !== null)
  } catch (error) {
    console.error('获取分镜头信息失败:', error)
  }
}

// 加载所有数据
const loadAllData = async () => {
  await fetchStories()
  await fetchStoryboards()
}

// 开始创作分镜头
const startCreation = (story) => {
  try {
    const url = new URL('/storyboard', window.location.origin)
    url.searchParams.set('storyId', story.id)
    url.searchParams.set('title', story.title)

    const newWindow = window.open(url.toString(), '_blank')
    if (newWindow) {
      ElMessage.success('已在新标签页中打开分镜头创作')
    } else {
      ElMessage.error('无法打开新窗口，请检查浏览器设置')
    }
  } catch (error) {
    console.error('打开新窗口失败:', error)
    ElMessage.error('打开新窗口失败')
  }
}

// 查看故事详情
const viewStory = () => {
  try {
    const newWindow = window.open('/stories', '_blank')
    if (newWindow) {
      ElMessage.success('已在新标签页中打开故事梗概页面')
    } else {
      ElMessage.error('无法打开新窗口，请检查浏览器设置')
    }
  } catch (error) {
    console.error('打开新窗口失败:', error)
    ElMessage.error('打开新窗口失败')
  }
}

// 搜索故事
const searchStories = async () => {
  if (!searchTitle.value.trim()) {
    await fetchStories()
    return
  }

  loading.value = true
  try {
    const response = await axiosInstance.get('/api/stories/search', {
      params: { title: searchTitle.value }
    })
    stories.value = response.data
    await fetchStoryboards()
  } catch (error) {
    ElMessage.error('搜索故事失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchTitle.value = ''
  loadAllData()
}

// 创建新故事
const createNewStory = () => {
  try {
    const newWindow = window.open('/stories', '_blank')
    if (newWindow) {
      ElMessage.success('已在新标签页中打开故事梗概页面，开始创建新故事')
    } else {
      ElMessage.error('无法打开新窗口，请检查浏览器设置')
    }
  } catch (error) {
    console.error('打开新窗口失败:', error)
    ElMessage.error('打开新窗口失败')
  }
}

// 检查故事是否有分镜头
const hasStoryboard = (storyId) => {
  return storyboards.value.some(sb => sb.storyId === storyId)
}

// 获取分镜头数量
const getStoryboardCount = (storyId) => {
  const storyboard = storyboards.value.find(sb => sb.storyId === storyId)
  return storyboard ? storyboard.count : 0
}

// 工具函数
const formatDate = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const truncateText = (text, maxLength = 150) => {
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

onMounted(loadAllData)
</script>

<template>
  <div class="scripts-page">
    <el-card class="scripts-card">
      <template #header>
        <div class="scripts-header">
          <span class="scripts-title">我的脚本</span>
          <el-button type="primary" @click="createNewStory">
            <el-icon><Plus /></el-icon>
            去创建故事
          </el-button>
        </div>
      </template>

      <!-- 统计信息 -->
      <div class="stats-section">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-number">{{ scriptStats.total }}</div>
                <div class="stat-label">已制作脚本</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-number">{{ scriptStats.totalStoryboards }}</div>
                <div class="stat-label">分镜总数</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-number">{{ scriptStats.avgStoryboards }}</div>
                <div class="stat-label">平均分镜数</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-number">{{ scriptStats.recent }}</div>
                <div class="stat-label">最近创建</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 搜索区域 -->
      <div class="search-section">
        <el-input
          v-model="searchTitle"
          placeholder="搜索已制作的脚本标题..."
          @keyup.enter="searchStories"
          class="search-input"
        >
          <template #append>
            <el-button @click="searchStories" :icon="Search">搜索</el-button>
          </template>
        </el-input>
        <el-button @click="resetSearch" v-if="searchTitle">重置</el-button>
      </div>

      <!-- 脚本列表 -->
      <div v-loading="loading" class="scripts-list">
        <div v-if="filteredStories.length" class="script-items">
          <el-card
            v-for="story in filteredStories"
            :key="story.id"
            class="script-item"
            shadow="hover"
          >
            <template #header>
              <div class="script-item-header">
                <div class="script-title-section">
                  <span class="script-item-title">{{ story.title }}</span>
                  <el-tag type="success" size="small">
                    {{ getStoryboardCount(story.id) }} 个分镜
                  </el-tag>
                </div>
                <div class="script-meta">
                  <span class="script-date">{{ formatDate(story.createdAt) }}</span>
                </div>
              </div>
            </template>

            <div class="script-item-content">
              <p class="script-content">{{ truncateText(story.content) }}</p>
            </div>

            <template #footer>
              <div class="script-item-actions">
                <el-button type="success" size="small" @click="startCreation(story)">
                  <el-icon><VideoCamera /></el-icon>
                  继续制作
                </el-button>
                <el-button type="primary" size="small" @click="viewStory(story)">
                  <el-icon><Edit /></el-icon>
                  编辑故事
                </el-button>
              </div>
            </template>
          </el-card>
        </div>

        <div v-else class="no-scripts">
          <el-empty description="暂无已制作的脚本">
            <template #description>
              <p>还没有制作任何分镜头脚本</p>
              <p class="help-text">请先在故事梗概页面创建故事，然后制作分镜头，制作完成的脚本将在这里显示</p>
            </template>
            <el-button type="primary" @click="createNewStory">
              <el-icon><Plus /></el-icon>
              去创建故事
            </el-button>
          </el-empty>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.scripts-page {
  max-width: 1200px;
  margin: 20px auto;
  padding: 20px;
}

.scripts-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.15);
  border-radius: 16px;
}

.scripts-header {
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  margin-bottom: 10px;
}

.scripts-title {
  font-size: 28px;
  font-weight: bold;
  color: #fff;
  text-align: center;
  position: relative;
  padding: 15px 50px;
  z-index: 1;
  letter-spacing: 1px;
}

.scripts-title::before {
  content: '';
  position: absolute;
  top: 0;
  left: -20px;
  right: -20px;
  bottom: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  transform: skew(-15deg);
  z-index: -1;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.3);
}

.scripts-header .el-button {
  position: absolute;
  right: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  padding: 12px 24px;
  font-weight: 600;
  letter-spacing: 0.5px;
  transition: all 0.3s ease;
}

.scripts-header .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.stats-section {
  margin: 30px 0;
}

.stat-card {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.1);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.2);
}

.stat-item {
  padding: 20px;
  text-align: center;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 15px;
  color: #4a5568;
  font-weight: 500;
}

.search-section {
  display: flex;
  gap: 15px;
  margin: 30px 0;
  align-items: center;
  justify-content: center;
}

.search-input {
  width: 500px;
  max-width: 500px;
}

:deep(.el-input__wrapper) {
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.1);
  border-radius: 12px;
  padding: 4px 12px;
}

:deep(.el-input__inner) {
  height: 42px;
}

.scripts-list {
  min-height: 200px;
  padding: 10px;
}

.script-items {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.script-item {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 16px;
  transition: all 0.3s ease;
  overflow: hidden;
}

.script-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.2);
}

.script-item-header {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  padding: 15px 20px;
  border-bottom: 1px solid rgba(102, 126, 234, 0.1);
}

.script-title-section {
  display: flex;
  align-items: center;
  gap: 15px;
  flex: 1;
}

.script-item-title {
  font-size: 18px;
  font-weight: 600;
  color: #667eea;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.el-tag--success) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  padding: 4px 8px;
  border-radius: 8px;
  color: white;
}

.script-meta {
  display: flex;
  align-items: center;
  gap: 15px;
}

.script-date {
  font-size: 14px;
  color: #764ba2;
  opacity: 0.8;
}

.script-item-content {
  padding: 20px;
  background: rgba(255, 255, 255, 0.5);
}

.script-content {
  color: #4a5568;
  line-height: 1.8;
  margin: 0;
  word-break: break-word;
  font-size: 15px;
}

.script-item-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding: 15px 20px;
  background: rgba(102, 126, 234, 0.05);
}

.script-item-actions .el-button {
  border-radius: 8px;
  padding: 8px 16px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.script-item-actions .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.script-item-actions .el-button--success {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.script-item-actions .el-button--primary {
  background: #667eea;
  border: none;
}

.no-scripts {
  text-align: center;
  padding: 60px 0;
  color: #764ba2;
}

.help-text {
  color: #4a5568;
  font-size: 15px;
  margin: 10px 0 20px 0;
  line-height: 1.6;
}

:deep(.el-empty__description) {
  color: #764ba2;
  font-size: 16px;
  margin-top: 20px;
}

:deep(.el-empty__description p) {
  margin: 8px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .scripts-page {
    padding: 10px;
  }

  .scripts-title {
    font-size: 24px;
    padding: 12px 30px;
  }

  .script-item-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .script-item-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .search-section {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    max-width: none;
    width: 100%;
  }

  .stats-section {
    margin: 20px 0;
  }

  .stat-number {
    font-size: 28px;
  }

  .stat-label {
    font-size: 14px;
  }
}
</style>
