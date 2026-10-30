<template>
  <div>
    <el-card>
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="教室名称" clearable style="width: 220px" @change="load" />
        <el-button type="primary" @click="openForm()">新增教室</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="name" label="教室名称" width="180" />
        <el-table-column prop="location" label="位置" />
        <el-table-column prop="capacity" label="容量" width="100" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ row.status === 1 ? '可用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" :title="form.id ? '编辑教室' : '新增教室'" width="420px">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="90px">
        <el-form-item label="教室名称" prop="name">
          <el-input v-model="form.name" placeholder="如：实验楼A-301" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="容量" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" :max="500" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="可用" />
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
import { listClassrooms, createClassroom, updateClassroom, deleteClassroom } from '../../api'

const rows = ref([])
const loading = ref(false)
const saving = ref(false)
const dialog = ref(false)
const keyword = ref('')
const formRef = ref()
const form = reactive({ id: null, name: '', location: '', capacity: 40, status: 1 })
const formRules = {
  name: [{ required: true, message: '请输入教室名称', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入容量', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    rows.value = await listClassrooms({ keyword: keyword.value })
  } finally {
    loading.value = false
  }
}

function openForm(row) {
  Object.assign(form, row
    ? { id: row.id, name: row.name, location: row.location, capacity: row.capacity, status: row.status }
    : { id: null, name: '', location: '', capacity: 40, status: 1 })
  dialog.value = true
}

async function onSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateClassroom(form.id, form)
    } else {
      await createClassroom(form)
    }
    ElMessage.success('保存成功')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除教室「${row.name}」吗？`, '删除', { type: 'warning' })
  await deleteClassroom(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.toolbar .el-button { margin-left: auto; }
</style>
