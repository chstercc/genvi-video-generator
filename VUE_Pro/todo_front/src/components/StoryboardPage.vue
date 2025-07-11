<template>
  <div class="storyboard-page">
    <el-card class="storyboard-card">
      <template #header>
        <div class="storyboard-header">
          <el-button type="primary" @click="goBack" plain>
            <el-icon><Back /></el-icon>
            {{ backButtonText }}
          </el-button>
          <div class="storyboard-title">
            <span class="story-title">{{ storyTitle }}</span>
            <span class="subtitle">分镜头脚本</span>
          </div>
        </div>
      </template>

      <div class="storyboard-content">
        <!-- 生成短片按钮 -->
        <div v-if="canGenerateVideo" class="generate-video-section">
          <el-button
            type="warning"
            @click="goToVideoGeneration"
            size="large"
            plain
            class="generate-video-btn"
          >
            <el-icon class="video-camera-icon"><VideoCamera /></el-icon>
            生成短片
          </el-button>
        </div>

        <!-- 生成短片提示 -->
        <div v-else-if="storyboardData.length > 0" class="generate-video-tip">
          <el-alert
            title="完成概念图生成和图生视频提示词编辑后，即可生成短片"
            type="info"
            show-icon
            :closable="false"
            class="video-tip-alert"
          />
        </div>

        <div v-if="loading" class="loading-section">
          <el-skeleton :rows="5" animated />
        </div>

        <div v-else-if="storyboardData.length > 0" class="storyboard-table">
          <el-table :data="storyboardData" style="width: 100%" stripe>
            <el-table-column prop="scene" label="场景" width="80" align="center" />
            <el-table-column prop="script" label="分镜头脚本" min-width="180">
              <template #default="{ row, $index }">
                <div v-if="!row.editing" class="script-content">
                  {{ row.script }}
                  <div class="script-actions">
                    <el-button type="text" size="small" @click="editScript($index)">
                      <el-icon><Edit /></el-icon>
                      编辑
                    </el-button>
                  </div>
                </div>
                <div v-else class="script-editor">
                  <el-input
                    v-model="row.tempScript"
                    type="textarea"
                    :rows="4"
                    placeholder="请输入分镜头脚本..."
                    class="script-textarea"
                  />
                  <div class="editor-actions">
                    <el-button type="success" size="small" @click="saveScript($index)" :loading="row.saving">
                      <el-icon><Check /></el-icon>
                      保存
                    </el-button>
                    <el-button type="info" size="small" @click="cancelEdit($index)">
                      <el-icon><Close /></el-icon>
                      取消
                    </el-button>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="imagePrompt" label="文生图提示词" min-width="200">
              <template #default="{ row, $index }">
                <div v-if="!row.editingImagePrompt" class="prompt-content" :class="{ 'prompt-updated': row.promptUpdated }">
                  {{ row.imagePrompt }}
                  <el-tag v-if="row.promptUpdated" type="success" size="small" class="update-tag">
                    已更新
                  </el-tag>
                  <div class="prompt-actions">
                    <el-button type="text" size="small" @click="editImagePrompt($index)">
                      <el-icon><Edit /></el-icon>
                      编辑
                    </el-button>
                  </div>
                </div>
                <div v-else class="prompt-editor">
                  <el-input
                    v-model="row.tempImagePrompt"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入文生图提示词..."
                    class="prompt-textarea"
                  />
                  <div class="editor-actions">
                    <el-button type="success" size="small" @click="saveImagePrompt($index)" :loading="row.savingImagePrompt">
                      <el-icon><Check /></el-icon>
                      保存
                    </el-button>
                    <el-button type="info" size="small" @click="cancelEditImagePrompt($index)">
                      <el-icon><Close /></el-icon>
                      取消
                    </el-button>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="videoPrompt" label="图生视频提示词" min-width="200">
              <template #default="{ row, $index }">
                <div v-if="!row.editingVideoPrompt" class="prompt-content" :class="{ 'prompt-updated': row.promptUpdated }">
                  {{ row.videoPrompt }}
                  <el-tag v-if="row.promptUpdated" type="success" size="small" class="update-tag">
                    已更新
                  </el-tag>
                  <div class="prompt-actions">
                    <el-button type="text" size="small" @click="editVideoPrompt($index)">
                      <el-icon><Edit /></el-icon>
                      编辑
                    </el-button>
                  </div>
                </div>
                <div v-else class="prompt-editor">
                  <el-input
                    v-model="row.tempVideoPrompt"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入图生视频提示词..."
                    class="prompt-textarea"
                  />
                  <div class="editor-actions">
                    <el-button type="success" size="small" @click="saveVideoPrompt($index)" :loading="row.savingVideoPrompt">
                      <el-icon><Check /></el-icon>
                      保存
                    </el-button>
                    <el-button type="info" size="small" @click="cancelEditVideoPrompt($index)">
                      <el-icon><Close /></el-icon>
                      取消
                    </el-button>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="conceptImage" label="概念图" width="200" align="center">
              <template #default="{ row }">
                <div class="concept-image">
                  <el-image
                    v-if="row.conceptImage"
                    :src="row.conceptImage"
                    :preview-src-list="[row.conceptImage]"
                    fit="cover"
                    style="width: 168px; height: 95px;"
                    preview-teleported="true"
                  />
                  <div v-else class="no-image">
                    <el-icon><Picture /></el-icon>
                    <span>暂无图片</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" align="center">
              <template #default="{ row, $index }">
                <div class="operation-buttons">
                  <el-button type="primary" size="small" @click="generateImage($index)" :loading="row.generating">
                    {{ row.generating ? '生成中' : '生成图片' }}
                  </el-button>
                  <el-button type="success" size="small" @click="regeneratePrompts($index)" :loading="row.regenerating" plain>
                    {{ row.regenerating ? 'AI生成中...' : 'AI生成提示词' }}
                  </el-button>
                  <el-button type="warning" size="small" @click="openStyleDialog($index)" plain>
                    <el-icon><Brush /></el-icon>
                    选择风格
                  </el-button>
                  <el-button type="danger" size="small" @click="deleteScene($index)" v-if="storyboardData.length > 1" plain>
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <!-- 添加场景按钮 -->
          <div class="add-scene-button">
            <el-button type="primary" @click="addScene" circle size="large">
              <el-icon><Plus /></el-icon>
            </el-button>
          </div>
        </div>

        <div v-else class="no-storyboard">
          <el-empty description="暂无分镜头脚本">
            <template #image>
              <el-icon size="100"><VideoCamera /></el-icon>
            </template>
            <template #description>
              <span>暂时没有分镜头脚本，请手动添加场景开始创作</span>
            </template>
          </el-empty>
        </div>
      </div>
    </el-card>

    <!-- 风格选择对话框 -->
    <el-dialog
      v-model="styleDialogVisible"
      title="选择图像风格"
      width="80%"
      :before-close="closeStyleDialog"
      class="style-dialog"
    >
      <div class="style-content">
        <div class="style-tip">
          <el-alert
            title="选择您喜欢的风格，将自动添加到文生图提示词中"
            type="info"
            show-icon
            :closable="false"
          />
        </div>

        <div class="style-grid">
          <div
            v-for="style in imageStyles"
            :key="style.id"
            class="style-card"
            :class="{ 'selected': selectedStyles.includes(style.id) }"
            @click="toggleStyle(style.id)"
          >
            <div class="style-icon">
              <el-icon size="30">
                <component :is="style.icon" />
              </el-icon>
            </div>
            <div class="style-info">
              <h4>{{ style.name }}</h4>
              <p>{{ style.description }}</p>
              <div class="style-keywords">
                <el-tag
                  v-for="keyword in style.keywords.slice(0, 3)"
                  :key="keyword"
                  size="small"
                  effect="plain"
                >
                  {{ keyword }}
                </el-tag>
              </div>
            </div>
            <div class="style-check" v-if="selectedStyles.includes(style.id)">
              <el-icon><Check /></el-icon>
            </div>
          </div>
        </div>

        <div class="selected-preview" v-if="selectedStyles.length > 0">
          <h4>已选择的风格关键词：</h4>
          <div class="preview-text">
            {{ getSelectedStyleKeywords() }}
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeStyleDialog">取消</el-button>
          <el-button @click="clearSelectedStyles" v-if="selectedStyles.length > 0">清空选择</el-button>
          <el-button type="primary" @click="applySelectedStyles" :disabled="selectedStyles.length === 0">
            应用风格 ({{ selectedStyles.length }})
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Picture, VideoCamera, Edit, Check, Close, Back, Plus, Delete, Brush,
  Camera, Cpu, Setting, Monitor, Lightning, Tools
} from '@element-plus/icons-vue'
import { storyboardService } from '@/services/storyboardService'

