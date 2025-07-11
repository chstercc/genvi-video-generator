import 'element-plus/dist/index.css'

import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import router from './router'
import { userStore } from './stores/userStore'
import '@fortawesome/fontawesome-free/css/all.css'

const app = createApp(App)
app.use(ElementPlus)
app.use(router)

// 初始化用户状态
userStore.methods.init()

app.mount('#app')
