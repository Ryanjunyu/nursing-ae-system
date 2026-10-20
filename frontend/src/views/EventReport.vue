<template>
  <div>
    <h3 class="page-title">不良事件填报</h3>
    <el-card>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" style="max-width:640px">
        <el-form-item label="发生时间" prop="occurTime">
          <el-date-picker
            v-model="form.occurTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择事件发生时间"
            style="width:100%" />
        </el-form-item>

        <el-form-item label="发生科室" prop="deptId">
          <el-select v-model="form.deptId" placeholder="选择科室" style="width:100%">
            <el-option v-for="d in departments" :key="d.deptId" :label="d.deptName" :value="d.deptId" />
          </el-select>
        </el-form-item>

        <el-form-item label="事件类型" prop="eventType">
          <el-select v-model="form.eventType" placeholder="选择事件类型" style="width:100%">
            <el-option v-for="t in eventTypes" :key="t.typeCode" :label="t.typeName" :value="t.typeCode" />
          </el-select>
        </el-form-item>

        <el-form-item label="损伤分级" prop="injuryLevel">
          <el-select v-model="form.injuryLevel" placeholder="选择损伤分级" style="width:100%">
            <el-option v-for="l in injuryLevels" :key="l.levelCode" :label="l.levelName" :value="l.levelCode" />
          </el-select>
        </el-form-item>

        <el-form-item label="事件描述" prop="eventDesc">
          <el-input v-model="form.eventDesc" type="textarea" :rows="3" placeholder="请描述不良事件经过" />
        </el-form-item>

        <el-form-item label="现场情况">
          <el-input v-model="form.sceneDesc" type="textarea" :rows="2" placeholder="环境、人员、设备等现场因素（可选）" />
        </el-form-item>

        <el-form-item label="匿名上报">
          <el-switch v-model="anonymous" active-text="匿名" inactive-text="实名" />
          <span class="tip">（匿名后上报人身份不入库，列表显示为"匿名"）</span>
        </el-form-item>

        <template v-if="!anonymous">
          <el-divider>患者脱敏信息（仅存脱敏字段，不存明文）</el-divider>
          <el-form-item label="脱敏姓名">
            <el-input v-model="form.patientMaskedName" placeholder="如：张*" />
          </el-form-item>
          <el-form-item label="性别">
            <el-select v-model="form.patientGender" placeholder="选择" style="width:100%">
              <el-option label="男" value="M" />
              <el-option label="女" value="F" />
            </el-select>
          </el-form-item>
          <el-form-item label="年龄">
            <el-input v-model="form.patientAge" type="number" placeholder="岁" />
          </el-form-item>
          <el-form-item label="床号">
            <el-input v-model="form.patientBedNo" placeholder="如：12-3" />
          </el-form-item>
        </template>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">提交上报</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getDepartments, getEventTypes, getInjuryLevels } from '../api/dict'
import { reportEvent } from '../api/event'
import { auth } from '../store/auth'

const departments = ref([])
const eventTypes = ref([])
const injuryLevels = ref([])
const submitting = ref(false)
const anonymous = ref(false)
const formRef = ref()

const user = auth.getUser()
const form = reactive({
  occurTime: '',
  deptId: user ? user.deptId : null,
  eventType: '',
  injuryLevel: '',
  eventDesc: '',
  sceneDesc: '',
  isAnonymous: 0,
  patientMaskedName: '',
  patientGender: '',
  patientAge: null,
  patientBedNo: ''
})

const rules = {
  occurTime: [{ required: true, message: '请选择发生时间', trigger: 'change' }],
  deptId: [{ required: true, message: '请选择科室', trigger: 'change' }],
  eventType: [{ required: true, message: '请选择事件类型', trigger: 'change' }],
  injuryLevel: [{ required: true, message: '请选择损伤分级', trigger: 'change' }],
  eventDesc: [{ required: true, message: '请填写事件描述', trigger: 'blur' }]
}

const isAnonymous = computed(() => (anonymous.value ? 1 : 0))

onMounted(async () => {
  const [d, t, l] = await Promise.all([getDepartments(), getEventTypes(), getInjuryLevels()])
  departments.value = d.data
  eventTypes.value = t.data
  injuryLevels.value = l.data
})

function reset() {
  formRef.value.resetFields()
  form.patientMaskedName = ''
  form.patientGender = ''
  form.patientAge = null
  form.patientBedNo = ''
  anonymous.value = false
}

async function onSubmit() {
  await formRef.value.validate()
  form.isAnonymous = isAnonymous.value
  submitting.value = true
  try {
    const res = await reportEvent(form)
    ElMessage.success('上报成功，事件编号 #' + res.data)
    reset()
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.tip { color: #909399; font-size: 12px; margin-left: 10px; }
</style>
