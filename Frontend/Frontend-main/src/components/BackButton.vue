<script setup lang="ts">
defineProps<{
  to?: string
  ariaLabel?: string
}>()

const emit = defineEmits<{
  click: []
}>()
</script>

<template>
  <RouterLink
    v-if="to"
    :to="to"
    :aria-label="ariaLabel ?? '뒤로가기'"
    class="inline-flex h-10 items-center justify-center rounded-full border border-gray-300 bg-white px-4 text-gray-700 shadow-sm transition hover:border-gray-400 hover:bg-gray-100"
  >
    ←
  </RouterLink>
  <button
    v-else
    type="button"
    :aria-label="ariaLabel ?? '뒤로가기'"
    class="inline-flex h-10 items-center justify-center rounded-full border border-gray-300 bg-white px-4 text-gray-700 shadow-sm transition hover:border-gray-400 hover:bg-gray-100"
    @click="emit('click')"
  >
    ←
  </button>
</template>
