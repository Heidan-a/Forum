<script setup>

import LightCard from "@/components/LightCard.vue";
import {
  Calendar,
  Clock,
  CollectionTag,
  EditPen,
  Link,
  Edit,
  Document,
  Compass,
  Microphone,
  Picture, CircleCheck, Star, FolderOpened, ArrowRight
} from "@element-plus/icons-vue";
import Weather from "@/components/Weather.vue";
import {computed, reactive, ref, watch} from "vue";
import {get} from "@/net";
import {ElMessage} from "element-plus";
import TopicEditor from "@/components/TopicEditor.vue";
import {useStore} from "@/store";
import axios from "axios";
import ColorDot from "@/components/ColorDot.vue";
import router from "@/router";
import TopicTag from "@/components/TopicTag.vue";
import TopicCollectList from "@/components/TopicCollectList.vue";


const store = useStore()

const today= computed(() =>{
  const date=new Date()
  return `${date.getFullYear()} 年 ${date.getMonth() + 1} 月 ${date.getDate()} 日`
})
const collects = ref(false)
const editor = ref(false)
const weather = reactive({
  location: {},
  now: {},
  hourly: [],
  success: false
})
const topics = reactive({
  list: [],
  type: 0,
  page: 0,
  end: false,
  top: []
})

watch(() => topics.type, () => {
  resetList()
}, {immediate: true})

function resetList() {
  topics.page = 0
  topics.end = false
  topics.list = []
  uploadList()
}

function uploadList() {
  if (topics.end) return
  get(`/api/judge/list-topic?page=${topics.page}&type=${topics.type}`, data => {
    if (data) {
      data.forEach(d => topics.list.push(d))
      topics.page++
    }
    if (!data || data.length < 10) {
      topics.end = true
    }
  })
}
</script>

<template>
  <div style="display:flex;margin: 20px auto;gap: 20px;max-width: 900px">
    <div style="flex: 1">
      <!--      分类块-->
      <light-card style="margin-top: 10px;display: flex;gap: 7px">
        <div :class="`type-select-card ${topics.type === item.id ? 'active' : ''}`"
             v-for="item in store.forum.types"
             @click="topics.type = item.id">
          <color-dot :color="item.color"></color-dot>
          <span style="margin-left: 5px">{{ item.name }}</span>
        </div>
      </light-card>
      <!--      主列表-->
      <transition name="el-fade-in" mode="out-in">
        <div v-if="topics.list.length">
          <div style="margin-top: 10px;display: flex;flex-direction: column;gap: 10px;"
               v-infinite-scroll="uploadList">
            <light-card v-for="item in topics.list" class="topic-card"
                        @click="router.push('/index/judge/judge-detail/'+item.id)">
              <div style="display: flex">
                <div>
                  <el-avatar :size="30" :src="store.avatarUserUrl(item.avatar)"></el-avatar>
                </div>
                <div style="margin-left: 7px">
                  <div style="font-size: 15px;font-weight: bold;">{{ item.username }}</div>
                  <div style="font-size: 12px;color: grey">
                    <el-icon>
                      <Clock/>
                    </el-icon>
                    <div style="margin-left: 2px;display: inline-block;transform: translateY(-2px)">
                      {{ new Date(item.time).toLocaleDateString() }}
                    </div>
                  </div>
                </div>
              </div>
              <div style="margin-top: 5px;">
                <topic-tag :type="item.type"/>
                <span style="margin-left: 7px">{{ item.title }}</span>
              </div>
              <div class="topic-content">{{ item.text }}</div>
              <div style="display: grid;grid-template-columns: repeat(3, 1fr);grid-gap: 10px">
                <el-image class="topic-image" v-for="img in item.images" :src="img" fit="cover"></el-image>
              </div>

            </light-card>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<style lang="less" scoped>
.collect-list-button {
  font-size: 14px;
  display: flex;
  justify-content: space-between;
  transition: .3s;

  &:hover {
    cursor: pointer;
    opacity: 0.4;
  }
}

.top-topic {
  display: flex;

  div:first-of-type {
    font-size: 14px;
    margin-left: 10px;
    font-weight: bold;
    opacity: 0.8;
    transition: color 0.3s;

    &:hover {
      color: grey;
    }
  }

  div:nth-of-type(2) {
    flex: 1;
    color: grey;
    font-size: 13px;
    text-align: right;
  }

  &:hover {
    cursor: pointer;
  }
}

.topic-card {
  padding: 15px;
  transition: scale 0.3s;

  &:hover {
    scale: 1.05;
    cursor: pointer;
  }

  .topic-content {
    font-size: 13px;
    color: grey;
    margin: 5px 0;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3;
    overflow: hidden;
    text-overflow: ellipsis;
  }

}

.info-text {
    display: flex;
    justify-content: space-between;
    color: grey;
    font-size: 14px
  }

.friend-link {
    border-radius: 5px;
    overflow: hidden;
  }

.create-topic {
    border-radius: 5px;
    height: 40px;
  border-color: var(--el-bg-color);
    padding: 0 10px;
    font-size: 14px;
    line-height: 40px;
    color: grey;
    &:hover{
      cursor: pointer;
    }
  }

.topic-image {
  height: 100%;
  width: 100%;
  max-height: 110px;
  border-radius: 5px;

}

.type-select-card {
  background-color: #f5f5f5;
  font-size: 14px;
  padding: 2px 7px;
  border-radius: 3px;
  box-sizing: border-box;
  transition: background-color .3s;

  &.active {
    box-sizing: border-box;
    border: solid 1px #ead4c4;
  }

  &:hover {
    background-color: #dadada;
    cursor: pointer;
  }
}

.dark {
  .create-topic {
    background-color: #232323
  }

  .type-select-card {
    background-color: #282828;

    &.active {
      border: 1px solid #64594b;
    }

    &:hover {
      background-color: #5e5e5e;
    }
  }
}
</style>