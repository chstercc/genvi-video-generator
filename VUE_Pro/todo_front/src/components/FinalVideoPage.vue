<template>
  <div class="final-video-page">
    <el-card class="final-video-card">
      <template #header>
        <div class="final-video-header">
          <el-button type="primary" @click="goBack" plain>
            <el-icon><Back /></el-icon>
            返回视频生成
          </el-button>
          <div class="final-video-title">
            <span class="story-title">{{ storyTitle }}</span>
            <span class="subtitle">最终作品</span>
          </div>
        </div>
      </template>

      <div class="final-video-content">
        <!-- 场景视频预览 -->
        <div class="scenes-section">
          <h3>场景视频预览</h3>
          <div class="scenes-container">
            <div class="scenes-scroll-area">
              <div
                v-for="(scene, index) in sceneVideos"
                :key="scene.id"
                class="scene-card"
                :class="{ active: currentSceneIndex === index }"
                @click="setCurrentScene(index)"
              >
                <div class="scene-header">
                  <div class="scene-badge">场景 {{ scene.scene }}</div>
                  <div class="scene-status">
                    <el-tag
                      :type="getStatusTagType(scene.videoStatus)"
                      size="small"
                    >
                      {{ getStatusText(scene.videoStatus) }}
                    </el-tag>
                  </div>
                </div>
                <div class="scene-thumbnail">
                  <video
                    :src="scene.audioVideo || scene.generatedVideo"
                    :muted="currentSceneIndex !== index"
                    preload="metadata"
                    poster=""
                  />
                  <div class="play-overlay">
                    <el-icon size="24"><VideoCamera /></el-icon>
                  </div>
                  <!-- 视频类型标识 -->
                  <div class="video-type-tag" v-if="scene.audioVideo">
                    <el-tag type="success" size="small">带音效</el-tag>
                  </div>
                  <div class="video-type-tag" v-else>
                    <el-tag type="info" size="small">原始版本</el-tag>
                  </div>
                </div>
                <div class="scene-info">
                  <div class="scene-title">{{ scene.script.substring(0, 20) }}...</div>
                  <div class="scene-duration" :class="{ 'loading': scene.duration === '获取中...' }">
                    <span v-if="scene.duration === '获取中...'">
                      <i class="loading-icon"></i>
                      获取时长中...
                    </span>
                    <span v-else>时长: {{ scene.duration }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 主视频播放器 -->
        <div class="main-video-section">
          <div class="video-player">
            <video
              ref="mainVideoPlayer"
              :src="currentVideo"
              controls
              style="width: 100%; max-height: 500px; border-radius: 12px;"
              @loadedmetadata="onVideoLoaded"
              @ended="onVideoEnded"
            >
              您的浏览器不支持视频播放。
            </video>
          </div>

          <div class="video-controls">
            <div class="current-scene-info">
              <div class="scene-meta">
              <span>当前播放: 场景 {{ currentSceneIndex + 1 }}</span>
                <el-tag
                  :type="currentScene?.audioVideo ? 'success' : 'info'"
                  size="small"
                  class="scene-type-tag"
                >
                  {{ currentScene?.audioVideo ? '带音效版本' : '原始版本' }}
                </el-tag>
              </div>
              <span class="scene-description">{{ currentScene?.script }}</span>
            </div>

            <div class="playback-controls">
              <el-button-group>
                <el-button @click="previousScene" :disabled="currentSceneIndex === 0">
                  <el-icon><ArrowLeft /></el-icon>
                  上一个
                </el-button>
                <el-button @click="nextScene" :disabled="currentSceneIndex === sceneVideos.length - 1">
                  下一个
                  <el-icon><ArrowRight /></el-icon>
                </el-button>
              </el-button-group>
            </div>
          </div>
        </div>

        <!-- 音乐选择 -->
        <div class="music-selection">
          <h4>🎵 背景音乐</h4>
          <div class="music-container">
            <!-- 无背景音乐选项 -->
            <div class="music-grid">
              <div class="music-card no-music-card" :class="{ active: selectedMusic === '' }">
                <el-radio
                  v-model="selectedMusic"
                  label=""
                  size="small"
                  @change="stopAllMusic"
                >
                  <div class="music-card-content">
                    <el-icon class="music-icon"><CloseBold /></el-icon>
                    <span class="music-title">无音乐</span>
                  </div>
                </el-radio>
              </div>

              <!-- 音乐列表 -->
              <div
                v-for="music in musicList"
                :key="music.name"
                class="music-card"
                :class="{ active: selectedMusic === music.name }"
              >
                <el-radio
                  v-model="selectedMusic"
                  :label="music.name"
                  size="small"
                  @change="selectMusic(music.name)"
                >
                  <div class="music-card-content">
                    <el-icon class="music-icon"><Mic /></el-icon>
                    <div class="music-info">
                      <span class="music-title">{{ music.displayName }}</span>
                      <span class="music-size">{{ formatFileSize(music.size) }}</span>
                    </div>
                  </div>
                </el-radio>

                <el-button
                  :type="playingMusic === music.name ? 'warning' : 'primary'"
                  size="small"
                  @click="toggleMusicPlay(music)"
                  :loading="musicLoading"
                  class="play-btn"
                  circle
                >
                  <el-icon>
                    <VideoPause v-if="playingMusic === music.name" />
                    <VideoPlay v-else />
                  </el-icon>
                </el-button>
              </div>
            </div>

            <div v-if="musicList.length === 0 && !musicLoading" class="no-music">
              <el-empty description="暂无可用的背景音乐" :image-size="60" />
            </div>

            <div v-if="musicLoading" class="music-loading">
              <el-skeleton :rows="2" animated />
            </div>
          </div>
        </div>

        <!-- 生成最终视频按钮 -->
        <div class="generate-section">
          <el-button
            type="success"
            size="large"
            @click="generateFinalVideo"
            :loading="generating"
            :disabled="!canGenerate"
            class="generate-final-video-btn"
          >
            <el-icon><VideoCamera /></el-icon>
            {{ generating ? '生成中...' : '生成最终视频' }}
          </el-button>
        </div>

        <!-- 生成进度 -->
        <div v-if="generating" class="generation-progress">
          <h3>视频生成进度</h3>
          <el-progress
            :percentage="progress"
            :color="progressColor"
            :status="progressStatus"
            :stroke-width="8"
            class="progress-bar"
          />
          <p class="progress-text">{{ progressText }}</p>
        </div>

        <!-- 生成结果 -->
        <div v-if="finalVideoUrl" class="final-video-result">
          <h3>生成完成</h3>
          <div class="result-video">
            <video
              :src="finalVideoUrl"
              controls
              style="width: 100%; max-height: 600px; border-radius: 12px;"
              preload="metadata"
            >
              您的浏览器不支持视频播放。
            </video>
          </div>
          <div class="result-actions">
            <el-button
              type="success"
              size="large"
              @click="downloadFinalVideo"
              :loading="downloading"
            >
              <el-icon><Download /></el-icon>
              {{ downloading ? '下载中...' : '下载最终视频' }}
            </el-button>
            <el-button
              type="warning"
              size="large"
              @click="regenerateFinalVideo"
            >
              <el-icon><Refresh /></el-icon>
              重新生成
            </el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Back,
  VideoCamera,
  Download,
  Refresh,
  ArrowLeft,
  ArrowRight,
  CloseBold,
  Mic,
  VideoPlay,
  VideoPause
} from '@element-plus/icons-vue'
import { storyboardService } from '@/services/storyboardService'
import { musicService } from '@/services/musicService'

