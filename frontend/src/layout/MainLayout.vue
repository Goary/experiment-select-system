<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="logo">实验选课系统</div>
      <div class="user">
        <el-tag size="small" effect="plain">{{ roleText }}</el-tag>
        <span class="name">{{ store.user?.username }}</span>
        <el-button link type="primary" @click="onLogout">退出登录</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" class="aside">
        <el-menu :default-active="$route.path" router>
          <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">{{ m.title }}</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '../store/user'

const store = useUserStore()
const router = useRouter()

const MENU = {
  admin: [
    { path: '/admin/users', title: '用户管理' },
    { path: '/admin/classrooms', title: '教室管理' }
  ],
  teacher: [
    { path: '/teacher/courses', title: '我的课程' }
  ],
  student: [
    { path: '/student/hall', title: '选课大厅' },
    { path: '/student/schedule', title: '我的课表' }
  ]
}

const menus = computed(() => MENU[store.role] || [])
const roleText = computed(() => ({ admin: '超级管理员', teacher: '教师', student: '学生' }[store.role] || ''))

async function onLogout() {
  await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
  store.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { height: 100vh; }
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #1f2d3d;
  color: #fff;
}
.logo { font-size: 18px; font-weight: 600; letter-spacing: 2px; }
.user { display: flex; align-items: center; gap: 10px; }
.user .name { font-size: 14px; }
.aside { border-right: 1px solid #e4e7ed; background: #fff; }
.main { background: #f5f7fa; padding: 20px; }
</style>
