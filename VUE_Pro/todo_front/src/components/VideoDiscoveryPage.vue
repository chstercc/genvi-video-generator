<template>
  <div class="video-discovery">
    <!-- 顶部英雄区域 -->
    <div class="hero-section">
      <div class="hero-background">
        <img src="https://images.unsplash.com/photo-1536431311719-398b6704d4cc?ixlib=rb-4.0.3&auto=format&fit=crop&w=2000&q=80" alt="AI Video Creation" />
        <div class="hero-overlay"></div>
      </div>
      <div class="hero-content">
        <h1 class="hero-title">
          <span class="gradient-text">AI创作视界</span>
        </h1>
        <p class="hero-subtitle">探索无限想象，发现AI生成的精彩视频世界</p>
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-number">1,234</span>
            <span class="stat-label">精彩作品</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">5,678</span>
            <span class="stat-label">创作者</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">9,876</span>
            <span class="stat-label">观看次数</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分类筛选栏 -->
    <div class="category-section">
      <div class="container">
        <div class="category-header">
          <h2>精选内容</h2>
          <p>发现最新、最热门的AI创作视频</p>
        </div>
        <div class="category-tabs">
          <button
            v-for="category in categories"
            :key="category.key"
            :class="['category-btn', { active: activeCategory === category.key }]"
            @click="handleCategoryChange(category.key)"
          >
            <span class="category-icon">{{ category.icon }}</span>
            <span>{{ category.label }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 视频网格展示区域 -->
    <div class="video-grid-container">
      <div class="container">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-grid">
          <div
            v-for="n in 12"
            :key="n"
            class="video-card-skeleton"
          >
            <div class="skeleton-thumbnail"></div>
            <div class="skeleton-content">
              <div class="skeleton-title"></div>
              <div class="skeleton-meta"></div>
              <div class="skeleton-author"></div>
            </div>
          </div>
        </div>

        <!-- 视频网格 -->
        <div v-else class="video-grid">
          <div
            v-for="video in displayVideos"
            :key="video.id"
            class="video-card"
            @click="playVideo(video)"
          >
            <div class="video-thumbnail">
              <img
                :src="video.thumbnailUrl"
                :alt="video.title"
                class="thumbnail-image"
              />
              <div class="video-overlay">
                <div class="play-button">
                  <span class="play-icon">▶</span>
                </div>
                <div class="video-duration">{{ formatDuration(video.duration) }}</div>
                <div class="video-quality">4K</div>
              </div>
              <div class="video-category">{{ video.category }}</div>
            </div>

            <div class="video-info">
              <h3 class="video-title">{{ video.title }}</h3>
              <div class="video-meta">
                <div class="author-info">
                  <el-avatar :size="28" class="author-avatar">
                    <img :src="video.authorAvatar" :alt="video.author" />
                  </el-avatar>
                  <span class="author-name">{{ video.author }}</span>
                  <span class="verified-icon" v-if="video.verified">✅</span>
                </div>
                <div class="video-stats">
                  <span class="view-count">
                    <span class="stat-icon">👁</span>
                    {{ formatViewCount(video.views) }}
                  </span>
                  <span class="like-count">
                    <span class="stat-icon">❤</span>
                    {{ formatViewCount(video.likes) }}
                  </span>
                  <span class="upload-time">{{ formatTimeAgo(video.createdAt) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载更多按钮 -->
        <div class="load-more-section" v-if="!loading && displayVideos.length > 0">
          <el-button
            type="primary"
            size="large"
            :loading="loadingMore"
            @click="loadMoreVideos"
            class="load-more-btn"
          >
            <span class="btn-icon">➕</span>
            加载更多精彩内容
          </el-button>
        </div>

        <!-- 空状态 -->
        <div v-if="!loading && displayVideos.length === 0" class="empty-state">
          <div class="empty-illustration">
            <span class="empty-icon">📹</span>
          </div>
          <h3>暂无视频内容</h3>
          <p>期待创作者们的精彩作品</p>
          <el-button type="primary" size="large" @click="goToCreate">
            <span class="btn-icon">➕</span>
            开始创作
          </el-button>
        </div>
      </div>
    </div>

    <!-- 视频播放弹窗 -->
    <el-dialog
      v-model="showVideoDialog"
      :title="currentVideo?.title"
      width="90%"
      align-center
      class="video-dialog"
      :before-close="handleDialogClose"
    >
      <div class="video-player-container">
        <div class="video-placeholder">
          <img
            :src="currentVideo?.thumbnailUrl"
            :alt="currentVideo?.title"
            class="video-poster"
          />
          <div class="play-overlay">
            <el-button type="primary" size="large" circle>
              <span class="large-play-icon">▶</span>
            </el-button>
            <p>点击播放视频（演示模式）</p>
          </div>
        </div>
      </div>

      <div v-if="currentVideo" class="video-details">
        <div class="video-main-info">
          <div class="video-description">
            <h4>作品描述</h4>
            <p>{{ currentVideo.description }}</p>
            <div class="video-tags">
              <el-tag
                v-for="tag in currentVideo.tags"
                :key="tag"
                type="info"
                effect="light"
                size="small"
              >
                {{ tag }}
              </el-tag>
            </div>
          </div>

          <div class="video-sidebar">
            <div class="author-card">
              <el-avatar :size="60">
                <img :src="currentVideo.authorAvatar" :alt="currentVideo.author" />
              </el-avatar>
              <div class="author-details">
                <h5>{{ currentVideo.author }}</h5>
                <span class="author-meta">{{ formatTimeAgo(currentVideo.createdAt) }} 发布</span>
                <span class="author-followers">{{ formatViewCount(currentVideo.followers) }} 关注者</span>
              </div>
            </div>

            <div class="video-actions">
              <el-button type="primary"><span class="btn-icon">👍</span>点赞</el-button>
              <el-button type="default"><span class="btn-icon">⭐</span>收藏</el-button>
              <el-button type="default"><span class="btn-icon">📤</span>分享</el-button>
              <el-button type="default"><span class="btn-icon">⬇️</span>下载</el-button>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
// 移除Element Plus图标导入

const router = useRouter()

// 响应式数据
const loading = ref(true)
const loadingMore = ref(false)
const displayVideos = ref([])
const activeCategory = ref('all')
const showVideoDialog = ref(false)
const currentVideo = ref(null)

// 分类配置
const categories = ref([
  { key: 'all', label: '全部', icon: '🎬' },
  { key: 'latest', label: '最新', icon: '🔥' },
  { key: 'popular', label: '最热', icon: '⭐' },
  { key: 'recommended', label: '推荐', icon: '💎' }
])

// 样本视频数据
const sampleVideos = ref([
  {
    id: 1,
    title: '赛博朋克城市：未来之光',
    description: '在霓虹灯闪烁的赛博朋克城市中，探索科技与人性的交融。这部AI生成的短片展现了未来城市的壮丽景象，每一帧都充满了科幻美感。',
    thumbnailUrl: 'https://picsum.photos/800/600?random=1',
    videoUrl: '#',
    duration: 180,
    views: 15420,
    likes: 2341,
    author: 'AI视觉大师',
    authorAvatar: 'https://picsum.photos/100/100?random=101',
    verified: true,
    followers: 8950,
    category: 'Sci-Fi',
    createdAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000),
    tags: ['赛博朋克', '未来城市', 'AI生成', '科幻']
  },
  {
    id: 2,
    title: '梦幻森林：精灵的家园',
    description: '走进充满魔法的梦幻森林，与精灵一起探索自然的奥秘。这个AI创作的奇幻世界将带您体验不同寻常的冒险之旅。',
    thumbnailUrl: 'https://picsum.photos/800/600?random=2',
    videoUrl: '#',
    duration: 240,
    views: 12890,
    likes: 1876,
    author: '奇幻创作者',
    authorAvatar: 'https://picsum.photos/100/100?random=102',
    verified: true,
    followers: 6720,
    category: 'Fantasy',
    createdAt: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000),
    tags: ['奇幻', '森林', '精灵', '魔法']
  },
  {
    id: 3,
    title: '星际旅行：宇宙深处',
    description: '跟随宇宙飞船深入星际空间，探索未知的星系和文明。这部AI生成的太空史诗展现了宇宙的无穷魅力。',
    thumbnailUrl: 'https://picsum.photos/800/600?random=3',
    videoUrl: '#',
    duration: 300,
    views: 23450,
    likes: 3210,
    author: '宇宙探索家',
    authorAvatar: 'https://picsum.photos/100/100?random=103',
    verified: true,
    followers: 12300,
    category: 'Space',
    createdAt: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000),
    tags: ['太空', '星际', '科幻', '探索']
  },
  {
    id: 4,
    title: '水下世界：海洋奇观',
    description: '潜入神秘的海洋深处，发现色彩斑斓的海底世界。AI技术完美还原了海洋生物的美丽和海底景观的壮观。',
    thumbnailUrl: 'https://picsum.photos/800/600?random=4',
    videoUrl: '#',
    duration: 210,
    views: 9876,
    likes: 1543,
    author: '海洋艺术家',
    authorAvatar: 'https://picsum.photos/100/100?random=104',
    verified: false,
    followers: 4560,
    category: 'Nature',
    createdAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000),
    tags: ['海洋', '水下', '自然', '生物']
  },
  {
    id: 5,
    title: '古代文明：失落的帝国',
    description: '重现古代文明的辉煌，探索失落帝国的神秘遗迹。通过AI的力量，古老的历史在我们眼前重新焕发生机。',
    thumbnailUrl: 'https://picsum.photos/800/600?random=5',
    videoUrl: '#',
    duration: 270,
    views: 18900,
    likes: 2890,
    author: '历史重现者',
    authorAvatar: 'https://picsum.photos/100/100?random=105',
    verified: true,
    followers: 11200,
    category: 'Historical',
    createdAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000),
    tags: ['历史', '古代', '文明', '遗迹']
  },
  {
    id: 6,
    title: '机械舞蹈：机器人芭蕾',
    description: '优雅的机器人演绎经典芭蕾舞蹈，科技与艺术的完美结合。这个独特的AI创作展现了未来艺术的无限可能。',
    thumbnailUrl: 'https://picsum.photos/800/600?random=6',
    videoUrl: '#',
    duration: 195,
    views: 14320,
    likes: 2104,
    author: '科技艺术家',
    authorAvatar: 'https://images.unsplash.com/photo-1489424731084-a5d8b219a5bb?ixlib=rb-4.0.3&auto=format&fit=crop&w=100&q=80',
    verified: true,
    followers: 7890,
    category: 'Art',
    createdAt: new Date(Date.now() - 4 * 24 * 60 * 60 * 1000),
    tags: ['机器人', '舞蹈', '艺术', '科技']
  },
  {
    id: 7,
    title: '极光之夜：北极奇迹',
    description: '北极夜空中绚烂的极光舞动，大自然最美的光影表演。AI技术捕捉到了这一刻的永恒之美。',
    thumbnailUrl: 'https://picsum.photos/800/600?random=7',
    videoUrl: '#',
    duration: 225,
    views: 21560,
    likes: 3456,
    author: '自然摄影师',
    authorAvatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?ixlib=rb-4.0.3&auto=format&fit=crop&w=100&q=80',
    verified: true,
    followers: 15600,
    category: 'Nature',
    createdAt: new Date(Date.now() - 6 * 24 * 60 * 60 * 1000),
    tags: ['极光', '北极', '自然', '奇观']
  },
  {
    id: 8,
    title: '城市变迁：时间的印记',
    description: '见证一座城市从古到今的华丽变迁，时间在这里留下了深刻的印记。AI重现了历史的变迁过程。',
    thumbnailUrl: 'https://picsum.photos/800/600?random=8',
    videoUrl: '#',
    duration: 315,
    views: 17890,
    likes: 2650,
    author: '时间记录者',
    authorAvatar: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?ixlib=rb-4.0.3&auto=format&fit=crop&w=100&q=80',
    verified: false,
    followers: 9340,
    category: 'Documentary',
    createdAt: new Date(Date.now() - 8 * 24 * 60 * 60 * 1000),
    tags: ['城市', '变迁', '历史', '纪录']
  }
])

