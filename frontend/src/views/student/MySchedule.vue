<template>
  <div>
    <el-card>
      <template #header><span class="title">我的课表</span></template>
      <el-empty v-if="!loading && !rows.length" description="还没有已选课程，去选课大厅看看吧" />
      <el-table v-else :data="rows" v-loading="loading" stripe>
        <el-table-column prop="courseName" label="课程名称" min-width="150" />
        <el-table-column prop="courseCode" label="课程编号" width="110" />
        <el-table-column prop="batchNo" label="批次" width="90" />
        <el-table-column label="课次" width="70">
          <template #default="{ row }">第{{ row.sessionNo }}次</template>
        </el-table-column>
        <el-table-column prop="classroomName" label="教室" width="130" />
        <el-table-column label="授课教师" min-width="110">
          <template #default="{ row }">{{ row.teacherNames?.join('、') }}</template>
        </el-table-column>
        <el-table-column label="上课时间" width="270">
          <template #default="{ row }">{{ fmt(row.startTime) }} ~ {{ fmt(row.endTime).slice(11) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.batchStatus === 'closed' ? 'info' : 'success'" effect="plain">
              {{ row.batchStatus === 'closed' ? '已结课' : '已选' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button v-if="row.batchStatus !== 'closed'" link type="danger" @click="onDrop(row)">退选</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { mySchedule, dropSelection } from '../../api'

const rows = ref([])
const loading = ref(false)

function fmt(t) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

async function load() {
  loading.value = true
  try {
    rows.value = await mySchedule()
  } finally {
    loading.value = false
  }
}

async function onDrop(row) {
  await ElMessageBox.confirm(`确定退选「${row.courseName} - ${row.batchNo}」的全部课次吗？`, '退选', { type: 'warning' })
  await dropSelection(row.selectionId)
  ElMessage.success('已退选')
  load()
}

onMounted(load)
</script>

<style scoped>
.title { font-weight: 600; }
</style>