const route = useRoute()
const router = useRouter()

// 响应式数据
const storyId = ref(route.query.storyId)
const storyTitle = ref(route.query.title || '未知故事')
const sceneVideos = ref([])
const currentSceneIndex = ref(0)
const generating = ref(false)
const downloading = ref(false)
const progress = ref(0)
const progressText = ref('')
const finalVideoUrl = ref('')
const mainVideoPlayer = ref(null)

// 音乐相关数据
const musicList = ref([])
const selectedMusic = ref('')
const musicLoading = ref(false)
const playingMusic = ref('') // 当前正在播放的音乐名称
const audioElements = ref(new Map()) // 存储音频元素

// 计算属性
const currentScene = computed(() => sceneVideos.value[currentSceneIndex.value])
const currentVideo = computed(() => {
  const scene = currentScene.value
  if (!scene) return ''
  // 优先使用音效视频，如果没有则使用原始视频
  return scene.audioVideo || scene.generatedVideo || ''
})
const canGenerate = computed(() => {
  const hasScenes = sceneVideos.value.length > 0
  console.log('canGenerate:', hasScenes, 'sceneVideos length:', sceneVideos.value.length)
  return hasScenes
})
const progressColor = computed(() => {
  if (progress.value === 100) return '#67c23a'
  if (progress.value > 80) return '#e6a23c'
  return '#409eff'
})
const progressStatus = computed(() => {
  if (progress.value === 100) return 'success'
  if (progress.value > 0) return undefined
  return undefined
})

