<template>
  <div class="video-generation-page">
    <el-card class="video-generation-card">
      <template #header>
        <div class="video-generation-header">
          <el-button type="primary" @click="goBack" plain>
            <el-icon><Back /></el-icon>
            返回分镜头
          </el-button>
          <div class="video-generation-title">
            <span class="story-title">{{ storyTitle }}</span>
            <span class="subtitle">图生视频</span>
          </div>
          <el-button type="success" @click="generateAllVideos" :loading="allGenerating">
            {{ allGenerating ? '生成中...' : '一键生成所有视频' }}
          </el-button>
          <el-button
            v-if="canGenerateFinalVideo"
            type="danger"
            @click="goToFinalVideo"
            plain
          >
            <el-icon><VideoCamera /></el-icon>
            生成最终作品
          </el-button>
        </div>
      </template>

      <div class="video-generation-content">
        <div v-if="loading" class="loading-section">
          <el-skeleton :rows="5" animated />
        </div>

        <div v-else-if="storyboardData.length > 0" class="scenes-container">
          <div v-for="(scene, index) in storyboardData" :key="scene.id" class="scene-card">
            <el-card>
              <template #header>
                <div class="scene-header">
                  <span class="scene-number">场景 {{ scene.scene }}</span>
                  <el-button
                    type="primary"
                    @click="generateVideo(index)"
                    :loading="scene.videoGenerating"
                    size="default"
                    class="generate-video-btn"
                  >
                    {{ scene.videoGenerating ? '生成中' : '生成视频' }}
                  </el-button>
                </div>
              </template>

              <div class="scene-content">
                <div class="scene-row">
                  <!-- 左侧：概念图和提示词 -->
                  <div class="left-section">
                    <!-- 概念图 -->
                    <div class="concept-image-section">
                      <h4>概念图</h4>
                      <el-image
                        :src="scene.conceptImage"
                        :preview-src-list="[scene.conceptImage]"
                        fit="cover"
                        style="width: 100%; height: 200px; border-radius: 8px;"
                        preview-teleported="true"
                      />
                    </div>

                                         <!-- 视频提示词 -->
                     <div class="video-prompt-section">
                       <h4>图生视频提示词</h4>
                       <div v-if="!scene.editingVideoPrompt" class="prompt-content">
                         {{ scene.videoPrompt }}
                         <div class="prompt-actions">
                           <el-button type="text" size="small" @click="editVideoPrompt(index)">
                             <el-icon><Edit /></el-icon>
                             编辑
                           </el-button>
                         </div>
                       </div>
                       <div v-else class="prompt-editor">
                         <el-input
                           v-model="scene.tempVideoPrompt"
                           type="textarea"
                           :rows="4"
                           placeholder="请输入图生视频提示词..."
                           class="prompt-textarea"
                         />
                         <div class="editor-actions">
                           <el-button type="success" size="small" @click="saveVideoPrompt(index)" :loading="scene.savingVideoPrompt">
                             <el-icon><Check /></el-icon>
                             保存
                           </el-button>
                           <el-button type="info" size="small" @click="cancelEditVideoPrompt(index)">
                             <el-icon><Close /></el-icon>
                             取消
                           </el-button>
                         </div>
                       </div>
                     </div>
                  </div>

                  <!-- 右侧：生成的视频 -->
                  <div class="right-section">
                    <!-- 视频状态显示 -->
                    <div class="video-status" v-if="scene.videoStatus">
                      <el-tag
                        :type="getStatusType(scene.videoStatus)"
                        size="small"
                        class="status-tag"
                      >
                        {{ getStatusText(scene.videoStatus) }}
                      </el-tag>
                      <el-tag v-if="scene.audioVideo && scene.videoStatus === 'completed'" type="success" size="small" effect="light" class="status-tag">
                        带音效版本
                      </el-tag>
                                             <!-- 生成中的进度提示 -->
                       <div v-if="scene.videoGenerating || ['generating', 'submitted', 'submitting'].includes(scene.videoStatus)" class="generating-info">
                         <el-icon class="is-loading" style="margin-right: 8px;"><Loading /></el-icon>
                         <span class="generating-text">
                           {{ getGeneratingMessage(scene) }}
                         </span>
                       </div>
                    </div>

                    <!-- 生成的视频 -->
                    <div class="generated-video-section" v-if="scene.generatedVideo && scene.videoStatus === 'completed'">


                       <video
                         :src="scene.audioVideo || scene.generatedVideo"
                         controls
                         crossorigin="anonymous"
                         style="width: 100%; max-width: 800px; height: auto; border-radius: 8px;"
                         preload="metadata"
                         @error="handleVideoError($event, scene)"
                         @loadstart="console.log('视频开始加载:', scene.audioVideo || scene.generatedVideo)"
                         @loadeddata="console.log('视频数据已加载:', scene.audioVideo || scene.generatedVideo)"
                         @canplay="console.log('视频可以播放:', scene.audioVideo || scene.generatedVideo)"
                       >
                         您的浏览器不支持视频播放。
                         <source :src="scene.audioVideo || scene.generatedVideo" type="video/mp4">
                       </video>

                       <!-- 如果视频加载失败，显示备选方案 -->
                       <div class="video-fallback" style="margin-top: 10px;">
                         <el-button type="primary" size="small" @click="openVideoInNewTab(scene.audioVideo || scene.generatedVideo)">
                           <el-icon><VideoCamera /></el-icon>
                           在新窗口打开视频
                         </el-button>
                         <el-button type="info" size="small" @click="copyVideoUrl(scene.audioVideo || scene.generatedVideo)">
                           <el-icon><Download /></el-icon>
                           复制视频链接
                         </el-button>
                       </div>
                       <div class="video-actions">
                         <el-button type="success" size="small" @click="downloadVideo(scene)" :loading="scene.downloading">
                           <el-icon><Download /></el-icon>
                           {{ scene.downloading ? '下载中...' : '下载视频' }}
                         </el-button>
                         <el-button type="warning" size="small" @click="regenerateVideo(index)">
                           <el-icon><Refresh /></el-icon>
                           重新生成
                         </el-button>
                         <el-button
                           type="primary"
                           size="small"
                           @click="generateAudio(scene)"
                           :loading="scene.audioGenerating"
                           :disabled="!scene.generatedVideo"
                         >
                           <el-icon><VideoCamera /></el-icon>
                           {{ scene.audioGenerating ? '生成音效中...' : (scene.audioVideo ? '重新生成音效' : '生成音效') }}
                         </el-button>
                       </div>
                     </div>

                    <!-- 视频生成占位 -->
                    <div class="video-placeholder" v-else>
                      <el-icon size="80" color="#c0c4cc"><VideoCamera /></el-icon>
                      <span>点击"生成视频"按钮开始制作</span>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </div>
        </div>

        <div v-else class="no-scenes">
          <el-empty description="暂无场景数据">
            <template #image>
              <el-icon size="100"><VideoCamera /></el-icon>
            </template>
            <template #description>
              <span>请先回到分镜头页面完善场景信息</span>
            </template>
          </el-empty>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, VideoCamera, Download, Refresh, Edit, Check, Close, Loading } from '@element-plus/icons-vue'
