<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import PageHeader from '../components/PageHeader.vue'
import { progressAPI, stageAPI, type Stage } from '../api/learning'
import { useAuthStore } from '../store/auth'

interface StageProgress {
  stage: string | number
  clear: number[]
  total_question_count?: number
  totalQuestionCount?: number
}

const getProgressTotalCount = (progress: StageProgress) =>
  progress.totalQuestionCount ?? progress.total_question_count ?? 0

const route = useRoute()
const auth = useAuthStore()

const stages = ref<Stage[]>([])
const stageProgress = ref<Record<number, StageProgress>>({})
const isLoading = ref(true)

const loadStages = async () => {
  try {
    const response = await stageAPI.getStageList({ page: 0, size: 100, sort: 'number', is_asc: true })
    stages.value = response.results
  } catch (error) {
    console.error('스테이지 목록을 불러오지 못했습니다.', error)
  } finally {
    isLoading.value = false
  }
}

const loadProgress = async () => {
  if (!auth.state.user) {
    return
  }

  try {
    const response = await progressAPI.getProgress(auth.state.user.uuid)
    const progressMap: Record<number, StageProgress> = {}
    response.results.forEach((progress) => {
      const stageNumber = Number(progress.stage)
      progressMap[stageNumber] = progress
    })
    stageProgress.value = progressMap
  } catch (error) {
    console.error('진행 데이터를 불러오지 못했습니다.', error)
  }
}

const getStageProgress = (stageNumber: number) => {
  const progress = stageProgress.value[stageNumber]
  if (!progress) {
    return { completed: 0, total: 0 }
  }

  return {
    completed: progress.clear.length,
    total: getProgressTotalCount(progress),
  }
}

const getStageStatus = (stageNumber: number) => {
  const progress = getStageProgress(stageNumber)

  if (progress.total > 0 && progress.completed === progress.total) {
    return { label: '완료', tone: 'bg-[#FFDBB6]' }
  }

  if (progress.completed > 0) {
    return { label: '진행 중', tone: 'bg-[#F7A5A5]' }
  }

  return { label: '시작 전', tone: 'bg-[#FFF2EF]' }
}

onMounted(async () => {
  await auth.restoreAuth(route.path)
  await loadStages()
  await loadProgress()
})
</script>

<template>
  <div class="min-h-screen bg-[#FFF2EF]">
    <PageHeader
      title="스테이지"
      subtitle="전체 학습 커리큘럼을 한눈에 살펴보고 현재 진행 상태를 확인해 보세요."
      back-link="/"
    />

    <main class="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <div class="luxe-panel p-6">
        <h2 class="text-2xl font-black text-[#1A2A4F]">학습 로드맵</h2>
        <p class="mt-2 text-sm leading-6 text-slate-600">
          스테이지별 제목, 진행률, 보상 코인을 확인하면서 전체 커리큘럼 흐름을 둘러볼 수 있습니다.
        </p>
      </div>

      <div
        v-if="isLoading"
        class="luxe-panel mt-6 p-10 text-center"
      >
        <div class="mx-auto mb-4 h-14 w-14 animate-spin rounded-full border-2 border-[#FFDBB6] border-b-[#1A2A4F]" />
        <p class="font-bold text-[#1A2A4F]">스테이지를 불러오는 중입니다.</p>
      </div>

      <div v-else class="mt-6 grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
        <article
          v-for="stage in stages"
          :key="stage.uuid"
          class="luxe-card p-6"
        >
          <div class="flex items-start justify-between gap-4">
            <h3 class="text-2xl font-black text-[#1A2A4F]">{{ stage.title }}</h3>
            <span
              :class="getStageStatus(stage.number).tone"
              class="luxe-pill shrink-0 px-3 py-1 text-xs font-medium text-[#1A2A4F]"
            >
              {{ getStageStatus(stage.number).label }}
            </span>
          </div>

          <div class="mt-5 rounded-[20px] border border-[#1A2A4F]/10 bg-[#FFF8F4] p-4">
            <div class="flex items-center justify-between text-sm font-bold text-[#1A2A4F]">
              <span>진행률</span>
              <span>{{ getStageProgress(stage.number).completed }} / {{ getStageProgress(stage.number).total }}</span>
            </div>

            <div v-if="getStageProgress(stage.number).total > 0" class="mt-3 h-3 rounded-full bg-[#F3E7E1]">
              <div
                class="h-3 rounded-full bg-[linear-gradient(90deg,#ffcfaa,#f3ab9b)]"
                :style="{ width: `${(getStageProgress(stage.number).completed / getStageProgress(stage.number).total) * 100}%` }"
              />
            </div>

            <p v-else class="mt-3 text-xs font-bold text-slate-500">아직 집계된 진행 정보가 없습니다.</p>
          </div>

          <div class="mt-4 flex items-center justify-between rounded-[20px] border border-[#1A2A4F]/10 bg-[#FFF6EC] p-4">
            <span class="text-sm font-medium text-[#1A2A4F]">보상 코인</span>
            <span class="text-lg font-semibold text-[#1A2A4F]">{{ stage.reward ?? 0 }}</span>
          </div>
        </article>
      </div>
    </main>
  </div>
</template>