// 返回视频生成页面
const goBack = () => {
  router.push({
    path: '/video-generation',
    query: {
      storyId: storyId.value,
      title: storyTitle.value
    }
  })
}

// 获取视频真实时长
const getVideoDuration = (videoUrl) => {
  return new Promise((resolve) => {
    const video = document.createElement('video')
    video.preload = 'metadata'
    video.crossOrigin = 'anonymous' // 支持跨域
    video.muted = true // 避免自动播放限制

    let resolved = false // 防止重复resolve

    const cleanup = () => {
      if (video.parentNode) {
        video.parentNode.removeChild(video)
      }
    }

    let timeout = null

    const resolveOnce = (value) => {
      if (!resolved) {
        resolved = true
        if (timeout) {
          clearTimeout(timeout)
        }
        cleanup()
        resolve(value)
      }
    }

    video.onloadedmetadata = () => {
      const duration = video.duration
      if (isNaN(duration) || duration === 0 || !isFinite(duration)) {
        resolveOnce('未知')
      } else {
        const minutes = Math.floor(duration / 60)
        const seconds = Math.floor(duration % 60)
        if (minutes > 0) {
          resolveOnce(`${minutes}分${seconds.toString().padStart(2, '0')}秒`)
        } else {
          resolveOnce(`${seconds}秒`)
        }
      }
    }

    video.onerror = (e) => {
      console.warn('视频加载失败:', videoUrl, e)
      resolveOnce('未知')
    }

    video.onloadstart = () => {
      console.log('开始加载视频时长:', videoUrl)
    }

    // 设置超时，避免长时间等待
    timeout = setTimeout(() => {
      console.warn('获取视频时长超时:', videoUrl)
      resolveOnce('未知')
    }, 15000) // 增加超时时间到15秒

    video.src = videoUrl

    // 手动触发加载
    video.load()
  })
}

// 加载音乐列表
const loadMusicList = async () => {
  try {
    musicLoading.value = true
    const response = await musicService.getMusicList()
    if (response.success) {
      musicList.value = response.musicList || []
      console.log('音乐列表加载完成:', musicList.value)
    } else {
      console.warn('获取音乐列表失败:', response.message)
      musicList.value = []
    }
  } catch (error) {
    console.error('加载音乐列表失败:', error)
    musicList.value = []
  } finally {
    musicLoading.value = false
  }
}

// 切换音乐播放状态
const toggleMusicPlay = async (music) => {
  try {
    const musicName = music.name

    // 如果当前正在播放这首音乐，则停止
    if (playingMusic.value === musicName) {
      stopMusicPlay(musicName)
      return
    }

    // 停止其他正在播放的音乐
    if (playingMusic.value) {
      stopMusicPlay(playingMusic.value)
    }

    // 播放新音乐
    await playMusic(music)
  } catch (error) {
    console.error('切换音乐播放失败:', error)
    ElMessage.error('音乐播放失败')
  }
}

// 播放音乐
const playMusic = async (music) => {
  try {
    const musicName = music.name
    const musicUrl = `http://localhost:8080/api/music/file/${musicName}`

    console.log('播放音乐:', musicName, 'URL:', musicUrl)

    // 创建或获取音频元素
    let audio = audioElements.value.get(musicName)
    if (!audio) {
      audio = new Audio(musicUrl)
      audio.volume = 0.3
      audio.preload = 'auto'

      // 添加事件监听器
      audio.addEventListener('ended', () => {
        playingMusic.value = ''
        console.log('音乐播放结束:', musicName)
      })

      audio.addEventListener('error', (e) => {
        console.error('音乐加载错误:', e)
        ElMessage.error(`音乐加载失败: ${music.displayName}`)
        playingMusic.value = ''
      })

      audioElements.value.set(musicName, audio)
    }

    // 播放音乐
    await audio.play()
    playingMusic.value = musicName
    ElMessage.success(`正在播放: ${music.displayName}`)

  } catch (error) {
    console.error('播放音乐失败:', error)
    if (error.name === 'NotAllowedError') {
      ElMessage.warning('浏览器阻止了音频播放，请先与页面交互')
    } else {
      ElMessage.error('音乐播放失败: ' + error.message)
    }
  }
}

