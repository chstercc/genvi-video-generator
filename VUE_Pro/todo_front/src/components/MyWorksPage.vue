<template>
  <div class="works-page">
    <el-card class="works-card">
      <template #header>
        <div class="works-header">
          <span class="works-title">我的作品</span>
          <el-button type="primary" @click="createNewStory">
            <el-icon><Plus /></el-icon>
            去创建作品
          </el-button>
        </div>
      </template>

      <!-- 统计信息 -->
      <div class="stats-section">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-number">{{ worksStats.totalVideos }}</div>
                <div class="stat-label">完成作品</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-number">{{ worksStats.totalScenes }}</div>
                <div class="stat-label">场景总数</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-number">{{ worksStats.averageScenes }}</div>
                <div class="stat-label">平均场景数</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-number">{{ worksStats.weeklyVideos }}</div>
                <div class="stat-label">本周新作</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 搜索区域 -->
      <div class="search-section">
        <el-input
          v-model="searchTitle"
          placeholder="搜索已完成的作品标题..."
          @keyup.enter="searchWorks"
          class="search-input"
        >
          <template #append>
            <el-button @click="searchWorks" :icon="Search">搜索</el-button>
          </template>
        </el-input>
        <el-button @click="resetSearch" v-if="searchTitle">重置</el-button>
      </div>

      <!-- 作品列表 -->
      <div v-loading="loading" class="works-list">
        <div v-if="filteredWorks.length" class="work-items">
          <el-card
            v-for="work in filteredWorks"
            :key="work.id"
            class="work-item"
            shadow="hover"
          >
            <template #header>
              <div class="work-item-header">
                <div class="work-title-section">
                  <span class="work-item-title">{{ work.title }}</span>
                </div>
                <div class="work-meta">
                  <div class="work-tags">
                    <el-tag type="success" size="small">
                      {{ work.sceneCount }} 个场景
                    </el-tag>
                  </div>
                  <span class="work-date">{{ formatDate(work.createdAt) }}</span>
                </div>
              </div>
            </template>

            <div class="work-item-content">
              <div class="work-preview">
                <div class="work-thumbnail">
                  <video
                    v-if="work.previewVideo"
                    :src="work.previewVideo"
                    :poster="work.thumbnail"
                    preload="metadata"
                    muted
                    loop
                    class="preview-video"
                    @click="playPreview($event)"
                    @mouseenter="startPreview($event)"
                    @mouseleave="stopPreview($event)"
                  />
                  <div v-else class="no-preview">
                    <el-icon size="48"><VideoCamera /></el-icon>
                    <span>暂无预览</span>
                  </div>
                </div>
                <div class="work-description">
                  <div class="work-stats">
                    <span v-if="work.fileSize"><el-icon><Document /></el-icon> {{ formatFileSize(work.fileSize) }}</span>
                  </div>
                </div>
              </div>
            </div>

            <template #footer>
                                          <div class="work-item-actions">
                <el-button type="success" size="small" @click="viewWork(work)">
                  <el-icon><VideoPlay /></el-icon>
                  查看作品
                </el-button>
                <el-button type="primary" size="small" @click="downloadWork(work)" :loading="work.downloading">
                  <el-icon><Download /></el-icon>
                  {{ work.downloading ? '下载中...' : '下载' }}
                </el-button>
                <el-button type="warning" size="small" @click="editWork(work)">
                  <el-icon><Edit /></el-icon>
                  重新制作
                </el-button>
                <el-button type="danger" size="small" @click="deleteWork(work)">
                  <el-icon><Delete /></el-icon>

                </el-button>
              </div>
            </template>
          </el-card>
        </div>

        <div v-else class="no-works">
          <el-empty description="暂无完成的作品">
            <template #description>
              <p>还没有完成任何视频作品</p>
              <p class="help-text">请先创建故事，制作分镜头，生成视频，完成的作品将在这里显示</p>
            </template>
            <el-button type="primary" @click="createNewStory">
              <el-icon><Plus /></el-icon>
              开始创作
            </el-button>
          </el-empty>
        </div>
      </div>
    </el-card>

    <!-- 作品预览对话框 -->
    <el-dialog
      v-model="previewDialogVisible"
      :title="currentPreviewWork?.title"
      width="80%"
      center
    >
      <div class="preview-dialog-content">
        <video
          v-if="currentPreviewWork?.finalVideo"
          :src="currentPreviewWork.finalVideo"
          controls
          style="width: 100%; max-height: 500px;"
          preload="metadata"
        >
          您的浏览器不支持视频播放。
        </video>
        <div v-else>
          <el-empty description="暂无最终视频">
            <el-button type="primary" @click="generateFinalVideo(currentPreviewWork)">
              生成最终视频
            </el-button>
          </el-empty>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="previewDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="downloadWork(currentPreviewWork)" :loading="currentPreviewWork?.downloading">
            <el-icon><Download /></el-icon>
            {{ currentPreviewWork?.downloading ? '下载中...' : '下载作品' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, VideoCamera, VideoPlay, Download, Edit, Delete, Plus,
  Document
} from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()

// 创建axios实例
const axiosInstance = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
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
const works = ref([])
const loading = ref(false)
const searchTitle = ref('')
const previewDialogVisible = ref(false)
const currentPreviewWork = ref(null)
const userControlledVideos = ref(new Set()) // 跟踪用户手动控制的视频

// 统计信息状态
const worksStats = ref({
  totalVideos: 0,
  totalScenes: 0,
  averageScenes: 0,
  weeklyVideos: 0
})

// 获取统计信息
const fetchStats = async () => {
  try {
    const response = await axiosInstance.get('/api/video/my-works/stats')
    worksStats.value = response.data
  } catch (error) {
    console.error('获取统计信息失败:', error)
    // 保持默认值
  }
}

// 过滤后的作品列表
const filteredWorks = computed(() => {
  if (!searchTitle.value.trim()) {
    return works.value
  }
  return works.value.filter(work =>
    work.title.toLowerCase().includes(searchTitle.value.toLowerCase()) ||
    work.description?.toLowerCase().includes(searchTitle.value.toLowerCase())
  )
})

// 获取作品列表
const fetchWorks = async () => {
  loading.value = true
  try {
    const response = await axiosInstance.get('/api/video/my-works')
    works.value = response.data.map(video => ({
      id: video.id,
      title: video.title,
      description: video.description,
      sceneCount: video.sceneCount,
      createdAt: video.createdAt,
      previewVideo: video.videoUrl,
      finalVideo: video.videoUrl,
      thumbnail: video.thumbnailUrl,
      fileSize: video.fileSize,
      storyId: video.storyId,
      downloading: false
    }))

    // 同时获取统计信息
    await fetchStats()
  } catch (error) {
    console.error('获取作品列表失败:', error)
    if (error.response?.status === 401) {
      ElMessage.error('请先登录')
      router.push('/login')
    } else {
      ElMessage.error('获取作品列表失败')
    }
  } finally {
    loading.value = false
  }
}

// 搜索作品
const searchWorks = async () => {
  if (!searchTitle.value.trim()) {
    await fetchWorks()
    return
  }

  loading.value = true
  try {
    const response = await axiosInstance.get('/api/video/my-works/search', {
      params: { title: searchTitle.value }
    })
    works.value = response.data.map(video => ({
      id: video.id,
      title: video.title,
      description: video.description,
      sceneCount: video.sceneCount,
      createdAt: video.createdAt,
      previewVideo: video.videoUrl,
      finalVideo: video.videoUrl,
      thumbnail: video.thumbnailUrl,
      fileSize: video.fileSize,
      storyId: video.storyId,
      downloading: false
    }))
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchTitle.value = ''
}

// 查看作品
const viewWork = (work) => {
  currentPreviewWork.value = work
  previewDialogVisible.value = true
}

// 播放预览
const playPreview = (event) => {
  event.stopPropagation() // 防止事件冒泡
  const video = event.target
  const videoId = video.src

  // 标记为用户手动控制
  userControlledVideos.value.add(videoId)

  if (video.paused) {
    video.play().catch(() => {
      // 播放可能被阻止，忽略错误
    })
  } else {
    video.pause()
  }

  // 3秒后清除手动控制标记，允许悬停控制
  setTimeout(() => {
    userControlledVideos.value.delete(videoId)
  }, 3000)
}

// 鼠标悬停开始播放
const startPreview = (event) => {
  const video = event.target
  const videoId = video.src

  // 如果用户刚刚手动控制过，不执行悬停播放
  if (userControlledVideos.value.has(videoId)) {
    return
  }

  video.play().catch(() => {
    // 自动播放可能被浏览器阻止，忽略错误
  })
}

// 鼠标离开停止播放
const stopPreview = (event) => {
  const video = event.target
  const videoId = video.src

  // 如果用户刚刚手动控制过，不执行悬停停止
  if (userControlledVideos.value.has(videoId)) {
    return
  }

  video.pause()
  video.currentTime = 0 // 重置到开头
}

// 下载作品
const downloadWork = async (work) => {
  if (!work.finalVideo && !work.previewVideo) {
    ElMessage.warning('该作品还没有可下载的视频文件')
    return
  }

  work.downloading = true

  try {
    const videoUrl = work.finalVideo || work.previewVideo
    ElMessage.info('正在下载视频文件...')

    // 直接使用fetch下载文件
    const response = await fetch(videoUrl, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}` // 如果需要认证
      }
    })

    if (!response.ok) {
      throw new Error(`下载失败: ${response.status} ${response.statusText}`)
    }

    // 获取文件内容
    const blob = await response.blob()

    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${work.title}.mp4`
    link.style.display = 'none'

    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    // 清理临时URL
    window.URL.revokeObjectURL(url)

    ElMessage.success('视频下载完成')
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败: ' + error.message)
  } finally {
    work.downloading = false
  }
}

// 编辑作品（重新制作）
const editWork = (work) => {
  router.push({
    path: '/final-video',
    query: {
      storyId: work.storyId || 1,
      title: work.title
    }
  })
}

// 删除作品
const deleteWork = async (work) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除作品"${work.title}"吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    // 调用删除API
    await axiosInstance.delete(`/api/video/${work.id}`)

    // 从本地列表中移除
    const index = works.value.findIndex(w => w.id === work.id)
    if (index > -1) {
      works.value.splice(index, 1)
      ElMessage.success('作品删除成功')
      // 重新获取统计信息
      await fetchStats()
    }
  } catch {
    // 用户取消删除
  }
}

