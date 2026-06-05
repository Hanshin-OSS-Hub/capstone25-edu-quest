<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { userAPI } from '../api/auth'
import { bookmarkAPI } from '../api/bookmark'
import { hintAPI, problemAPI, submissionAPI, type HintResponse, type ProblemDetail } from '../api/learning'
import { useAuthStore } from '../store/auth'

const props = defineProps<{
  problemId?: string | null
  successRedirect?: string | null
}>()

const router = useRouter()
const auth = useAuthStore()
const problem = ref<ProblemDetail | null>(null)
const code = ref('# Python 코드를 작성하세요.\nprint("Hello, World!")')
const blockAnswer = ref<number[]>([])
const hintMessage = ref('')
const usedHintLevels = ref<Set<number>>(new Set())
const submissionResult = ref<{ success: boolean; message: string } | null>(null)
const isLoading = ref(false)
const isBookmarked = ref(false)
const isBookmarkLoading = ref(false)
const bookmarkMessage = ref('')

const parsedBlock = computed(() => {
  const rawBlock = problem.value?.block
  if (!rawBlock) {
    return null
  }

  if (typeof rawBlock === 'string') {
    try {
      return JSON.parse(rawBlock) as { answer?: number[]; blocks?: Array<{ order?: number; code: string }> }
    } catch {
      return null
    }
  }

  return rawBlock
})

const hasBlocks = computed(() => Boolean(parsedBlock.value?.blocks?.length))
const currentProblemUuid = computed(() => problem.value?.uuid ?? '')
const availableHints = computed<HintResponse[]>(() =>
  [...(problem.value?.hints ?? [])].sort((left, right) => left.level - right.level),
)

const decodeHtmlText = (value?: string) => {
  if (!value) return ''

  let decoded = value

  for (let i = 0; i < 3; i += 1) {
    decoded = decoded
      .replaceAll('&amp;', '&')
      .replaceAll('&quot;', '"')
      .replaceAll('&#34;', '"')
      .replaceAll('&#39;', "'")
      .replaceAll('&lt;', '<')
      .replaceAll('&gt;', '>')
      .replaceAll('&#96;', '`')
      .replace(/&#(x?[0-9a-fA-F]+);/g, (_, rawCode: string) => {
        const isHex = rawCode.startsWith('x') || rawCode.startsWith('X')
        const codePoint = Number.parseInt(isHex ? rawCode.slice(1) : rawCode, isHex ? 16 : 10)
        return Number.isFinite(codePoint) ? String.fromCodePoint(codePoint) : `&#${rawCode};`
      })
  }

  return decoded
}

const syncBookmarkState = async () => {
  bookmarkMessage.value = ''
  isBookmarked.value = false

  if (!problem.value) {
    return
  }

  await auth.restoreAuth('/game')

  const userUuid = auth.state.user?.uuid
  if (!userUuid) {
    return
  }

  try {
    const response = await bookmarkAPI.getBookmarkList(userUuid, {
      page: 0,
      size: 100,
      sort: 'created_at',
      is_asc: false,
    })

    const targetProblemUuid = currentProblemUuid.value
    isBookmarked.value = response.results.some((item) => {
      const problemUuid = item.problem_uuid ?? item.problemUuid
      return problemUuid === targetProblemUuid
    })
  } catch (error) {
    console.warn('failed to sync bookmark state:', error)
  }
}

const refreshCurrentUserCoin = async () => {
  const user = auth.state.user
  const userUuid = user?.uuid
  if (!userUuid) {
    return
  }

  try {
    const profile = await userAPI.getProfile(userUuid)
    user.balance = profile.wallet?.balance ?? profile.point ?? user.balance
  } catch (error) {
    console.warn('failed to refresh user coin after hint:', error)
  }
}

const wait = (ms: number) => new Promise((resolve) => window.setTimeout(resolve, ms))

const pollEvaluationResult = async (submissionUuid: string) => {
  const maxAttempts = 20

  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    const evaluation = await submissionAPI.getEvaluation(submissionUuid)

    if (evaluation.status === 'completed') {
      return Boolean(evaluation.result)
    }

    await wait(700)
  }

  throw new Error('채점 결과를 가져오지 못했습니다.')
}

const fetchProblem = async () => {
  if (!props.problemId) {
    return
  }

  try {
    problem.value = await problemAPI.getProblem(props.problemId)
    blockAnswer.value = []
    hintMessage.value = ''
    usedHintLevels.value = new Set()
    submissionResult.value = null
    bookmarkMessage.value = ''
    await syncBookmarkState()
  } catch (error) {
    console.error('failed to fetch problem:', error)
    alert('문제를 불러오지 못했습니다.')
  }
}

