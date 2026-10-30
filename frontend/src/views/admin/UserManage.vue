<template>
  <div>
    <el-card>
      <div class="toolbar">
        <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 140px" @change="load(1)">
          <el-option label="教师" value="teacher" />
          <el-option label="学生" value="student" />
          <el-option label="管理员" value="admin" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="工号/学号或姓名" clearable style="width: 220px" @change="load(1)" />
        <el-button type="primary" @click="openForm()">新增用户</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="username" label="工号/学号" width="140" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="{ admin: 'danger', teacher: 'primary', student: 'success' }[row.role]" effect="plain">
              {{ { admin: '管理员', teacher: '教师', student: '学生' }[row.role] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button link type="warning" @click="onReset(row)">重置密码</el-button>
            <el-button v-if="row.role !== 'admin'" link type="danger" @click="onDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialog" :title="form.id ? '编辑用户' : '新增用户'" width="420px">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="90px">
        <el-form-item label="工号/学号" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="教师" value="teacher" />
            <el-option label="学生" value="student" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.role === 'student'" label="班级">
          <el-input v-model="form.className" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码">
          <el-input v-model="form.password" placeholder="留空默认 123456" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" />
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
import { pageUsers, createUser, updateUser, deleteUser, resetPassword } from '../../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialog = ref(false)
const formRef = ref()
const query = reactive({ page: 1, size: 10, role: '', keyword: '' })
const form = reactive({ id: null, username: '', realName: '', role: 'teacher', className: '', status: 1, password: '' })
const formRules = {
  username: [{ required: true, message: '请输入工号/学号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await pageUsers(query)
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openForm(row) {
  Object.assign(form, row
    ? { id: row.id, username: row.username, realName: row.realName, role: row.role, className: row.className, status: row.status, password: '' }
    : { id: null, username: '', realName: '', role: 'teacher', className: '', status: 1, password: '' })
  dialog.value = true
}

async function onSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateUser(form.id, form)
    } else {
      await createUser(form)
    }
    ElMessage.success('保存成功')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function onReset(row) {
  await ElMessageBox.confirm(`确定将「${row.realName}」的密码重置为 123456 吗？`, '重置密码', { type: 'warning' })
  await resetPassword(row.id)
  ElMessage.success('已重置为 123456')
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除用户「${row.realName}」吗？`, '删除', { type: 'warning' })
  await deleteUser(row.id)
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
