<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import PageHeader from '../components/PageHeader.vue'
import { noteAPI, type Note } from '../api/note'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const auth = useAuthStore()
const notices = ref<Note[]>([])
const isLoading = ref(true)
const error = ref('')

const formatDate = (value?: string) => {
  if (!value) {
    return ''
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date)
}

const loadNotices = async () => {
  isLoading.value = true
  error.value = ''

  try {
    const response = await noteAPI.getNoteList({
      page: 0,
      size: 50,
      sort: 'created_at',
      is_asc: false,
    })

    notices.value = response.results
  } catch (fetchError) {
    console.error('failed to load notices:', fetchError)
    error.value = '공지사항을 불러오지 못했습니다.'
  } finally {
    isLoading.value = false
  }
}

onMounted(async () => {
  await auth.restoreAuth(route.path)
  await loadNotices()
})
</script>

<template>
  <div class="min-h-screen bg-[#FFF2EF]">
    <PageHeader
      title="공지사항"
      subtitle="EduQuest의 최신 소식과 업데이트 내용을 확인해 보세요."
      back-link="/"
    />

    <main class="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <section
        v-if="error"
        class="luxe-card p-6 text-sm font-bold text-[#B24A5A]"
      >
        {{ error }}
      </section>

      <section
        v-else-if="isLoading"
        class="luxe-panel p-10 text-center"
      >
        <div class="mx-auto mb-4 h-14 w-14 animate-spin rounded-full border-2 border-[#FFDBB6] border-b-[#1A2A4F]" />
        <p class="font-bold text-[#1A2A4F]">공지사항을 불러오는 중입니다.</p>
      </section>

      <section v-else-if="notices.length === 0" class="luxe-panel p-10 text-center">
        <p class="text-2xl font-black text-[#1A2A4F]">등록된 공지사항이 없습니다.</p>
        <p class="mt-3 text-sm leading-6 text-slate-600">
          새로운 공지가 등록되면 이곳에서 확인할 수 있습니다.
        </p>
      </section>

      <section v-else class="space-y-5">
        <article
          v-for="notice in notices"
          :key="notice.uuid ?? notice.title"
          class="luxe-card p-6"
        >
          <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <p class="text-xs font-black uppercase tracking-[0.18em] text-[#1A2A4F]/45">
                Notice
              </p>
              <h2 class="mt-2 text-2xl font-black text-[#1A2A4F]">
                {{ notice.title }}
              </h2>
            </div>
            <span class="luxe-pill shrink-0 px-4 py-2 text-xs font-bold text-[#1A2A4F]">
              {{ formatDate(notice.created_at ?? notice.createdAt) }}
            </span>
          </div>

          <p class="mt-5 whitespace-pre-line text-sm font-medium leading-7 text-[#1A2A4F]/70">
            {{ notice.content }}
          </p>
        </article>
      </section>
    </main>
  </div>
</template>