const handleSubmit = async () => {
  if (!problem.value) {
    return
  }

  const answer = hasBlocks.value ? buildOrderingAnswer() : code.value
  if (!answer.trim()) {
    return
  }

  isLoading.value = true
  submissionResult.value = null

  try {
    const response = await submissionAPI.submitProblem(problem.value.uuid, answer)
    const isCorrect = await pollEvaluationResult(response.uuid)

    submissionResult.value = {
      success: isCorrect,
      message: isCorrect ? '정답입니다.' : '오답입니다. 다시 도전해 보세요.',
    }

    if (isCorrect) {
      setTimeout(() => {
        router.push(props.successRedirect || '/')
      }, 2000)
    }
  } catch (error: any) {
    submissionResult.value = {
      success: false,
      message: error.response?.data?.details ?? error.response?.data?.message ?? '제출에 실패했습니다.',
    }
  } finally {
    isLoading.value = false
  }
}

const handleHint = async (level: number) => {
  if (!problem.value) {
    return
  }

  try {
    const hintMeta = availableHints.value.find((hint) => hint.level === level)
    const wasAlreadyUsedInThisView = usedHintLevels.value.has(level)
    const response = await hintAPI.getHint(problem.value.uuid, level)
    const nextUsedLevels = new Set(usedHintLevels.value)
    nextUsedLevels.add(level)
    usedHintLevels.value = nextUsedLevels

    const cost = response.point ?? hintMeta?.point ?? 0
    const prefix = wasAlreadyUsedInThisView
      ? '이미 확인한 힌트입니다.'
      : `힌트 사용 완료: ${cost}코인이 차감되었습니다.`
    hintMessage.value = `${prefix}\n${response.content ?? ''}`.trim()
    await refreshCurrentUserCoin()

    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('eduquest:coin-updated'))
    }
  } catch (error: any) {
    hintMessage.value = resolveHintErrorMessage(error)
  }
}

const resolveHintErrorMessage = (error: any) => {
  const data = error?.response?.data
  const details = data?.details
  const detailCode = typeof details === 'object' && details !== null ? details.code : ''
  const serverMessage = data?.message ?? data?.error ?? (typeof details === 'string' ? details : '')

  if (
    data?.code === 'INSUFFICIENT_BALANCE' ||
    detailCode === 'INSUFFICIENT_BALANCE' ||
    String(serverMessage).includes('코인이 부족')
  ) {
    return '코인이 부족합니다.'
  }

  if (
    data?.code === 'HINT_NOT_FOUND' ||
    detailCode === 'HINT_NOT_FOUND' ||
    String(serverMessage).includes('해당 단계의 힌트가 없습니다') ||
    String(serverMessage).includes('힌트')
  ) {
    return String(serverMessage).includes('해당 단계의 힌트가 없습니다')
      ? '해당 단계의 힌트가 없습니다.'
      : serverMessage || '해당 단계의 힌트가 없습니다.'
  }

  return serverMessage || '힌트를 불러오지 못했습니다.'
}

const appendBlock = (index: number) => {
  blockAnswer.value = [...blockAnswer.value, index]
}

const buildOrderingAnswer = () => {
  const blocks = parsedBlock.value?.blocks ?? []
  const codeByOrder = new Map<number, string>()

  blocks.forEach((block, index) => {
    codeByOrder.set(block.order ?? index + 1, decodeHtmlText(block.code).trim())
  })

  return blockAnswer.value
    .map((order) => codeByOrder.get(order) ?? '')
    .filter((blockCode) => blockCode.length > 0)
    .join('\n')
}

const toggleBookmark = async () => {
  if (!problem.value || isBookmarkLoading.value) {
    return
  }

  isBookmarkLoading.value = true
  bookmarkMessage.value = ''

  try {
    if (isBookmarked.value) {
      await bookmarkAPI.deleteBookmark(problem.value.uuid)
      isBookmarked.value = false
      bookmarkMessage.value = '북마크를 해제했습니다.'
    } else {
      await bookmarkAPI.createBookmark(problem.value.uuid)
      isBookmarked.value = true
      bookmarkMessage.value = '북마크에 저장했습니다.'
    }
  } catch (error: any) {
    const serverMessage =
      error?.response?.data?.message ??
      error?.response?.data?.details ??
      error?.response?.data?.error ??
      ''

    if (!isBookmarked.value && String(serverMessage).includes('이미')) {
      isBookmarked.value = true
      bookmarkMessage.value = '이미 북마크에 저장된 문제입니다.'
    } else if (
      isBookmarked.value &&
      (error?.response?.status === 404 || String(serverMessage).includes('없'))
    ) {
      isBookmarked.value = false
      bookmarkMessage.value = '이미 해제된 북마크입니다.'
    } else {
      bookmarkMessage.value = serverMessage || '북마크 처리에 실패했습니다.'
    }

    console.error('bookmark toggle failed:', error)
  } finally {
    isBookmarkLoading.value = false
  }
}

