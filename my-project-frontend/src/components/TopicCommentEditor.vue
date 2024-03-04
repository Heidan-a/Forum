<script setup>
import {Delta, QuillEditor} from "@vueup/vue-quill";
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import {ref} from "vue";
import {post} from "@/net";
import {ElMessage} from "element-plus";

const props = defineProps({
  show: Boolean,
  tid: String,
  quote: Object
})
const init = () => content.value = new Delta()
const emit = defineEmits(['close', 'comment'])
const content = ref()

function submitComment() {
  if (deltaToText(content.value).length > 2000) {
    ElMessage.warning("评论超出2000字")
    return
  }
  post('/api/forum/add-comment', {
    tid: props.tid,
    quote: props.quote ? props.quote.id : -1,
    content: JSON.stringify(content.value)
  }, () => {
    ElMessage.success('评论发表成功')
    emit('comment')
  })
}

function deltaToText(delta) {
  if (!delta?.ops) return ""
  let str = ""
  for (let op of delta.ops)
    str += op.insert
  return str.replace(/\s/g, "")
}

function deltaToSimpleText(delta) {
  let str = deltaToText(JSON.parse(delta))
  if (str.length > 35) str = str.substring(0, 35) + "..."
  return str
}
</script>

<template>
  <div>
    <el-drawer :model-value="show" @close="emit('close')"
               :title="quote ? `发表对评论:${deltaToSimpleText(quote.content)}的评论` : `发表回复`" @open="init"
               direction="btt" :size="370" :close-on-click-modal="false">
      <div>
        <div>
          <quill-editor style="height: 200px" v-model:content="content" placeholder="留下你的评论"/>
        </div>
        <div style="margin-top: 10px;display: flex">
          <div style="flex: 1;font-size: 13px;color: grey">
            字数统计:{{ deltaToText(content).length }} 最大支持2000字
          </div>
          <div style="margin-top: 10px;text-align: right">
            <el-button type="success" @click="submitComment" plain round>发表评论</el-button>
          </div>
        </div>
      </div>
    </el-drawer>

  </div>
</template>

<style scoped>
:deep(.el-drawer) {
  width: 800px;
  margin: 20px auto;
  border-radius: 10px;
}

:deep(.el-drawer__header) {
  margin-bottom: 10px;
}

:deep(.el-drawer__body) {
  padding: 10px;
}
</style>