// 获取视频列表
const fetchVideos = async () => {
  loading.value = true
  try {
    // 模拟网络延迟
    await new Promise(resolve => setTimeout(resolve, 1500))

    // 根据分类筛选视频
    let filteredVideos = [...sampleVideos.value]

    if (activeCategory.value === 'latest') {
      filteredVideos.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    } else if (activeCategory.value === 'popular') {
      filteredVideos.sort((a, b) => b.views - a.views)
    } else if (activeCategory.value === 'recommended') {
      filteredVideos.sort((a, b) => b.likes - a.likes)
    }

    displayVideos.value = filteredVideos.slice(0, 6)
  } catch (error) {
    console.error('获取视频列表失败:', error)
    ElMessage.error('获取视频列表失败')
  } finally {
    loading.value = false
  }
}

// 加载更多视频
const loadMoreVideos = async () => {
  if (loadingMore.value) return

  loadingMore.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 1000))

    const currentLength = displayVideos.value.length
    const nextVideos = sampleVideos.value.slice(currentLength, currentLength + 6)

    if (nextVideos.length > 0) {
      displayVideos.value = [...displayVideos.value, ...nextVideos]
    } else {
      ElMessage.info('已加载全部内容')
    }
  } catch {
    ElMessage.error('加载更多失败')
  } finally {
    loadingMore.value = false
  }
}