import { storyboardService } from '@/services/storyboardService'
import { Client } from '@gradio/client'

const route = useRoute()
const router = useRouter()

// 响应式数据
const storyId = ref(route.query.storyId)
const storyTitle = ref(route.query.title || '未知故事')
const storyboardData = ref([])
const loading = ref(false)
const allGenerating = ref(false)

// 返回分镜头页面
const goBack = () => {
  router.push({
    path: '/storyboard',
    query: {
      storyId: storyId.value,
      title: storyTitle.value
    }
  })
}

// 检查是否可以生成最终作品（所有场景都有视频）
const canGenerateFinalVideo = computed(() => {
  return storyboardData.value.length > 0 &&
         storyboardData.value.every(scene => scene.videoStatus === 'completed' && scene.generatedVideo)
})

// 跳转到最终作品页面
const goToFinalVideo = () => {
  router.push({
    path: '/final-video',
    query: {
      storyId: storyId.value,
      title: storyTitle.value
    }
  })
}

// 获取分镜头数据
const loadStoryboard = async () => {
  loading.value = true
  try {
    const storyboards = await storyboardService.getStoryboard(storyId.value)

    // 转换数据格式
    storyboardData.value = storyboards.map(item => ({
      id: item.id,
      scene: item.scene,
      script: item.script,
      imagePrompt: item.imagePrompt,
      videoPrompt: item.videoPrompt,
      conceptImage: item.conceptImage,
      networkImageUrl: item.networkImageUrl,
      // 视频生成相关字段（从数据库加载）
      generatedVideo: item.generatedVideo,
      videoStatus: item.videoStatus,
      videoGeneratedAt: item.videoGeneratedAt,
      // 音效相关字段（从数据库加载）
      audioVideo: item.audioVideo,
      audioStatus: item.audioStatus,
      audioGeneratedAt: item.audioGeneratedAt,
      // 前端状态字段
      videoGenerating: false,
      downloading: false,
      audioGenerating: false,
      // 编辑状态字段
      editingVideoPrompt: false,
      tempVideoPrompt: '',
      savingVideoPrompt: false
    }))
  } catch (error) {
    console.error('获取分镜头数据失败:', error)
    ElMessage.error('获取分镜头数据失败')
  } finally {
    loading.value = false
  }
}

