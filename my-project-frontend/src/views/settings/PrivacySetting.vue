<script setup>
import card from "@/components/Card.vue";
import {Lock, Setting, Switch} from "@element-plus/icons-vue";
import {reactive, ref} from "vue";
import {get, post} from "@/net";
import {ElMessage} from "element-plus";

const form=reactive({
  password:'',
  new_password:'',
  new_password_repeat:''
})
const validateNewPwd=(rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.new_password) {
    callback(new Error("两次输入的密码不一致"))
  } else {
    callback()
  }
}
const rules = {
  password: [
    {required: true, trigger: 'blur', message: '密码的长度必须在2-8个字符之间'}
  ],
  new_password: [
    {required: true,message: '请输入新的密码' },
    {min:6, max:16,trigger: 'blur', message: '密码的长度必须在2-8个字符之间'}
  ],
  new_password_repeat: [
    {required: true, message: '请再次输入新密码'},
    {validator:validateNewPwd , trigger: 'blur'}
  ]
}
const formRef=ref()
const valid=ref(false)
const onValid=(prop,isValid) =>{valid.value=isValid}
function resetPwd(){
  formRef.value.validate((isValid)=>{
    if(isValid){
      post('/api/user/change-password',form,()=>{
        ElMessage.success('密码修改成功')
        formRef.value.resetFields();
      })
    }
  })
}
const saving=ref(true)
const privacy=reactive({
  phone:false,
  qq:false,
  wx:false,
  email:false,
  gender:false,
})
get('/api/user/privacy',(data) =>{
  privacy.email=data.email
  privacy.qq=data.qq
  privacy.gender=data.gender
  privacy.phone=data.phone
  privacy.wx=data.wx
  saving.value=false
})
function savePrivacy(type,status){
  saving.value=true
  post('/api/user/save-privacy',{
    type: type,
    status: status
  },()=>{
    ElMessage.success('隐私设置保存成功')
    saving.value=false
  })
}
</script>

<template>
  <div style="margin: auto;max-width: 600px">
    <div style="margin-top: 20px">
      <card :icon="Setting" title="隐私设置" desc="这里可以设置个人隐私信息,请注意个人隐私">
        <div class="checkbox-list">
          <el-checkbox @change="savePrivacy('phone',privacy.phone)" v-model="privacy.phone">公开展示我的手机号</el-checkbox>
          <el-checkbox @change="savePrivacy('email',privacy.email)" v-model="privacy.email">公开展示我的电子邮件地址</el-checkbox>
          <el-checkbox @change="savePrivacy('qq',privacy.qq)" v-model="privacy.qq">公开展示我的QQ号</el-checkbox>
          <el-checkbox @change="savePrivacy('wx',privacy.wx)" v-model="privacy.wx">公开展示我的WX号</el-checkbox>
          <el-checkbox @change="savePrivacy('gender',privacy.gender)" v-model="privacy.gender">公开展示我的性别</el-checkbox>
        </div>
      </card>
      <card style="margin: 20px 0px" :icon="Setting" title="修改密码" desc="这里可以修改密码,请注意密码泄露" v-loading="saving">
        <el-form @validate="onValid" :model="form" label-width="120px" style="margin: 20px" :rules="rules" ref="formRef">
          <el-form-item label="当前密码" prop="password">
            <el-input type="password" :prefix-icon="Lock" placeholder="当前密码" v-model="form.password" maxlength="16"/>
          </el-form-item>
          <el-form-item label="新密码" prop="new_password">
            <el-input type="password" :prefix-icon="Lock" placeholder="新密码" v-model="form.new_password" maxlength="16"/>
          </el-form-item>
          <el-form-item label="再次输入密码" prop="new_password_repeat">
            <el-input type="password" :prefix-icon="Lock" placeholder="再次输入密码" v-model="form.new_password_repeat" maxlength="16"/>
          </el-form-item>
          <div style="text-align: center">
            <el-button :icon="Switch" @click="resetPwd">重置密码</el-button>
          </div>
        </el-form>
      </card>
    </div>
  </div>
</template>

<style scoped>
.checkbox-list{
  margin: 10px 0 0 10px;
  display: flex;
  flex-direction: column;
}
</style>