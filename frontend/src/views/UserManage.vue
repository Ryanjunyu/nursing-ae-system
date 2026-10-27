<template>
  <div>
    <h3 class="page-title">用户权限管理</h3>

    <el-empty v-if="!isAdmin" description="仅系统管理员可管理用户" />

    <template v-else>
      <el-card>
        <div style="margin-bottom:12px">
          <el-button type="primary" @click="openCreate">新增用户</el-button>
          <span class="tip">角色：普通护士 / 科室护士长 / 系统管理员；护士长仅可查看本科室数据</span>
        </div>

        <el-table :data="rows" stripe border v-loading="loading">
          <el-table-column prop="username" label="用户名" width="140" />
          <el-table-column prop="realName" label="姓名" width="120" />
          <el-table-column label="角色" width="120">
            <template #default="{ row }">
              <el-tag :type="roleTag(row.role)">{{ roleText(row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="deptName" label="科室" width="150" />
          <el-table-column prop="phone" label="电话" width="140" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="480px">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" :disabled="isEdit" placeholder="登录账号" />
          </el-form-item>
          <el-form-item label="密码" :prop="isEdit ? '' : 'password'">
            <el-input v-model="form.password" type="password" show-password
              :placeholder="isEdit ? '留空表示不修改密码' : '初始密码'" />
          </el-form-item>
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="form.realName" />
          </el-form-item>
          <el-form-item label="角色" prop="role">
            <el-select v-model="form.role" style="width:100%">
              <el-option label="普通护士" value="NURSE" />
              <el-option label="科室护士长" value="HEAD_NURSE" />
              <el-option label="系统管理员" value="ADMIN" />
            </el-select>
          </el-form-item>
          <el-form-item label="科室">
            <el-select v-model="form.deptId" clearable style="width:100%">
              <el-option v-for="d in departments" :key="d.deptId" :label="d.deptName" :value="d.deptId" />
            </el-select>
          </el-form-item>
          <el-form-item label="电话">
            <el-input v-model="form.phone" />
          </el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { listUsers, createUser, updateUser } from '../api/user'
import { getDepartments } from '../api/dict'
import { auth } from '../store/auth'

const user = auth.getUser()
const isAdmin = computed(() => user && user.role === 'ADMIN')

const rows = ref([])
const departments = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()

const form = reactive({ userId: null, username: '', password: '', realName: '', role: 'NURSE', deptId: null, phone: '', status: 1 })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const roleMap = { ADMIN: '系统管理员', HEAD_NURSE: '科室护士长', NURSE: '普通护士' }
const roleText = (r) => roleMap[r] || r
const roleTag = (r) => (r === 'ADMIN' ? 'danger' : r === 'HEAD_NURSE' ? 'warning' : 'info')

onMounted(async () => {
  if (!isAdmin.value) return
  const [u, d] = await Promise.all([listUsers(), getDepartments()])
  rows.value = u.data
  departments.value = d.data
})

function resetForm() {
  Object.assign(form, { userId: null, username: '', password: '', realName: '', role: 'NURSE', deptId: null, phone: '', status: 1 })
}

function openCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, { userId: row.userId, username: row.username, password: '', realName: row.realName, role: row.role, deptId: row.deptId, phone: row.phone, status: 1 })
  form.status = row.status == null ? 1 : row.status
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (isEdit.value) {
      await updateUser(form.userId, { ...form })
      ElMessage.success('已保存')
    } else {
      const res = await createUser({ ...form })
      ElMessage.success('已创建，用户ID #' + res.data)
    }
    dialogVisible.value = false
    rows.value = (await listUsers()).data
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.tip { color: #909399; font-size: 12px; margin-left: 12px; }
</style>