const route = useRoute()
const router = useRouter()

// 响应式数据
const storyId = ref(route.query.storyId)
const storyTitle = ref(route.query.title || '未知故事')
const storyboardData = ref([])
const loading = ref(false)

// 风格选择相关
const styleDialogVisible = ref(false)
const currentEditingIndex = ref(-1)
const selectedStyles = ref([])

// 按钮文案
const backButtonText = ref(window.opener ? '关闭窗口' : '返回故事列表')

// 预设图像风格数据
const imageStyles = ref([
  {
    id: 'realistic',
    name: '写实风格',
    description: '真实摄影般的效果',
    icon: Camera,
    keywords: ['写实风格', '摄影级效果', '高清相机拍摄', '自然光影', '景深真实', '光影写实']
  },
  {
    id: 'illustration',
    name: '插画风格',
    description: '手绘动漫风格',
    icon: Brush,
    keywords: ['插画风格', '手绘风格', '动漫渲染', '二次元风', '卡通线条', '明亮色彩', '干净轮廓']
  },
  {
    id: 'cyberpunk',
    name: '赛博朋克风',
    description: '未来科技感',
    icon: Cpu,
    keywords: ['赛博朋克风', '未来都市风', '霓虹风格', '蓝紫色调', '科技质感', '夜景风格', '金属光泽']
  },
  {
    id: 'oilpainting',
    name: '油画风格',
    description: '古典艺术质感',
    icon: Picture,
    keywords: ['油画风格', '厚涂笔触', '复古艺术风', '画布质感', '文艺复兴风格', '色彩层次丰富']
  },
  {
    id: 'chinese',
    name: '中国风',
    description: '传统水墨意境',
    icon: Monitor,
    keywords: ['中国风', '水墨风格', '国风插画', '古典韵味', '意境山水', '留白构图', '仙气缭绕']
  },
  {
    id: 'gothic',
    name: '哥特/黑暗风格',
    description: '神秘黑暗氛围',
    icon: Setting,
    keywords: ['哥特风', '黑暗艺术风', '阴影构图', '灰暗色调', '颓废氛围', '恐怖幻想风格']
  },
  {
    id: 'fairytale',
    name: '童话梦幻风',
    description: '奇幻梦境效果',
    icon: Lightning,
    keywords: ['梦幻风格', '童话插画', '奇幻森林', '发光元素', '光晕效果', '漂浮物体', '精灵风']
  },
  {
    id: 'scifi',
    name: '科幻未来风',
    description: '太空科技感',
    icon: Monitor,
    keywords: ['未来科技风', '宇宙背景', '太空站元素', 'AI感十足', '赛博元素', '蓝光氛围']
  },
  {
    id: 'steampunk',
    name: '蒸汽朋克风',
    description: '复古机械美学',
    icon: Tools,
    keywords: ['蒸汽朋克风', '齿轮装置', '铜铁质感', '复古机械', '雾气氤氲']
  },
  {
    id: 'minimalism',
    name: '极简风格',
    description: '简约几何美学',
    icon: Tools,
    keywords: ['极简风', '留白构图', '单色背景', '几何美学', '干净画面', '简约构图']
  }
])

