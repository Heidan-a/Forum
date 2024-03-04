<script setup>
import {useStore} from "@/store";
import {get} from "@/net";

const store = useStore()
get('/api/forum/types', data => {
  const arr = [];
  arr.push({id: 0, name: '全部', desc: '全部帖子', color: 'linear-gradient(45deg,white,red,orange,gold,green,blue)',})
  data.forEach(d => arr.push(d))
  console.log(arr)
  store.forum.types = arr
})
</script>

<template>
  <div>
    <router-view v-slot="{Component}">
      <transition name="el-fade-in-linear" mode="out-in">
        <keep-alive include="TopicList">
          <component :is="Component"/>
        </keep-alive>
      </transition>
    </router-view>
    <el-backtop target=".main-content-page .el-scrollbar__wrap" :right="20" :bottom="70"></el-backtop>
  </div>
</template>