// 分类改变处理
const handleCategoryChange = (category) => {
  activeCategory.value = category
  fetchVideos()
}

// 播放视频
const playVideo = (video) => {
  currentVideo.value = video
  showVideoDialog.value = true
}

// 关闭弹窗
const handleDialogClose = () => {
  showVideoDialog.value = false
  currentVideo.value = null
}

// 前往创建页面
const goToCreate = () => {
  router.push('/stories')
}

// 工具函数
const formatDuration = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

const formatViewCount = (count) => {
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + '万'
  } else if (count >= 1000) {
    return (count / 1000).toFixed(1) + 'k'
  }
  return count.toString()
}

const formatTimeAgo = (date) => {
  const now = new Date()
  const diff = now - new Date(date)
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  if (days < 365) return `${Math.floor(days / 30)}个月前`
  return `${Math.floor(days / 365)}年前`
}

// 生命周期
onMounted(() => {
  fetchVideos()
})
</script>

<style scoped>
.video-discovery {
  min-height: 100vh;
  background: transparent; /* 使用父容器的背景 */
}

/* 英雄区域 */
.hero-section {
  position: relative;
  height: 70vh;
  min-height: 600px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-background {
  position: absolute;
  top: -80px;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1;
}

.hero-background img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: blur(1px);
}

