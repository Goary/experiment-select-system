<template>
  <div class="login-page">
    <el-card class="card">
      <h2 class="title">实验选课系统</h2>
      <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter="onSubmit">
        <el-form-item prop="employeeNo">
          <el-input v-model="form.employeeNo" placeholder="工号 / 学号" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="btn" :loading="loading" @click="onSubmit">登 录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../../api'
import { useUserStore } from '../../store/user'

const router = useRouter()
const store = useUserStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ employeeNo: '', password: '' })
const rules = {
  employeeNo: [{ required: true, message: '请输入工号/学号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await login(form)
    store.login(data.token, data.user)
    ElMessage.success('登录成功')
    router.push(store.homePath)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f0f8 0%, #f5f7fa 100%);
}
.card { width: 380px; border-radius: 8px; }
.title { text-align: center; margin: 8px 0 28px; color: #1f2d3d; letter-spacing: 4px; }
.btn { width: 100%; margin-top: 6px; }
</style>