// 检查是否可以生成视频
const canGenerateVideo = computed(() => {
  return storyboardData.value.length > 0 &&
         storyboardData.value.every(scene =>
           scene.conceptImage &&
           scene.videoPrompt &&
           scene.videoPrompt.trim() !== '请输入图生视频提示词...'
         )
})

// 风格选择相关方法
const openStyleDialog = (index) => {
  currentEditingIndex.value = index
  selectedStyles.value = []
  styleDialogVisible.value = true
}

const closeStyleDialog = () => {
  styleDialogVisible.value = false
  currentEditingIndex.value = -1
  selectedStyles.value = []
}

const toggleStyle = (styleId) => {
  const index = selectedStyles.value.indexOf(styleId)
  if (index > -1) {
    selectedStyles.value.splice(index, 1)
  } else {
    selectedStyles.value.push(styleId)
  }
}

const clearSelectedStyles = () => {
  selectedStyles.value = []
}

const getSelectedStyleKeywords = () => {
  const keywords = []
  selectedStyles.value.forEach(styleId => {
    const style = imageStyles.value.find(s => s.id === styleId)
    if (style) {
      keywords.push(...style.keywords)
    }
  })
  return keywords.join(', ')
}

const applySelectedStyles = async () => {
  if (selectedStyles.value.length === 0 || currentEditingIndex.value === -1) {
    return
  }

  const item = storyboardData.value[currentEditingIndex.value]
  const styleKeywords = getSelectedStyleKeywords()

  // 清理现有提示词中的默认文本
  let currentPrompt = item.imagePrompt || ''
  if (currentPrompt.includes('请输入文生图提示词...') || currentPrompt.trim() === '') {
    currentPrompt = ''
  }

  // 添加风格关键词
  const newPrompt = currentPrompt ? `${currentPrompt}, ${styleKeywords}` : styleKeywords

  try {
    // 准备更新数据
    const updatedItem = {
      storyId: storyId.value,
      scene: item.scene,
      script: item.script,
      imagePrompt: newPrompt,
      videoPrompt: item.videoPrompt,
      conceptImage: null // 清除概念图，需要重新生成
    }

    // 调用API更新图像提示词
    const updatedStoryboard = await storyboardService.updateStoryboard(item.id, updatedItem)

    // 更新本地数据
    item.imagePrompt = updatedStoryboard.imagePrompt
    item.conceptImage = updatedStoryboard.conceptImage

    // 标记提示词已更新
    item.promptUpdated = true

    ElMessage.success(`已应用 ${selectedStyles.value.length} 种风格到文生图提示词`)

    // 3秒后移除更新标记
    setTimeout(() => {
      item.promptUpdated = false
    }, 3000)

    closeStyleDialog()
  } catch (error) {
    ElMessage.error(error.message || '应用风格失败')
    console.error(error)
  }
}

