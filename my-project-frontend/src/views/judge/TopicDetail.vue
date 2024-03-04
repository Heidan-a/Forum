<script setup>
import {useRoute} from "vue-router";
import {get, post} from "@/net";
import axios from "axios";
import {computed, reactive, ref} from "vue";
import {
    ArrowLeft,
    ChatSquare,
    CircleCheck,
    CircleClose,
    Delete,
    EditPen,
    Female,
    Male,
    Plus,
    Star
} from "@element-plus/icons-vue";
import {QuillDeltaToHtmlConverter} from 'quill-delta-to-html';
import Card from "@/components/Card.vue";
import router from "@/router";
import TopicTag from "@/components/TopicTag.vue";
import InteractButton from "@/components/InteractButton.vue";
import {ElMessage} from "element-plus";
import {useStore} from "@/store";
import TopicEditor from "@/components/TopicEditor.vue";
import TopicCommentEditor from "@/components/TopicCommentEditor.vue";

const store = useStore()
const route = useRoute()
const tid = route.params.tid
const topic = reactive({
  data: null,
  page: 0
})

const edit = ref(false)
const init = () =>
    get(`/api/judge/topic?tid=${tid}`, data => {
      topic.data = data
    })

init()


function convertToHtml(content) {
  const ops = JSON.parse(content).ops
  const converter = new QuillDeltaToHtmlConverter(ops, {inlineStyles: true})
  return converter.convert()
}

function interact(type) {
    if (type === "agree") {
        get(`/api/judge/agree?tid=${tid}`, () => {
            ElMessage.success(`审核成功！`)
        })
    } else {
        get(`/api/judge/delete?tid=${tid}`, () => {
            ElMessage.warning(`删除成功！`)
        })
    }
}

</script>

<template>
  <div class="topic-page" v-if="topic.data">
    <div class="topic-main" style="position:sticky; top: 0;z-index: 5">
      <Card style="display: flex;width: 100%">
        <el-button :icon="ArrowLeft" size="small" type="info"
                   plain round @click="router.push('/index/judge')">返回主页
        </el-button>
        <div style="flex: 1;text-align: center">
          <topic-tag :type="topic.data.type"/>
          <span style="font-weight: bold;margin-left: 8px">{{ topic.data.title }}</span>
        </div>
      </Card>
    </div>
    <div class="topic-main">
      <div class="topic-main-left">
        <el-avatar :src="store.avatarUserUrl(topic.data.user.avatar)"
                   :size="60"/>
        <div>
          <div style="font-size: 14px;font-weight: bold">
            {{topic.data.user.username}}
            <span style="color: hotpink" v-if="topic.data.user.gender === 1">
              <el-icon><Female/></el-icon>
            </span>
            <span style="color: dodgerblue" v-if="topic.data.user.gender === 0">
              <el-icon><Male/></el-icon>
            </span>
          </div>
          <div class="desc">{{ topic.data.user.email }}</div>
          <el-divider style="margin: 10px 0"/>
          <div style="text-align: left;margin: 5px 0">
            <div class="desc">手机号: {{ topic.data.user.phone || '用户隐藏或未填写' }}</div>
            <div class="desc">微信号: {{ topic.data.user.wx || '用户隐藏或未填写' }}</div>
            <div class="desc">QQ号: {{ topic.data.user.qq || '用户隐藏或未填写' }}</div>
          </div>
          <el-divider style="margin: 10px 0"/>
          <div class="desc" style="margin: 0 5px">个签: {{ topic.data.user.desc || '这个用户很懒，什么都没留下' }}</div>
        </div>
      </div>
      <div class="topic-main-right">
        <div class="topic-content" v-html="convertToHtml(topic.data.content)"></div>

        <el-divider/>
        <div>
          <div style="font-size: 13px;color: grey;text-align: right;">
            发帖时间为:{{ new Date(topic.data.time).toLocaleTimeString() }}
          </div>
        </div>
        <div style="text-align: right;margin-top: 30px">
            <interact-button name="不通过"  color="pink"
                             @click="interact('delete');router.push('/index/judge/')">
                <el-icon>
                    <CircleClose/>
                </el-icon>
            </interact-button>
          <interact-button name="通过"  color="green"
                           @click="interact('agree');router.push('/index/judge/')">
            <el-icon>
              <CircleCheck/>
            </el-icon>
          </interact-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
.add-comment {
  width: 40px;
  height: 40px;
  position: fixed;
  bottom: 20px;
  right: 20px;
  border-radius: 50%;
  text-align: center;
  line-height: 45px;
  color: var(--el-color-primary);
  background: var(--el-bg-color-overlay);
  box-shadow: var(--el-box-shadow-light);

  &:hover {
    cursor: pointer;
    background: var(--el-border-color-extra-light);
  }
}

.topic-page {
  display: flex;
  flex-direction: column;
  padding: 10px 5px;
  gap: 10px;
}

.topic-main {
  width: 800px;
  display: flex;
  margin: 0 auto;
  border-radius: 6px;
  background-color: var(--el-bg-color);

  .topic-main-left {
    width: 200px;
    padding: 10px;
    text-align: center;
    border-right: 1px solid var(--el-border-color);

    .desc {
      font-size: 12px;
      color: grey;
    }
  }

  .topic-main-right {
    width: 600px;
    padding: 10px 20px;
    display: flex;
    flex-direction: column;

    .topic-content {
      font-size: 16px;
      opacity: 0.8;
      line-height: 24px;
      flex: 1;
    }
  }
}

.comment-quote {
  font-size: 13px;
  color: grey;
  background-color: rgba(94, 94, 94, 0.1);
  padding: 10px;
  margin-top: 10px;
  border-radius: 5px;
}
</style>