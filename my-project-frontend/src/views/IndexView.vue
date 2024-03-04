<script setup>
import {get, logout} from '@/net'
import router from "@/router";
import {useStore} from "@/store";
import {reactive, ref} from "vue";
import {
    Bell,
    Notification,
    ChatDotSquare,
    Location,
    Guide,
    School,
    Position,
    Document,
    Files, Monitor, Collection, DataLine, Operation, User, Lock, Search, Back, Message, Check
} from "@element-plus/icons-vue";
import TopicEditor from "@/components/TopicEditor.vue";
import LightCard from "@/components/LightCard.vue";
const store=useStore()
const loading=ref(true)
let role = store.user.role
const searchInput=reactive({
  type:'1',
  text:''
})
const notification = ref([])
get('/api/user/info',(data) =>{
  store.user=data
  role = data.role
  loading.value=false
})

const loadNotification = () =>
    get('/api/notification/list', (data) => notification.value = data)
loadNotification()
function userLogout() {
  logout(() => router.push("/"))
}

function confirmNotice(id, url) {
    get(`/api/notification/delete?id=${id}`, () => {
        loadNotification()
        window.open(url)
    })
}

function deleteAllNotice() {
    get('/api/notification/delete-all', () => {
        loadNotification()
    })
}
console.log(role)
</script>
<template>
  <div class="main-content" v-loading="loading" element-loading-text="正在加载，请稍候...">
    <el-container style="height: 100%" v-if="!loading">
      <el-header class="main-content-header" >
        <el-image style="margin-left: 30px" class="logo" src="/public/9_5社区论坛.svg"></el-image>
        <div style="flex: 1;padding: 0 20px;text-align: center">
          <el-input v-model="searchInput.text" style="width: 100%;max-width: 800px" placeholder="搜索论坛相关内容...">
            <template #prefix>
              <el-icon><Search/></el-icon>
            </template>
            <template #append>
              <el-select style="width:120px" v-model="searchInput.type">
                <el-option value="1" label="帖子广场"></el-option>
                <el-option value="2" label="校园活动"></el-option>
                <el-option value="3" label="表白墙"></el-option>
                <el-option value="4" label="教务通知"></el-option>
              </el-select>
            </template>
          </el-input>
        </div>
        <div style="max-width: 300px" class="user-info">
            <el-popover :width="400" placement="bottom" trigger="click">
                <template #reference>
                    <el-badge is-dot style="margin-right: 20px" :hidden="!notification.length">
                        <div class="notification">
                            <el-icon>
                                <Bell/>
                            </el-icon>
                            <div style="font-size: 10px">消息</div>
                        </div>
                    </el-badge>
                </template>
                <el-empty :image-size="80" description="暂时没有未读消息哦~" v-if="!notification.length"/>
                <el-scrollbar v-for="item in notification" :max-height="500" v-else>
                    <light-card class="notification-item" @click="confirmNotice(item.id,item.url)">
                        <div>
                            <el-tag :type="item.type">消息</el-tag>
                            <span style="font-weight: bold">{{ item.title }}</span>
                        </div>
                        <el-divider style="margin: 7px 0 3px 0"/>
                        <div style="font-size: 14px">
                            {{ item.content }}
                        </div>
                    </light-card>
                </el-scrollbar>
                <div style="margin-top: 10px">
                    <el-button type="success" :icon="Check" size="small" @click="deleteAllNotice"
                               style="width: 100%" plain>清除所有未读消息
                    </el-button>
                </div>
            </el-popover>
            <div class="profile">
              <div>{{store.user.username}}</div>
              <div>{{store.user.email}}</div>
            </div>
          <el-dropdown>
            <el-avatar :src=store.avatarUrl></el-avatar>
            <template #dropdown>
              <el-dropdown-item @click="router.push('/index/user-setting')">
                <el-icon><Operation/></el-icon>
                个人设置
              </el-dropdown-item>
              <el-dropdown-item>
                <el-icon><Message/></el-icon>
                消息列表
              </el-dropdown-item>
              <el-dropdown-item divided @click="userLogout">
                <el-icon><Back/></el-icon>
                退出登录
              </el-dropdown-item>
            </template>
          </el-dropdown>
          </div>
      </el-header>
      <el-container>
        <el-aside width="230px">
          <el-scrollbar style="height: calc(100vh - 55px)">
            <el-menu
                router
                :default-active="$route.path"
                :default-openeds="['1','2','3']"
                style="min-height: calc(100vh - 55px)">