.hero-overlay {
  position: absolute;
  top: -80px;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.8) 0%, rgba(118, 75, 162, 0.8) 100%);
}

.hero-content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: white;
  max-width: 800px;
  padding: 0 2rem;
}

.hero-title {
  font-size: 4rem;
  font-weight: 700;
  margin-bottom: 1.5rem;
  text-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  line-height: 1.2;
}

.gradient-text {
  background: linear-gradient(45deg, #fff, #f0f8ff, #e6f3ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 1.3rem;
  opacity: 0.95;
  margin-bottom: 3rem;
  font-weight: 300;
}

.hero-stats {
  display: flex;
  justify-content: center;
  gap: 3rem;
  margin-top: 2rem;
}

.stat-item {
  text-align: center;
}

.stat-number {
  display: block;
  font-size: 2.2rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 0.5rem;
}

.stat-label {
  font-size: 0.9rem;
  opacity: 0.8;
  color: #fff;
}

/* 分类筛选栏样式 */
.category-section {
  padding: 2rem 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(240, 242, 255, 0.9));
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(229, 231, 235, 0.5);
  position: relative;
  z-index: 10;
}

.category-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg,
    rgba(255, 255, 255, 0),
    rgba(123, 97, 255, 0.2),
    rgba(255, 255, 255, 0)
  );
}

.category-header {
  text-align: center;
  margin-bottom: 2rem;
}

.category-header h2 {
  font-size: 2rem;
  font-weight: 700;
  background: linear-gradient(135deg, #2d3748, #4a5568);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 0.5rem;
  letter-spacing: -0.5px;
}

.category-header p {
  color: #718096;
  font-size: 1.1rem;
  font-weight: 400;
  margin-top: 0.5rem;
}

.category-tabs {
  display: flex;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
  padding: 0 1rem;
}

.category-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.8);
  color: #4a5568;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  position: relative;
  overflow: hidden;
}

.category-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(123, 97, 255, 0.1), rgba(123, 97, 255, 0));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.category-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.category-btn:hover::before {
  opacity: 1;
}

.category-btn.active {
  background: linear-gradient(135deg, #7b61ff, #6b46c1);
  color: white;
  box-shadow: 0 4px 12px rgba(123, 97, 255, 0.3);
}

.category-btn.active::before {
  display: none;
}

.category-icon {
  font-size: 1.25rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s ease;
}

.category-btn:hover .category-icon {
  transform: scale(1.1);
}

/* 视频网格 */
.video-grid-container {
  background: linear-gradient(180deg, rgba(220, 231, 255, 0.8) 0%, rgba(237, 243, 255, 0.9) 100%);
  min-height: 60vh;
  padding: 3rem 0;
}

.loading-grid,
.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 2rem;
  margin-bottom: 3rem;
}

/* 骨架屏 */
.video-card-skeleton {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.skeleton-thumbnail {
  width: 100%;
  height: 240px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
}

.skeleton-content {
  padding: 1.5rem;
}

.skeleton-title,
.skeleton-meta,
.skeleton-author {
  height: 16px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: 8px;
  margin-bottom: 1rem;
}

.skeleton-title {
  height: 20px;
  width: 80%;
}

.skeleton-meta {
  width: 60%;
}

.skeleton-author {
  width: 40%;
}

@keyframes loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* 视频卡片 */
.video-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  position: relative;
}

.video-card:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.video-thumbnail {
  position: relative;
  width: 100%;
  height: 240px;
  overflow: hidden;
}

