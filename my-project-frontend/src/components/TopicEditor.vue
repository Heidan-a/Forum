<script setup>
import {Check, Document} from "@element-plus/icons-vue";
import {computed, reactive, ref} from "vue";
import axios from "axios";
import {accessHeader, get, post} from "@/net";
import {ElMessage} from "element-plus";
import {Delta, Quill, QuillEditor} from "@vueup/vue-quill";
import ImageResize from "quill-image-resize-vue";
import {ImageExtend, QuillWatch} from "quill-image-super-solution-module";
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import ColorDot from "@/components/ColorDot.vue";
import {useStore} from "@/store";


const editor = reactive({
  type: null,
  title: '',
  text: '',
  uploading: false,
})
const props = defineProps({
  show: Boolean,
  defaultTitle: {
    default: '',
    type: String
  },
  defaultText: {
    default: '',
    type: String
  },
  defaultType: {
    default: null,
    type: Number
  },
  submitButton: {
    default: '立即发表主题',
    type: String
  },
  submit: {
    default: (editor, success) => {
      post('/api/forum/create-topic', {
        type: editor.type.id,
        title: editor.title,
        content: editor.text
      }, () => {
        ElMessage.success("帖子发表成功")
        success()
      })
    },
    type: Function
  }
})
const store = useStore()
const emit = defineEmits(['close', 'success'])
const refEditor = ref()


function initEditor() {
  if (props.defaultText) {
    editor.text = new Delta(JSON.parse(props.defaultText))
  } else {
    refEditor.value.setContents('', 'user')
  }
  editor.title = props.defaultTitle
  editor.type = findTypeById(props.defaultType)
}

function findTypeById(id) {
  for (let type of store.forum.types) {
    if (type.id === id)
      return type
  }
}

function submitTopic() {
  const text = deltaToText(editor.text)
  if (text.length > 20000) {
    ElMessage.warning("限制字数为20000字")
    return
  }
  if (!editor.title) {
    ElMessage.warning("请输入标题")
    return
  }
  if (!editor.type) {
    ElMessage.warning("请选择帖子类型")
    return
  }
  props.submit(editor, () => emit('success'))
}

function deltaToText(delta) {
  if (!delta.ops) return ""
  let str = ""
  for (let op of delta.ops)
    str += op.insert
  return str.replace(/\s/g, "")
}

const ContentLength = computed(() => deltaToText(editor.text).length)

Quill.register('modules/imageResize', ImageResize)
Quill.register('modules/ImageExtend', ImageExtend)
const editorOption = {
  modules: {
    toolbar: {
      container: [
        "bold", "italic", "underline", "strike", "clean",
        {color: []}, {'background': []},
        {size: ["small", false, "large", "huge"]},
        {header: [1, 2, 3, 4, 5, 6, false]},
        {list: "ordered"}, {list: "bullet"}, {align: []},
        "blockquote", "code-block", "link", "image",
        {indent: '-1'}, {indent: '+1'}
      ],
      handlers: {
        'image': function () {
          QuillWatch.emit(this.quill.id)
        }
      }
    },
    imageResize: {
      displayStyles: {
        backgroundColor: 'black',
        border: 'none',
        color: 'white',
      },
      modules: ['Resize', 'DisplaySize']
    },
    ImageExtend: {
      action: axios.defaults.baseURL + '/api/image/cache',
      name: 'file',
      size: 5,
      loading: true,
      accept: 'image/png, image/jpeg',
      response: (resp) => {
        if (resp.data) {
          return axios.defaults.baseURL + '/images' + resp.data
        } else {
          return null
        }
      },
      methods: 'POST',
      headers: xhr => {
        xhr.setRequestHeader('Authorization', accessHeader().Authorization);
      },
      start: () => editor.uploading = true,
      success: () => {
        ElMessage.success('图片上传成功!')
        editor.uploading = false
      },
      error: () => {
        ElMessage.warning('图片上传失败，请联系管理员!')
        editor.uploading = false
      }
    }
  }
}

</script>

<template>
  <div>
    <el-drawer :size="650"
               direction="btt"
               :close-on-click-modal="false"
               :close-on-press-escape="true"
               :model-value="show"
               @open="initEditor"
               @close="emit('close')">
      <template #header>
        <div>
          <div style="font-weight: bold">发表新的帖子</div>
          <div style="font-size: 13px">发表内容之前，请遵守社区公约规范，不要发表违规内容</div>
        </div>
      </template>
      <div style="display: flex;gap: 10px">
        <div style="width: 150px;">
          <el-select placeholder="选择主题类型" v-model="editor.type" value-key="id"
                     :disabled="!store.forum.types.length">
            <el-option v-for="item in store.forum.types.filter(type => type.id > 0)" :value="item" :label="item.name">
              <div>
                <color-dot :color="item.color"/>
                <span style="margin-top: 10px">{{ item.name }}</span>
              </div>
            </el-option>
          </el-select>
        </div>
        <div style="flex: 1;">
          <el-input style="height: 100%" placeholder="请输入帖子标题" :prefix-icon="Document" maxlength="30"
                    v-model="editor.title"/>
        </div>
      </div>
      <div style="margin-top: 5px;font-size: 13px;color: grey">
        <color-dot :color="editor.type ? editor.type.color : '#dedede'"/>
        <span style="margin-top: 5px">
           {{ editor.type ? editor.type.desc : '请在上方选择帖子类型' }}
        </span>
      </div>
      <div style="margin-top: 15px;height: 425px;overflow: hidden;border-radius: 5px"
           v-loading="editor.uploading"
           element-loading-text="正在上传图片...">
        <quill-editor placeholder="想分享点什么呢？" v-model:content="editor.text" style="height: calc(100% - 43px)"
                      content-type="delta" :options="editorOption" ref="refEditor"></quill-editor>
      </div>
      <div style="display: flex;justify-content: space-between;margin-top:5px">
        <div style="font-size: 13px;color: grey">
          当前字数{{ ContentLength }}(最多可输入20000字)
        </div>
        <div style="">
          <el-button type="success" :icon="Check" @click="submitTopic" plain>{{ submitButton }}</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style lang="less" scoped>
:deep(.el-drawer) {
  width: 800px;
  margin: auto;
  border-radius: 10px 10px 0 0;
}

:deep(.el-drawer__header) {
  margin-bottom: 10px;
}
</style>