// 返回故事列表
const goBack = () => {
  // 检查是否是在新窗口中打开
  if (window.opener) {
    // 如果是在新窗口中打开，关闭当前窗口
    window.close()
  } else {
    // 如果不是在新窗口中打开，使用路由跳转
    router.push('/stories')
  }
}

// 跳转到视频生成页面
const goToVideoGeneration = () => {
  router.push({
    path: '/video-generation',
    query: {
      storyId: storyId.value,
      title: storyTitle.value
    }
  })
}

// 获取分镜头脚本
const loadStoryboard = async () => {
  loading.value = true
  try {
    const storyboards = await storyboardService.getStoryboard(storyId.value)

    // 转换后端数据格式为前端需要的格式
    storyboardData.value = storyboards.map(item => ({
      id: item.id,
      scene: item.scene,
      script: item.script,
      imagePrompt: item.imagePrompt,
      videoPrompt: item.videoPrompt,
      conceptImage: item.conceptImage,
      // 前端状态字段
      generating: false,
      editing: false,
      tempScript: '',
      saving: false,
      regenerating: false,
      promptUpdated: false,
      editingImagePrompt: false,
      tempImagePrompt: '',
      savingImagePrompt: false,
      editingVideoPrompt: false,
      tempVideoPrompt: '',
      savingVideoPrompt: false
    }))
  } catch (error) {
    console.error('获取分镜头脚本失败:', error)
    // 如果获取失败，可能是还没有生成分镜头脚本
    storyboardData.value = []
  } finally {
    loading.value = false
  }
}