// 创建新故事
const createNewStory = () => {
  router.push('/stories')
}

// 生成最终视频
const generateFinalVideo = (work) => {
  router.push({
    path: '/final-video',
    query: {
      storyId: work.storyId || 1,
      title: work.title
    }
  })
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

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

onMounted(fetchWorks)
</script>

<style scoped>
.works-page {
  max-width: 1200px;
  margin: 20px auto;
  padding: 20px;
}

.works-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.15);
  border-radius: 16px;
}

.works-header {
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  margin-bottom: 10px;
}

.works-title {
  font-size: 28px;
  font-weight: bold;
  color: #fff;
  text-align: center;
  position: relative;
  padding: 15px 50px;
  z-index: 1;
  letter-spacing: 1px;
}

.works-title::before {
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

.works-header .el-button {
  position: absolute;
  right: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  padding: 12px 24px;
  font-weight: 600;
  letter-spacing: 0.5px;
  transition: all 0.3s ease;
}

.works-header .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

/* 统计信息样式 */
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

/* 搜索区域样式 */
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

/* 作品列表样式 */
.works-list {
  min-height: 200px;
  padding: 10px;
}

.work-items {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 25px;
}

.work-item {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 16px;
  transition: all 0.3s ease;
  overflow: hidden;
}

.work-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.2);
}

