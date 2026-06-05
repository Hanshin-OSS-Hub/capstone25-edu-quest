<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '../components/PageHeader.vue'
import { problemAPI, type ProblemDetail } from '../api/learning'
import { useAuthStore } from '../store/auth'

interface ReviewProblem {
  uuid: string
  title: string
  summary: string
  stageTitle: string
  stageNumber?: number
  number: number
  type: string
}

const router = useRouter()
const auth = useAuthStore()

const reviewProblems = ref<ReviewProblem[]>([])
const isLoading = ref(false)
const error = ref('')

const sortedReviewProblems = computed(() =>
  [...reviewProblems.value].sort((left, right) => {
    const stageCompare = (left.stageNumber ?? 0) - (right.stageNumber ?? 0)
    return stageCompare !== 0 ? stageCompare : left.number - right.number
  }),
)

const getProblemTitle = (problem: ProblemDetail) => {
  const summary = problem.summary?.trim()
  return summary || `문제 ${problem.number}`
}

const getProblemDescription = (problem: ProblemDetail) => {
  const typeLabel = problem.type === 'ordering' ? '순서 맞추기' : problem.type === 'final' ? '최종 문제' : '코드 작성'
  return `${typeLabel} 문제를 다시 풀며 정답으로 통과했던 풀이 흐름을 복습해 보세요.`
}

const loadReviewProblems = async () => {
  if (!auth.state.user) {
    error.value = '로그인이 필요한 기능입니다.'
    return
  }

  isLoading.value = true
  error.value = ''

  try {
    const response = await problemAPI.getReviewProblems(auth.state.user.uuid)

    reviewProblems.value = response.results
      .map((problem) => ({
        uuid: problem.uuid,
        title: getProblemTitle(problem),
        summary: getProblemDescription(problem),
        stageTitle: String(problem.stageTitle ?? problem.stage ?? '스테이지'),
        stageNumber: problem.stageNumber,
        number: problem.number,
        type: problem.type,
      }))
  } catch (loadError) {
    console.error('failed to load review problems:', loadError)
    error.value = '복습 문제를 불러오지 못했습니다.'
  } finally {
    isLoading.value = false
  }
}

const startReview = (problem: ReviewProblem) => {
  router.push(`/game?problem=${problem.uuid}&from=review`)
}

onMounted(async () => {
  await auth.restoreAuth('/review')
  await loadReviewProblems()
})
</script>

<template>
  <div class="min-h-screen bg-[#FFF2EF]">
    <PageHeader
      title="복습하기"
      subtitle="지금까지 정답으로 통과한 문제를 다시 풀어보며 학습 흐름을 다져 보세요."
      back-link="/home"
    />

    <main class="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      <section
        v-if="isLoading"
        class="luxe-card p-10 text-center"
      >
        <div class="mx-auto mb-4 h-12 w-12 animate-spin rounded-full border-2 border-[#FFDBB6] border-b-[#1A2A4F]" />
        <p class="font-medium text-[#1A2A4F]">복습 문제를 불러오는 중입니다.</p>
      </section>

      <section
        v-else-if="error"
        class="luxe-card p-8 text-center text-sm font-medium text-[#1A2A4F]"
      >
        {{ error }}
      </section>

      <section v-else-if="sortedReviewProblems.length" class="grid gap-4">
        <article
          v-for="(problem, index) in sortedReviewProblems"
          :key="problem.uuid"
          class="luxe-card flex flex-col gap-5 p-6 sm:flex-row sm:items-center sm:justify-between"
        >
          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-3">
              <p class="rounded-full border border-[#1A2A4F]/10 bg-[#FFF8F4] px-3 py-1 text-xs font-medium uppercase tracking-[0.14em] text-[#1A2A4F]">
                Review {{ index + 1 }}
              </p>
              <p class="text-sm font-medium text-[#1A2A4F]/45">
                {{ problem.stageTitle }} · 문제 {{ problem.number }}
              </p>
            </div>
            <h2 class="mt-4 text-2xl font-black text-[#1A2A4F]">{{ problem.title }}</h2>
            <p class="mt-3 max-w-2xl text-sm leading-6 text-[#1A2A4F]/68">
              {{ problem.summary }}
            </p>
          </div>

          <button
            type="button"
            class="luxe-button-accent cursor-pointer shrink-0 rounded-full px-5 py-3 text-sm font-medium transition duration-300 hover:translate-y-[-1px]"
            @click="startReview(problem)"
          >
            복습 시작
          </button>
        </article>
      </section>

      <section
        v-else
        class="luxe-card p-10 text-center"
      >
        <p class="text-lg font-black text-[#1A2A4F]">아직 복습할 문제가 없습니다.</p>
        <p class="mt-3 text-sm font-medium text-[#1A2A4F]/60">
          Unity나 문제 풀이 화면에서 문제를 정답으로 맞추면 이곳에 복습 문제가 쌓입니다.
        </p>
        <button
          type="button"
          class="luxe-button-accent mt-6 cursor-pointer rounded-full px-5 py-3 text-sm font-medium"
          @click="router.push('/game')"
        >
          게임 시작
        </button>
      </section>
    </main>
  </div>
</template>