// 生成概念图
const generateImage = async (index) => {
  const item = storyboardData.value[index]

  // 检查是否有文生图提示词
  if (!item.imagePrompt || item.imagePrompt.trim() === '' || item.imagePrompt.includes('请输入')) {
    ElMessage.warning('请先生成或编辑文生图提示词，然后再生成概念图')
    return
  }

  item.generating = true

  try {
    // 创建AbortController用于超时控制
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 60000) // 60秒超时

    // 调用后端API生成概念图
    const response = await fetch(`http://localhost:8080/api/storyboards/${item.id}/generate-concept-image`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      signal: controller.signal
    })

    clearTimeout(timeoutId)

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`生成概念图失败: ${errorText}`)
    }

    const updatedStoryboard = await response.json()

    // 更新本地数据
    item.conceptImage = updatedStoryboard.conceptImage
    ElMessage.success(`场景 ${item.scene} 概念图生成成功！`)
  } catch (error) {
    if (error.name === 'AbortError') {
      ElMessage.error('生成概念图超时，请重试')
    } else {
      ElMessage.error(error.message || '生成概念图失败')
    }
    console.error(error)
  } finally {
    item.generating = false
  }
}

// 开始编辑脚本
const editScript = (index) => {
  const item = storyboardData.value[index]
  item.editing = true
  item.tempScript = item.script
}

// 取消编辑
const cancelEdit = (index) => {
  const item = storyboardData.value[index]
  item.editing = false
  item.tempScript = ''
}

// 保存脚本修改
const saveScript = async (index) => {
  const item = storyboardData.value[index]

  if (!item.tempScript.trim()) {
    ElMessage.warning('分镜头脚本不能为空')
    return
  }

  item.saving = true

  try {
    // 准备更新数据
    const updatedItem = {
      storyId: storyId.value,
      scene: item.scene,
      script: item.tempScript,
      imagePrompt: item.imagePrompt,
      videoPrompt: item.videoPrompt,
      conceptImage: item.conceptImage
    }

    // 调用API更新脚本
    const updatedStoryboard = await storyboardService.updateStoryboard(item.id, updatedItem)

    // 更新本地数据
    item.script = updatedStoryboard.script
    item.imagePrompt = updatedStoryboard.imagePrompt
    item.videoPrompt = updatedStoryboard.videoPrompt
    item.conceptImage = updatedStoryboard.conceptImage

    item.editing = false
    item.tempScript = ''
    ElMessage.success('分镜头脚本保存成功！')
  } catch (error) {
    ElMessage.error(error.message || '保存分镜头脚本失败')
    console.error(error)
  } finally {
    item.saving = false
  }
}

// 开始编辑图像提示词
const editImagePrompt = (index) => {
  const item = storyboardData.value[index]
  item.editingImagePrompt = true
  item.tempImagePrompt = item.imagePrompt
}

// 取消编辑图像提示词
const cancelEditImagePrompt = (index) => {
  const item = storyboardData.value[index]
  item.editingImagePrompt = false
  item.tempImagePrompt = ''
}

// 保存图像提示词
const saveImagePrompt = async (index) => {
  const item = storyboardData.value[index]

  if (!item.tempImagePrompt.trim()) {
    ElMessage.warning('文生图提示词不能为空')
    return
  }

  item.savingImagePrompt = true

  try {
    // 准备更新数据
    const updatedItem = {
      storyId: storyId.value,
      scene: item.scene,
      script: item.script,
      imagePrompt: item.tempImagePrompt,
      videoPrompt: item.videoPrompt,
      conceptImage: null // 清除概念图，需要重新生成
    }

    // 调用API更新图像提示词
    const updatedStoryboard = await storyboardService.updateStoryboard(item.id, updatedItem)

    // 更新本地数据
    item.imagePrompt = updatedStoryboard.imagePrompt
    item.conceptImage = updatedStoryboard.conceptImage
    item.editingImagePrompt = false
    item.tempImagePrompt = ''

    // 标记提示词已更新
    item.promptUpdated = true

    ElMessage.success('文生图提示词保存成功！')

    // 3秒后移除更新标记
    setTimeout(() => {
      item.promptUpdated = false
    }, 3000)
  } catch (error) {
    ElMessage.error(error.message || '保存文生图提示词失败')
    console.error(error)
  } finally {
    item.savingImagePrompt = false
  }
}