// 生成单个视频
const generateVideo = async (index) => {
  const scene = storyboardData.value[index]
  scene.videoGenerating = true

  try {
    // 设置状态为生成中
    await storyboardService.updateVideoInfo(scene.id, null, 'generating')
    scene.videoStatus = 'generating'

    // 提交图生视频任务到山火API
    const generateRequest = {
      storyboardId: scene.id,
      imageUrl: scene.networkImageUrl || scene.conceptImage, // 优先使用网络URL
      prompt: scene.videoPrompt || '',
      aspectRatio: '16:9' // 默认使用16:9比例
    }

    // 调试信息
    console.log('发送视频生成请求:', {
      storyboardId: scene.id,
      imageUrl: generateRequest.imageUrl,
      networkImageUrl: scene.networkImageUrl,
      conceptImage: scene.conceptImage,
      prompt: scene.videoPrompt
    })

    // 调用后端API提交视频生成任务
    const response = await fetch('http://localhost:8080/api/video/generate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify(generateRequest)
    })

    const result = await response.json()

    if (!response.ok || !result.success) {
      throw new Error(result.error || '提交视频生成任务失败')
    }

    ElMessage.success(`场景 ${scene.scene} 视频生成任务提交成功，正在处理中...`)

    // 开始轮询任务状态
    await pollVideoTaskStatus(result.taskId, scene)

  } catch (error) {
    // 更新状态为失败
    try {
      await storyboardService.updateVideoInfo(scene.id, null, 'failed')
      scene.videoStatus = 'failed'
    } catch (updateError) {
      console.error('更新视频状态失败:', updateError)
    }

    ElMessage.error(`场景 ${scene.scene} 视频生成失败: ${error.message}`)
    console.error(error)
  } finally {
    scene.videoGenerating = false
  }
}

// 轮询视频任务状态
const pollVideoTaskStatus = async (taskId, scene) => {
  const maxAttempts = 60 // 最多轮询60次 (约10分钟)
  const pollInterval = 10000 // 每10秒轮询一次
  let attempts = 0

  const poll = async () => {
    attempts++

    try {
      const response = await fetch(`http://localhost:8080/api/video/task/${taskId}/status`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      })

      const result = await response.json()

      if (!response.ok) {
        throw new Error(result.error || '查询任务状态失败')
      }

      console.log(`场景 ${scene.scene} 任务状态:`, result.status)

      if (result.status === 'completed') {
        // 任务完成
        const videoUrl = result.videoUrl

        // 保存视频信息到数据库
        await storyboardService.updateVideoInfo(scene.id, videoUrl, 'completed')

        // 更新前端数据
        scene.generatedVideo = videoUrl
        scene.videoStatus = 'completed'
        scene.videoGeneratedAt = new Date().toISOString()

        ElMessage.success(`场景 ${scene.scene} 视频生成完成！`)
        return

      } else if (result.status === 'failed') {
        // 任务失败
        throw new Error(result.errorMessage || '视频生成失败')

      } else if (result.status === 'generating' || result.status === 'submitted') {
        // 任务仍在进行中，继续轮询
        if (attempts < maxAttempts) {
          setTimeout(poll, pollInterval)
        } else {
          throw new Error('视频生成超时，请稍后重试')
        }

      } else {
        // 未知状态
        throw new Error(`未知的任务状态: ${result.status}`)
      }

    } catch (error) {
      console.error('轮询任务状态失败:', error)

      // 更新状态为失败
      try {
        await storyboardService.updateVideoInfo(scene.id, null, 'failed')
        scene.videoStatus = 'failed'
      } catch (updateError) {
        console.error('更新视频状态失败:', updateError)
      }

      ElMessage.error(`场景 ${scene.scene} 视频生成失败: ${error.message}`)
    }
  }

  // 开始轮询
  setTimeout(poll, pollInterval)
}

