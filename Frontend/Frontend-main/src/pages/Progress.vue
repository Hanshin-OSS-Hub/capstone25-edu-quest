<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '../components/PageHeader.vue'
import { progressAPI } from '../api/learning'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const stages = ref<{ stage: string | number; total_question_count?: number; totalQuestionCount?: number; clear: number[] }[]>([])
const error = ref('')

const getStageTotalCount = (stage: { total_question_count?: number; totalQuestionCount?: number }) =>
  stage.totalQuestionCount ?? stage.total_question_count ?? 0

const normalizedStages = computed(() =>
  stages.value.map((stage) => ({
    uuid: String(stage.stage),
    number: Number(stage.stage),
    title: `Stage ${stage.stage}`,
    reward: '-',
    clearCount: stage.clear.length,
    totalCount: getStageTotalCount(stage),
    isCleared: stage.clear.length === getStageTotalCount(stage) && getStageTotalCount(stage) > 0,
  }))
)

onMounted(async () => {
  await auth.restoreAuth(route.path)
  if (!auth.state.user) {
    error.value = '로그인이 필요한 기능입니다.'
    return
  }

  try {
    const response = await progressAPI.getProgress(auth.state.user.uuid)
    stages.value = response.results
  } catch {
    error.value = '학습 현황을 불러오지 못했습니다.'
  }
})
</script>

<template>
  <div class="min-h-screen bg-[#FFF2EF]">
    <PageHeader title="학습 현황" subtitle="어디까지 왔는지 보고, 다음 스테이지도 이어서 도전해 보세요." back-link="/" />

    <main class="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      <div v-if="error" class="rounded-[28px] border border-[#F7A5A5] bg-white p-6 text-[#1A2A4F]">
        {{ error }}
      </div>

      <div v-else class="space-y-5">
        <div class="luxe-panel p-6">
          <h2 class="text-2xl font-black text-[#1A2A4F]">한눈에 보는 내 기록</h2>
          <p class="mt-2 text-sm leading-6 text-slate-600">스테이지마다 얼마나 풀었는지 확인하고, 원하는 곳부터 다시 시작할 수 있어요.</p>
        </div>

        <div
          v-for="stage in normalizedStages"
          :key="stage.uuid"
          class="luxe-card p-6"
          :class="stage.isCleared ? 'border-[#FFDBB6]' : 'border-[#F7A5A5]'"
        >
          <div class="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
            <div class="flex items-center gap-5">
              <div class="flex h-16 w-16 items-center justify-center rounded-2xl text-2xl font-semibold" :class="stage.isCleared ? 'bg-[#FFF6EC] text-[#1A2A4F]' : 'bg-[#FFF1EF] text-[#1A2A4F]'">
                {{ stage.number }}
              </div>
              <div>
                <p class="text-2xl font-black text-[#1A2A4F]">{{ stage.title }}</p>
                <p class="mt-1 text-sm text-slate-500">보상: {{ stage.reward }}</p>
              </div>
            </div>

            <div class="flex flex-col gap-4 md:min-w-[260px]">
              <div>
                <div class="mb-2 flex justify-between text-sm font-bold text-[#1A2A4F]">
                  <span>진행률</span>
                  <span>{{ stage.clearCount }} / {{ stage.totalCount }}</span>
                </div>
                <div class="h-3 rounded-full bg-[#F3E7E1]">
                  <div
                    class="h-3 rounded-full bg-[linear-gradient(90deg,#ffcfaa,#f3ab9b)]"
                    :style="{ width: `${stage.totalCount ? (stage.clearCount / stage.totalCount) * 100 : 0}%` }"
                  />
                </div>
              </div>
              <button
                type="button"
                class="luxe-button-accent rounded-full px-6 py-3 font-medium transition hover:translate-y-[-1px]"
                @click="router.push(`/game?stage=${stage.number}`)"
              >
                {{ stage.isCleared ? '다시 도전하기' : '이어서 풀기' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>
