<template>
  <div class="ai-assistant">
    <!-- 悬浮按钮 -->
    <button
      class="ai-button"
      @click="toggleChat"
      :class="{ 'active': isChatOpen }"
    >
      <i class="fas fa-robot"></i>
      AI小助手
    </button>

    <!-- 对话框 -->
    <div class="chat-window" v-if="isChatOpen">
      <div class="chat-header">
        <h3>AI故事助手</h3>
        <button class="close-button" @click="toggleChat">×</button>
      </div>

      <div class="chat-messages" ref="chatMessages">
        <div v-for="(message, index) in messages"
             :key="index"
             :class="['message', message.type]">
          <div class="message-content">{{ message.content }}</div>
        </div>
      </div>

      <div class="chat-input" v-if="!isGenerating">
        <div v-if="currentStep === 'initial'" class="start-options">
          <button @click="startStoryGeneration" class="option-button">
            生成新故事
          </button>
        </div>

        <div v-else-if="currentStep === 'title'" class="input-group">
          <input
            v-model="storyTitle"
            @keyup.enter="submitTitle"
            placeholder="请输入故事标题..."
          >
          <button @click="submitTitle">确定</button>
        </div>

        <div v-else-if="currentStep === 'modify'" class="input-group">
          <input
            v-model="modifyInput"
            @keyup.enter="submitModification"
            placeholder="输入修改要求或按Enter完成..."
          >
          <button @click="submitModification">确定</button>
        </div>
      </div>

      <div v-else class="generating-indicator">
        <span class="loading-dots">生成中</span>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, nextTick } from 'vue'
import axios from 'axios'

export default {
  name: 'AIAssistant',
  setup() {
    const isChatOpen = ref(false)
    const messages = ref([])
    const isGenerating = ref(false)
    const currentStep = ref('initial')
    const storyTitle = ref('')
    const modifyInput = ref('')
    const chatMessages = ref(null)
    const generatedSummary = ref('')

    // 创建一个专门用于AI服务的axios实例
    const aiAxios = axios.create({
      baseURL: 'http://localhost:8000',
      timeout: 60000 // 60秒超时，因为AI生成可能需要较长时间
    })

    const toggleChat = () => {
      isChatOpen.value = !isChatOpen.value
      if (isChatOpen.value) {
        messages.value = [{
          type: 'assistant',
          content: '你好！我是AI故事助手，我可以帮你生成故事梗概。'
        }]
        currentStep.value = 'initial'
      }
    }

    const scrollToBottom = async () => {
      await nextTick()
      if (chatMessages.value) {
        chatMessages.value.scrollTop = chatMessages.value.scrollHeight
      }
    }

    const addMessage = async (content, type = 'assistant') => {
      messages.value.push({ content, type })
      await scrollToBottom()
    }

    const startStoryGeneration = async () => {
      currentStep.value = 'title'
      await addMessage('请输入故事标题：')
    }

    const submitTitle = async () => {
      const trimmedTitle = storyTitle.value.trim()
      if (!trimmedTitle) {
        await addMessage('请输入有效的故事标题', 'assistant')
        return
      }

      await addMessage(`标题：${trimmedTitle}`, 'user')
      isGenerating.value = true

      try {
        const response = await aiAxios.post('/generate-story', {
          title: trimmedTitle
        })

        if (response.data.success) {
          const savedTitle = trimmedTitle  // 保存标题以供后续使用
          generatedSummary.value = response.data.summary
          await addMessage(response.data.summary)
          await addMessage('你想修改这个故事吗？（输入修改要求或直接按Enter完成）')
          currentStep.value = 'modify'
          storyTitle.value = savedTitle  // 保持标题不变
        } else {
          throw new Error(response.data.message || '生成故事失败')
        }
      } catch (error) {
        console.error('生成故事错误:', error)
        let errorMessage = '生成故事时出错了'

        if (error.response) {
          // 服务器返回错误响应
          if (error.response.status === 422) {
            errorMessage = '请输入有效的故事标题（1-100个字符）'
          } else {
            errorMessage = error.response.data?.detail || error.response.data?.message || errorMessage
          }
        } else if (error.request) {
          // 请求发出但没有收到响应
          errorMessage = '无法连接到AI服务，请检查服务是否运行'
        } else {
          // 请求设置时出错
          errorMessage = error.message || errorMessage
        }

        await addMessage('抱歉，' + errorMessage, 'assistant')
        currentStep.value = 'initial'
      }

      isGenerating.value = false
    }

    const submitModification = async () => {
      if (!modifyInput.value.trim()) {
        // 完成修改，保存故事
        try {
          // 检查用户登录状态
          const token = localStorage.getItem('token')
          const user = JSON.parse(localStorage.getItem('user') || '{}')

          if (!token) {
            await addMessage('请先登录后再保存故事')
            return
          }

          if (!user.id) {
            await addMessage('用户信息无效，请重新登录')
            return
          }

          console.log('Saving story with token:', token.substring(0, 20) + '...')
          console.log('User ID:', user.id)

          // 首先测试认证是否正常
          try {
            const testResponse = await axios.get('http://localhost:8080/api/stories', {
              headers: {
                'Authorization': `Bearer ${token}`
              }
            })
            console.log('Auth test successful, user has', testResponse.data.length, 'stories')
          } catch (testError) {
            console.error('Auth test failed:', testError.response?.status, testError.response?.data)
            if (testError.response?.status === 401) {
              await addMessage('认证失败，请重新登录')
              localStorage.removeItem('token')
              localStorage.removeItem('user')
              return
            }
          }

          const response = await axios.post('http://localhost:8080/api/stories', {
            title: storyTitle.value,  // 使用保存的标题
            content: generatedSummary.value,
            userId: user.id  // 确保包含用户ID
          }, {
            headers: {
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json'
            }
          })

          console.log('Save response:', response.data)
          await addMessage('故事已保存！')
          currentStep.value = 'initial'
          storyTitle.value = ''  // 只在成功保存后清空标题
        } catch (error) {
          console.error('Save error:', error)
          let errorMessage = '保存故事时出错'

          if (error.response) {
            if (error.response.status === 401) {
              errorMessage = '登录已过期，请重新登录'
              // 清除过期的登录信息
              localStorage.removeItem('token')
              localStorage.removeItem('user')
            } else if (error.response.status === 403) {
              errorMessage = '没有保存权限'
            } else {
              errorMessage = error.response.data?.message || `服务器错误 (${error.response.status})`
            }
          } else {
            errorMessage = error.message || errorMessage
          }

          await addMessage(errorMessage)
        }
        return
      }

      await addMessage(modifyInput.value, 'user')
      isGenerating.value = true

      try {
        const response = await aiAxios.post('/modify-story', {
          title: storyTitle.value,
          summary: generatedSummary.value,
          instruction: modifyInput.value
        })

        if (response.data.success) {
          generatedSummary.value = response.data.summary
          await addMessage(response.data.summary)
          await addMessage('还想继续修改吗？（输入修改要求或直接按Enter完成）')
        } else {
          throw new Error(response.data.message || '修改故事失败')
        }
      } catch (error) {
        await addMessage('修改故事时出错：' + error.message)
      }

      isGenerating.value = false
      modifyInput.value = ''
    }

    return {
      isChatOpen,
      messages,
      isGenerating,
      currentStep,
      storyTitle,
      modifyInput,
      chatMessages,
      toggleChat,
      startStoryGeneration,
      submitTitle,
      submitModification
    }
  }
}
</script>

