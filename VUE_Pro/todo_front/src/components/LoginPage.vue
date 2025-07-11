<template>
  <div class="login-container">
    <div class="login-wrapper">
      <div class="login-header">
        <h1>欢迎登录</h1>
        <p>请输入您的账号信息</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        label-position="top"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            class="login-button"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>

        <div class="login-footer">
          <span>还没有账号？</span>
          <el-link type="primary" @click="$router.push('/register')">
            立即注册
          </el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import authService from '@/services/authService'

const router = useRouter()

// 表单数据
const loginForm = reactive({
  username: '',
  password: ''
})

// 表单验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度必须在3-50个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度必须在6-100个字符之间', trigger: 'blur' }
  ]
}

// 响应式变量
const loginFormRef = ref()
const loading = ref(false)

// 登录处理
const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    await loginFormRef.value.validate()
    loading.value = true

    const result = await authService.login(loginForm)

    if (result.success) {
      ElMessage.success('登录成功')
      // 跳转到首页或之前的页面
      const redirect = router.currentRoute.value.query.redirect || '/'
      router.push(redirect)
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    console.error('登录失败:', error)
    ElMessage.error('登录失败，请检查输入信息')
  } finally {
    loading.value = false
  }
}

// 组件挂载时检查是否已登录
import { onMounted } from 'vue'
onMounted(() => {
  if (authService.isAuthenticated()) {
    router.push('/')
  }
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

/* 添加左下角背景动画效果 */
.login-container::before {
  content: '';
  position: absolute;
  left: -50px;
  bottom: -50px;
  width: 300px;
  height: 300px;
  background: linear-gradient(
    45deg,
    rgba(255, 255, 255, 0.1) 25%,
    transparent 25%,
    transparent 50%,
    rgba(255, 255, 255, 0.1) 50%,
    rgba(255, 255, 255, 0.1) 75%,
    transparent 75%
  );
  background-size: 30px 30px;
  animation: moveBackground 20s linear infinite;
  opacity: 0.3;
  border-radius: 50%;
  transform-origin: center;
  backdrop-filter: blur(5px);
}

.login-container::after {
  content: '';
  position: absolute;
  left: -25px;
  bottom: -25px;
  width: 200px;
  height: 200px;
  background: linear-gradient(
    -45deg,
    rgba(255, 255, 255, 0.1) 25%,
    transparent 25%,
    transparent 50%,
    rgba(255, 255, 255, 0.1) 50%,
    rgba(255, 255, 255, 0.1) 75%,
    transparent 75%
  );
  background-size: 20px 20px;
  animation: moveBackground 15s linear infinite reverse;
  opacity: 0.2;
  border-radius: 50%;
  transform-origin: center;
  backdrop-filter: blur(3px);
}

@keyframes moveBackground {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.login-wrapper {
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.95);
  padding: 40px;
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  transform: translateY(0);
  transition: all 0.3s ease;
  position: relative;
  z-index: 1;
}

.login-wrapper:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.2);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
  position: relative;
}

.login-header::after {
  content: '';
  position: absolute;
  bottom: -15px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border-radius: 2px;
}

.login-header h1 {
  color: #2c3e50;
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 10px;
  background: linear-gradient(135deg, #2c3e50, #3498db);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: fadeInDown 0.5s ease;
}

.login-header p {
  color: #7f8c8d;
  font-size: 16px;
  margin: 0;
  animation: fadeInUp 0.5s ease 0.2s both;
}

.login-form {
  width: 100%;
  animation: fadeIn 0.5s ease 0.3s both;
}

.login-form .el-form-item {
  margin-bottom: 25px;
  position: relative;
  overflow: hidden;
}

.login-form .el-input__wrapper {
  box-shadow: none !important;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.login-form .el-input__wrapper:hover,
.login-form .el-input__wrapper:focus-within {
  border-color: #667eea;
  transform: translateY(-2px);
}

.login-form .el-input__inner {
  height: 48px;
  font-size: 16px;
  padding-left: 45px;
}

.login-form .el-input__prefix {
  font-size: 20px;
  left: 15px;
}

.login-button {
  width: 100%;
  height: 50px;
  font-size: 18px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.login-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.2),
    transparent
  );
  transition: 0.5s;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
}

.login-button:hover::before {
  left: 100%;
}

.login-footer {
  text-align: center;
  margin-top: 30px;
  color: #7f8c8d;
  font-size: 15px;
  animation: fadeIn 0.5s ease 0.4s both;
}

.login-footer .el-link {
  margin-left: 5px;
  font-weight: 600;
  font-size: 15px;
  text-decoration: none;
  position: relative;
}

.login-footer .el-link::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 100%;
  height: 2px;
  background: #667eea;
  transform: scaleX(0);
  transition: transform 0.3s ease;
  transform-origin: right;
}

.login-footer .el-link:hover::after {
  transform: scaleX(1);
  transform-origin: left;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-wrapper {
    padding: 30px 20px;
    margin: 20px;
  }

  .login-header h1 {
    font-size: 28px;
  }

  .login-header p {
    font-size: 14px;
  }

  .login-form .el-input__inner {
    height: 44px;
    font-size: 15px;
  }

  .login-button {
    height: 46px;
    font-size: 16px;
  }
}
</style>