// 停止音乐播放
const stopMusicPlay = (musicName) => {
  try {
    const audio = audioElements.value.get(musicName)
    if (audio) {
      audio.pause()
      audio.currentTime = 0
    }
    if (playingMusic.value === musicName) {
      playingMusic.value = ''
    }
    console.log('停止播放:', musicName)
  } catch (error) {
    console.error('停止音乐失败:', error)
  }
}

// 停止所有音乐
const stopAllMusic = () => {
  audioElements.value.forEach((audio, musicName) => {
    stopMusicPlay(musicName)
  })
}

// 选择音乐
const selectMusic = (musicName) => {
  selectedMusic.value = musicName
  stopAllMusic()
}

// 加载场景视频数据
const loadSceneVideos = async () => {
  try {
    console.log('从数据库加载分镜头视频数据')

    // 从后端获取真实的分镜头数据（包含实际的视频URL）
    const storyboards = await storyboardService.getStoryboard(storyId.value)

        // 过滤出有生成视频的分镜头数据，并转换为场景视频数据
    const validStoryboards = storyboards
      .filter(storyboard => {
        // 检查是否有生成的视频（原始视频或音效视频都可以）
        const hasOriginalVideo = storyboard.generatedVideo && storyboard.generatedVideo.trim() !== ''
        const hasAudioVideo = storyboard.audioVideo && storyboard.audioVideo.trim() !== ''
        const hasVideo = hasOriginalVideo || hasAudioVideo

        if (!hasVideo) {
          console.warn(`场景${storyboard.scene}没有生成视频:`, {
            videoStatus: storyboard.videoStatus,
            videoUrl: storyboard.generatedVideo,
            audioVideo: storyboard.audioVideo,
            audioStatus: storyboard.audioStatus
          })
        } else {
          console.log(`场景${storyboard.scene}有视频，状态: ${storyboard.videoStatus}，音效: ${hasAudioVideo ? '有' : '无'}`)
        }

        return hasVideo // 只要有任一种视频URL就允许
      })
      .sort((a, b) => a.scene - b.scene) // 按场景编号排序

    // 将分镜头数据转换为场景视频数据，使用数据库中的实际视频URL
    sceneVideos.value = validStoryboards.map(storyboard => ({
      id: storyboard.id,
      scene: storyboard.scene,
      script: storyboard.script, // 使用真实的分镜头剧本内容
      imagePrompt: storyboard.imagePrompt,
      videoPrompt: storyboard.videoPrompt,
      conceptImage: storyboard.conceptImage,
      // 使用数据库中存储的实际生成视频URL
      generatedVideo: storyboard.generatedVideo,
      audioVideo: storyboard.audioVideo, // 音效视频URL
      audioStatus: storyboard.audioStatus, // 音效状态
      videoStatus: storyboard.videoStatus,
      duration: '获取中...' // 初始显示获取中
    }))

    if (sceneVideos.value.length === 0) {
      // 检查是否有分镜头但没有视频
      if (storyboards.length > 0) {
        ElMessage.warning('该故事的场景视频还未生成，请先在视频生成页面生成场景视频')
      } else {
        ElMessage.warning('没有找到分镜头数据')
      }
      goBack()
      return
    }

    console.log('场景视频数据加载完成，共', sceneVideos.value.length, '个已完成的场景')
    console.log('场景详情:', sceneVideos.value.map(s => `场景${s.scene}: ${s.script.substring(0, 50)}... [${s.videoStatus}]`))

    // 并行获取每个视频的真实时长，优先使用音效版本
    console.log('开始获取视频时长...')
    const durationPromises = sceneVideos.value.map(async (scene, index) => {
      try {
        const videoUrl = scene.audioVideo || scene.generatedVideo
        const duration = await getVideoDuration(videoUrl)
        scene.duration = duration
        console.log(`场景${scene.scene}时长: ${duration} (${scene.audioVideo ? '音效版本' : '原始版本'})`)
        return { index, duration, success: true }
      } catch (error) {
        console.error(`获取场景${scene.scene}时长失败:`, error)
        scene.duration = '未知'
        return { index, duration: '未知', success: false }
      }
    })

    // 等待所有时长获取完成
    await Promise.allSettled(durationPromises)
    console.log('视频时长获取完成')

  } catch (error) {
    console.error('加载场景视频失败:', error)
    ElMessage.error('加载场景视频失败: ' + error.message)
  }
}