<style scoped>
.ai-assistant {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 1000;
}

.ai-button {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  border: none;
  border-radius: 50%;
  width: 60px;
  height: 60px;
  cursor: pointer;
  box-shadow: 0 4px 15px rgba(79, 70, 229, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  overflow: hidden;
  position: relative;
}

.ai-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.1), rgba(255,255,255,0));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.ai-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.4);
}

.ai-button:hover::before {
  opacity: 1;
}

.ai-button i {
  font-size: 24px;
  margin-bottom: 2px;
  transition: transform 0.3s ease;
}

.ai-button:hover i {
  transform: scale(1.1);
}

.ai-button.active {
  background: linear-gradient(135deg, #4f46e5 0%, #4338ca 100%);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 4px 15px rgba(79, 70, 229, 0.3);
  }
  50% {
    box-shadow: 0 4px 25px rgba(79, 70, 229, 0.5);
  }
  100% {
    box-shadow: 0 4px 15px rgba(79, 70, 229, 0.3);
  }
}

.chat-window {
  position: fixed;
  right: 20px;
  bottom: 90px;
  width: min(350px, calc(100vw - 40px));
  height: min(500px, calc(100vh - 120px));
  background: white;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  animation: slideIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.chat-header {
  padding: 15px 20px;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  border-radius: 20px 20px 0 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.chat-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.close-button {
  background: none;
  border: none;
  color: white;
  font-size: 24px;
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.close-button:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: rotate(90deg);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  scroll-behavior: smooth;
}

.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}

.message {
  max-width: 85%;
  padding: 12px 16px;
  border-radius: 16px;
  margin: 5px 0;
  animation: messageIn 0.3s ease;
  position: relative;
  line-height: 1.5;
}

@keyframes messageIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.assistant {
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  align-self: flex-start;
  border-bottom-left-radius: 4px;
  color: #374151;
}

.message.user {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  align-self: flex-end;
  border-bottom-right-radius: 4px;
  color: white;
}

.chat-input {
  padding: 15px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  background: rgba(255, 255, 255, 0.95);
}

.input-group {
  display: flex;
  gap: 10px;
}

.input-group input {
  flex: 1;
  padding: 10px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  font-size: 14px;
  transition: all 0.3s ease;
  background: white;
}

.input-group input:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.input-group button,
.option-button {
  padding: 10px 20px;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.input-group button::before,
.option-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(255,255,255,0.1), rgba(255,255,255,0));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.input-group button:hover::before,
.option-button:hover::before {
  opacity: 1;
}

.input-group button:hover,
.option-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
}

.start-options {
  display: flex;
  justify-content: center;
  padding: 10px;
}

.generating-indicator {
  padding: 15px;
  text-align: center;
  color: #6366f1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.loading-dots {
  display: inline-flex;
  align-items: center;
  height: 24px;
}

.loading-dots::after {
  content: '';
  width: 4px;
  height: 4px;
  background: currentColor;
  border-radius: 50%;
  animation: loadingDot 1.4s infinite ease-in-out both;
  margin-left: 6px;
}

.loading-dots::before {
  content: '';
  width: 4px;
  height: 4px;
  background: currentColor;
  border-radius: 50%;
  animation: loadingDot 1.4s infinite ease-in-out both -0.32s;
  margin-left: 6px;
}

@keyframes loadingDot {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

@media (max-width: 480px) {
  .chat-window {
    right: 10px;
    left: 10px;
    bottom: 80px;
    width: auto;
  }

  .ai-assistant {
    right: 10px;
    bottom: 10px;
  }

  .message {
    max-width: 90%;
  }
}
</style>