// 重新生成视频
const regenerateVideo = async (index) => {
  const scene = storyboardData.value[index]
  scene.generatedVideo = null
  await generateVideo(index)
}

// 一键生成所有视频
const generateAllVideos = async () => {
  allGenerating.value = true

  try {
    const ungenerated = storyboardData.value.filter(scene => !scene.generatedVideo)

    for (let i = 0; i < ungenerated.length; i++) {
      const sceneIndex = storyboardData.value.findIndex(s => s.id === ungenerated[i].id)
      await generateVideo(sceneIndex)
    }

    ElMessage.success('所有视频生成完成！')
  } catch (error) {
    console.error('批量生成视频时出现错误:', error)
    ElMessage.error('批量生成视频时出现错误')
  } finally {
    allGenerating.value = false
  }
}

// 下载视频
const downloadVideo = async (scene) => {
  const videoUrl = scene.audioVideo || scene.generatedVideo

  if (!videoUrl) {
    ElMessage.warning('该场景还没有生成视频')
    return
  }

  // 设置下载状态
  scene.downloading = true

  try {
    ElMessage.info('正在准备下载...')

    // 方法1: 尝试使用fetch下载（支持跨域和进度显示）
    try {
      const response = await fetch(videoUrl, {
        mode: 'cors',
        method: 'GET',
        headers: {
          'Accept': 'video/mp4,video/*,*/*'
        }
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      // 获取视频数据
      const blob = await response.blob()

      if (blob.size === 0) {
        throw new Error('视频文件为空')
      }

      // 创建下载链接
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `场景${scene.scene}_${scene.audioVideo ? '音效' : '原始'}视频.mp4`
      link.style.display = 'none'

      // 添加到DOM并触发下载
      document.body.appendChild(link)
      link.click()

      // 清理
      document.body.removeChild(link)
      URL.revokeObjectURL(url)

      ElMessage.success('视频下载完成！')
    } catch (fetchError) {
      console.warn('Fetch下载失败，尝试直接下载:', fetchError)

      // 方法2: 直接下载（适用于同域或允许直接访问的视频）
      const link = document.createElement('a')
      link.href = videoUrl
      link.download = `场景${scene.scene}_${scene.audioVideo ? '音效' : '原始'}视频.mp4`
      link.target = '_blank'
      link.rel = 'noopener noreferrer'
      link.style.display = 'none'

      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)

      ElMessage.success('视频下载开始（如未自动下载，请右键视频另存为）')
    }

  } catch (error) {
    console.error('下载视频失败:', error)

    // 方法3: 最后的备用方案 - 在新窗口打开
    try {
      window.open(videoUrl, '_blank')
      ElMessage.warning('无法直接下载，已在新窗口打开视频。请右键选择"另存为"进行下载。')
    } catch (openError) {
      ElMessage.error('下载失败，请复制视频链接手动下载')
      console.error('所有下载方法都失败:', openError)
    }
  } finally {
    scene.downloading = false
  }
}

// 开始编辑视频提示词
const editVideoPrompt = (index) => {
  const scene = storyboardData.value[index]
  scene.editingVideoPrompt = true
  scene.tempVideoPrompt = scene.videoPrompt
}

// 取消编辑视频提示词
const cancelEditVideoPrompt = (index) => {
  const scene = storyboardData.value[index]
  scene.editingVideoPrompt = false
  scene.tempVideoPrompt = ''
}

// 保存视频提示词
const saveVideoPrompt = async (index) => {
  const scene = storyboardData.value[index]

  if (!scene.tempVideoPrompt.trim()) {
    ElMessage.warning('图生视频提示词不能为空')
    return
  }

  scene.savingVideoPrompt = true

  try {
    // 准备更新数据
    const updatedItem = {
      storyId: storyId.value,
      scene: scene.scene,
      script: scene.script,
      imagePrompt: scene.imagePrompt,
      videoPrompt: scene.tempVideoPrompt,
      conceptImage: scene.conceptImage
    }

    // 调用API更新视频提示词
    const updatedStoryboard = await storyboardService.updateStoryboard(scene.id, updatedItem)

    // 更新本地数据
    scene.videoPrompt = updatedStoryboard.videoPrompt
    scene.editingVideoPrompt = false
    scene.tempVideoPrompt = ''

    ElMessage.success('图生视频提示词保存成功！')
  } catch (error) {
    console.error('保存图生视频提示词失败:', error)
    ElMessage.error('保存图生视频提示词失败')
  } finally {
    scene.savingVideoPrompt = false
  }
}

// 获取状态标签类型
const getStatusType = (status) => {
  switch (status) {
    case 'completed': return 'success'
    case 'generating': return 'warning'
    case 'submitted': return 'warning'
    case 'submitting': return 'info'
    case 'failed': return 'danger'
    case 'pending': return 'info'
    default: return 'info'
  }
}

// 获取状态文本
const getStatusText = (status) => {
  switch (status) {
    case 'completed': return '已完成'
    case 'generating': return '生成中'
    case 'submitted': return '任务已提交'
    case 'submitting': return '提交中'
    case 'failed': return '生成失败'
    case 'pending': return '等待生成'
    default: return '未知状态'
  }
}

// 获取生成状态消息
const getGeneratingMessage = (scene) => {
  if (scene.videoGenerating) {
    return '正在提交任务...'
  }
  switch (scene.videoStatus) {
    case 'submitting': return '正在提交任务...'
    case 'submitted': return '任务已提交，等待处理中...'
    case 'generating': return '视频生成中，预计需要3-5分钟，请耐心等待'
    default: return '处理中...'
  }
}

// 处理视频加载错误
const handleVideoError = (event, scene) => {
  const videoUrl = scene.audioVideo || scene.generatedVideo
  console.error('视频加载失败:', event, videoUrl)
  ElMessage.warning(`场景 ${scene.scene} 视频加载失败，请尝试复制链接在新窗口打开`)
}

// 在新窗口打开视频
const openVideoInNewTab = (videoUrl) => {
  if (videoUrl) {
    window.open(videoUrl, '_blank', 'noopener,noreferrer')
    ElMessage.info('已在新窗口打开视频')
  }
}

// 复制视频URL
const copyVideoUrl = async (videoUrl) => {
  if (!videoUrl) return

  try {
    await navigator.clipboard.writeText(videoUrl)
    ElMessage.success('视频链接已复制到剪贴板')
  } catch {
    // 降级方案
    const textArea = document.createElement('textarea')
    textArea.value = videoUrl
    document.body.appendChild(textArea)
    textArea.select()
    document.execCommand('copy')
    document.body.removeChild(textArea)
    ElMessage.success('视频链接已复制到剪贴板')
  }
}

// 生成音效（使用Gradio客户端，类似Python代码）
const generateAudio = async (scene) => {
  if (!scene.generatedVideo) {
    ElMessage.warning('该场景还没有生成视频')
    return
  }

  scene.audioGenerating = true

  try {
    ElMessage.info('正在连接Gradio服务...')

    // 1. 连接到Gradio服务
    const client = await Client.connect("http://127.0.0.1:7860/")

    // 2. 获取视频文件作为Blob
    const videoResponse = await fetch(scene.generatedVideo)
    if (!videoResponse.ok) {
      throw new Error('无法获取视频文件')
    }
    const videoBlob = await videoResponse.blob()

    ElMessage.info('正在生成音效...')

    // 3. 调用Gradio API，模仿Python代码的逻辑
    const result = await client.predict("/video_to_audio", {
      video: {"video": videoBlob},  // 类似Python的 handle_file
      prompt: "",                   // 正向提示词（空字符串表示无特定要求）
      negative_prompt: "music",     // 反向提示词
      seed: 0,                      // 随机种子
      num_steps: 25,                // 推理步数
      cfg_strength: 4.5,            // 提示词强度
      duration: 8                   // 音频持续时长（秒）
    })

        console.log('Gradio API完整响应:', result)

    // 4. 处理返回结果 - 根据正确的返回格式解析
    if (!result || !result.data || result.data.length === 0) {
      throw new Error('Gradio API返回空结果')
    }

    const resultData = result.data[0] // 获取第一个结果
    console.log('解析结果数据:', resultData)

    // 检查返回格式：dict(video: filepath, subtitles: filepath | None)
    if (!resultData || typeof resultData !== 'object' || !resultData.video) {
      throw new Error(`Gradio API返回格式异常: ${JSON.stringify(resultData)}`)
    }

        // 5. 获取生成的音效视频URL
    let audioVideoUrl = resultData.video

    // 检查video字段的类型并处理
    console.log('video字段类型:', typeof audioVideoUrl, 'video值:', audioVideoUrl)

    if (typeof audioVideoUrl === 'object' && audioVideoUrl !== null) {
      // 如果是对象，可能包含文件信息，尝试获取路径
      if (audioVideoUrl.path) {
        audioVideoUrl = audioVideoUrl.path
      } else if (audioVideoUrl.name) {
        audioVideoUrl = audioVideoUrl.name
      } else if (audioVideoUrl.url) {
        audioVideoUrl = audioVideoUrl.url
      } else {
        throw new Error(`无法从video对象中提取文件路径: ${JSON.stringify(audioVideoUrl)}`)
      }
    }

    // 确保是字符串类型
    audioVideoUrl = String(audioVideoUrl)

        console.log('Gradio返回的本地文件路径:', audioVideoUrl)
    console.log('字幕文件:', resultData.subtitles)

    // 6. 调用后端API复制文件到可访问位置
    ElMessage.info('正在复制音效文件...')

    const copyResponse = await fetch('http://localhost:8080/api/video/copy-audio-file', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        sourceFilePath: audioVideoUrl,
        storyboardId: scene.id
      })
    })

    if (!copyResponse.ok) {
      const errorData = await copyResponse.json()
      throw new Error(errorData.error || '复制音效文件失败')
    }

    const copyResult = await copyResponse.json()
    console.log('文件复制成功:', copyResult)

    // 7. 使用后端返回的可访问URL设置音效视频
    scene.audioVideo = copyResult.accessUrl
    scene.audioStatus = 'completed'
    scene.audioGeneratedAt = new Date().toISOString()

    console.log('音效视频URL:', copyResult.accessUrl)
    ElMessage.success('音效生成成功！已切换为带音效版本')

  } catch (error) {
    console.error('生成音效失败:', error)

    let errorMessage = '音效生成失败'
    if (error.message.includes('connect') || error.message.includes('ECONNREFUSED')) {
      errorMessage = '无法连接到Gradio服务，请确保服务运行在 http://127.0.0.1:7860'
    } else if (error.message.includes('获取视频文件')) {
      errorMessage = '无法获取原视频文件'
    } else {
      errorMessage = `音效生成失败: ${error.message}`
    }

    ElMessage.error(errorMessage)

  } finally {
    scene.audioGenerating = false
  }
}

