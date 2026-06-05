<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Navbar from '../components/Navbar.vue'
import PageHeader from '../components/PageHeader.vue'
import { noteAPI, type Note } from '../api/note'
import { userAPI, type UserListItem } from '../api/auth'
import { problemAPI, stageAPI, type ProblemDetail, type Stage } from '../api/learning'
import { useAuthStore } from '../store/auth'

type AdminTab = 'dashboard' | 'users' | 'stages' | 'problems' | 'notices'

interface RoleOption {
  uuid: string
  name: string
}

interface AdminUserRow extends UserListItem {
  role?: string
  role_uuid?: string
  is_locked?: boolean
  created_at?: string
  joined_at?: string
}

interface HintForm {
  level: number
  point: number
  content: string
}

const problemTypeOptions = [
  { label: '직접 입력 문제', value: 'typing' },
  { label: '순서 배열 문제', value: 'ordering' },
  { label: '최종 코드 문제', value: 'final' },
] as const

type ProblemType = (typeof problemTypeOptions)[number]['value']

const defaultHintForms = (): HintForm[] => [
  { level: 1, point: 10, content: '' },
  { level: 2, point: 20, content: '' },
  { level: 3, point: 30, content: '' },
]

const defaultBlockAnswer = '1,2,3,4'
const defaultBlockItems = `1: print(
2: "Hello"
3: )
4: `

const router = useRouter()
const auth = useAuthStore()

const activeTab = ref<AdminTab>('dashboard')
const isBooting = ref(true)
const pageError = ref('')
const globalMessage = ref('')
const failedSections = ref<string[]>([])

const users = ref<AdminUserRow[]>([])
const roles = ref<RoleOption[]>([])
const stages = ref<Stage[]>([])
const problems = ref<ProblemDetail[]>([])
const notices = ref<Note[]>([])

const userSearch = ref('')
const problemSearch = ref('')
const noticeSearch = ref('')

const editingStageUuid = ref('')
const stageTitle = ref('')
const stageNumber = ref<number | null>(null)
const stageReward = ref<number | null>(null)
const isStageLoading = ref(false)
const stageMessage = ref('')
const stageError = ref('')

const problemStageUuid = ref('')
const problemType = ref<ProblemType>('typing')
const problemNumber = ref<number | null>(null)
const problemSummary = ref('')
const problemExample = ref('')
const problemExpectedOutput = ref('')
const editingProblemUuid = ref('')
const blockAnswerInput = ref(defaultBlockAnswer)
const blockItemsInput = ref(defaultBlockItems)
const hintForms = ref<HintForm[]>(defaultHintForms())
const isProblemLoading = ref(false)
const problemMessage = ref('')
const problemError = ref('')

const editingNoticeUuid = ref('')
const noticeTitle = ref('')
const noticeContent = ref('')
const noticeMessage = ref('')
const noticeError = ref('')
const isNoticeLoading = ref(false)

const isAdminRole = (role?: string | null) => {
  const normalized = role?.trim().toLowerCase()
  return normalized === 'admin' || normalized === 'admine' || normalized === 'role_admin'
}

const sidebarTabs: Array<{ id: AdminTab; label: string; description: string }> = [
  { id: 'dashboard', label: '대시보드', description: '전체 운영 현황' },
  { id: 'users', label: '유저 관리', description: '권한과 상태 관리' },
  { id: 'stages', label: '스테이지', description: '학습 흐름 구성' },
  { id: 'problems', label: '문제 관리', description: '문제 등록과 정리' },
  { id: 'notices', label: '공지 관리', description: '공지 작성과 수정' },
]

const stageOptions = computed(() =>
  stages.value.map((stage) => ({
    label: `Stage ${stage.number} · ${stage.title}`,
    value: stage.uuid,
  }))
)

const filteredUsers = computed(() => {
  const keyword = userSearch.value.trim().toLowerCase()
  if (!keyword) {
    return users.value
  }

  return users.value.filter((user) =>
    [user.nickname, user.email, user.id, user.role]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword))
  )
})

const filteredProblems = computed(() => {
  const keyword = problemSearch.value.trim().toLowerCase()
  if (!keyword) {
    return problems.value
  }

  return problems.value.filter((problem) =>
    [problem.summary, problem.type, problem.stageTitle, problem.stageNumber]
      .filter((value) => value !== undefined && value !== null)
      .some((value) => String(value).toLowerCase().includes(keyword))
  )
})

const currentProblemType = computed<ProblemType>(() => normalizeProblemType(problemType.value))

const expectedAnswerMeta = computed(() => {
  if (currentProblemType.value === 'typing') {
    return {
      label: '정답 코드',
      placeholder: '예) print("Hello")',
      help: '학생이 입력해야 하는 정답 코드를 그대로 입력하세요. 이 값이 채점 기준으로 사용됩니다.',
    }
  }

  if (currentProblemType.value === 'ordering') {
    return {
      label: '실행 결과 정답',
      placeholder: '예) Hello',
      help: '코드 블록을 올바르게 조합해 실행했을 때 나와야 하는 출력 결과를 입력하세요.',
    }
  }

  return {
    label: '실행 결과 정답',
    placeholder: '예) Hello',
    help: '학생 코드 실행 결과와 비교할 정답 출력을 입력하세요.',
  }
})

const showBlockInputs = computed(() => currentProblemType.value === 'ordering')

const filteredNotices = computed(() => {
  const keyword = noticeSearch.value.trim().toLowerCase()
  if (!keyword) {
    return notices.value
  }

  return notices.value.filter((notice) =>
    [notice.title, notice.content].some((value) => value.toLowerCase().includes(keyword))
  )
})

