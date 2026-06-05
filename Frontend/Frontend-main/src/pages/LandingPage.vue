<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import type { ComponentPublicInstance } from 'vue'
import Navbar from '../components/Navbar.vue'

const previewVideo = '/unity_preview.mp4'
const scrollY = ref(0)
const revealTargets = ref<HTMLElement[]>([])
const videoAvailable = ref(true)

let revealObserver: IntersectionObserver | null = null

const navStyle = computed(() => {
  const intensity = Math.min(scrollY.value / 320, 1)
  const blur = 8 + intensity * 16
  const backgroundOpacity = 0.82 + intensity * 0.12

  return {
    '--nav-blur': `${blur}px`,
    '--nav-bg': `rgba(255, 255, 255, ${backgroundOpacity})`,
    '--nav-border': `rgba(26, 42, 79, ${0.12 + intensity * 0.1})`,
  }
})

const handleScroll = () => {
  scrollY.value = window.scrollY
}

const handleVideoError = () => {
  videoAvailable.value = false
}

const registerReveal = (element: Element | ComponentPublicInstance | null) => {
  if (element instanceof HTMLElement && !revealTargets.value.includes(element)) {
    revealTargets.value.push(element)
  }
}

onMounted(() => {
  handleScroll()
  window.addEventListener('scroll', handleScroll, { passive: true })

  revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible')
          revealObserver?.unobserve(entry.target)
        }
      })
    },
    {
      threshold: 0.2,
      rootMargin: '0px 0px -10% 0px',
    },
  )

  revealTargets.value.forEach((element) => revealObserver?.observe(element))
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
  revealObserver?.disconnect()
})
</script>

<template>
  <div class="landing-shell min-h-screen bg-[#FFF2EF]" :style="navStyle">
    <Navbar />

    <main class="relative isolate min-h-[calc(100vh-80px)] overflow-hidden">
      <div class="absolute inset-0 bg-[#1A2A4F]" />
      <div class="absolute inset-0 bg-[radial-gradient(circle_at_top,rgba(255,219,182,0.22),transparent_35%),radial-gradient(circle_at_bottom,rgba(247,165,165,0.18),transparent_30%)]" />

      <video
        v-if="videoAvailable"
        autoplay
        muted
        loop
        playsinline
        class="absolute inset-0 h-full w-full object-cover"
        @error="handleVideoError"
      >
        <source :src="previewVideo" type="video/mp4" />
      </video>

      <div class="absolute inset-0 bg-[#1A2A4F]/65" />
      <div
        v-if="!videoAvailable"
        class="absolute inset-0 bg-[linear-gradient(135deg,rgba(26,42,79,0.88),rgba(26,42,79,0.62)),repeating-linear-gradient(90deg,rgba(255,255,255,0.04)_0,rgba(255,255,255,0.04)_2px,transparent_2px,transparent_22px)]"
      />

      <section class="relative z-10 flex min-h-[calc(100vh-80px)] items-center px-4 py-5 sm:px-6 sm:py-6 lg:px-8">
        <div class="mx-auto flex w-full max-w-6xl flex-col items-center justify-center gap-6 text-center sm:gap-8">
          <div
            :ref="registerReveal"
            class="fade-up flex w-full max-w-4xl flex-col items-center px-4 py-4 text-white sm:px-8 sm:py-6"
          >
            <p class="rounded-full border-2 border-[#1A2A4F] bg-[#FFDBB6] px-5 py-2 text-sm font-black text-[#1A2A4F] sm:text-base">
              게임처럼 배우는 재미있는 코딩
            </p>
            <h1 class="mt-4 text-4xl font-black leading-tight tracking-[-0.04em] text-white sm:text-5xl lg:text-6xl">
              코딩 학습의 새로운
              <span class="block text-[#FFDBB6]">EduQuest!</span>
            </h1>
            <p class="mt-4 max-w-3xl text-base font-bold leading-7 text-slate-100 sm:text-lg sm:leading-8 lg:text-xl">
              코딩 문제를 풀고 스테이지를 하나씩 클리어해요!
              <br class="hidden lg:block">
              틀린 문제도, 다시 보고 싶은 문제도 EduQuest가 기억해 줄게요 😉
            </p>
          </div>

          <div class="flex w-full max-w-3xl flex-col justify-center gap-4 sm:flex-row">
            <RouterLink
              to="/signup"
              class="flex-1 rounded-[24px] border-4 border-[#1A2A4F] bg-[#F7A5A5] px-6 py-4 text-center text-lg font-black text-[#1A2A4F] shadow-[8px_8px_0_0_rgba(26,42,79,0.16)] transition duration-300 hover:-translate-y-1"
            >
              처음이에요! 모험 시작하기
            </RouterLink>
            <RouterLink
              to="/login"
              class="flex-1 rounded-[24px] border-4 border-[#1A2A4F] bg-[#FFDBB6] px-6 py-4 text-center text-lg font-black text-[#1A2A4F] shadow-[8px_8px_0_0_rgba(26,42,79,0.16)] transition duration-300 hover:-translate-y-1"
            >
              이미 계정이 있어요!
            </RouterLink>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.landing-shell :deep(nav) {
  background: var(--nav-bg);
  border-color: var(--nav-border);
  backdrop-filter: blur(var(--nav-blur));
  -webkit-backdrop-filter: blur(var(--nav-blur));
  transition:
    background 220ms ease,
    border-color 220ms ease,
    backdrop-filter 220ms ease;
}

.fade-up {
  opacity: 0;
  transform: translateY(32px);
  transition:
    opacity 0.7s ease,
    transform 0.7s ease;
}

.fade-up.is-visible {
  opacity: 1;
  transform: translateY(0);
}
</style>
