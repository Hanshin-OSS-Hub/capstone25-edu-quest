<script setup lang="ts">
import BackButton from './BackButton.vue'

defineProps<{
  title: string
  subtitle?: string
  backLink?: string
  backLabel?: string
}>()

const emit = defineEmits<{
  back: []
}>()
</script>

<template>
  <header class="relative overflow-hidden border-b border-[#1A2A4F]/10 bg-white/80 backdrop-blur">
    <div class="absolute inset-x-0 top-0 h-full bg-[radial-gradient(circle_at_left_top,rgba(247,165,165,0.22),transparent_32%),radial-gradient(circle_at_right_top,rgba(255,219,182,0.35),transparent_28%)]" />
    <div class="relative mx-auto flex max-w-7xl items-center justify-between gap-4 px-5 py-5 sm:px-8">
      <BackButton :to="backLink" :aria-label="backLabel" @click="emit('back')" />

      <div class="flex-1 text-center">
        <p class="text-xs font-bold uppercase tracking-[0.28em] text-[#1A2A4F]/55">EduQuest</p>
        <h1 class="mt-1 text-2xl font-black text-[#1A2A4F] sm:text-3xl">{{ title }}</h1>
        <p v-if="subtitle" class="mx-auto mt-2 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
          {{ subtitle }}
        </p>
      </div>

      <div class="flex min-w-[4rem] justify-end">
        <slot name="rightAction" />
      </div>
    </div>
  </header>
</template>