onMounted(async () => {
  if (!storyId.value) {
    ElMessage.warning('未找到故事信息，返回故事列表')
    router.push('/stories')
    return
  }

  await loadStoryboard()
})

// 组件卸载时的清理工作（如果有需要的话）
onUnmounted(() => {
  // 目前使用HTTP URL，无需特别清理
  console.log('VideoGenerationPage组件已卸载')
})
</script>

<style scoped>
.video-generation-page {
  max-width: 1400px;
  margin: 20px auto;
  padding: 20px;
}

.video-generation-card {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.video-generation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.video-generation-title {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.story-title {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 5px;
}

.subtitle {
  font-size: 16px;
  color: #606266;
}

.video-generation-content {
  min-height: 400px;
}

.loading-section {
  padding: 20px;
}

.scenes-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.scene-card {
  width: 100%;
}

.scene-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.scene-number {
  font-size: 18px;
  font-weight: bold;
  color: #409EFF;
}

.scene-content {
  padding: 10px 0;
}

.scene-row {
  display: flex;
  gap: 30px;
  margin-bottom: 20px;
}

.left-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  flex: 1;
  max-width: 350px;
  margin-left: 50px;
}

.right-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: center;
  min-height: 450px;
  padding-top: 20px;
}

.concept-image-section, .video-prompt-section {
  width: 100%;
}