// 开始编辑视频提示词
const editVideoPrompt = (index) => {
  const item = storyboardData.value[index]
  item.editingVideoPrompt = true
  item.tempVideoPrompt = item.videoPrompt
}

// 取消编辑视频提示词
const cancelEditVideoPrompt = (index) => {
  const item = storyboardData.value[index]
  item.editingVideoPrompt = false
  item.tempVideoPrompt = ''
}

// 保存视频提示词
const saveVideoPrompt = async (index) => {
  const item = storyboardData.value[index]

  if (!item.tempVideoPrompt.trim()) {
    ElMessage.warning('图生视频提示词不能为空')
    return
  }

  item.savingVideoPrompt = true

  try {
    // 准备更新数据
    const updatedItem = {
      storyId: storyId.value,
      scene: item.scene,
      script: item.script,
      imagePrompt: item.imagePrompt,
      videoPrompt: item.tempVideoPrompt,
      conceptImage: item.conceptImage
    }

    // 调用API更新视频提示词
    const updatedStoryboard = await storyboardService.updateStoryboard(item.id, updatedItem)

    // 更新本地数据
    item.videoPrompt = updatedStoryboard.videoPrompt
    item.editingVideoPrompt = false
    item.tempVideoPrompt = ''

    // 标记提示词已更新
    item.promptUpdated = true

    ElMessage.success('图生视频提示词保存成功！')

    // 3秒后移除更新标记
    setTimeout(() => {
      item.promptUpdated = false
    }, 3000)
  } catch (error) {
    ElMessage.error(error.message || '保存图生视频提示词失败')
    console.error(error)
  } finally {
    item.savingVideoPrompt = false
  }
}