// 设置当前场景
const setCurrentScene = (index) => {
  currentSceneIndex.value = index
  if (mainVideoPlayer.value) {
    mainVideoPlayer.value.load()
  }
}

// 上一个场景
const previousScene = () => {
  if (currentSceneIndex.value > 0) {
    setCurrentScene(currentSceneIndex.value - 1)
  }
}

// 下一个场景
const nextScene = () => {
  if (currentSceneIndex.value < sceneVideos.value.length - 1) {
    setCurrentScene(currentSceneIndex.value + 1)
  }
}

// 视频加载完成
const onVideoLoaded = () => {
  console.log('视频加载完成')
}

// 视频播放结束
const onVideoEnded = () => {
  // 自动播放下一个场景
  if (currentSceneIndex.value < sceneVideos.value.length - 1) {
    nextScene()
    setTimeout(() => {
      if (mainVideoPlayer.value) {
        mainVideoPlayer.value.play()
      }
    }, 500)
  }
}

// 生成最终视频
const generateFinalVideo = async () => {
  console.log('generateFinalVideo被调用，场景数量:', sceneVideos.value.length)
  console.log('当前场景列表:', sceneVideos.value)

  if (sceneVideos.value.length === 0) {
    ElMessage.warning('没有场景视频可以拼接')
    return
  }

  generating.value = true
  progress.value = 0
  progressText.value = '正在准备视频拼接...'

  try {
    // 准备视频URL列表，优先使用音效版本
    const videoUrls = sceneVideos.value.map(scene => scene.audioVideo || scene.generatedVideo)

    // 模拟进度更新
    const progressInterval = setInterval(() => {
      if (progress.value < 90) {
        progress.value += Math.random() * 10

        if (progress.value < 30) {
          progressText.value = '正在下载场景视频...'
        } else if (progress.value < 60) {
          progressText.value = '正在拼接视频...'
        } else if (progress.value < 90) {
          progressText.value = '正在处理音视频同步...'
        }
      }
    }, 500)

    // 调用后端拼接API
    const response = await fetch('http://localhost:8080/api/video/concat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        videoUrls: videoUrls,
        outputName: `${storyTitle.value}_final_video.mp4`,
        backgroundMusic: selectedMusic.value || null
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    // 获取响应的JSON数据
    const result = await response.json()

    if (!result.success) {
      throw new Error(result.message || '视频拼接失败')
    }

    // 获取持久化的视频URL
    const videoUrl = result.videoUrl

    // 完成进度
    clearInterval(progressInterval)
    progress.value = 100
    progressText.value = '视频生成完成！'

    finalVideoUrl.value = videoUrl

    // 创建视频作品记录到数据库
    try {
      const token = localStorage.getItem('token')
      if (token && storyId.value) {
        await fetch('http://localhost:8080/api/video/create-from-story', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({
            storyId: storyId.value,
            finalVideoUrl: videoUrl
          })
        })
        console.log('视频作品记录已保存到数据库')
      }
    } catch (error) {
      console.error('保存视频作品记录失败:', error)
      // 不影响主流程，仅记录错误
    }

    ElMessage.success('最终视频生成成功！已永久保存')

  } catch (error) {
    console.error('生成最终视频失败:', error)
    ElMessage.error('生成最终视频失败: ' + error.message)
    progressText.value = '生成失败'
  } finally {
    generating.value = false
  }
}

// 重新生成最终视频
const regenerateFinalVideo = () => {
  finalVideoUrl.value = ''
  progress.value = 0
  progressText.value = ''
  generateFinalVideo()
}

// 获取状态标签类型
const getStatusTagType = (status) => {
  switch (status) {
    case 'completed':
      return 'success'
    case 'generating':
      return 'warning'
    case 'failed':
      return 'danger'
    case 'pending':
      return 'info'
    default:
      return 'info'
  }
}