.concept-image-section h4,
.video-prompt-section h4 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 16px;
}

.prompt-content {
  background-color: #f9f9f9;
  padding: 15px;
  border-radius: 8px;
  line-height: 1.6;
  color: #606266;
  min-height: 120px;
  word-break: break-word;
  position: relative;
}

.prompt-actions {
  margin-top: 8px;
  text-align: right;
}

.prompt-editor {
  padding: 10px;
  background-color: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.prompt-textarea {
  margin-bottom: 10px;
}

.editor-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.video-status {
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: center;
}

.status-tag {
  font-weight: bold;
}

.generate-time {
  font-size: 12px;
  color: #909399;
}

.generating-info {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 8px;
  padding: 8px 12px;
  background-color: #ecf5ff;
  border: 1px solid #b3d8ff;
  border-radius: 6px;
  color: #409eff;
  font-size: 13px;
}

.generating-text {
  font-weight: 500;
}

.generated-video-section {
  margin-top: 0;
  text-align: center;
}

.video-actions {
  margin-top: 15px;
  display: flex;
  justify-content: center;
  gap: 10px;
}

.video-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background-color: #f9f9f9;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  color: #c0c4cc;
  width: 100%;
  max-width: 800px;
  min-height: 350px;
  margin-top: 0;
}