// 重新生成提示词（使用千帆AI）
const regeneratePrompts = async (index) => {
  const item = storyboardData.value[index]

  // 检查分镜头脚本是否为空
  if (!item.script || item.script.trim() === '' || item.script.includes('请输入')) {
    ElMessage.warning('请先编写分镜头脚本内容，然后再生成提示词')
    return
  }

  item.regenerating = true

  try {
    // 创建AbortController用于超时控制
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 60000) // 60秒超时

    // 调用千帆API生成AI提示词
    const response = await fetch(`http://localhost:8080/api/storyboards/${item.id}/generate-ai-prompts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      signal: controller.signal
    })

    clearTimeout(timeoutId)

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`生成AI提示词失败: ${errorText}`)
    }

    const updatedStoryboard = await response.json()

    // 更新本地数据
    item.imagePrompt = updatedStoryboard.imagePrompt
    item.videoPrompt = updatedStoryboard.videoPrompt
    // 清除概念图，需要重新生成
    item.conceptImage = null

    // 标记提示词已更新
    item.promptUpdated = true

    ElMessage.success('AI提示词生成成功！')

    // 3秒后移除更新标记
    setTimeout(() => {
      item.promptUpdated = false
    }, 3000)
  } catch (error) {
    if (error.name === 'AbortError') {
      ElMessage.error('生成AI提示词超时，请重试')
    } else {
      ElMessage.error(error.message || '生成AI提示词失败')
    }
    console.error(error)
  } finally {
    item.regenerating = false
  }
}

// 添加场景
const addScene = async () => {
  try {
    const newSceneNumber = storyboardData.value.length + 1
    const newScene = {
      storyId: parseInt(storyId.value),
      scene: newSceneNumber,
      script: '请输入新场景的分镜头脚本...',
      imagePrompt: '请输入文生图提示词...',
      videoPrompt: '请输入图生视频提示词...'
    }

    console.log('准备发送新场景数据:', newScene)

    // 直接使用简单的POST请求，不走拦截器
    const response = await fetch('http://localhost:8080/api/storyboards', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(newScene)
    })

    console.log('响应状态:', response.status)

    if (response.ok) {
      const savedScene = await response.json()
      console.log('保存成功，返回数据:', savedScene)

            // 使用简单的fetch请求重新排序场景编号
      const reorderResponse = await fetch(`http://localhost:8080/api/storyboards/story/${storyId.value}/reorder`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        }
      })

      if (reorderResponse.ok) {
        const reorderData = await reorderResponse.json()
        // 更新本地数据为重新排序后的数据
        storyboardData.value = reorderData.map(item => {
          // 查找是否已存在前端状态字段
          const existingItem = storyboardData.value.find(existing => existing.id === item.id)
          return {
            id: item.id,
            scene: item.scene,
            script: item.script,
            imagePrompt: item.imagePrompt,
            videoPrompt: item.videoPrompt,
            conceptImage: item.conceptImage,
            // 保持现有的前端状态字段，新场景设置默认值
            generating: existingItem?.generating || false,
            editing: existingItem?.editing || false,
            tempScript: existingItem?.tempScript || '',
            saving: existingItem?.saving || false,
            regenerating: existingItem?.regenerating || false,
            promptUpdated: existingItem?.promptUpdated || false,
            editingImagePrompt: existingItem?.editingImagePrompt || false,
            tempImagePrompt: existingItem?.tempImagePrompt || '',
            savingImagePrompt: existingItem?.savingImagePrompt || false,
            editingVideoPrompt: existingItem?.editingVideoPrompt || false,
            tempVideoPrompt: existingItem?.tempVideoPrompt || '',
            savingVideoPrompt: existingItem?.savingVideoPrompt || false
          }
        })
        ElMessage.success('场景添加成功！')
      } else {
        console.error('重新排序失败:', reorderResponse.status)
        ElMessage.error('添加场景后重新排序失败')
      }
    } else {
      const errorText = await response.text()
      console.error('添加场景失败:', response.status, errorText)
      ElMessage.error(`添加场景失败: ${response.status}`)
    }
  } catch (error) {
    console.error('请求错误:', error)
    ElMessage.error('添加场景失败')
  }
}

// 删除场景
const deleteScene = async (index) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个场景吗？此操作不可撤销。',
      '删除场景',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const scene = storyboardData.value[index]

    // 从数据库删除
    await storyboardService.deleteStoryboard(scene.id)

        // 使用简单的fetch请求重新排序场景编号
    const reorderResponse = await fetch(`http://localhost:8080/api/storyboards/story/${storyId.value}/reorder`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      }
    })

    if (reorderResponse.ok) {
      const reorderData = await reorderResponse.json()
      // 更新本地数据为重新排序后的数据
      storyboardData.value = reorderData.map(item => {
        // 查找是否已存在前端状态字段
        const existingItem = storyboardData.value.find(existing => existing.id === item.id)
        return {
          id: item.id,
          scene: item.scene,
          script: item.script,
          imagePrompt: item.imagePrompt,
          videoPrompt: item.videoPrompt,
          conceptImage: item.conceptImage,
          // 保持现有的前端状态字段
          generating: existingItem?.generating || false,
          editing: existingItem?.editing || false,
          tempScript: existingItem?.tempScript || '',
          saving: existingItem?.saving || false,
          regenerating: existingItem?.regenerating || false,
          promptUpdated: existingItem?.promptUpdated || false,
          editingImagePrompt: existingItem?.editingImagePrompt || false,
          tempImagePrompt: existingItem?.tempImagePrompt || '',
          savingImagePrompt: existingItem?.savingImagePrompt || false,
          editingVideoPrompt: existingItem?.editingVideoPrompt || false,
          tempVideoPrompt: existingItem?.tempVideoPrompt || '',
          savingVideoPrompt: existingItem?.savingVideoPrompt || false
        }
      })
      ElMessage.success('场景删除成功！')
    } else {
      ElMessage.error('删除场景失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除场景失败')
      console.error(error)
    }
  }
}