<!--              这里用min-height是为了旁边的滚动条在你打开折叠菜单才变大动态适应-->
              <el-sub-menu index="1">
                <template #title>
                  <el-icon><Location/></el-icon>
                  <span><b>校园论坛</b></span>
                </template>
                <el-menu-item index="/index">
                  <template #title>
                    <el-icon><ChatDotSquare /></el-icon>
                    <span>帖子广场</span>
                  </template>
                </el-menu-item>
                  <el-menu-item v-if="role === 'admin'"  @click="router.push('/index/judge')">
                      <template #title>
                          <el-icon><Files /></el-icon>
                          <span>帖子管理</span>
                      </template>
                  </el-menu-item>
                <el-menu-item>
                  <template #title>
                    <el-icon><Bell /></el-icon>
                    <span>失物招领</span>
                  </template>
                </el-menu-item>
                <el-menu-item>
                  <template #title>
                    <el-icon><Notification /></el-icon>
                    <span>校园活动</span>
                  </template>
                </el-menu-item>
                <el-menu-item>
                  <template #title>
                    <el-icon><Guide /></el-icon>
                    <span>表白墙</span>
                  </template>
                </el-menu-item>
                <el-menu-item>
                  <template #title>
                    <el-icon><School /></el-icon>
                    <span>海文考研</span>
                    <el-tag style="margin-left: 10px" size="small">合作机构</el-tag>
                  </template>
                </el-menu-item>
              </el-sub-menu>
              <el-sub-menu index="2">
                <template #title>
                  <el-icon><Position/></el-icon>
                  <span><b>探索与发现</b></span>
                </template>
                <el-menu-item>
                  <template #title>
                    <el-icon><Document /></el-icon>
                    <span>成绩查询</span>
                  </template>
                </el-menu-item>
                <el-menu-item>
                  <template #title>
                    <el-icon><Files /></el-icon>
                    <span>班级课程</span>
                  </template>
                </el-menu-item>
                <el-menu-item>
                  <template #title>
                    <el-icon><Monitor /></el-icon>
                    <span>教务通知</span>
                  </template>
                </el-menu-item>
                <el-menu-item>
                  <template #title>
                    <el-icon><Collection /></el-icon>
                    <span>在线图书馆</span>
                  </template>
                </el-menu-item>
                <el-menu-item>
                  <template #title>
                    <el-icon><DataLine /></el-icon>
                    <span>预约教室</span>
                  </template>
                </el-menu-item>
              </el-sub-menu>
              <el-sub-menu index="3">
                <template #title>
                  <el-icon><Operation /></el-icon>
                  <span><b>设置</b></span>
                </template>
                <el-menu-item index="/index/user-setting">
                  <template #title>
                    <el-icon><User /></el-icon>
                    <span>个人信息设置</span>
                  </template>
                </el-menu-item>
                <el-menu-item index="/index/privacy-setting">
                  <template #title>
                    <el-icon><Lock /></el-icon>
                    <span>账号安全设置</span>
                  </template>
                </el-menu-item>
              </el-sub-menu>
            </el-menu>
          </el-scrollbar>
        </el-aside>
        <el-main style="padding: 0" class="main-content-page">
          <el-scrollbar style="height: calc(100vh - 55px)">
            <router-view v-slot="{Component}">
              <transition name="el-fade-in-linear" mode="out-in">
                <component :is="Component" style="height: 100%"/>
              </transition>
            </router-view>
          </el-scrollbar>
        </el-main>
      </el-container>

    </el-container>
  </div>
</template>



<style scoped lang="less">
.notification {
    font-size: 22px;
    line-height: 14px;
    text-align: center;
    transition: color .3s;

    &:hover {
        color: grey;
        cursor: pointer;
    }
}

.notification-item {
    transition: .3s;

    &:hover {
        cursor: pointer;
        opacity: 0.7;
    }
}
  .main-content{
    height: 100vh;
    width: 100vw;
  }
  .main-content-page{
    background-color: #f7f8fa;
  }
  .dark .main-content-page{
    background-color: #242528;
  }
  .main-content-header{
    border-bottom: 1px solid var(--el-border-color);
    height: 55px;
    display: flex;
    align-items: center;
    box-sizing: border-box;
  }
  .logo{
    height: 32px;

  }
  .user-info{
    display: flex;
    justify-content: flex-end;
    align-items: center;

    .el-avatar:hover{
      cursor: pointer;
    }

    .profile {
      text-align: right;
      margin-right: 20px;

      :first-child {
        font-size: 18px;
        font-weight: bold;
        line-height: 20px;
      }

      :last-child {
        font-size: 10px;
        color: grey;
      }
    }
  }
</style>
