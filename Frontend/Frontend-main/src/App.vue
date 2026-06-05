<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import FloatingNote from './components/FloatingNote.vue'
import { useAuthStore } from './store/auth'

const route = useRoute()
const auth = useAuthStore()
const showFloatingNote = computed(
  () =>
    auth.state.isLoggedIn &&
    !['/login', '/signup', '/unity', '/service-intro'].includes(route.path) &&
    !(route.path === '/game' && !route.query.stage && !route.query.problem)
)

watch(
  () => route.path,
  async (path) => {
    await auth.restoreAuth(path)
  },
  { immediate: true }
)
</script>

<template>
  <RouterView />
  <FloatingNote v-if="showFloatingNote" />
</template>
