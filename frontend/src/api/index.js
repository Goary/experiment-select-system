import request from './request'

// 认证
export const login = data => request.post('/auth/login', data)
export const getMe = () => request.get('/auth/me')

// 超管：用户管理
export const pageUsers = params => request.get('/admin/users', { params })
export const createUser = data => request.post('/admin/users', data)
export const updateUser = (id, data) => request.put(`/admin/users/${id}`, data)
export const deleteUser = id => request.delete(`/admin/users/${id}`)
export const resetPassword = id => request.post(`/admin/users/${id}/reset-password`)

// 超管：教室管理
export const listClassrooms = params => request.get('/admin/classrooms', { params })
export const createClassroom = data => request.post('/admin/classrooms', data)
export const updateClassroom = (id, data) => request.put(`/admin/classrooms/${id}`, data)
export const deleteClassroom = id => request.delete(`/admin/classrooms/${id}`)

// 教师：课程
export const myCourses = params => request.get('/courses/my', { params })
export const courseDetail = id => request.get(`/courses/${id}`)
export const createCourse = data => request.post('/courses', data)
export const updateCourse = (id, data) => request.put(`/courses/${id}`, data)
export const publishCourse = id => request.post(`/courses/${id}/publish`)
export const deleteCourse = id => request.delete(`/courses/${id}`)

// 教师：批次
export const listBatches = courseId => request.get(`/courses/${courseId}/batches`)
export const createBatch = data => request.post('/batches', data)
export const updateBatch = (id, data) => request.put(`/batches/${id}`, data)
export const publishBatch = id => request.post(`/batches/${id}/publish`)
export const openBatch = id => request.post(`/batches/${id}/open`)
export const closeBatch = id => request.post(`/batches/${id}/close`)
export const deleteBatch = id => request.delete(`/batches/${id}`)
export const batchStudents = id => request.get(`/batches/${id}/students`)

// 学生
export const openCourses = () => request.get('/student/courses/open')
export const selectBatch = batchId => request.post(`/student/select/${batchId}`)
export const dropSelection = selectionId => request.post(`/student/drop/${selectionId}`)
export const mySchedule = () => request.get('/student/schedule')

// 导出（返回 blob 下载地址用 window.open 即可，鉴权通过 header 需单独处理）
export const exportUrl = params => {
  const q = new URLSearchParams(params).toString()
  return `/api/export/selections?${q}`
}

// 公共下拉数据
export const listTeachers = () => request.get('/common/teachers')
export const listAvailableClassrooms = () => request.get('/common/classrooms')
