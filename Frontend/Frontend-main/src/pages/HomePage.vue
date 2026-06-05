<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import type { ComponentPublicInstance } from 'vue'
import Navbar from '../components/Navbar.vue'
import { userAPI } from '../api/auth'
import { bookmarkAPI, type BookmarkItem } from '../api/bookmark'
import { progressAPI } from '../api/learning'
import { useAuthStore } from '../store/auth'
import { wrongNoteAPI } from '../api/wrong_note'

const auth = useAuthStore()
const previewVideo = '/unity_preview.mp4'

interface UserProgress {
  level: number
  exp: number
  expMax: number
  clearedStages: number
  totalStages: number
}

interface BookmarkPreview {
  id: string
  stage: string
  title: string
  note: string
  difficulty: string
}

const getStageTotalCount = (stage: { total_question_count?: number; totalQuestionCount?: number }) =>
  stage.totalQuestionCount ?? stage.total_question_count ?? 0

const getProfilePoint = (profile: Awaited<ReturnType<typeof userAPI.getProfile>>) =>
  profile.wallet?.balance ?? profile.point ?? 0

const userProgress = ref<UserProgress | null>(null)
const scrollY = ref(0)
const animatedCoinRatio = ref(0)
const animatedAccuracy = ref(0)
const revealTargets = ref<HTMLElement[]>([])
const videoAvailable = ref(true)
const pageError = ref('')
const stats = ref({
  correct: 0,
  wrong: 0,
  coinCurrent: 0,
  coinGoal: 100,
})
const bookmarkedProblems = ref<BookmarkPreview[]>([])

let revealObserver: IntersectionObserver | null = null
let metricsAnimated = false

const greetingTemplates = [
  '{nickname}님, 오늘도 찾아와 줘서 반가워요!',
  '{nickname}님, 이어서 학습을 시작해 볼까요?',
  '{nickname}님, 오늘의 목표를 향해 천천히 나아가 봐요.',
  '{nickname}님, 다음 스테이지가 기다리고 있어요!',
  '{nickname}님, 다시 돌아와 줘서 정말 반가워요.',
]

const selectedGreetingTemplate = ref(
  greetingTemplates[Math.floor(Math.random() * greetingTemplates.length)],
)
const nickname = computed(() => auth.state.user?.nickname || '게스트')
const completedStages = computed(() => userProgress.value?.clearedStages ?? 0)
const greetingTitle = computed(
  () => `${selectedGreetingTemplate.value.replace('{nickname}', nickname.value)}\n오늘의 학습을 이어가요.`,
)
const heroBodyText = computed(
  () =>
    `지금까지 ${completedStages.value}개의 스테이지를 완료했어요.\n다음 스테이지에 도전하거나 복습 모드로 다시 실력을 다져 보세요.\n오답 노트와 북마크 문제도 EduQuest가 바로 이어서 도와드릴게요.`,
)
const accuracy = computed(() => {
  const total = stats.value.correct + stats.value.wrong
  return total === 0 ? 0 : Math.round((stats.value.correct / total) * 100)
})
const displayedCoinAmount = computed(() => Math.round(stats.value.coinCurrent * animatedCoinRatio.value))
const displayedAccuracy = computed(() => Math.round(accuracy.value * animatedAccuracy.value))
const progressPercent = computed(() => {
  if (!userProgress.value || userProgress.value.totalStages === 0) {
    return 0
  }

  return Math.round((userProgress.value.clearedStages / userProgress.value.totalStages) * 100)
})
const donutCircumference = 2 * Math.PI * 52
const donutOffset = computed(() => donutCircumference * (1 - animatedAccuracy.value * (accuracy.value / 100)))
const navStyle = computed(() => {
  const intensity = Math.min(scrollY.value / 360, 1)
  const blur = 8 + intensity * 16
  const backgroundOpacity = 0.84 + intensity * 0.12

  return {
    '--nav-blur': `${blur}px`,
    '--nav-bg': `rgba(255, 255, 255, ${backgroundOpacity})`,
    '--nav-border': `rgba(26, 42, 79, ${0.12 + intensity * 0.12})`,
  }
})

