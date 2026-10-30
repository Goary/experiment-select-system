import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/login/LoginPage.vue') },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    children: [
      { path: 'admin/users', component: () => import('../views/admin/UserManage.vue'), meta: { role: 'admin', title: '用户管理' } },
      { path: 'admin/classrooms', component: () => import('../views/admin/ClassroomManage.vue'), meta: { role: 'admin', title: '教室管理' } },
      { path: 'teacher/courses', component: () => import('../views/teacher/CourseList.vue'), meta: { role: 'teacher', title: '我的课程' } },
      { path: 'teacher/courses/:id', component: () => import('../views/teacher/CourseDetail.vue'), meta: { role: 'teacher', title: '课程详情' } },
      { path: 'student/hall', component: () => import('../views/student/CourseHall.vue'), meta: { role: 'student', title: '选课大厅' } },
      { path: 'student/schedule', component: () => import('../views/student/MySchedule.vue'), meta: { role: 'student', title: '我的课表' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(to => {
  const store = useUserStore()
  if (to.path === '/login') {
    return store.token ? store.homePath : true
  }
  if (!store.token) {
    return '/login'
  }
  if (to.path === '/') {
    return store.homePath
  }
  if (to.meta.role && to.meta.role !== store.role) {
    return store.homePath
  }
  return true
})

export default router
