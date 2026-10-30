<template>
  <div>
    <el-empty v-if="!loading && !courses.length" description="当前没有开放选课的课程" />
    <el-card v-for="c in courses" :key="c.courseId" class="course-card" v-loading="loading">
      <template #header>
        <div class="course-header">
          <span class="course-name">{{ c.courseName }}</span>
          <span class="course-code">{{ c.courseCode }}</span>
          <el-tag v-if="c.mySelectedBatchId" type="success" effect="plain">已选</el-tag>
        </div>
      </template>
      <p v-if="c.description" class="desc">{{ c.description }}</p>
      <el-table :data="c.batches" stripe>
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
        <el-table-column prop="classroomName" label="教室" width="140" />
        <el-table-column label="授课教师" min-width="120">
          <template #default="{ row }">{{ row.teacherNames?.join('、') }}</template>
        </el-table-column>
        <el-table-column label="上课课次" width="190">
          <template #default="{ row }">
            <div>共 {{ row.sessions?.length || 0 }} 次</div>
            <div class="secondary">{{ fmt(row.sessions?.[0]?.startTime) }} 起</div>
          </template>
        </el-table-column>
        <el-table-column label="剩余名额" width="100">
          <template #default="{ row }">
            <el-tag :type="row.remaining > 0 ? 'primary' : 'danger'" effect="plain">{{ row.remaining }} / {{ row.capacity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-tag v-if="c.mySelectedBatchId === row.id" type="success">已选此批次</el-tag>
            <el-button
              v-else
              type="primary"
              size="small"
              :disabled="c.mySelectedBatchId > 0 || row.remaining <= 0"
              @click="onSelect(c, row)">
              {{ row.remaining <= 0 ? '已满' : '选课' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { openCourses, selectBatch } from '../../api'

const courses = ref([])
const loading = ref(false)

function fmt(t) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

async function load() {
  loading.value = true
  try {
    courses.value = await openCourses()
  } finally {
    loading.value = false
  }
}

async function onSelect(course, batch) {
  const times = batch.sessions
    .map(s => `第${s.sessionNo}次：${fmt(s.startTime)} ~ ${fmt(s.endTime).slice(11)}`)
    .join('\n')
  await ElMessageBox.confirm(
    `确定选择「${course.courseName} - ${batch.batchNo}」吗？\n选择后需参加全部课次：\n${times}`,
    '确认选课',
    { type: 'info' }
  )
  await selectBatch(batch.id)
  ElMessage.success('选课成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.course-card { margin-bottom: 16px; }
.course-header { display: flex; align-items: center; gap: 10px; }
.course-name { font-size: 16px; font-weight: 600; }
.course-code { color: #909399; font-size: 13px; }
.desc { color: #606266; font-size: 13px; margin: 0 0 12px; }
.secondary { margin-top: 4px; color: #909399; font-size: 12px; }
.session-expand { padding: 4px 24px; }
.session-line { display: flex; align-items: center; gap: 12px; margin: 8px 0; }
</style>