watch(() => props.problemId, fetchProblem, { immediate: true })
</script>

<template>
  <div
    v-if="!problem"
    class="rounded-2xl border-4 border-dashed border-gray-600 bg-gray-800 p-10 text-center text-white"
  >
    <h2 class="mb-4 text-3xl font-bold">문제 로딩 중...</h2>
    <div class="mx-auto h-12 w-12 animate-spin rounded-full border-b-2 border-blue-400" />
  </div>

  <div
    v-else
    class="flex max-h-[90vh] flex-col overflow-hidden rounded-2xl border-4 border-gray-600 bg-gray-800 p-8 text-white"
  >
    <div class="mb-6 shrink-0">
      <div class="mb-2 flex items-center justify-between gap-4">
        <h2 class="text-2xl font-bold text-blue-400">문제 {{ problem.number }}</h2>

        <button
          type="button"
          :disabled="isBookmarkLoading"
          class="rounded-lg border border-yellow-300 px-3 py-2 text-sm font-bold text-yellow-200 transition hover:bg-yellow-300 hover:text-gray-900 disabled:cursor-not-allowed disabled:opacity-50"
          @click="toggleBookmark"
        >
          {{ isBookmarked ? '★ 북마크 해제' : '☆ 북마크' }}
        </button>
      </div>
      <p v-if="bookmarkMessage" class="mb-3 text-sm font-bold text-yellow-200">
        {{ bookmarkMessage }}
      </p>
      <p class="mb-4 whitespace-pre-line text-gray-300">{{ problem.summary }}</p>

      <div v-if="problem.example" class="mb-4 rounded-lg bg-gray-700 p-4">
        <h3 class="mb-2 font-bold text-green-400">예제 입력</h3>
        <pre class="text-sm text-gray-200">{{ problem.example }}</pre>
      </div>

      <div v-if="problem.expectedOutput" class="rounded-lg bg-gray-700 p-4">
        <h3 class="mb-2 font-bold text-green-400">예제 출력</h3>
        <pre class="text-sm text-gray-200">{{ problem.expectedOutput }}</pre>
      </div>
    </div>

    <div class="flex min-h-0 flex-1 flex-col">
      <div v-if="hasBlocks" class="mb-4 rounded-xl bg-gray-900 p-4">
        <p class="mb-3 text-sm text-gray-300">
          블록 문제인 경우 버튼을 눌러 정답 순서를 만들어 보세요.
        </p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="(block, index) in parsedBlock?.blocks"
            :key="`${index}-${block.code}`"
            type="button"
            class="rounded-lg bg-gray-700 px-3 py-2 text-sm hover:bg-gray-600"
            @click="appendBlock(block.order ?? index + 1)"
          >
            {{ block.order ?? index + 1 }}. {{ decodeHtmlText(block.code) }}
          </button>
        </div>
        <p class="mt-3 text-sm text-blue-300">선택 순서: {{ blockAnswer.join(', ') || '아직 없음' }}</p>
      </div>

      <textarea
        v-model="code"
        class="w-full flex-1 resize-none rounded-lg bg-gray-900 p-4 font-mono text-sm text-green-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
        placeholder="Python 코드를 작성하세요."
      />

      <div v-if="availableHints.length" class="mt-4 flex flex-wrap gap-2">
        <button
          v-for="hint in availableHints"
          :key="hint.level"
          type="button"
          class="rounded-lg border border-blue-400 px-3 py-2 text-sm text-blue-300 hover:bg-blue-400 hover:text-white"
          @click="handleHint(hint.level)"
        >
          힌트 {{ hint.level }} (-{{ hint.point }} 코인)
        </button>
      </div>

      <div v-if="hintMessage" class="mt-4 whitespace-pre-line rounded-lg bg-blue-950 p-4 text-sm text-blue-100">
        {{ hintMessage }}
      </div>

      <div
        v-if="submissionResult"
        class="mt-4 rounded-lg p-4"
        :class="submissionResult.success ? 'bg-green-800' : 'bg-red-800'"
      >
        <h3 class="font-bold">{{ submissionResult.success ? '제출 성공' : '제출 실패' }}</h3>
        <p class="mt-2 text-sm">{{ submissionResult.message }}</p>
      </div>

      <div class="mt-4 flex gap-4">
        <button
          type="button"
          disabled
          class="flex-1 cursor-not-allowed rounded-lg bg-blue-400 px-6 py-3 font-bold text-white"
        >
          코드 실행 (미지원)
        </button>
        <button
          type="button"
          :disabled="isLoading"
          class="flex-1 rounded-lg bg-green-600 px-6 py-3 font-bold text-white transition-colors hover:bg-green-500 disabled:bg-gray-600"
          @click="handleSubmit"
        >
          {{ isLoading ? '제출 중...' : '정답 제출' }}
        </button>
      </div>
    </div>
  </div>
</template>