.thumbnail-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.video-card:hover .thumbnail-image {
  transform: scale(1.1);
}

.video-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.video-card:hover .video-overlay {
  opacity: 1;
}

.play-button {
  color: white;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  padding: 1rem;
  backdrop-filter: blur(10px);
  transition: transform 0.3s ease;
}

.play-button:hover {
  transform: scale(1.1);
  background: rgba(255, 255, 255, 0.3);
}

.video-duration {
  position: absolute;
  bottom: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 500;
}

.video-quality {
  position: absolute;
  top: 12px;
  left: 12px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
}

.video-category {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(255, 255, 255, 0.9);
  color: #2c3e50;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 500;
}

/* 视频信息 */
.video-info {
  padding: 1.5rem;
}

.video-title {
  font-size: 1.2rem;
  font-weight: 600;
  margin-bottom: 1rem;
  color: #2c3e50;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.video-meta {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.author-avatar {
  border: 2px solid #e9ecef;
}

.author-name {
  font-weight: 500;
  color: #495057;
  flex: 1;
}

.verified-icon {
  color: #28a745;
  font-size: 16px;
}

.video-stats {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 0.9rem;
  color: #6c757d;
}

.view-count,
.like-count {
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

/* 加载更多 */
.load-more-section {
  text-align: center;
  margin: 2rem 0;
}

.load-more-btn {
  padding: 1rem 2rem;
  border-radius: 50px;
  font-size: 1.1rem;
  font-weight: 500;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.load-more-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  color: #6c757d;
}

.empty-illustration {
  margin-bottom: 2rem;
  color: #dee2e6;
}

.empty-state h3 {
  font-size: 1.5rem;
  margin-bottom: 1rem;
  color: #495057;
}

.empty-state p {
  font-size: 1.1rem;
  margin-bottom: 2rem;
}

/* 视频播放弹窗 */
.video-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

.video-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  padding: 1.5rem 2rem;
}

.video-dialog :deep(.el-dialog__title) {
  color: white;
  font-weight: 600;
}

.video-player-container {
  position: relative;
  width: 100%;
  padding-bottom: 56.25%;
  height: 0;
  overflow: hidden;
  background: #000;
  border-radius: 8px;
  margin-bottom: 2rem;
}

.video-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-poster {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.7;
}

.play-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: white;
}

.play-overlay p {
  margin-top: 1rem;
  font-size: 1.1rem;
}

.video-details {
  padding: 0 1rem;
}

.video-main-info {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 2rem;
}

.video-description h4 {
  margin-bottom: 1rem;
  color: #2c3e50;
  font-size: 1.2rem;
}

.video-description p {
  color: #495057;
  line-height: 1.6;
  margin-bottom: 1.5rem;
}

.video-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.author-card {
  background: #f8f9fa;
  padding: 1.5rem;
  border-radius: 12px;
  text-align: center;
  margin-bottom: 1.5rem;
}

.author-details h5 {
  margin: 1rem 0 0.5rem;
  color: #2c3e50;
  font-size: 1.1rem;
}

.author-meta,
.author-followers {
  display: block;
  font-size: 0.9rem;
  color: #6c757d;
  margin-bottom: 0.3rem;
}

.video-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.8rem;
}

/* 文本图标样式 */
.category-icon {
  font-size: 18px;
  margin-right: 0.5rem;
}

.play-icon {
  font-size: 32px;
  color: white;
}

.large-play-icon {
  font-size: 32px;
}

.stat-icon {
  font-size: 14px;
  margin-right: 0.3rem;
}

.btn-icon {
  font-size: 14px;
  margin-right: 0.5rem;
}

.empty-icon {
  font-size: 80px;
  display: block;
  margin-bottom: 2rem;
}

.verified-icon {
  font-size: 16px;
  margin-left: 0.3rem;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .video-main-info {
    grid-template-columns: 1fr;
  }

  .video-actions {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 2.5rem;
  }

  .hero-subtitle {
    font-size: 1.1rem;
    padding: 0 1rem;
  }

  .hero-stats {
    flex-direction: column;
    gap: 1.5rem;
  }

  .video-grid {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }

  .container {
    padding: 0 1rem;
  }

  .category-tabs {
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 0.5rem;
  }

  .category-btn {
    white-space: nowrap;
    flex-shrink: 0;
  }

  .video-actions {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 480px) {
  .hero-title {
    font-size: 2rem;
  }

  .category-btn {
    padding: 0.8rem 1.5rem;
    font-size: 0.9rem;
  }

  .video-card {
    margin: 0 0.5rem;
  }
}
</style>
