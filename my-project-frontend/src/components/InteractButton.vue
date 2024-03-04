<script setup>
defineProps({
  name: String,
  color: String,
  check: Boolean,
  checkName: String
})
const emit = defineEmits(['check'])
</script>

<template>
  <div class="interact-button">
  <span class="icon" :style="{'color': color}" @click="emit('check')">
    <slot/>
  </span>
    <span class="name" :style="{'color': check ? color :'unset'}">
    {{ check ? checkName : name }}
  </span>
  </div>
</template>

<style scoped>
.interact-button {
  display: inline-block;
  height: 20px;

  .name {
    font-size: 13px;
    opacity: 0.7;
    margin-left: 5px;
    margin-left: 5px;
  }

  .icon {
    vertical-align: middle;
    transition: 0.3s;

    &:hover {
      cursor: pointer;
      font-size: 18px;
    }
  }
}
</style>