const totalLockedUsers = computed(() => users.value.filter((user) => user.is_locked).length)
const totalPublishedNotices = computed(() => notices.value.length)
const totalProblems = computed(() => problems.value.length)
const totalStages = computed(() => stages.value.length)

const summaryCards = computed(() => [
  {
    title: '전체 유저',
    value: users.value.length,
    note: `${totalLockedUsers.value}명 잠금 상태`,
  },
  {
    title: '스테이지',
    value: totalStages.value,
    note: '학습 흐름 기준',
  },
  {
    title: '문제 수',
    value: totalProblems.value,
    note: '현재 등록된 문제',
  },
  {
    title: '공지 수',
    value: totalPublishedNotices.value,
    note: '운영 노트 포함',
  },
])

const recentUsers = computed(() => users.value.slice(0, 5))
const recentStages = computed(() => [...stages.value].sort((a, b) => a.number - b.number).slice(0, 6))
const recentNotices = computed(() => notices.value.slice(0, 4))

const isProblemType = (value: string): value is ProblemType =>
  problemTypeOptions.some((option) => option.value === value)

function normalizeProblemType(type?: string | null): ProblemType {
  const normalized = type?.trim().toLowerCase()
  if (normalized === 'basic') {
    return 'typing'
  }

  return normalized && isProblemType(normalized) ? normalized : 'typing'
}

const getProblemTypeLabel = (type?: string | null) => {
  const normalizedType = normalizeProblemType(type)
  return problemTypeOptions.find((option) => option.value === normalizedType)?.label ?? '직접 입력 문제'
}

const formatDate = (value?: string) => {
  if (!value) {
    return '-'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleDateString()
}

const clearGlobalMessage = () => {
  globalMessage.value = ''
}

const resetStageForm = () => {
  editingStageUuid.value = ''
  stageTitle.value = ''
  stageNumber.value = null
  stageReward.value = null
}

const resetProblemForm = () => {
  editingProblemUuid.value = ''
  problemStageUuid.value = stages.value[0]?.uuid ?? ''
  problemType.value = 'typing'
  problemNumber.value = null
  problemSummary.value = ''
  problemExample.value = ''
  problemExpectedOutput.value = ''
  blockAnswerInput.value = defaultBlockAnswer
  blockItemsInput.value = defaultBlockItems
  hintForms.value = defaultHintForms()
}

const resetNoticeForm = () => {
  editingNoticeUuid.value = ''
  noticeTitle.value = ''
  noticeContent.value = ''
}

const parseBlockPayload = () => {
  const answer = blockAnswerInput.value
    .split(',')
    .map((item) => Number(item.trim()))
    .filter((item) => !Number.isNaN(item))

  const blocks = blockItemsInput.value
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line, index) => {
      const matched = line.match(/^(\d+)\s*[:/]\s*(.*)$/)
      const parsedOrder = matched ? Number(matched[1]) : Number.NaN
      const order = Number.isNaN(parsedOrder) ? index + 1 : parsedOrder

      return {
        order,
        code: matched ? matched[2].trim() : line,
      }
    })

  return { answer, blocks }
}

const buildBlockPayload = () => {
  if (!showBlockInputs.value) {
    return { answer: [], blocks: [] }
  }

  return parseBlockPayload()
}

const validateOrderingBlock = (block: ReturnType<typeof parseBlockPayload>) => {
  if (currentProblemType.value !== 'ordering') {
    return true
  }

  if (!problemExpectedOutput.value.trim()) {
    problemError.value = '순서 배열 문제는 실행 결과 정답을 입력해야 합니다.'
    problemMessage.value = ''
    return false
  }

  if (block.blocks.length === 0 || block.answer.length === 0) {
    problemError.value = '순서 배열 문제는 코드 블록 목록과 정답 순서 answer가 모두 필요합니다.'
    problemMessage.value = ''
    return false
  }

  const blockOrders = new Set(block.blocks.map((blockItem) => blockItem.order))
  const invalidAnswer = block.answer.find((order) => !blockOrders.has(order))

  if (invalidAnswer !== undefined) {
    problemError.value = `정답 순서 ${invalidAnswer}에 해당하는 코드 블록 order가 없습니다.`
    problemMessage.value = ''
    return false
  }

  return true
}

const setBlockInputs = (block: ProblemDetail['block']) => {
  if (!block) {
    blockAnswerInput.value = defaultBlockAnswer
    blockItemsInput.value = defaultBlockItems
    return
  }

  try {
    const payload = typeof block === 'string' ? JSON.parse(block) : block
    const answer = Array.isArray(payload?.answer) ? payload.answer : []
    const blocks = Array.isArray(payload?.blocks) ? payload.blocks : []

    blockAnswerInput.value = answer.length > 0 ? answer.join(',') : defaultBlockAnswer
    blockItemsInput.value = blocks.length > 0
      ? blocks
          .map((item: { order?: number; number?: number; code?: string }, index: number) => `${item.order ?? item.number ?? index + 1}: ${item.code ?? ''}`)
          .join('\n')
      : defaultBlockItems
  } catch (error) {
    console.error(error)
    blockAnswerInput.value = defaultBlockAnswer
    blockItemsInput.value = defaultBlockItems
  }
}

const loadUsers = async () => {
  const [userListResponse, rolesResponse] = await Promise.all([
    userAPI.getUserList({
      page: 0,
      size: 100,
      sort: 'created_at',
      is_asc: false,
    }),
    userAPI.getRoles().catch(() => []),
  ])

  users.value = userListResponse.results as AdminUserRow[]
  roles.value = rolesResponse
}