onMounted(async () => {
  if (!storyId.value) {
    ElMessage.warning('未找到故事信息，返回故事列表')
    router.push('/stories')
    return
  }

  // 加载现有的分镜头脚本
  await loadStoryboard()
})
</script>

<style scoped>
.storyboard-page {
  min-height: 100vh;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.storyboard-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.15);
  transition: all 0.3s ease;
}

.storyboard-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 10px 0;
}

.storyboard-title {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.story-title {
  font-size: 24px;
  font-weight: 600;
  color: #4a5568;
}

.subtitle {
  font-size: 24px;
  color: #667eea;
  font-weight: 600;
}

.generate-video-section {
  margin: 20px 0;
  text-align: center;
}

.generate-video-btn {
  padding: 12px 24px;
  font-size: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.generate-video-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.video-camera-icon {
  margin-right: 8px;
  font-size: 20px;
}

.storyboard-table {
  margin: 20px 0;
}

.storyboard-table :deep(.el-table) {
  background: transparent;
  border-radius: 12px;
  overflow: hidden;
}

.storyboard-table :deep(.el-table__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.storyboard-table :deep(.el-table__header-row th) {
  background: transparent;
  color: white;
  font-weight: 600;
  padding: 16px 0;
}

.storyboard-table :deep(.el-table__row) {
  background: rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.storyboard-table :deep(.el-table__row:hover) {
  background: rgba(255, 255, 255, 0.95);
  transform: translateY(-2px);
}

.script-content, .prompt-content {
  position: relative;
  padding: 12px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  border: 1px solid rgba(102, 126, 234, 0.2);
  min-height: 80px;
}

.script-actions, .prompt-actions {
  position: absolute;
  right: 8px;
  bottom: 8px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.script-content:hover .script-actions,
.prompt-content:hover .prompt-actions {
  opacity: 1;
}

.script-editor, .prompt-editor {
  padding: 12px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.1);
}

.editor-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.concept-image {
  padding: 8px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  border: 1px solid rgba(102, 126, 234, 0.2);
  transition: all 0.3s ease;
}

.concept-image:hover {
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.no-image {
  height: 95px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #a0aec0;
  background: rgba(102, 126, 234, 0.05);
  border-radius: 8px;
}

.operation-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.add-scene-button {
  margin: 20px 0;
  text-align: center;
}

.add-scene-button .el-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  width: 48px;
  height: 48px;
  transition: all 0.3s ease;
}

.add-scene-button .el-button:hover {
  transform: rotate(90deg) scale(1.1);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.style-dialog {
  border-radius: 16px;
  overflow: hidden;
}

.style-dialog :deep(.el-dialog) {
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.15);
}

.style-content {
  padding: 20px 0;
}

.style-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
  margin: 20px 0;
}

.style-card {
  position: relative;
  padding: 20px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.style-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.style-card.selected {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  border-color: #667eea;
}

.style-icon {
  margin-bottom: 12px;
  color: #667eea;
}

.style-info h4 {
  margin: 0 0 8px;
  color: #4a5568;
  font-size: 16px;
}

.style-info p {
  margin: 0 0 12px;
  color: #718096;
  font-size: 14px;
}

.style-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.style-check {
  position: absolute;
  top: 12px;
  right: 12px;
  color: #667eea;
  font-size: 20px;
}

.selected-preview {
  margin-top: 20px;
  padding: 16px;
  background: rgba(102, 126, 234, 0.05);
  border-radius: 12px;
}

.selected-preview h4 {
  margin: 0 0 12px;
  color: #4a5568;
}

.preview-text {
  color: #667eea;
  font-size: 14px;
  line-height: 1.6;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid rgba(102, 126, 234, 0.1);
}

/* 响应式优化 */
@media (max-width: 768px) {
  .storyboard-page {
    padding: 10px;
  }

  .storyboard-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .operation-buttons {
    flex-direction: column;
  }

  .style-grid {
    grid-template-columns: 1fr;
  }
}
</style>