// 获取状态文本
const getStatusText = (status) => {
  switch (status) {
    case 'completed':
      return '已完成'
    case 'generating':
      return '生成中'
    case 'failed':
      return '生成失败'
    case 'pending':
      return '待生成'
    default:
      return '未知状态'
  }
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 下载最终视频
const downloadFinalVideo = async () => {
  if (!finalVideoUrl.value) {
    ElMessage.warning('没有可下载的视频')
    return
  }

  downloading.value = true

  try {
    // 创建下载链接
    const link = document.createElement('a')
    link.href = finalVideoUrl.value
    link.download = `${storyTitle.value}_最终作品.mp4`
    link.target = '_blank' // 在新标签页打开，避免被拦截
    link.style.display = 'none'

    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    ElMessage.success('视频下载开始！')
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败')
  } finally {
    downloading.value = false
  }
}

onMounted(async () => {
  if (!storyId.value) {
    ElMessage.warning('未找到故事信息')
    router.push('/stories')
    return
  }

  // 并行加载场景视频和音乐列表
  await Promise.all([
    loadSceneVideos(),
    loadMusicList()
  ])
})

// 组件卸载时清理资源
onUnmounted(() => {
  stopAllMusic()
  // 清理音频元素
  audioElements.value.forEach((audio) => {
    if (audio) {
      audio.pause()
      audio.src = ''
      audio.load()
    }
  })
  audioElements.value.clear()
})
</script>

<style scoped>
.final-video-page {
  max-width: 1600px;
  margin: 20px auto;
  padding: 20px;
}

.final-video-card {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.final-video-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.final-video-title {
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

.final-video-content {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.scenes-section h3 {
  color: #303133;
  margin-bottom: 20px;
  font-size: 20px;
  font-weight: 600;
}

/* 横向滚动容器 */
.scenes-container {
  position: relative;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.scenes-scroll-area {
  display: flex;
  gap: 20px;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 10px 5px 20px 5px;
  scroll-behavior: smooth;
}

/* 自定义滚动条样式 */
.scenes-scroll-area::-webkit-scrollbar {
  height: 8px;
}

.scenes-scroll-area::-webkit-scrollbar-track {
  background: #f1f3f4;
  border-radius: 10px;
}

.scenes-scroll-area::-webkit-scrollbar-thumb {
  background: linear-gradient(90deg, #409EFF, #67c23a);
  border-radius: 10px;
  box-shadow: inset 0 0 3px rgba(0, 0, 0, 0.1);
}

.scenes-scroll-area::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(90deg, #337ecc, #529b2e);
}

/* 场景卡片样式 */
.scene-card {
  min-width: 240px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: relative;
}

.scene-card:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 12px 32px rgba(64, 158, 255, 0.15);
}

.scene-card.active {
  border: 3px solid #409EFF;
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.25);
  transform: translateY(-4px);
}

.scene-card.active::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #409EFF, #67c23a);
  z-index: 1;
}

/* 场景头部 */
.scene-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 15px 10px 15px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
}

.scene-badge {
  font-weight: 700;
  color: #409EFF;
  font-size: 16px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.scene-status {
  opacity: 0.9;
}

/* 场景缩略图 */
.scene-thumbnail {
  position: relative;
  width: 100%;
  height: 140px;
  overflow: hidden;
}

.scene-thumbnail video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.scene-card:hover .scene-thumbnail video {
  transform: scale(1.05);
}

.play-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(64, 158, 255, 0.8);
  color: white;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
  transition: all 0.3s ease;
  opacity: 0.7;
}

.scene-card:hover .play-overlay {
  opacity: 1;
  transform: translate(-50%, -50%) scale(1.1);
  background: rgba(64, 158, 255, 0.9);
}

.scene-card.active .play-overlay {
  background: rgba(103, 194, 58, 0.9);
  opacity: 1;
}

/* 视频类型标识 */
.video-type-tag {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 2;
  pointer-events: none;
}

.video-type-tag .el-tag {
  font-size: 10px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 场景信息 */
.scene-info {
  padding: 15px;
  background: white;
}

.scene-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  line-height: 1.4;
  height: 38px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.scene-duration {
  font-size: 12px;
  color: #67c23a;
  font-weight: 500;
  background: #f0f9ff;
  padding: 4px 8px;
  border-radius: 8px;
  text-align: center;
  transition: all 0.3s ease;
}

.scene-duration.loading {
  background: #fff7e6;
  color: #e6a23c;
}

.loading-icon {
  display: inline-block;
  width: 10px;
  height: 10px;
  border: 2px solid #e6a23c;
  border-top: 2px solid transparent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 4px;
  vertical-align: middle;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.main-video-section {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 12px;
}

.video-player {
  margin-bottom: 20px;
}

.video-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.current-scene-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.scene-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.scene-type-tag {
  font-size: 11px;
  border-radius: 4px;
}

.scene-description {
  font-size: 14px;
  color: #606266;
  max-width: 300px;
}

.generation-progress {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #e4e7ed;
}

.generation-progress h3 {
  color: #303133;
  margin-bottom: 20px;
}

.progress-bar {
  margin-bottom: 15px;
}

.progress-text {
  text-align: center;
  color: #606266;
  font-size: 14px;
}

.final-video-result {
  background: #f0f9ff;
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #409EFF;
}

.final-video-result h3 {
  color: #409EFF;
  margin-bottom: 20px;
}

.result-video {
  margin-bottom: 20px;
}

.result-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
}

/* 音乐选择样式 - 紧凑版 */
.music-selection {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  margin-bottom: 20px;
}

.music-selection h4 {
  color: #303133;
  margin: 0 0 12px 0;
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.music-container {
  width: 100%;
}

.music-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px;
}

.music-card {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px;
  transition: all 0.2s ease;
  position: relative;
  display: flex;
  align-items: center;
  min-height: 60px;
}

.music-card:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.1);
}

.music-card.active {
  border-color: #409EFF;
  background: #f0f9ff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.music-card .el-radio {
  flex: 1;
  margin-right: 8px;
}

.music-card-content {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.music-icon {
  color: #409EFF;
  font-size: 16px;
  flex-shrink: 0;
}

.music-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.music-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.music-size {
  font-size: 11px;
  color: #909399;
}

.no-music-card .music-title {
  text-align: center;
  width: 100%;
}

.play-btn {
  width: 32px !important;
  height: 32px !important;
  padding: 0 !important;
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  flex-shrink: 0;
}

.play-btn .el-icon {
  font-size: 14px;
}

.no-music {
  text-align: center;
  padding: 20px;
  color: #909399;
}

.music-loading {
  padding: 12px;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .music-grid {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .music-card {
    padding: 10px;
    min-height: 50px;
  }

  .music-title {
    font-size: 12px;
  }

  .play-btn {
    width: 28px !important;
    height: 28px !important;
  }
}

@media (max-width: 480px) {
  .music-selection {
    padding: 12px;
  }

  .music-selection h4 {
    font-size: 14px;
  }
}

/* 生成最终视频按钮样式 */
.generate-section {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px 0;
  margin: 20px 0;
}

.generate-final-video-btn {
  font-size: 16px !important;
  font-weight: 600 !important;
  padding: 16px 32px !important;
  border-radius: 8px !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  box-shadow: 0 4px 15px rgba(67, 196, 58, 0.3) !important;
}

.generate-final-video-btn:hover {
  transform: translateY(-3px) !important;
  box-shadow: 0 8px 25px rgba(67, 196, 58, 0.4) !important;
}

.generate-final-video-btn:active {
  transform: translateY(-1px) !important;
}

.generate-final-video-btn .el-icon {
  font-size: 18px !important;
  margin-right: 8px !important;
}

/* 禁用状态样式 */
.generate-final-video-btn:disabled {
  background: #f0f0f0 !important;
  border-color: #d9d9d9 !important;
  color: #bfbfbf !important;
  box-shadow: none !important;
  transform: none !important;
  cursor: not-allowed !important;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .generate-section {
    padding: 20px 0;
    margin: 16px 0;
  }

  .generate-final-video-btn {
    font-size: 14px !important;
    padding: 12px 24px !important;
  }

  .generate-final-video-btn .el-icon {
    font-size: 16px !important;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .final-video-header {
    flex-direction: column;
    gap: 15px;
  }

  .scenes-container {
    padding: 15px;
  }

  .scene-card {
    min-width: 200px;
  }

  .scene-thumbnail {
    height: 120px;
  }

  .scenes-scroll-area {
    gap: 15px;
  }

  .scene-header {
    padding: 12px;
  }

  .scene-info {
    padding: 12px;
  }

  .video-controls {
    flex-direction: column;
    align-items: stretch;
  }

  .result-actions {
    flex-direction: column;
  }
}

@media (max-width: 480px) {
  .scene-card {
    min-width: 180px;
  }

  .scene-thumbnail {
    height: 100px;
  }

  .play-overlay {
    width: 40px;
    height: 40px;
  }

  .scene-title {
    font-size: 13px;
    height: 32px;
  }

  .scene-duration {
    font-size: 11px;
  }
}
</style>