.work-item-header {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  padding: 12px 16px;
  border-bottom: 1px solid rgba(102, 126, 234, 0.1);
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

.work-title-section {
  flex: 1;
  text-align: left;
  padding-left: 8px;
}

.work-item-title {
  font-size: 18px;
  font-weight: 600;
  color: #667eea;
  margin-bottom: 6px;
  display: block;
  line-height: 1.4;
}

.work-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

:deep(.el-tag--success) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  padding: 4px 8px;
  border-radius: 8px;
  color: white;
}

.work-meta {
  color: #764ba2;
  font-size: 14px;
  opacity: 0.8;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
}

.work-date {
  font-size: 11px;
  color: #718096;
  white-space: nowrap;
}

/* 作品预览样式 */
.work-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin: 12px 0;
  padding: 0 16px;
}

.work-thumbnail {
  position: relative;
  width: 280px;
  height: 158px;
  border-radius: 12px;
  overflow: hidden;
  background: rgba(102, 126, 234, 0.05);
  flex-shrink: 0;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.1);
  margin: 0 auto;
}

.preview-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
  filter: grayscale(30%) brightness(0.9);
  transition: all 0.3s ease;
}

.preview-video:hover {
  filter: grayscale(0%) brightness(1);
  transform: scale(1.02);
}

.no-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #667eea;
  gap: 10px;
}

.work-description {
  width: 100%;
  text-align: center;
}

.work-content {
  color: #4a5568;
  line-height: 1.6;
  margin-bottom: 8px;
  font-size: 14px;
}

.work-stats {
  display: flex;
  gap: 15px;
  color: #667eea;
  font-size: 13px;
  opacity: 0.8;
}

.work-stats span {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 操作按钮样式 */
.work-item-actions {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  background: rgba(102, 126, 234, 0.05);
  border-top: 1px solid rgba(102, 126, 234, 0.1);
}

.work-item-actions .el-button {
  border-radius: 6px;
  padding: 4px 8px;
  font-weight: 500;
  font-size: 12px;
  transition: all 0.3s ease;
}

.work-item-actions .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.work-item-actions .el-button--success {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.work-item-actions .el-button--primary {
  background: #667eea;
  border: none;
}

.work-item-actions .el-button--warning {
  background: #764ba2;
  border: none;
  color: white;
}

.work-item-actions .el-button--danger {
  background: #ff6b6b;
  border: none;
}

/* 空状态样式 */
.no-works {
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

/* 预览对话框样式 */
:deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  margin: 0;
}

:deep(.el-dialog__title) {
  color: white;
  font-weight: 600;
  font-size: 20px;
}

.preview-dialog-content {
  text-align: center;
  padding: 20px;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  padding-top: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .works-page {
    padding: 10px;
  }

  .works-title {
    font-size: 24px;
    padding: 12px 30px;
  }

  .work-items {
    grid-template-columns: 1fr;
  }

  .work-preview {
    flex-direction: column;
    padding: 15px;
  }

  .work-thumbnail {
    width: 100%;
    height: 180px;
  }

  .work-item-actions {
    flex-wrap: wrap;
  }

  .work-item-actions .el-button {
    flex: 1;
    min-width: 120px;
    font-size: 13px;
    padding: 6px 12px;
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