const loadStages = async () => {
  const response = await stageAPI.getStageList({
    page: 0,
    size: 100,
    sort: 'number',
    is_asc: true,
  })

  stages.value = [...response.results].sort((a, b) => a.number - b.number)

  if (!problemStageUuid.value || !stages.value.some((stage) => stage.uuid === problemStageUuid.value)) {
    problemStageUuid.value = stages.value[0]?.uuid ?? ''
  }
}

const loadProblems = async () => {
  const response = await problemAPI.getProblemList({
    page: 0,
    size: 200,
    sort: 'number',
    is_asc: true,
  })

  problems.value = response.results
}

const loadNotices = async () => {
  const response = await noteAPI.getNoteList({
    page: 0,
    size: 100,
    sort: 'created_at',
    is_asc: false,
  })

  notices.value = response.results
}

const loadAdminData = async () => {
  pageError.value = ''
  clearGlobalMessage()
  failedSections.value = []

  const [userResult, stageResult, problemResult, noticeResult] = await Promise.allSettled([
    loadUsers(),
    loadStages(),
    loadProblems(),
    loadNotices(),
  ])

  const sectionResults: Array<{ label: string; result: PromiseSettledResult<void> }> = [
    { label: '유저', result: userResult },
    { label: '스테이지', result: stageResult },
    { label: '문제', result: problemResult },
    { label: '공지', result: noticeResult },
  ]

  const failures = sectionResults.filter((entry) => entry.result.status === 'rejected')

  if (failures.length > 0) {
    console.error('failed to load some admin data:', failures)
    failedSections.value = failures.map((entry) => entry.label)
    pageError.value = `일부 관리자 데이터를 불러오지 못했습니다: ${failedSections.value.join(', ')}`
  }
}

const getRoleUuid = (user: AdminUserRow) => {
  if (user.role_uuid) {
    return user.role_uuid
  }

  const matchedRole = roles.value.find((role) => role.name.toLowerCase() === String(user.role ?? '').toLowerCase())
  return matchedRole?.uuid ?? ''
}

const handleRoleChange = async (user: AdminUserRow, roleUuid: string) => {
  if (!roleUuid) {
    return
  }

  clearGlobalMessage()

  try {
    await userAPI.updateRole(user.uuid, roleUuid)
    const nextRole = roles.value.find((role) => role.uuid === roleUuid)
    user.role_uuid = roleUuid
    user.role = nextRole?.name ?? user.role
    globalMessage.value = `${user.nickname}님의 권한을 변경했습니다.`
  } catch (error) {
    console.error(error)
    globalMessage.value = `${user.nickname}님의 권한 변경에 실패했습니다.`
  }
}

const handleLockUser = async (user: AdminUserRow) => {
  clearGlobalMessage()

  try {
    await userAPI.lockUser(user.uuid)
    user.is_locked = true
    globalMessage.value = `${user.nickname}님 계정을 잠갔습니다.`
  } catch (error) {
    console.error(error)
    globalMessage.value = `${user.nickname}님 계정 잠금에 실패했습니다.`
  }
}

const handleDeleteUser = async (user: AdminUserRow) => {
  if (!confirm(`${user.nickname}님 계정을 삭제할까요?`)) {
    return
  }

  clearGlobalMessage()

  try {
    await userAPI.deleteUser(user.uuid)
    users.value = users.value.filter((item) => item.uuid !== user.uuid)
    globalMessage.value = `${user.nickname}님 계정을 삭제했습니다.`
  } catch (error) {
    console.error(error)
    globalMessage.value = `${user.nickname}님 계정 삭제에 실패했습니다.`
  }
}

const startStageEdit = (stage: Stage) => {
  activeTab.value = 'stages'
  editingStageUuid.value = stage.uuid
  stageTitle.value = stage.title
  stageNumber.value = stage.number
  stageReward.value = stage.reward
  stageMessage.value = ''
  stageError.value = ''
}

const handleSaveStage = async () => {
  if (!stageTitle.value.trim() || stageNumber.value == null || stageReward.value == null) {
    stageError.value = '스테이지 제목, 번호, 보상 코인을 모두 입력해주세요.'
    stageMessage.value = ''
    return
  }

  isStageLoading.value = true
  stageError.value = ''
  stageMessage.value = ''

  try {
    if (editingStageUuid.value) {
      await stageAPI.updateStage(editingStageUuid.value, {
        title: stageTitle.value.trim(),
        number: stageNumber.value,
        reward: stageReward.value,
      })
      stageMessage.value = '스테이지를 수정했습니다.'
    } else {
      await stageAPI.createStage({
        title: stageTitle.value.trim(),
        number: stageNumber.value,
        reward: stageReward.value,
      })
      stageMessage.value = '스테이지를 생성했습니다.'
    }

    resetStageForm()
    await loadStages()
    if (stages.value.length > 0) {
      problemStageUuid.value = stages.value[stages.value.length - 1]?.uuid ?? problemStageUuid.value
    }
  } catch (error) {
    console.error(error)
    stageError.value = '스테이지 저장에 실패했습니다.'
  } finally {
    isStageLoading.value = false
  }
}

const handleDeleteStage = async (stage: Stage) => {
  if (!confirm(`Stage ${stage.number}를 삭제할까요?`)) {
    return
  }

  clearGlobalMessage()
  stageError.value = ''

  try {
    await stageAPI.deleteStage(stage.uuid)
    if (editingStageUuid.value === stage.uuid) {
      resetStageForm()
    }
    await loadStages()
    globalMessage.value = `Stage ${stage.number}를 삭제했습니다.`
  } catch (error) {
    console.error(error)
    stageError.value = '스테이지 삭제에 실패했습니다.'
  }
}

