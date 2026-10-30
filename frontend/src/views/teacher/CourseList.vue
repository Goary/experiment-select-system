<template>
  <div>
    <el-card>
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="课程名称" clearable style="width: 220px" @change="load(1)" />
        <el-button type="primary" @click="openForm()">创建课程</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="courseCode" label="课程编号" width="130" />
        <el-table-column prop="name" label="课程名称" />
        <el-table-column prop="credit" label="学分" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : 'info'" effect="plain">
              {{ { draft: '草稿', published: '已发布', archived: '已归档' }[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/teacher/courses/${row.id}`)">批次管理</el-button>
            <el-button v-if="row.status === 'draft'" link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button v-if="row.status === 'draft'" link type="success" @click="onPublish(row)">发布</el-button>
            <el-button v-if="row.status === 'draft'" link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.size"
        :current-page="query.page"
        @current-change="load" />
    </el-card>

    <el-dialog v-model="dialog" :title="form.id ? '编辑课程' : '创建课程'" width="480px">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="90px">
        <el-form-item label="课程编号" prop="courseCode">
          <el-input v-model="form.courseCode" placeholder="如 EXP101" />
        </el-form-item>
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="学分">
          <el-input-number v-model="form.credit" :min="0" :max="20" :step="0.5" />
        </el-form-item>
        <el-form-item label="课程简介">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { myCourses, createCourse, updateCourse, publishCourse, deleteCourse } from '../../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialog = ref(false)
const formRef = ref()
const query = reactive({ page: 1, size: 10, keyword: '' })
const form = reactive({ id: null, courseCode: '', name: '', credit: 1, description: '' })
const formRules = {
  courseCode: [{ required: true, message: '请输入课程编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }]
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await myCourses(query)
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openForm(row) {
  Object.assign(form, row
    ? { id: row.id, courseCode: row.courseCode, name: row.name, credit: row.credit, description: row.description }
    : { id: null, courseCode: '', name: '', credit: 1, description: '' })
  dialog.value = true
}

async function onSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateCourse(form.id, form)
    } else {
      await createCourse(form)
    }
    ElMessage.success('保存成功')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function onPublish(row) {
  await ElMessageBox.confirm(`发布后不可再编辑基本信息，确定发布「${row.name}」吗？`, '发布课程', { type: 'warning' })
  await publishCourse(row.id)
  ElMessage.success('已发布，现在可以创建批次')
  load()
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除课程「${row.name}」吗？`, '删除', { type: 'warning' })
  await deleteCourse(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(() => load())
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.toolbar .el-button { margin-left: auto; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
