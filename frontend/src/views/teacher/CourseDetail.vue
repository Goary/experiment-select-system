<template>
  <div>
    <el-card class="info-card">
      <template #header>
        <div class="card-header">
          <el-button link type="primary" @click="$router.back()">← 返回</el-button>
          <span class="title">{{ course.name || '课程详情' }}</span>
          <el-tag v-if="course.status" :type="course.status === 'published' ? 'success' : 'info'" effect="plain">
            {{ { draft: '草稿', published: '已发布' }[course.status] }}
          </el-tag>
        </div>
      </template>
      <el-descriptions :column="3">
        <el-descriptions-item label="课程编号">{{ course.courseCode }}</el-descriptions-item>
        <el-descriptions-item label="学分">{{ course.credit ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="课程简介">{{ course.description || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card>
      <div class="toolbar">
        <span class="section-title">批次列表</span>
        <el-button type="primary" :disabled="course.status !== 'published'" @click="openForm()">
          创建批次{{ course.status !== 'published' ? '（需先发布课程）' : '' }}
        </el-button>
      </div>
      <el-table :data="batches" v-loading="loading" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="session-expand">
              <div v-for="session in row.sessions" :key="session.id" class="session-line">
                <el-tag size="small" effect="plain">第{{ session.sessionNo }}次</el-tag>
                <span>{{ fmt(session.startTime) }} ~ {{ fmt(session.endTime).slice(11) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次" width="90" />
        <el-table-column prop="classroomName" label="教室" width="130" />
        <el-table-column label="授课教师" min-width="120">
          <template #default="{ row }">{{ row.teacherNames?.join('、') }}</template>
        </el-table-column>
        <el-table-column label="上课课次" width="190">
          <template #default="{ row }">
            <div>共 {{ row.sessions?.length || 0 }} 次</div>
            <div class="secondary">{{ fmt(row.sessions?.[0]?.startTime) }} 起</div>
          </template>
        </el-table-column>
        <el-table-column label="名额" width="110">
          <template #default="{ row }">{{ row.selectedCount }} / {{ row.capacity }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="batchTagType(row)" effect="plain">{{ batchStatusText(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="330">
          <template #default="{ row }">
            <el-button v-if="row.status === 'draft'" link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button v-if="row.status === 'draft'" link type="success" @click="doAction(publishBatch, row, '已发布')">发布</el-button>
            <el-button v-if="row.status === 'published' && !row.selectOpen" link type="warning" @click="doAction(openBatch, row, '已开放选课')">开放选课</el-button>
            <el-button v-if="row.selectOpen === 1" link type="warning" @click="doAction(closeBatch, row, '已关闭')">关闭选课</el-button>
            <el-button link type="primary" @click="showStudents(row)">名单</el-button>
            <el-button link type="primary" @click="onExport(row)">导出</el-button>
            <el-button v-if="row.status === 'draft'" link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" :title="form.id ? '编辑批次' : '创建批次'" width="720px">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="批次号" prop="batchNo">
          <el-input v-model="form.batchNo" placeholder="如：第1批" />
        </el-form-item>
        <el-form-item label="上课教室" prop="classroomId">
          <el-select v-model="form.classroomId" style="width: 100%" placeholder="选择教室">
            <el-option v-for="c in classrooms" :key="c.id" :value="c.id" :label="`${c.name}（${c.location || '-'}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="批次容量">
          <el-input :model-value="selectedCapacity ? `${selectedCapacity} 人（随教室固定）` : '选择教室后自动带出'" disabled />
        </el-form-item>
        <el-form-item label="授课教师" prop="teacherIds">
          <el-select v-model="form.teacherIds" multiple style="width: 100%" placeholder="可选择多名教师">
            <el-option v-for="t in teachers" :key="t.id" :value="t.id" :label="`${t.username}（${t.employeeNo}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="上课课次" required>
          <div class="session-editor">
            <div v-for="(session, index) in form.sessions" :key="index" class="session-row">
              <span class="session-label">第{{ index + 1 }}次</span>
              <el-date-picker
                v-model="session.timeRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DDTHH:mm:ss"
                class="session-picker" />
              <el-button link type="danger" :disabled="form.sessions.length === 1" @click="removeSession(index)">删除</el-button>
            </div>
            <el-button type="primary" plain @click="addSession">+ 添加课次</el-button>
            <div class="form-tip">学生选择批次后需参加全部课次，各课次共用以上教室和教师。</div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="studentDialog" :title="`选课名单 - ${currentBatch?.batchNo || ''}`" width="640px">
      <el-table :data="students" stripe max-height="420">
        <el-table-column prop="employeeNo" label="工号/学号" width="120" />
        <el-table-column prop="username" label="姓名" width="120" />
        <el-table-column prop="className" label="班级" />
        <el-table-column prop="selectTime" label="选课时间" width="180" />
      </el-table>
      <el-empty v-if="!students.length" description="暂无学生选课" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  courseDetail, listBatches, createBatch, updateBatch, publishBatch, openBatch,
  closeBatch, deleteBatch, batchStudents, listTeachers, listAvailableClassrooms, exportUrl
} from '../../api'
import { useUserStore } from '../../store/user'

const route = useRoute()
const store = useUserStore()
const courseId = route.params.id

const course = ref({})
const batches = ref([])
const classrooms = ref([])
const teachers = ref([])
const loading = ref(false)
const saving = ref(false)
const dialog = ref(false)
const studentDialog = ref(false)
const students = ref([])
const currentBatch = ref(null)
const formRef = ref()
const emptySession = () => ({ timeRange: [] })
const form = reactive({ id: null, batchNo: '', classroomId: null, teacherIds: [], sessions: [emptySession()] })
const formRules = {
  batchNo: [{ required: true, message: '请输入批次号', trigger: 'blur' }],
  classroomId: [{ required: true, message: '请选择教室', trigger: 'change' }],
  teacherIds: [{ required: true, type: 'array', min: 1, message: '请选择授课教师', trigger: 'change' }]
}

const selectedCapacity = computed(() => classrooms.value.find(c => c.id === form.classroomId)?.capacity)

function fmt(t) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

function batchStatusText(row) {
  if (row.status === 'closed') return '已关闭'
  if (row.status === 'draft') return '草稿'
  return row.selectOpen === 1 ? '选课中' : '已发布'
}

function batchTagType(row) {
  if (row.status === 'closed') return 'info'
  if (row.status === 'draft') return 'info'
  return row.selectOpen === 1 ? 'warning' : 'success'
}

async function load() {
  loading.value = true
  try {
    ;[course.value, batches.value] = await Promise.all([courseDetail(courseId), listBatches(courseId)])
  } finally {
    loading.value = false
  }
}

async function openForm(row) {
  if (!teachers.value.length || !classrooms.value.length) {
    ;[teachers.value, classrooms.value] = await Promise.all([listTeachers(), listAvailableClassrooms()])
  }
  Object.assign(form, row
    ? {
        id: row.id,
        batchNo: row.batchNo,
        classroomId: row.classroomId,
        teacherIds: row.teacherIds || [],
        sessions: row.sessions.map(s => ({ timeRange: [s.startTime, s.endTime] }))
      }
    : {
        id: null,
        batchNo: `第${batches.value.length + 1}批`,
        classroomId: null,
        teacherIds: [store.user.id],
        sessions: [emptySession()]
      })
  dialog.value = true
}

function addSession() {
  form.sessions.push(emptySession())
}

function removeSession(index) {
  form.sessions.splice(index, 1)
}

async function onSave() {
  await formRef.value.validate()
  for (let i = 0; i < form.sessions.length; i++) {
    const range = form.sessions[i].timeRange
    if (!range || range.length !== 2) {
      ElMessage.warning(`请选择第${i + 1}个课次的起止时间`)
      return
    }
    if (range[1] <= range[0]) {
      ElMessage.warning(`第${i + 1}个课次的结束时间必须晚于开始时间`)
      return
    }
  }
  saving.value = true
  try {
    const payload = {
      courseId: Number(courseId),
      batchNo: form.batchNo,
      classroomId: form.classroomId,
      teacherIds: form.teacherIds,
      sessions: form.sessions.map(s => ({ startTime: s.timeRange[0], endTime: s.timeRange[1] }))
    }
    if (form.id) {
      await updateBatch(form.id, payload)
    } else {
      await createBatch(payload)
    }
    ElMessage.success('保存成功')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function doAction(fn, row, msg) {
  await fn(row.id)
  ElMessage.success(msg)
  load()
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.batchNo}」吗？`, '删除', { type: 'warning' })
  await deleteBatch(row.id)
  ElMessage.success('已删除')
  load()
}

async function showStudents(row) {
  currentBatch.value = row
  students.value = await batchStudents(row.id)
  studentDialog.value = true
}

function onExport(row) {
  // 下载需携带 token，使用 fetch + blob
  fetch(exportUrl({ by: 'batch', batchId: row.id }), {
    headers: { Authorization: `Bearer ${store.token}` }
  }).then(async res => {
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `选课名单-${course.value.name}-${row.batchNo}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  })
}

onMounted(load)
</script>

<style scoped>
.info-card { margin-bottom: 16px; }
.card-header { display: flex; align-items: center; gap: 12px; }
.card-header .title { font-size: 16px; font-weight: 600; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-title { font-weight: 600; }
.secondary { margin-top: 4px; color: #909399; font-size: 12px; }
.session-expand { padding: 4px 24px; }
.session-line { display: flex; align-items: center; gap: 12px; margin: 8px 0; }
.session-editor { width: 100%; }
.session-row { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.session-label { flex: 0 0 48px; color: #606266; }
.session-picker { flex: 1; min-width: 0; }
.form-tip { margin-top: 8px; color: #909399; font-size: 12px; line-height: 1.6; }
</style>
