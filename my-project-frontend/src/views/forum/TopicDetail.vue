<script setup>
import {useRoute} from "vue-router";
import {get, post} from "@/net";
import axios from "axios";
import {computed, reactive, ref} from "vue";
import {ArrowLeft, ChatSquare, CircleCheck, Delete, EditPen, Female, Male, Plus, Star} from "@element-plus/icons-vue";
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
  like: false,
  collect: false,
  comments: null,
  page: 0
})
const comment = reactive({
  show: false,
  text: '',
  quote: null
})
const edit = ref(false)
const init = () =>
    get(`/api/forum/topic?tid=${tid}`, data => {
      topic.data = data
      topic.like = data.interact.like
      topic.collect = data.interact.collect
      loadComment(1)
    })


init()

function onCommentAdd() {
  comment.show = false
  loadComment(Math.floor((topic.data.comments + 1) / 10 + 1))
}

function convertToHtml(content) {
  const ops = JSON.parse(content).ops
  const converter = new QuillDeltaToHtmlConverter(ops, {inlineStyles: true})
  return converter.convert()
}

function interact(type, message) {
  get(`/api/forum/interact?tid=${tid}&type=${type}&state=${!topic[type]}`, () => {
    topic[type] = !topic[type]
    if (topic[type]) {
      ElMessage.success(`${message}成功！`)
    } else {
      ElMessage.warning(`已取消${message}！`)
    }
  })
}

function updateTopic(editor) {
  post('/api/forum/update-topic', {
    id: tid,
    type: editor.type.id,
    title: editor.title,
    content: editor.text
  }, () => {
    ElMessage.success('帖子更新成功')
    edit.value = false
    init()
  })
}

function loadComment(page) {
  topic.comments = null
  topic.page = page
  get(`/api/forum/comments?tid=${tid}&page=${page - 1}`, data => topic.comments = data)
}

function deleteComment(id) {
  get(`/api/forum/delete-comment?id=${id}`, data => {
    ElMessage.success('评论删除成功')
    loadComment(topic.page)
  })
}

</script>

<template>
  <div class="topic-page" v-if="topic.data">
    <div class="topic-main" style="position:sticky; top: 0px;z-index: 5">
      <Card style="display: flex;width: 100%">
        <el-button :icon="ArrowLeft" size="small" type="info"
                   plain round @click="router.push('/index')">返回主页
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
            {{ topic.data.user.username }}
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
          <interact-button name="编辑帖子" :check="false" color="dodgerblue"
                           @click="edit = true" style="margin-right: 20px" v-if="store.user.id === topic.data.user.id">
            <el-icon>
              <EditPen/>
            </el-icon>
          </interact-button>
          <interact-button name="点个赞" :check="topic.like" color="pink" check-name='已点赞'
                           @click="interact('like','点赞')">
            <el-icon>
              <CircleCheck/>
            </el-icon>
          </interact-button>
          <interact-button name="收藏本帖" :check="topic.collect" color="orange"
                           check-name='已收藏' style="margin-left: 20px" @click="interact('collect','收藏')">
            <el-icon>
              <Star/>
            </el-icon>
          </interact-button>
        </div>
      </div>
    </div>
    <!--    评论区-->
    <transition name="el-fade-in-linear" mode="out-in">
      <div v-if="topic.comments">
        <div class="topic-main" style="margin-top: 10px" v-for="item in topic.comments">
          <div class="topic-main-left">
            <el-avatar :src="store.avatarUserUrl(item.user.avatar)"
                       :size="60"/>
            <div>
              <div style="font-size: 14px;font-weight: bold">
                {{ item.user.username }}
                <span style="color: hotpink" v-if="item.user.gender === 1">
              <el-icon><Female/></el-icon>
            </span>
                <span style="color: dodgerblue" v-if="item.user.gender === 0">
              <el-icon><Male/></el-icon>
            </span>
              </div>
              <div class="desc">{{ item.user.email }}</div>
              <el-divider style="margin: 10px 0"/>
              <div style="text-align: left;margin: 5px 0">
                <div class="desc">手机号: {{ item.user.phone || '用户隐藏或未填写' }}</div>
                <div class="desc">微信号: {{ item.user.wx || '用户隐藏或未填写' }}</div>
                <div class="desc">QQ号: {{ item.user.qq || '用户隐藏或未填写' }}</div>
              </div>
            </div>
          </div>
          <div class="topic-main-right">
            <div style="font-size: 13px;color: grey;">
              评论时间为:{{ new Date(item.time).toLocaleString() }}
            </div>
            <div v-if="item.quote" class="comment-quote">
              回复:{{ item.quote }}
            </div>
            <div class="topic-content" v-html="convertToHtml(item.content)"></div>
            <div style="text-align: right">
              <!--              将当前评论给到新评论的quote-->
              <el-link :icon="ChatSquare" @click="comment.show = true;comment.quote = item" type="info">&nbsp;回复评论
              </el-link>
              <el-link :icon="Delete" v-if="item.user.id === store.user.id"
                       type="danger" style="margin-left: 20px" @click="deleteComment(item.id)">&nbsp;删除评论
              </el-link>
            </div>
          </div>
        </div>
        <div style="width: fit-content;margin: 20px auto">
          <el-pagination background layout="prev,pager,next"
                         v-model:current-page="topic.page" @current-change="loadComment "
                         :total="topic.data.comments" :page-size="10" hide-on-single-page/>
        </div>
      </div>
    </transition>
    <topic-editor :show="edit" @close="edit = false" v-if="topic.data && store.forum.types"
                  :default-text="topic.data.content"
                  :default-title="topic.data.title" :default-type="topic.data.type" submit-button="更新帖子内容"
                  :submit="updateTopic"></topic-editor>
    <topic-comment-editor :show="comment.show" @close="comment.show = false" :tid="tid" :quote="comment.quote"
                          @comment="onCommentAdd">

    </topic-comment-editor>
    <div class="add-comment" @click="comment.show = true;comment.quote = null">
      <el-icon>
        <Plus/>
      </el-icon>
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