const handleSaveProblem = async () => {
  if (!problemStageUuid.value || problemNumber.value == null || !problemSummary.value.trim()) {
    problemError.value = '스테이지, 문제 번호, 문제 요약은 필수입니다.'
    problemMessage.value = ''
    return
  }

  isProblemLoading.value = true
  problemError.value = ''
  problemMessage.value = ''

  try {
    const block = buildBlockPayload()
    if (!validateOrderingBlock(block)) {
      return
    }

    const payload = {
      stage_uuid: problemStageUuid.value,
      type: currentProblemType.value,
      number: problemNumber.value,
      summary: problemSummary.value.trim(),
      example: problemExample.value.trim(),
      expectedOutput: problemExpectedOutput.value.trim(),
      block,
      hints: hintForms.value
        .filter((hint) => hint.content.trim())
        .map((hint) => ({
          level: hint.level,
          point: hint.point,
          content: hint.content.trim(),
        })),
    }

    console.log('[Problem Save Payload]', payload)

    if (editingProblemUuid.value) {
      await problemAPI.updateProblem(editingProblemUuid.value, payload)
    } else {
      await problemAPI.createProblem(payload)
    }

    problemMessage.value = editingProblemUuid.value ? '문제를 수정했습니다.' : '문제를 생성했습니다.'
    resetProblemForm()
    await loadProblems()
  } catch (error) {
    console.error(error)
    problemError.value = '문제 저장에 실패했습니다. 블록 형식과 힌트 내용을 다시 확인해주세요.'
  } finally {
    isProblemLoading.value = false
  }
}

const startProblemEdit = (problem: ProblemDetail) => {
  const stageUuid = problem.stage_uuid
    ?? problem.stageUuid
    ?? stages.value.find((stage) => stage.number === problem.stageNumber)?.uuid
    ?? ''

  editingProblemUuid.value = problem.uuid
  problemStageUuid.value = stageUuid
  problemType.value = normalizeProblemType(problem.type)
  problemNumber.value = problem.number
  problemSummary.value = problem.summary ?? ''
  problemExample.value = problem.example ?? ''
  problemExpectedOutput.value = problem.expectedOutput ?? ''
  setBlockInputs(problem.block)
  problemMessage.value = ''
  problemError.value = ''
}

const handleDeleteProblem = async (problem: ProblemDetail) => {
  if (!confirm(`문제 ${problem.number}를 삭제할까요?`)) {
    return
  }

  clearGlobalMessage()

  try {
    await problemAPI.deleteProblem(problem.uuid)
    problems.value = problems.value.filter((item) => item.uuid !== problem.uuid)
    globalMessage.value = `문제 ${problem.number}를 삭제했습니다.`
  } catch (error) {
    console.error(error)
    globalMessage.value = `문제 ${problem.number} 삭제에 실패했습니다.`
  }
}

const startNoticeEdit = (notice: Note) => {
  activeTab.value = 'notices'
  editingNoticeUuid.value = notice.uuid ?? ''
  noticeTitle.value = notice.title
  noticeContent.value = notice.content
  noticeMessage.value = ''
  noticeError.value = ''
}

const handleSaveNotice = async () => {
  if (!noticeTitle.value.trim() || !noticeContent.value.trim()) {
    noticeError.value = '공지 제목과 내용을 모두 입력해주세요.'
    noticeMessage.value = ''
    return
  }

  isNoticeLoading.value = true
  noticeError.value = ''
  noticeMessage.value = ''

  try {
    if (editingNoticeUuid.value) {
      await noteAPI.updateNote(editingNoticeUuid.value, {
        title: noticeTitle.value.trim(),
        content: noticeContent.value.trim(),
      })
      noticeMessage.value = '공지를 수정했습니다.'
    } else {
      await noteAPI.createNote({
        title: noticeTitle.value.trim(),
        content: noticeContent.value.trim(),
      })
      noticeMessage.value = '공지를 등록했습니다.'
    }

    resetNoticeForm()
    await loadNotices()
  } catch (error) {
    console.error(error)
    noticeError.value = '공지 저장에 실패했습니다.'
  } finally {
    isNoticeLoading.value = false
  }
}

const handleDeleteNotice = async (notice: Note) => {
  if (!notice.uuid || !confirm(`"${notice.title}" 공지를 삭제할까요?`)) {
    return
  }

  clearGlobalMessage()

  try {
    await noteAPI.deleteNote(notice.uuid)
    notices.value = notices.value.filter((item) => item.uuid !== notice.uuid)
    if (editingNoticeUuid.value === notice.uuid) {
      resetNoticeForm()
    }
    globalMessage.value = '공지를 삭제했습니다.'
  } catch (error) {
    console.error(error)
    globalMessage.value = '공지 삭제에 실패했습니다.'
  }
}

onMounted(async () => {
  await auth.restoreAuth('/admin')

  const role = auth.state.user?.role
  if (!isAdminRole(role)) {
    alert('관리자 권한이 없습니다.')
    await router.replace('/')
    return
  }

  try {
    await loadAdminData()
  } finally {
    isBooting.value = false
  }
})
</script>

