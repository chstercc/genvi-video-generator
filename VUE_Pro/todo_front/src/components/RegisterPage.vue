<template>
  <div class="register-container">
    <div class="register-wrapper">
      <div class="register-header">
        <h1>创建账号</h1>
        <p>请填写以下信息完成注册</p>
      </div>

      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-position="top"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="请输入用户名（3-50个字符）"
            size="large"
            :prefix-icon="User"
            @blur="checkUsernameExists"
          />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="请输入邮箱地址"
            size="large"
            :prefix-icon="Message"
            @blur="checkEmailExists"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码（6-100个字符）"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
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
            @click="handleRegister"
            class="register-button"
          >
            {{ loading ? '注册中...' : '注册' }}
          </el-button>
        </el-form-item>

        <div class="register-footer">
          <span>已有账号？</span>
          <el-link type="primary" @click="$router.push('/login')">
            立即登录
          </el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import authService from '@/services/authService'

const router = useRouter()

// 表单数据
const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

// 邮箱验证正则
const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

// 自定义验证函数
const validateUsername = async (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入用户名'))
  } else if (value.length < 3 || value.length > 50) {
    callback(new Error('用户名长度必须在3-50个字符之间'))
  } else {
    callback()
  }
}

const validateEmail = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入邮箱'))
  } else if (!emailRegex.test(value)) {
    callback(new Error('请输入有效的邮箱地址'))
  } else {
    callback()
  }
}

const validatePassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入密码'))
  } else if (value.length < 6 || value.length > 100) {
    callback(new Error('密码长度必须在6-100个字符之间'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 表单验证规则
const registerRules = {
  username: [
    { validator: validateUsername, trigger: 'blur' }
  ],
  email: [
    { validator: validateEmail, trigger: 'blur' }
  ],
  password: [
    { validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 响应式变量
const registerFormRef = ref()
const loading = ref(false)

// 检查用户名是否存在
const checkUsernameExists = async () => {
  if (registerForm.username && registerForm.username.length >= 3) {
    const result = await authService.checkUsername(registerForm.username)
    if (result.success && result.exists) {
      ElMessage.warning('用户名已存在，请选择其他用户名')
    }
  }
}

// 检查邮箱是否存在
const checkEmailExists = async () => {
  if (registerForm.email && emailRegex.test(registerForm.email)) {
    const result = await authService.checkEmail(registerForm.email)
    if (result.success && result.exists) {
      ElMessage.warning('邮箱已存在，请选择其他邮箱')
    }
  }
}

// 注册处理
const handleRegister = async () => {
  if (!registerFormRef.value) return

  try {
    await registerFormRef.value.validate()
    loading.value = true

    // 构造注册数据
    const registerData = {
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password
    }

    const result = await authService.register(registerData)

    if (result.success) {
      ElMessage.success('注册成功！正在跳转到登录页面...')
      setTimeout(() => {
        router.push('/login')
      }, 1500)
    } else {
      ElMessage.error(result.message || '注册失败')
    }
  } catch (error) {
    console.error('注册失败:', error)
    ElMessage.error('注册失败，请检查输入信息')
  } finally {
    loading.value = false
  }
}

// 组件挂载时检查是否已登录
onMounted(() => {
  if (authService.isAuthenticated()) {
    router.push('/')
  }
})
</script>

<style scoped>
.register-container {
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
.register-container::before {
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

.register-container::after {
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

.register-wrapper {
  width: 100%;
  max-width: 450px;
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

.register-wrapper:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.2);
}

.register-header {
  text-align: center;
  margin-bottom: 40px;
  position: relative;
}

.register-header::after {
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

.register-header h1 {
  color: #2c3e50;
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 10px;
  background: linear-gradient(135deg, #2c3e50, #3498db);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: fadeInDown 0.5s ease;
}

.register-header p {
  color: #7f8c8d;
  font-size: 16px;
  margin: 0;
  animation: fadeInUp 0.5s ease 0.2s both;
}

.register-form {
  width: 100%;
  animation: fadeIn 0.5s ease 0.3s both;
}

.register-form .el-form-item {
  margin-bottom: 25px;
  position: relative;
  overflow: hidden;
}

.register-form .el-input__wrapper {
  box-shadow: none !important;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.register-form .el-input__wrapper:hover,
.register-form .el-input__wrapper:focus-within {
  border-color: #667eea;
  transform: translateY(-2px);
}

.register-form .el-input__inner {
  height: 48px;
  font-size: 16px;
  padding-left: 45px;
}

.register-form .el-input__prefix {
  font-size: 20px;
  left: 15px;
}

.register-button {
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

.register-button::before {
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

.register-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
}

.register-button:hover::before {
  left: 100%;
}

.register-footer {
  text-align: center;
  margin-top: 30px;
  color: #7f8c8d;
  font-size: 15px;
  animation: fadeIn 0.5s ease 0.4s both;
}

.register-footer .el-link {
  margin-left: 5px;
  font-weight: 600;
  font-size: 15px;
  text-decoration: none;
  position: relative;
}

.register-footer .el-link::after {
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

.register-footer .el-link:hover::after {
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
  .register-wrapper {
    padding: 30px 20px;
    margin: 20px;
  }

  .register-header h1 {
    font-size: 28px;
  }

  .register-header p {
    font-size: 14px;
  }

  .register-form .el-input__inner {
    height: 44px;
    font-size: 15px;
  }

  .register-button {
    height: 46px;
    font-size: 16px;
  }
}
</style>
