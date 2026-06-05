<script setup lang="ts">
import { computed } from 'vue'
import FloatingNote from '../components/FloatingNote.vue'
import Navbar from '../components/Navbar.vue'
import PageHeader from '../components/PageHeader.vue'

const unitySrc = computed(() => '/unity/EduQuest/index.html')
</script>

<template>
  <div class="min-h-screen bg-[#FFF2EF]">
    <Navbar />
    <PageHeader
      title="게임 플레이"
      back-link="/home"
    />

    <main class="mx-auto max-w-[1600px] px-4 py-8 sm:px-6 lg:px-8">
      <div class="grid min-h-[78vh] gap-6 xl:grid-cols-[3fr_1fr]">
        <section class="min-h-0 overflow-hidden rounded-[32px] border-4 border-[#1A2A4F] bg-white shadow-[8px_8px_0_0_rgba(26,42,79,0.14)]">
          <iframe
            :src="unitySrc"
            title="EduQuest Unity WebGL"
            class="h-full min-h-[78vh] w-full bg-white"
            allowfullscreen
          />
        </section>

        <aside class="min-h-[78vh]">
          <FloatingNote mode="panel" />
        </aside>
      </div>
    </main>
  </div>
</template>
