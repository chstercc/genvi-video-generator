<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, VideoCamera } from '@element-plus/icons-vue'
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
const loading = ref(false)
const dialogVisible = ref(false)
const editingStory = ref(null)
const generatingStoryboards = ref(new Set()) // 记录正在生成分镜头脚本的故事ID

// 表单数据
const storyForm = ref({
  title: '',
  content: ''
})

// 搜索
const searchTitle = ref('')

// 获取故事列表
const fetchStories = async () => {
  loading.value = true
    try {
    const response = await axiosInstance.get('/api/stories')
    stories.value = response.data

    // 为每个故事初始化storyboardExists属性并检查是否存在分镜头脚本
    for (let story of stories.value) {
      story.storyboardExists = false // 初始化默认值
      await checkStoryboardExists(story)
    }
  } catch (error) {
    ElMessage.error('获取故事列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 检查故事是否存在分镜头脚本
const checkStoryboardExists = async (story) => {
  try {
    const response = await axiosInstance.get(`/api/storyboards/story/${story.id}`)
    story.storyboardExists = response.data && response.data.length > 0
  } catch {
    story.storyboardExists = false
  }
}

// 打开新建故事对话框
const openCreateDialog = () => {
  editingStory.value = null
  storyForm.value = {
    title: '',
    content: ''
  }
  dialogVisible.value = true
}

// 打开编辑故事对话框
const openEditDialog = (story) => {
  editingStory.value = story
  storyForm.value = {
    title: story.title,
    content: story.content
  }
  dialogVisible.value = true
}

// 保存故事
const saveStory = async () => {
  if (!storyForm.value.title.trim()) {
    ElMessage.warning('请输入故事标题')
    return
  }
  if (!storyForm.value.content.trim()) {
    ElMessage.warning('请输入故事内容')
    return
  }

  try {
    if (editingStory.value) {
      // 编辑故事
      await axiosInstance.put(`/api/stories/${editingStory.value.id}`, storyForm.value)
      ElMessage.success('故事更新成功')
    } else {
      // 新建故事
      await axiosInstance.post('/api/stories', storyForm.value)
      ElMessage.success('故事创建成功')
    }
    dialogVisible.value = false
    await fetchStories()
  } catch (error) {
    ElMessage.error('保存故事失败')
    console.error(error)
  }
}

// 删除故事
const deleteStory = async (story) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除故事 "${story.title}" 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await axiosInstance.delete(`/api/stories/${story.id}`)
    ElMessage.success('故事删除成功')
    await fetchStories()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除故事失败')
      console.error(error)
    }
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

    // 为搜索结果也检查分镜头脚本状态
    for (let story of stories.value) {
      story.storyboardExists = false
      await checkStoryboardExists(story)
    }
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
  fetchStories()
}

// 重新生成分镜头脚本
const regenerateStoryboard = async (story) => {
  try {
    await ElMessageBox.confirm(
      '确定要重新生成分镜头脚本吗？这将覆盖现有的分镜头内容。',
      '重新生成确认',
      {
        confirmButtonText: '确定重新生成',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 调用原有的创建方法
    await startCreation(story)

    // 重新检查分镜头状态
    await checkStoryboardExists(story)

  } catch {
    // 用户取消操作
  }
}

// 开始创作
const startCreation = async (story) => {
  try {
    // 添加到生成中的集合
    generatingStoryboards.value.add(story.id)

    // 显示加载提示
    ElMessage.info('正在使用AI生成分镜头脚本，请稍候...')

    // 调用后端API生成分镜头脚本
    const response = await axiosInstance.post(`/api/stories/${story.id}/generate-storyboard`)

    if (response.status === 200) {
      // 显示成功消息并提供跳转选项
      ElMessageBox.confirm(
        '分镜头脚本生成完成！是否立即跳转到分镜头页面继续创作？',
        '生成成功',
        {
          confirmButtonText: '立即跳转',
          cancelButtonText: '稍后处理',
          type: 'success',
          closeOnClickModal: false
        }
             ).then(() => {
         // 用户确认跳转
         openStoryboardPage(story)
       }).catch(() => {
         // 用户选择稍后处理
         ElMessage.info('分镜头脚本已保存，您可以稍后在故事列表中继续创作')
       })

       // 更新故事的分镜头状态
       story.storyboardExists = true
    } else {
      ElMessage.error('生成分镜头脚本失败，请重试')
    }
  } catch (error) {
    console.error('生成分镜头脚本失败:', error)

    // 根据错误类型显示不同的错误信息
    if (error.response) {
      const errorMessage = error.response.data || '生成分镜头脚本失败'
      ElMessage.error(errorMessage)
    } else if (error.request) {
      ElMessage.error('网络连接失败，请检查网络后重试')
    } else {
      ElMessage.error('生成分镜头脚本时发生未知错误')
    }
  } finally {
    // 从生成中的集合移除
    generatingStoryboards.value.delete(story.id)
  }
}

// 打开分镜头页面
const openStoryboardPage = (story) => {
  try {
    // 构建新窗口的URL
    const url = new URL('/storyboard', window.location.origin)
    url.searchParams.set('storyId', story.id)
    url.searchParams.set('title', story.title)

    // 尝试在新标签页中打开
    const newWindow = window.open(url.toString(), '_blank')

    // 检查是否成功打开新窗口
    if (newWindow) {
      ElMessage.success('已在新标签页中打开分镜头脚本')
    } else {
      // 如果新窗口被拦截，在当前窗口跳转
      ElMessage.warning('新窗口被浏览器拦截，将在当前页面跳转')
      window.location.href = url.toString()
    }
  } catch (error) {
    console.error('打开分镜头页面失败:', error)
    ElMessage.error('打开分镜头页面失败')
  }
}

// 格式化时间
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

// 限制显示文本长度
const truncateText = (text, maxLength = 100) => {
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

onMounted(fetchStories)
</script>

<template>
  <div class="story-page">
    <el-card class="story-card">
      <template #header>
        <div class="story-header">
          <span class="story-title">故事梗概</span>
          <el-button type="primary" @click="openCreateDialog">
            新建故事
          </el-button>
        </div>
      </template>

      <!-- 搜索区域 -->
      <div class="search-section">
        <el-input
          v-model="searchTitle"
          placeholder="搜索故事标题..."
          @keyup.enter="searchStories"
          class="search-input"
        >
          <template #append>
            <el-button @click="searchStories" :icon="Search">搜索</el-button>
          </template>
        </el-input>
        <el-button @click="resetSearch" v-if="searchTitle">重置</el-button>
      </div>

      <!-- 故事列表 -->
      <div v-loading="loading" class="story-list">
        <div v-if="stories.length" class="story-items">
          <el-card
            v-for="story in stories"
            :key="story.id"
            class="story-item"
            shadow="hover"
          >
            <template #header>
              <div class="story-item-header">
                <span class="story-item-title">{{ story.title }}</span>
                <span class="story-item-date">
                  {{ formatDate(story.createdAt) }}
                </span>
              </div>
            </template>

            <div class="story-item-content">
              <p class="story-content">{{ truncateText(story.content) }}</p>
            </div>

                        <template #footer>
              <div class="story-item-actions">
                <el-button
                  v-if="!story.storyboardExists"
                  type="success"
                  size="small"
                  @click="startCreation(story)"
                  :loading="generatingStoryboards.has(story.id)"
                  :disabled="generatingStoryboards.has(story.id)"
                >
                  <el-icon v-if="!generatingStoryboards.has(story.id)"><VideoCamera /></el-icon>
                  {{ generatingStoryboards.has(story.id) ? 'AI生成中...' : '开始创作' }}
                </el-button>
                <el-button
                  v-if="story.storyboardExists"
                  type="success"
                  size="small"
                  @click="openStoryboardPage(story)"
                >
                  <el-icon><VideoCamera /></el-icon>
                  查看分镜头
                </el-button>
                <el-button
                  v-if="story.storyboardExists"
                  type="warning"
                  size="small"
                  @click="regenerateStoryboard(story)"
                  :loading="generatingStoryboards.has(story.id)"
                  :disabled="generatingStoryboards.has(story.id)"
                  plain
                >
                  {{ generatingStoryboards.has(story.id) ? 'AI重新生成中...' : '重新生成脚本' }}
                </el-button>
                <el-button type="primary" size="small" @click="openEditDialog(story)">
                  编辑
                </el-button>
                <el-button type="danger" size="small" @click="deleteStory(story)">
                  删除
                </el-button>
              </div>
            </template>
          </el-card>
        </div>

        <div v-else class="no-stories">
          <el-empty description="暂无故事梗概" />
        </div>
      </div>
    </el-card>

    <!-- 新建/编辑故事对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingStory ? '编辑故事' : '新建故事'"
      width="60%"
      :close-on-click-modal="false"
    >
      <el-form :model="storyForm" label-width="80px">
        <el-form-item label="故事标题" required>
          <el-input
            v-model="storyForm.title"
            placeholder="请输入故事标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="故事内容" required>
          <el-input
            v-model="storyForm.content"
            type="textarea"
            placeholder="请输入故事梗概内容..."
            :rows="8"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveStory">
            {{ editingStory ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.story-page {
  max-width: 1200px;
  margin: 20px auto;
  padding: 20px;
}

.story-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.15);
  border-radius: 16px;
}

.story-header {
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  margin-bottom: 10px;
}

.story-title {
  font-size: 28px;
  font-weight: bold;
  color: #fff;
  text-align: center;
  position: relative;
  padding: 15px 50px;
  z-index: 1;
  letter-spacing: 1px;
}

.story-title::before {
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

.story-header .el-button {
  position: absolute;
  right: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  padding: 12px 24px;
  font-weight: 600;
  letter-spacing: 0.5px;
  transition: all 0.3s ease;
}

.story-header .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
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

.story-list {
  min-height: 200px;
  padding: 10px;
}

.story-items {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.story-item {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 16px;
  transition: all 0.3s ease;
  overflow: hidden;
}

.story-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.2);
}

.story-item-header {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  padding: 15px 20px;
  border-bottom: 1px solid rgba(102, 126, 234, 0.1);
}

.story-item-title {
  font-size: 18px;
  font-weight: 600;
  color: #667eea;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.story-item-date {
  font-size: 14px;
  color: #764ba2;
  margin-left: 15px;
  opacity: 0.8;
}

.story-item-content {
  padding: 20px;
  background: rgba(255, 255, 255, 0.5);
}

.story-content {
  color: #4a5568;
  line-height: 1.8;
  margin: 0;
  word-break: break-word;
  font-size: 15px;
}

.story-item-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding: 15px 20px;
  background: rgba(102, 126, 234, 0.05);
}

.story-item-actions .el-button {
  border-radius: 8px;
  padding: 8px 16px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.story-item-actions .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.story-item-actions .el-button--success {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.story-item-actions .el-button--primary {
  background: #667eea;
  border: none;
}

.story-item-actions .el-button--danger {
  background: #ff6b6b;
  border: none;
}

.no-stories {
  text-align: center;
  padding: 60px 0;
  color: #764ba2;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 20px;
}

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

:deep(.el-dialog__body) {
  padding: 30px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #4a5568;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .story-page {
    padding: 10px;
  }

  .story-title {
    font-size: 24px;
    padding: 12px 30px;
  }

  .search-section {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    max-width: none;
    width: 100%;
  }

  .story-item-actions {
    flex-wrap: wrap;
  }

  .story-item-actions .el-button {
    flex: 1;
    min-width: 120px;
  }
}
</style>
