<template>
  <StudentForumPrototype :detail-id="detailId" />
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import StudentForumPrototype from '@/components/StudentForumPrototype.vue'

const detailId = ref('')
onLoad(options => {
  detailId.value = options?.id || ''
})
</script>

<style>
page { background: #fff; }
</style>