const handleScroll = () => {
  scrollY.value = window.scrollY
}

const handleVideoError = () => {
  videoAvailable.value = false
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const registerReveal = (element: Element | ComponentPublicInstance | null) => {
  if (element instanceof HTMLElement && !revealTargets.value.includes(element)) {
    revealTargets.value.push(element)
  }
}

const animateValue = (setter: (value: number) => void, duration: number) => {
  const start = performance.now()

  const tick = (now: number) => {
    const progress = Math.min((now - start) / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    setter(eased)

    if (progress < 1) {
      window.requestAnimationFrame(tick)
    }
  }

  window.requestAnimationFrame(tick)
}

const startMetricAnimations = () => {
  if (metricsAnimated) {
    return
  }

  metricsAnimated = true
  animateValue((value) => {
    animatedCoinRatio.value = value
  }, 1200)
  animateValue((value) => {
    animatedAccuracy.value = value
  }, 1400)
}

const mapBookmarkPreview = (bookmark: BookmarkItem): BookmarkPreview => ({
  id: bookmark.problem_uuid ?? `${bookmark.stage}-${bookmark.number}`,
  stage: bookmark.stage ?? 'Stage',
  title: bookmark.summary?.trim() || bookmark.title?.trim() || `문제 ${bookmark.number ?? '-'}`,
  note: `${bookmark.stage ?? '선택한 스테이지'}에서 다시 확인하고 싶은 문제예요.`,
  difficulty: bookmark.type ?? 'basic',
})

const loadHomeData = async () => {
  pageError.value = ''
  await auth.restoreAuth('/home')

  if (!auth.state.user) {
    throw new Error('auth required')
  }

  const [profileResult, progressResult, bookmarkResult, wrongNotesResult] = await Promise.allSettled([
    userAPI.getProfile(auth.state.user.uuid),
    progressAPI.getProgress(auth.state.user.uuid),
    bookmarkAPI.getBookmarkList(auth.state.user.uuid, {
      page: 0,
      size: 3,
      sort: 'created_at',
      is_asc: false,
    }),
    wrongNoteAPI.getUserWrongNotes(auth.state.user.uuid, {
      page: 0,
      size: 100,
      sort: 'created_at',
      is_asc: false,
    }),
  ])

  const profile = profileResult.status === 'fulfilled' ? profileResult.value : null
  const progressResponse = progressResult.status === 'fulfilled' ? progressResult.value : { results: [] }
  const bookmarkResponse =
    bookmarkResult.status === 'fulfilled' ? bookmarkResult.value : { results: [] as BookmarkItem[] }
  const wrongNotesResponse = wrongNotesResult.status === 'fulfilled' ? wrongNotesResult.value : { results: [] }

  const totalStages = progressResponse.results.length
  const clearedStages = progressResponse.results.filter(
    (stage) => getStageTotalCount(stage) > 0 && stage.clear.length === getStageTotalCount(stage),
  ).length
  const solvedCount = progressResponse.results.reduce((sum, stage) => sum + stage.clear.length, 0)
  const wrongCount = wrongNotesResponse.results.length

  userProgress.value = {
    level: clearedStages + 1,
    exp: profile ? getProfilePoint(profile) : 0,
    expMax: Math.max((clearedStages + 1) * 100, 100),
    clearedStages,
    totalStages,
  }

  stats.value.coinCurrent = profile ? getProfilePoint(profile) : 0
  stats.value.coinGoal = userProgress.value.expMax
  stats.value.correct = solvedCount
  stats.value.wrong = wrongCount
  bookmarkedProblems.value = bookmarkResponse.results.map(mapBookmarkPreview)

  if (
    profileResult.status !== 'fulfilled' ||
    progressResult.status !== 'fulfilled' ||
    bookmarkResult.status !== 'fulfilled' ||
    wrongNotesResult.status !== 'fulfilled'
  ) {
    console.warn('home data partial failure:', {
      profileResult,
      progressResult,
      bookmarkResult,
      wrongNotesResult,
    })
    pageError.value = '일부 학습 정보를 불러오지 못해 기본 데이터로 표시하고 있어요.'
  }
}

onMounted(async () => {
  handleScroll()
  window.addEventListener('scroll', handleScroll, { passive: true })

  try {
    await loadHomeData()
  } catch (error) {
    console.error('failed to load home data:', error)
    pageError.value = '홈 화면 데이터를 불러오지 못해 기본 정보로 표시하고 있어요.'
    userProgress.value = {
      level: 1,
      exp: 0,
      expMax: 100,
      clearedStages: 0,
      totalStages: 0,
    }
  }

  await nextTick()

  revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible')

          if (entry.target instanceof HTMLElement && entry.target.dataset.animateMetrics === 'true') {
            startMetricAnimations()
          }

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
  <div class="home-shell min-h-screen bg-[#FFF2EF]" :style="navStyle">
    <Navbar />

    <main class="relative px-4 pb-10 pt-6 sm:px-6 lg:px-8">
      <div class="absolute left-0 top-10 h-40 w-40 rounded-full bg-[#F7A5A5]/20 blur-3xl" />
      <div class="absolute right-10 top-24 h-48 w-48 rounded-full bg-[#FFDBB6]/30 blur-3xl" />

      <div class="relative mx-auto flex max-w-7xl flex-col gap-6">
        <div
          v-if="pageError"
          class="rounded-[24px] border border-[#1A2A4F]/10 bg-white/92 px-5 py-4 text-sm font-medium text-[#1A2A4F]/75 shadow-[0_12px_30px_rgba(15,23,42,0.05)]"
        >
          {{ pageError }}
        </div>

        <section class="grid gap-6 lg:grid-cols-[1.15fr_0.85fr] lg:items-stretch">
          <div
            :ref="registerReveal"
            class="fade-up relative isolate flex h-full flex-col overflow-hidden rounded-[36px] border border-[#1A2A4F]/10 bg-[#1A2A4F] p-6 text-white shadow-[0_24px_60px_rgba(15,23,42,0.14)] sm:p-8"
          >
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
            <div class="absolute inset-0 bg-[#1A2A4F]/62" />
            <div
              v-if="!videoAvailable"
              class="absolute inset-0 bg-[linear-gradient(135deg,rgba(26,42,79,0.92),rgba(35,56,104,0.78)),repeating-linear-gradient(90deg,rgba(255,255,255,0.05)_0,rgba(255,255,255,0.05)_2px,transparent_2px,transparent_24px)]"
            />

            <div class="relative z-10 flex flex-col gap-4">
              <p class="w-fit rounded-full border border-white/20 bg-white/12 px-4 py-2 text-sm font-medium text-white">
                오늘의 학습 상태
              </p>
              <div class="space-y-3">
                <h1 class="whitespace-pre-line text-2xl font-black leading-tight text-[#FFDBB6] sm:text-3xl lg:text-4xl">
                  {{ greetingTitle }}
                </h1>
                <p class="max-w-2xl whitespace-pre-line text-sm leading-7 text-gray-200 sm:text-base sm:leading-8">
                  {{ heroBodyText }}
                </p>
              </div>
            </div>

            <div class="relative z-10 mt-auto flex flex-col gap-4 pt-6 sm:flex-row sm:flex-wrap">
              <RouterLink
                to="/game"
                class="rounded-[22px] border border-[#FFDBB6]/80 bg-[#FFDBB6] px-6 py-4 text-center text-base font-bold text-[#1A2A4F] shadow-[0_14px_30px_rgba(0,0,0,0.18)] transition duration-300 hover:translate-y-[-1px] hover:bg-white"
              >
                게임 시작
              </RouterLink>
              <RouterLink
                to="/review"
                class="rounded-[22px] border border-white/18 bg-white/10 px-6 py-4 text-center text-base font-medium text-white transition duration-300 hover:bg-white/14"
              >
                복습하러 가기
              </RouterLink>
            </div>
          </div>

          <div
            :ref="registerReveal"
            data-animate-metrics="true"
            class="fade-up fade-delay-1 luxe-panel flex h-full flex-col gap-4 p-5 sm:p-6"
          >
            <p class="text-xs font-semibold uppercase tracking-[0.2em] text-[#1A2A4F]/45">
              Learning Dashboard
            </p>

            <div class="luxe-card-soft p-5">
              <div class="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p class="text-sm font-medium text-[#1A2A4F]/60">보유 코인</p>
                  <h2 class="mt-1 text-3xl font-black text-[#1A2A4F]">
                    {{ displayedCoinAmount }} 코인
                  </h2>
                </div>
                <span class="text-5xl leading-none">C</span>
              </div>
            </div>

            <div class="flex flex-1 flex-col justify-center rounded-[28px] border border-[#1A2A4F]/8 bg-[linear-gradient(180deg,#203257_0%,#1a2747_100%)] p-5 text-white shadow-[inset_0_1px_0_rgba(255,255,255,0.08)]">
              <div class="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
                <div class="flex justify-center">
                  <div class="relative h-[120px] w-[120px]">
                    <svg class="h-full w-full -rotate-90" viewBox="0 0 120 120">
                      <circle
                        cx="60"
                        cy="60"
                        r="52"
                        fill="none"
                        stroke="rgba(255, 255, 255, 0.14)"
                        stroke-width="14"
                      />
                      <circle
                        cx="60"
                        cy="60"
                        r="52"
                        fill="none"
                        stroke="#FFDBB6"
                        stroke-width="14"
                        stroke-linecap="round"
                        :stroke-dasharray="donutCircumference"
                        :stroke-dashoffset="donutOffset"
                        class="transition-[stroke-dashoffset] duration-[1400ms] ease-out"
                      />
                    </svg>
                    <div class="absolute inset-0 flex flex-col items-center justify-center">
                      <p class="text-2xl font-black text-[#FFDBB6]">{{ displayedAccuracy }}%</p>
                      <p class="mt-1 text-[10px] font-semibold uppercase tracking-[0.16em] text-white/60">
                        정답률
                      </p>
                    </div>
                  </div>
                </div>

                <div class="flex-1">
                  <div class="grid gap-3 sm:grid-cols-2">
                    <div class="rounded-[20px] border border-white/14 bg-white/8 p-4 text-center">
                      <p class="text-xs font-semibold uppercase tracking-[0.16em] text-white/50">해결한 문제</p>
                      <p class="mt-1 text-2xl font-black text-[#FFDBB6]">{{ stats.correct }}</p>
                    </div>
                    <div class="rounded-[20px] border border-white/14 bg-white/8 p-4 text-center">
                      <p class="text-xs font-semibold uppercase tracking-[0.16em] text-white/50">오답 노트</p>
                      <p class="mt-1 text-2xl font-black text-[#F7A5A5]">{{ stats.wrong }}</p>
                    </div>
                  </div>
                  <div class="mt-3 rounded-[20px] border border-white/14 bg-white/8 px-4 py-3 text-sm text-white/80">
                    전체 스테이지 진행률은 {{ progressPercent }}%예요.
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section
          :ref="registerReveal"
          class="fade-up fade-delay-2 luxe-panel p-6 sm:p-8"
        >
          <div class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.2em] text-[#1A2A4F]/45">
                Bookmark Review
              </p>
              <h2 class="mt-2 text-2xl font-black text-[#1A2A4F]">최근 저장한 문제</h2>
              <p class="mt-2 text-sm leading-6 text-[#1A2A4F]/70">
                북마크해 둔 문제를 빠르게 다시 보고, 헷갈리는 포인트를 이어서 복습해 보세요.
              </p>
            </div>
            <RouterLink
              to="/bookmark"
              class="luxe-button-accent w-full rounded-[20px] px-5 py-3 text-center text-sm font-medium transition duration-300 hover:translate-y-[-1px] sm:w-auto"
            >
              전체 북마크 보기
            </RouterLink>
          </div>

          <div
            v-if="bookmarkedProblems.length === 0"
            class="mt-6 rounded-[24px] border border-dashed border-[#1A2A4F]/14 bg-[#FFF8F4] p-6 text-sm font-medium text-[#1A2A4F]/65"
          >
            아직 저장한 문제가 없어요.
          </div>

          <div v-else class="mt-6 grid gap-4 lg:grid-cols-3">
            <article
              v-for="problem in bookmarkedProblems"
              :key="problem.id"
              class="luxe-card-soft flex h-full flex-col p-5"
            >
              <div class="flex flex-wrap items-center justify-between gap-3">
                <p class="rounded-full border border-[#1A2A4F]/10 bg-white px-3 py-1 text-xs font-semibold uppercase tracking-[0.14em] text-[#1A2A4F]">
                  {{ problem.stage }}
                </p>
                <span class="text-sm font-medium capitalize text-[#1A2A4F]/55">{{ problem.difficulty }}</span>
              </div>
              <h3 class="mt-4 text-xl font-black leading-snug text-[#1A2A4F]">
                {{ problem.title }}
              </h3>
              <p class="mt-3 flex-1 text-sm leading-6 text-[#1A2A4F]/70">
                {{ problem.note }}
              </p>
              <RouterLink
                to="/bookmark"
                class="luxe-button-accent mt-5 rounded-[18px] px-4 py-3 text-center text-sm font-medium transition duration-300 hover:translate-y-[-1px]"
              >
                다시 보기
              </RouterLink>
            </article>
          </div>
        </section>
      </div>
    </main>

    <footer class="border-t border-[#1A2A4F]/10 bg-[#FFF8F4] px-4 py-10 text-sm text-[#1A2A4F]/58 sm:px-6 lg:px-8">
      <div class="mx-auto max-w-7xl">
        <div class="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.22em] text-[#1A2A4F]/45">EduQuest</p>
          </div>
          <button
            type="button"
            class="luxe-button-soft h-11 w-11 shrink-0 cursor-pointer rounded-full text-xl font-semibold"
            aria-label="맨 위로 이동"
            @click="scrollToTop"
          >
            ↑
          </button>
        </div>

        <nav class="mt-7 flex flex-wrap gap-x-4 gap-y-3 font-medium text-[#1A2A4F]/72" aria-label="Footer navigation">
          <RouterLink to="/terms" class="transition hover:text-[#1A2A4F]">이용약관</RouterLink>
          <span class="text-[#1A2A4F]/20">|</span>
          <RouterLink to="/privacy" class="transition hover:text-[#1A2A4F]">
            개인정보처리방침
          </RouterLink><span class="text-[#1A2A4F]/20">|</span>
          <RouterLink to="/service-intro" class="transition hover:text-[#1A2A4F]">서비스 소개</RouterLink>
          <span class="text-[#1A2A4F]/20">|</span>
          <RouterLink to="/notice" class="transition hover:text-[#1A2A4F]">공지사항</RouterLink>
        </nav>

        <div class="mt-6 flex flex-col gap-2 border-t border-[#1A2A4F]/8 pt-5 text-xs font-medium text-[#1A2A4F]/45">
          <p>EduQuest 김진숙, 김원진, 김재원, 박현진</p>
          <p>&copy; 2026 EduQuest. All rights reserved.</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.home-shell {
  font-family:
    "Pretendard",
    -apple-system,
    BlinkMacSystemFont,
    system-ui,
    "Segoe UI",
    sans-serif;
}

.home-shell :deep(nav) {
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

.fade-delay-1 {
  transition-delay: 0.12s;
}

.fade-delay-2 {
  transition-delay: 0.22s;
}
</style>