<template>
  <div class="min-h-screen bg-[#FFF2EF]">
    <Navbar />
    <PageHeader
      title="관리자 페이지"
      subtitle="운영 현황을 확인하고 학습 콘텐츠와 사용자 상태를 한 곳에서 관리하세요."
      back-link="/home"
    />

    <main class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div v-if="isBooting" class="luxe-panel p-10 text-center">
        <div class="mx-auto mb-4 h-14 w-14 animate-spin rounded-full border-2 border-[#FFDBB6] border-b-[#1A2A4F]" />
        <p class="font-medium text-[#1A2A4F]">관리자 데이터를 불러오는 중입니다.</p>
      </div>

      <div v-else class="grid gap-6 lg:grid-cols-[280px_1fr]">
        <aside class="luxe-panel p-5">
          <p class="text-xs font-semibold uppercase tracking-[0.22em] text-[#1A2A4F]/45">Admin Menu</p>
          <div class="mt-5 flex flex-col gap-3">
            <button
              v-for="tab in sidebarTabs"
              :key="tab.id"
              type="button"
              class="cursor-pointer rounded-[22px] border px-4 py-4 text-left transition duration-300"
              :class="activeTab === tab.id ? 'border-[#1A2A4F]/12 bg-[#FFF8F4] shadow-[0_10px_24px_rgba(15,23,42,0.06)]' : 'border-transparent bg-transparent hover:bg-white/60'"
              @click="activeTab = tab.id"
            >
              <p class="text-sm font-semibold text-[#1A2A4F]">{{ tab.label }}</p>
              <p class="mt-1 text-xs text-[#1A2A4F]/50">{{ tab.description }}</p>
            </button>
          </div>
        </aside>

        <section class="space-y-6">
          <div
            v-if="pageError || globalMessage"
            class="luxe-card p-5 text-sm font-medium text-[#1A2A4F]"
          >
            {{ pageError || globalMessage }}
          </div>

          <template v-if="activeTab === 'dashboard'">
            <section class="luxe-panel p-6 sm:p-8">
              <p class="text-xs font-semibold uppercase tracking-[0.22em] text-[#1A2A4F]/45">Overview</p>
              <h2 class="mt-2 text-3xl font-black text-[#1A2A4F]">운영 요약</h2>
              <p class="mt-2 text-sm leading-6 text-[#1A2A4F]/65">
                실제 불러온 운영 데이터를 기준으로 현재 플랫폼 상태를 빠르게 확인할 수 있어요.
              </p>

              <div class="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                <article
                  v-for="card in summaryCards"
                  :key="card.title"
                  class="luxe-card-soft p-5"
                >
                  <p class="text-sm font-medium text-[#1A2A4F]/55">{{ card.title }}</p>
                  <p class="mt-3 text-4xl font-black text-[#1A2A4F]">{{ card.value }}</p>
                  <p class="mt-2 text-sm text-[#1A2A4F]/45">{{ card.note }}</p>
                </article>
              </div>
            </section>

            <section class="grid gap-6 xl:grid-cols-3">
              <div class="luxe-card p-6">
                <div class="flex items-center justify-between gap-3">
                  <h3 class="text-xl font-black text-[#1A2A4F]">최근 유저</h3>
                  <button type="button" class="text-sm font-medium text-[#1A2A4F]/55" @click="activeTab = 'users'">자세히</button>
                </div>
                <div class="mt-5 space-y-3">
                  <div
                    v-for="user in recentUsers"
                    :key="user.uuid"
                    class="rounded-[20px] border border-[#1A2A4F]/8 bg-[#FFF8F4] px-4 py-3"
                  >
                    <div class="flex items-center justify-between gap-3">
                      <div>
                        <p class="font-medium text-[#1A2A4F]">{{ user.nickname }}</p>
                        <p class="mt-1 text-sm text-[#1A2A4F]/48">{{ user.email }}</p>
                      </div>
                      <span class="luxe-pill px-3 py-1 text-xs font-medium text-[#1A2A4F]">
                        {{ user.is_locked ? '잠금' : '활성' }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="luxe-card p-6">
                <div class="flex items-center justify-between gap-3">
                  <h3 class="text-xl font-black text-[#1A2A4F]">스테이지 흐름</h3>
                  <button type="button" class="text-sm font-medium text-[#1A2A4F]/55" @click="activeTab = 'stages'">관리하기</button>
                </div>
                <div class="mt-5 space-y-3">
                  <div
                    v-for="stage in recentStages"
                    :key="stage.uuid"
                    class="rounded-[20px] border border-[#1A2A4F]/8 bg-white px-4 py-3"
                  >
                    <div class="flex items-center justify-between gap-3">
                      <p class="font-medium text-[#1A2A4F]">Stage {{ stage.number }} · {{ stage.title }}</p>
                      <span class="text-sm text-[#1A2A4F]/45">{{ stage.reward }} coin</span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="luxe-card p-6">
                <div class="flex items-center justify-between gap-3">
                  <h3 class="text-xl font-black text-[#1A2A4F]">최근 공지</h3>
                  <button type="button" class="text-sm font-medium text-[#1A2A4F]/55" @click="activeTab = 'notices'">편집하기</button>
                </div>
                <div class="mt-5 space-y-3">
                  <div
                    v-for="notice in recentNotices"
                    :key="notice.uuid ?? notice.title"
                    class="rounded-[20px] border border-[#1A2A4F]/8 bg-[#FFF8F4] px-4 py-3"
                  >
                    <p class="font-medium text-[#1A2A4F]">{{ notice.title }}</p>
                    <p class="mt-2 line-clamp-2 text-sm leading-6 text-[#1A2A4F]/55">{{ notice.content }}</p>
                  </div>
                </div>
              </div>
            </section>
          </template>

          <template v-else-if="activeTab === 'users'">
            <section class="luxe-panel p-6 sm:p-8">
              <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                <div>
                  <p class="text-xs font-semibold uppercase tracking-[0.22em] text-[#1A2A4F]/45">Users</p>
                  <h2 class="mt-2 text-3xl font-black text-[#1A2A4F]">유저 관리</h2>
                  <p class="mt-2 text-sm leading-6 text-[#1A2A4F]/65">
                    권한 변경, 계정 잠금, 삭제까지 실제 계정 상태를 기준으로 관리할 수 있어요.
                  </p>
                </div>
                <input
                  v-model="userSearch"
                  type="text"
                  placeholder="닉네임, 이메일, 아이디로 검색"
                  class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F] lg:max-w-xs"
                >
              </div>

              <div class="mt-6 overflow-x-auto">
                <table class="min-w-full">
                  <thead>
                    <tr class="border-b border-[#1A2A4F]/8 text-left text-sm text-[#1A2A4F]/48">
                      <th class="px-3 py-3 font-medium">닉네임</th>
                      <th class="px-3 py-3 font-medium">아이디</th>
                      <th class="px-3 py-3 font-medium">이메일</th>
                      <th class="px-3 py-3 font-medium">권한</th>
                      <th class="px-3 py-3 font-medium">상태</th>
                      <th class="px-3 py-3 font-medium">관리</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="user in filteredUsers"
                      :key="user.uuid"
                      class="border-b border-[#1A2A4F]/6 last:border-b-0"
                    >
                      <td class="px-3 py-4 font-medium text-[#1A2A4F]">{{ user.nickname }}</td>
                      <td class="px-3 py-4 text-sm text-[#1A2A4F]/65">{{ user.id }}</td>
                      <td class="px-3 py-4 text-sm text-[#1A2A4F]/65">{{ user.email }}</td>
                      <td class="px-3 py-4">
                        <select
                          :value="getRoleUuid(user)"
                          class="luxe-input rounded-[14px] px-3 py-2 text-sm text-[#1A2A4F]"
                          @change="handleRoleChange(user, String(($event.target as HTMLSelectElement).value))"
                        >
                          <option value="">권한 선택</option>
                          <option v-for="role in roles" :key="role.uuid" :value="role.uuid">{{ role.name }}</option>
                        </select>
                      </td>
                      <td class="px-3 py-4">
                        <span class="luxe-pill px-3 py-1 text-xs font-medium text-[#1A2A4F]">
                          {{ user.is_locked ? '잠금' : '활성' }}
                        </span>
                      </td>
                      <td class="px-3 py-4">
                        <div class="flex flex-wrap gap-2">
                          <button
                            type="button"
                            :disabled="user.is_locked"
                            class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-xs font-medium disabled:cursor-not-allowed disabled:opacity-50"
                            @click="handleLockUser(user)"
                          >
                            계정 잠금
                          </button>
                          <button
                            type="button"
                            class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-xs font-medium text-[#B24A5A]"
                            @click="handleDeleteUser(user)"
                          >
                            삭제
                          </button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
          </template>

          <template v-else-if="activeTab === 'stages'">
            <section class="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
              <div class="luxe-panel p-6">
                <p class="text-xs font-semibold uppercase tracking-[0.22em] text-[#1A2A4F]/45">Stage Editor</p>
                <h2 class="mt-2 text-3xl font-black text-[#1A2A4F]">
                  {{ editingStageUuid ? '스테이지 수정' : '스테이지 생성' }}
                </h2>

                <div class="mt-6 space-y-4">
                  <div>
                    <label class="mb-2 block text-sm font-medium text-[#1A2A4F]">스테이지 제목</label>
                    <input v-model="stageTitle" type="text" class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F]">
                  </div>

                  <div class="grid gap-4 sm:grid-cols-2">
                    <div>
                      <label class="mb-2 block text-sm font-medium text-[#1A2A4F]">번호</label>
                      <input v-model.number="stageNumber" type="number" min="1" class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F]">
                    </div>
                    <div>
                      <label class="mb-2 block text-sm font-medium text-[#1A2A4F]">보상 코인</label>
                      <input v-model.number="stageReward" type="number" min="0" class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F]">
                    </div>
                  </div>

                  <p v-if="stageMessage" class="rounded-[18px] border border-[#1A2A4F]/10 bg-[#FFF8F4] px-4 py-3 text-sm text-[#1A2A4F]">
                    {{ stageMessage }}
                  </p>
                  <p v-if="stageError" class="rounded-[18px] border border-[#F7A5A5] bg-white px-4 py-3 text-sm text-[#B24A5A]">
                    {{ stageError }}
                  </p>

                  <div class="flex justify-end gap-3">
                    <button type="button" class="luxe-button-soft cursor-pointer rounded-full px-5 py-3 text-sm font-medium" @click="resetStageForm">
                      초기화
                    </button>
                    <button
                      type="button"
                      :disabled="isStageLoading"
                      class="luxe-button-accent cursor-pointer rounded-full px-5 py-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-60"
                      @click="handleSaveStage"
                    >
                      {{ isStageLoading ? '저장 중...' : editingStageUuid ? '수정 저장' : '스테이지 생성' }}
                    </button>
                  </div>
                </div>
              </div>

              <div class="luxe-card p-6">
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <p class="text-xs font-semibold uppercase tracking-[0.22em] text-[#1A2A4F]/45">Stage List</p>
                    <h3 class="mt-2 text-2xl font-black text-[#1A2A4F]">등록된 스테이지</h3>
                  </div>
                  <button type="button" class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-sm font-medium" @click="loadStages">
                    새로고침
                  </button>
                </div>

                <div class="mt-5 space-y-3">
                  <article
                    v-for="stage in stages"
                    :key="stage.uuid"
                    class="rounded-[20px] border border-[#1A2A4F]/8 bg-[#FFF8F4] p-4"
                  >
                    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                      <div>
                        <p class="font-medium text-[#1A2A4F]">Stage {{ stage.number }} · {{ stage.title }}</p>
                        <p class="mt-1 text-sm text-[#1A2A4F]/48">보상 {{ stage.reward }} coin</p>
                      </div>
                      <div class="flex gap-2">
                        <button type="button" class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-sm font-medium" @click="startStageEdit(stage)">
                          수정
                        </button>
                        <button type="button" class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-sm font-medium text-[#B24A5A]" @click="handleDeleteStage(stage)">
                          삭제
                        </button>
                      </div>
                    </div>
                  </article>
                </div>
              </div>
            </section>
          </template>

          <template v-else-if="activeTab === 'problems'">
            <section class="luxe-panel p-6">
              <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                <div>
                  <p class="text-xs font-semibold uppercase tracking-[0.22em] text-[#1A2A4F]/45">Problem Builder</p>
                  <h2 class="mt-2 text-3xl font-black text-[#1A2A4F]">{{ editingProblemUuid ? '문제 수정' : '문제 생성' }}</h2>
                </div>
                <input
                  v-model="problemSearch"
                  type="text"
                  placeholder="문제 요약, 타입, 스테이지 검색"
                  class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F] lg:max-w-xs"
                >
              </div>

              <div class="mt-6 grid gap-5">
                <div class="grid gap-4 lg:grid-cols-3">
                  <div>
                    <label class="mb-2 block text-sm font-medium text-[#1A2A4F]">스테이지</label>
                    <select v-model="problemStageUuid" class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F]">
                      <option value="">스테이지 선택</option>
                      <option v-for="stage in stageOptions" :key="stage.value" :value="stage.value">{{ stage.label }}</option>
                    </select>
                  </div>
                  <div>
                    <label class="mb-2 block text-sm font-medium text-[#1A2A4F]">문제 타입</label>
                    <select v-model="problemType" class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F]">
                      <option v-for="option in problemTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                    </select>
                  </div>
                  <div>
                    <label class="mb-2 block text-sm font-medium text-[#1A2A4F]">문제 번호</label>
                    <input v-model.number="problemNumber" type="number" min="1" class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F]">
                  </div>
                </div>

                <div>
                  <label class="mb-2 block text-sm font-medium text-[#1A2A4F]">문제 요약</label>
                  <input v-model="problemSummary" type="text" placeholder="예: Hello 출력하기" class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F]">
                </div>

                <div class="grid gap-4 lg:grid-cols-2">
                  <div>
                    <label class="mb-2 block text-sm font-medium text-[#1A2A4F]">예시 코드</label>
                    <textarea v-model="problemExample" rows="5" placeholder='예) print("Hello")' class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F]" />
                  </div>
                  <div>
                    <label class="mb-2 block text-sm font-medium text-[#1A2A4F]">{{ expectedAnswerMeta.label }}</label>
                    <textarea v-model="problemExpectedOutput" rows="5" :placeholder="expectedAnswerMeta.placeholder" class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F]" />
                    <p class="mt-2 text-xs leading-5 text-[#1A2A4F]/55">{{ expectedAnswerMeta.help }}</p>
                  </div>
                </div>

                <div v-if="showBlockInputs" class="grid gap-4 lg:grid-cols-2">
                  <div>
                    <label class="mb-2 block text-sm font-medium text-[#1A2A4F]">정답 순서 answer</label>
                    <input v-model="blockAnswerInput" type="text" placeholder="예: 1,2,3,4" class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F]">
                  </div>
                  <div>
                    <label class="mb-2 block text-sm font-medium text-[#1A2A4F]">블록 코드 목록</label>
                    <textarea v-model="blockItemsInput" rows="5" placeholder="한 줄에 하나씩, `순서: 코드` 형태로 입력" class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F]" />
                  </div>
                </div>

                <div class="luxe-card-soft p-5">
                  <div class="flex items-center justify-between gap-3">
                    <h3 class="text-lg font-black text-[#1A2A4F]">힌트 설정</h3>
                    <span class="text-xs text-[#1A2A4F]/45">내용이 있는 힌트만 전송됩니다.</span>
                  </div>
                  <div class="mt-4 grid gap-4">
                    <div
                      v-for="hint in hintForms"
                      :key="hint.level"
                      class="rounded-[18px] border border-[#1A2A4F]/8 bg-white px-4 py-4"
                    >
                      <div class="grid gap-4 lg:grid-cols-[120px_140px_1fr]">
                        <input v-model.number="hint.level" type="number" min="1" class="luxe-input rounded-[14px] px-3 py-2 text-[#1A2A4F]">
                        <input v-model.number="hint.point" type="number" min="0" class="luxe-input rounded-[14px] px-3 py-2 text-[#1A2A4F]">
                        <input v-model="hint.content" type="text" class="luxe-input rounded-[14px] px-3 py-2 text-[#1A2A4F]">
                      </div>
                    </div>
                  </div>
                </div>

                <p v-if="problemMessage" class="rounded-[18px] border border-[#1A2A4F]/10 bg-[#FFF8F4] px-4 py-3 text-sm text-[#1A2A4F]">
                  {{ problemMessage }}
                </p>
                <p v-if="problemError" class="rounded-[18px] border border-[#F7A5A5] bg-white px-4 py-3 text-sm text-[#B24A5A]">
                  {{ problemError }}
                </p>

                <div class="flex justify-end gap-3">
                  <button type="button" class="luxe-button-soft cursor-pointer rounded-full px-5 py-3 text-sm font-medium" @click="resetProblemForm">
                    초기화
                  </button>
                  <button
                    type="button"
                    :disabled="isProblemLoading"
                    class="luxe-button-accent cursor-pointer rounded-full px-5 py-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-60"
                    @click="handleSaveProblem"
                  >
                    {{ isProblemLoading ? '저장 중...' : editingProblemUuid ? '수정 저장' : '문제 생성' }}
                  </button>
                </div>
              </div>
            </section>

            <section class="luxe-card p-6">
              <div class="flex items-center justify-between gap-3">
                <div>
                  <p class="text-xs font-semibold uppercase tracking-[0.22em] text-[#1A2A4F]/45">Problem List</p>
                  <h3 class="mt-2 text-2xl font-black text-[#1A2A4F]">등록된 문제</h3>
                </div>
                <button type="button" class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-sm font-medium" @click="loadProblems">
                  새로고침
                </button>
              </div>

              <div class="mt-5 grid gap-3">
                <article
                  v-for="problem in filteredProblems"
                  :key="problem.uuid"
                  class="rounded-[20px] border border-[#1A2A4F]/8 bg-[#FFF8F4] p-4"
                >
                  <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                      <p class="font-medium text-[#1A2A4F]">
                        문제 {{ problem.number }} · {{ problem.summary }}
                      </p>
                      <p class="mt-1 text-sm text-[#1A2A4F]/48">
                        {{ getProblemTypeLabel(problem.type) }} · {{ problem.stageTitle || `Stage ${problem.stageNumber ?? '-'}` }}
                      </p>
                    </div>
                    <div class="flex gap-2">
                      <button type="button" class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-sm font-medium" @click="startProblemEdit(problem)">
                        수정
                      </button>
                      <button type="button" class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-sm font-medium text-[#B24A5A]" @click="handleDeleteProblem(problem)">
                        삭제
                      </button>
                    </div>
                  </div>
                </article>
              </div>
            </section>
          </template>

          <template v-else>
            <section class="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
              <div class="luxe-panel p-6">
                <p class="text-xs font-semibold uppercase tracking-[0.22em] text-[#1A2A4F]/45">Notice Editor</p>
                <h2 class="mt-2 text-3xl font-black text-[#1A2A4F]">
                  {{ editingNoticeUuid ? '공지 수정' : '공지 작성' }}
                </h2>

                <div class="mt-6 space-y-4">
                  <div>
                    <label class="mb-2 block text-sm font-medium text-[#1A2A4F]">공지 제목</label>
                    <input v-model="noticeTitle" type="text" class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F]">
                  </div>
                  <div>
                    <label class="mb-2 block text-sm font-medium text-[#1A2A4F]">공지 내용</label>
                    <textarea v-model="noticeContent" rows="10" class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F]" />
                  </div>

                  <p v-if="noticeMessage" class="rounded-[18px] border border-[#1A2A4F]/10 bg-[#FFF8F4] px-4 py-3 text-sm text-[#1A2A4F]">
                    {{ noticeMessage }}
                  </p>
                  <p v-if="noticeError" class="rounded-[18px] border border-[#F7A5A5] bg-white px-4 py-3 text-sm text-[#B24A5A]">
                    {{ noticeError }}
                  </p>

                  <div class="flex justify-end gap-3">
                    <button type="button" class="luxe-button-soft cursor-pointer rounded-full px-5 py-3 text-sm font-medium" @click="resetNoticeForm">
                      초기화
                    </button>
                    <button
                      type="button"
                      :disabled="isNoticeLoading"
                      class="luxe-button-accent cursor-pointer rounded-full px-5 py-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-60"
                      @click="handleSaveNotice"
                    >
                      {{ isNoticeLoading ? '저장 중...' : editingNoticeUuid ? '수정 저장' : '공지 등록' }}
                    </button>
                  </div>
                </div>
              </div>

              <div class="luxe-card p-6">
                <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                  <div>
                    <p class="text-xs font-semibold uppercase tracking-[0.22em] text-[#1A2A4F]/45">Notice List</p>
                    <h3 class="mt-2 text-2xl font-black text-[#1A2A4F]">등록된 공지</h3>
                  </div>
                  <input
                    v-model="noticeSearch"
                    type="text"
                    placeholder="공지 제목 또는 내용 검색"
                    class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F] lg:max-w-xs"
                  >
                </div>

                <div class="mt-5 space-y-3">
                  <article
                    v-for="notice in filteredNotices"
                    :key="notice.uuid ?? notice.title"
                    class="rounded-[20px] border border-[#1A2A4F]/8 bg-[#FFF8F4] p-4"
                  >
                    <div class="flex flex-col gap-3">
                      <div class="flex items-start justify-between gap-3">
                        <div>
                          <p class="font-medium text-[#1A2A4F]">{{ notice.title }}</p>
                          <p class="mt-1 text-sm text-[#1A2A4F]/45">{{ formatDate(notice.created_at) }}</p>
                        </div>
                        <div class="flex gap-2">
                          <button type="button" class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-sm font-medium" @click="startNoticeEdit(notice)">
                            수정
                          </button>
                          <button type="button" class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-sm font-medium text-[#B24A5A]" @click="handleDeleteNotice(notice)">
                            삭제
                          </button>
                        </div>
                      </div>
                      <p class="line-clamp-3 text-sm leading-6 text-[#1A2A4F]/62">{{ notice.content }}</p>
                    </div>
                  </article>
                </div>
              </div>
            </section>
          </template>
        </section>
      </div>
    </main>
  </div>
</template>