.video-placeholder span {
  margin-top: 10px;
  font-size: 14px;
}

.no-scenes {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
}

/* 生成视频按钮样式 */
.generate-video-btn {
  position: relative;
  overflow: hidden;
  padding: 12px 24px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  background: linear-gradient(135deg, #409EFF 0%, #1890ff 100%) !important;
  border: none !important;
  border-radius: 8px !important;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  transform: translateY(0) !important;
}

.generate-video-btn:hover {
  transform: translateY(-2px) !important;
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.5) !important;
  background: linear-gradient(135deg, #5cadff 0%, #409EFF 100%) !important;
}

.generate-video-btn:active {
  transform: translateY(0) !important;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.4) !important;
}

/* 加载状态样式 */
.generate-video-btn.is-loading {
  background: linear-gradient(135deg, #a0cfff 0%, #79bbff 100%) !important;
  animation: pulse 1.5s infinite !important;
}

@keyframes pulse {
  0% {
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  }
  50% {
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.6);
  }
  100% {
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .video-generation-page {
    padding: 10px;
  }

  .video-generation-header {
    flex-direction: column;
    gap: 15px;
  }

  .scene-row {
    flex-direction: column;
    gap: 20px;
  }

  .left-section {
    max-width: 100%;
  }

  .right-section {
    min-height: 300px;
  }

  .concept-image-section .el-image {
    width: 100% !important;
    height: 160px !important;
  }

  .video-placeholder {
    min-height: 250px;
    max-width: 100%;
  }

  .generate-video-btn {
    padding: 10px 20px !important;
    font-size: 14px !important;
  }

  .prompt-content {
    min-height: 100px;
    padding: 12px;
  }

  .prompt-editor {
    padding: 8px;
  }

  .editor-actions {
    justify-content: center;
  }
}
</style>
