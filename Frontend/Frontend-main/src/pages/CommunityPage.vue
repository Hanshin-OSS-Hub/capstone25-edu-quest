<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import PageHeader from '../components/PageHeader.vue'
import { communityAnswerAPI, communityPostAPI, type CommunityAnswer, type CommunityPost } from '../api/community'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const auth = useAuthStore()

const questions = ref<CommunityPost[]>([])
const selectedQuestion = ref<CommunityPost | null>(null)
const answers = ref<CommunityAnswer[]>([])
const newQuestionTitle = ref('')
const newQuestionContent = ref('')
const newAnswerContent = ref('')
const isLoading = ref(false)
const isSubmitting = ref(false)
const showQuestionForm = ref(false)
const searchField = ref<'title' | 'content' | 'author'>('title')
const searchKeyword = ref('')
const pageError = ref('')
const actionMessage = ref('')

const displayUser = (user?: { nickname?: string; uuid?: string }) => user?.nickname ?? user?.uuid ?? '익명'
const isAdmin = computed(() => auth.state.user?.role?.toLowerCase() === 'admin')
const questionCount = computed(() => questions.value.length)
const canSubmitQuestion = computed(
  () => newQuestionTitle.value.trim().length > 0 && newQuestionContent.value.trim().length > 0
)
const canSubmitAnswer = computed(() => newAnswerContent.value.trim().length > 0)

const clearActionMessage = () => {
  actionMessage.value = ''
}

const fetchQuestions = async () => {
  pageError.value = ''

  try {
    const response = await communityPostAPI.getPostList({
      page: 0,
      size: 50,
      sort: 'created_at',
      is_asc: false,
    })
    const keyword = searchKeyword.value.trim().toLowerCase()

    if (!keyword) {
      questions.value = response.results
      return
    }

    questions.value = response.results.filter((question) => {
      const nickname = displayUser(question.user ?? question.member).toLowerCase()
      const title = (question.title ?? '').toLowerCase()
      const content = (question.content ?? '').toLowerCase()

      if (searchField.value === 'author') {
        return nickname.includes(keyword)
      }

      if (searchField.value === 'content') {
        return content.includes(keyword)
      }

      return title.includes(keyword)
    })
  } catch (error) {
    console.error('failed to fetch community questions:', error)
    pageError.value = '커뮤니티 글을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.'
  }
}

const handleSearch = async () => {
  searchKeyword.value = searchKeyword.value.trim()
  await fetchQuestions()
}

const handleQuestionClick = async (question: CommunityPost) => {
  clearActionMessage()
  isLoading.value = true

  try {
    selectedQuestion.value = await communityPostAPI.getPost(question.uuid)
    const response = await communityAnswerAPI.getAnswerList(question.uuid, { page: 0, size: 50, is_asc: true })
    answers.value = response.results
  } catch (error) {
    console.error('failed to load question detail:', error)
    actionMessage.value = '질문 상세 내용을 불러오지 못했습니다.'
  } finally {
    isLoading.value = false
  }
}

const handleCreateQuestion = async () => {
  clearActionMessage()

  if (!canSubmitQuestion.value) {
    actionMessage.value = '제목과 내용을 모두 입력해주세요.'
    return
  }

  isSubmitting.value = true
  try {
    const createdPost = await communityPostAPI.createPost({
      title: newQuestionTitle.value.trim(),
      content: newQuestionContent.value.trim(),
    })
    showQuestionForm.value = false
    newQuestionTitle.value = ''
    newQuestionContent.value = ''
    searchKeyword.value = ''
    actionMessage.value = '질문이 등록되었습니다.'
    await fetchQuestions()

    if (createdPost?.uuid) {
      await handleQuestionClick(createdPost)
    }
  } catch (error) {
    console.error('failed to create question:', error)
    actionMessage.value = '질문 등록에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}

const handleCreateAnswer = async () => {
  clearActionMessage()

  if (!selectedQuestion.value) {
    return
  }

  if (!canSubmitAnswer.value) {
    actionMessage.value = '답변 내용을 입력해주세요.'
    return
  }

  isSubmitting.value = true
  try {
    await communityAnswerAPI.createAnswer(selectedQuestion.value.uuid, { content: newAnswerContent.value.trim() })
    newAnswerContent.value = ''
    actionMessage.value = '답변이 등록되었습니다.'
    await handleQuestionClick(selectedQuestion.value)
  } catch (error) {
    console.error('failed to create answer:', error)
    actionMessage.value = '답변 등록에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}

const handleDelete = async (postId: string) => {
  if (!confirm('정말 이 글을 삭제할까요?')) {
    return
  }

  clearActionMessage()
  isSubmitting.value = true
  try {
    await communityPostAPI.deletePost(postId)

    if (selectedQuestion.value?.uuid === postId) {
      selectedQuestion.value = null
      answers.value = []
    }

    actionMessage.value = '글을 삭제했습니다.'
    await fetchQuestions()
  } catch (error) {
    console.error('failed to delete question:', error)
    actionMessage.value = '글 삭제에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}

onMounted(async () => {
  await auth.restoreAuth(route.path)
  await fetchQuestions()
})
</script>

<template>
  <div class="min-h-screen bg-[#FFF2EF]">
    <PageHeader
      title="커뮤니티"
      :subtitle="selectedQuestion ? '질문을 읽고 답변을 남기거나, 이미 달린 답변을 참고해보세요.' : '궁금한 점을 올리고 다른 학습자들과 해결 방법을 나눠보세요.'"
      :back-link="selectedQuestion ? undefined : '/home'"
      @back="selectedQuestion = null"
    >
      <template #rightAction>
        <button
          v-if="!selectedQuestion"
          type="button"
          class="luxe-button-accent cursor-pointer rounded-full px-4 py-2 text-sm font-medium"
          @click="showQuestionForm = !showQuestionForm"
        >
          {{ showQuestionForm ? '닫기' : '질문 작성' }}
        </button>
        <button
          v-else
          type="button"
          class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-sm font-medium"
          @click="selectedQuestion = null"
        >
          목록으로
        </button>
      </template>
    </PageHeader>

    <main class="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <div
        v-if="pageError || actionMessage"
        class="mb-6 rounded-[24px] border border-[#1A2A4F]/10 bg-white/92 px-5 py-4 text-sm font-medium text-[#1A2A4F]/75 shadow-[0_12px_30px_rgba(15,23,42,0.05)]"
      >
        {{ pageError || actionMessage }}
      </div>

      <template v-if="selectedQuestion">
        <section class="luxe-panel p-7 sm:p-8">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p class="inline-flex rounded-full border border-[#1A2A4F]/10 bg-[#FFF8F4] px-3 py-1 text-xs font-medium text-[#1A2A4F]">
                작성자 {{ displayUser(selectedQuestion.user ?? selectedQuestion.member) }}
              </p>
              <h1 class="mt-4 text-3xl font-black text-[#1A2A4F]">{{ selectedQuestion.title }}</h1>
              <p class="mt-3 text-sm text-[#1A2A4F]/45">
                {{ selectedQuestion.created_at ? new Date(selectedQuestion.created_at).toLocaleDateString() : '-' }}
              </p>
            </div>
            <button
              v-if="isAdmin"
              type="button"
              class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-sm font-medium text-[#B24A5A] disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="isSubmitting"
              @click="handleDelete(selectedQuestion.uuid)"
            >
              글 삭제
            </button>
          </div>

          <p class="mt-6 whitespace-pre-line text-base leading-8 text-[#1A2A4F]/82">
            {{ selectedQuestion.content }}
          </p>
        </section>

        <section class="mt-6">
          <div class="mb-4 flex items-end justify-between gap-3">
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.18em] text-[#1A2A4F]/45">Answers</p>
              <h2 class="mt-2 text-2xl font-black text-[#1A2A4F]">답변 {{ answers.length }}개</h2>
            </div>
          </div>

          <div v-if="isLoading" class="luxe-card-soft p-6 text-sm font-medium text-[#1A2A4F]/60">
            답변을 불러오는 중입니다.
          </div>

          <div v-else-if="answers.length === 0" class="luxe-card-soft p-6 text-sm font-medium text-[#1A2A4F]/60">
            아직 등록된 답변이 없어요. 첫 답변을 남겨보세요.
          </div>

          <div v-else class="space-y-4">
            <article
              v-for="answer in answers"
              :key="answer.uuid"
              class="luxe-card-soft p-5"
            >
              <div class="flex flex-wrap items-center justify-between gap-3">
                <p class="text-sm font-medium text-[#1A2A4F]/60">
                  작성자 {{ displayUser(answer.user ?? answer.member) }}
                </p>
                <p class="text-xs text-[#1A2A4F]/45">
                  {{ answer.created_at ? new Date(answer.created_at).toLocaleString() : '-' }}
                </p>
              </div>
              <p class="mt-3 whitespace-pre-line text-sm leading-7 text-[#1A2A4F]/78">
                {{ answer.content }}
              </p>
            </article>
          </div>
        </section>

        <section class="mt-6 luxe-panel p-6">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.18em] text-[#1A2A4F]/45">Write Answer</p>
              <h3 class="mt-2 text-xl font-black text-[#1A2A4F]">답변 남기기</h3>
            </div>
          </div>

          <textarea
            v-model="newAnswerContent"
            placeholder="질문 해결에 도움이 되는 내용을 정리해서 남겨주세요."
            class="luxe-input mt-4 h-36 w-full resize-none rounded-[22px] px-4 py-4 text-[#1A2A4F]"
          />

          <div class="mt-4 flex justify-end">
            <button
              type="button"
              :disabled="isSubmitting"
              class="luxe-button-accent cursor-pointer rounded-full px-6 py-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-60"
              @click="handleCreateAnswer"
            >
              답변 등록
            </button>
          </div>
        </section>
      </template>

      <template v-else>
        <section class="luxe-panel p-5 sm:p-6">
          <div class="flex flex-col gap-4 lg:flex-row lg:items-center">
            <select
              v-model="searchField"
              class="luxe-input rounded-[18px] px-4 py-3 font-medium text-[#1A2A4F]"
            >
              <option value="title">제목</option>
              <option value="content">내용</option>
              <option value="author">작성자</option>
            </select>
            <input
              v-model="searchKeyword"
              type="text"
              placeholder="검색어를 입력해주세요."
              class="luxe-input flex-1 rounded-[18px] px-4 py-3 text-[#1A2A4F] placeholder:text-[#1A2A4F]/40"
              @keyup.enter="void handleSearch()"
            >
            <button
              type="button"
              class="luxe-button-accent cursor-pointer rounded-[18px] px-6 py-3 text-sm font-medium transition hover:translate-y-[-1px]"
              @click="void handleSearch()"
            >
              검색
            </button>
          </div>

          <div class="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm text-[#1A2A4F]/60">
            <p>총 {{ questionCount }}개의 질문이 있어요.</p>
            <p>학습 중 막히는 부분을 구체적으로 적을수록 더 좋은 답변을 받기 쉬워요.</p>
          </div>
        </section>

        <section v-if="showQuestionForm" class="mt-6 luxe-panel p-6">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.18em] text-[#1A2A4F]/45">Write Question</p>
            <h2 class="mt-2 text-2xl font-black text-[#1A2A4F]">새 질문 작성</h2>
            <p class="mt-2 text-sm leading-6 text-[#1A2A4F]/65">
              제목은 짧고 분명하게, 내용은 현재 시도한 방식까지 적어주면 좋아요.
            </p>
          </div>

          <div class="mt-5 space-y-4">
            <input
              v-model="newQuestionTitle"
              type="text"
              placeholder="질문의 핵심을 제목으로 적어주세요."
              class="luxe-input w-full rounded-[22px] px-4 py-4 text-[#1A2A4F]"
            >
            <textarea
              v-model="newQuestionContent"
              placeholder="현재 막힌 부분이나 시도해본 방법을 구체적으로 적어주세요."
              class="luxe-input h-40 w-full resize-none rounded-[22px] px-4 py-4 text-[#1A2A4F]"
            />
            <div class="flex justify-end gap-3">
              <button
                type="button"
                class="luxe-button-soft cursor-pointer rounded-full px-5 py-3 text-sm font-medium"
                @click="showQuestionForm = false"
              >
                취소
              </button>
              <button
                type="button"
                :disabled="isSubmitting"
                class="luxe-button-accent cursor-pointer rounded-full px-5 py-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-60"
                @click="handleCreateQuestion"
              >
                질문 등록
              </button>
            </div>
          </div>
        </section>

        <section class="mt-6 grid gap-5">
          <article
            v-for="question in questions"
            :key="question.uuid"
            class="luxe-card cursor-pointer p-6 transition duration-300 hover:translate-y-[-2px]"
            @click="handleQuestionClick(question)"
          >
            <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div class="min-w-0">
                <p class="inline-flex rounded-full border border-[#1A2A4F]/10 bg-[#FFF8F4] px-3 py-1 text-xs font-medium text-[#1A2A4F]">
                  작성자 {{ displayUser(question.user ?? question.member) }}
                </p>
                <h3 class="mt-3 text-2xl font-black text-[#1A2A4F]">{{ question.title }}</h3>
                <p class="mt-3 line-clamp-2 text-sm leading-6 text-[#1A2A4F]/68">
                  {{ question.content || '내용이 비어 있습니다.' }}
                </p>
                <p class="mt-4 text-sm text-[#1A2A4F]/45">
                  {{ question.created_at ? new Date(question.created_at).toLocaleDateString() : '-' }}
                </p>
              </div>

              <div class="flex shrink-0 items-center gap-3">
                <button
                  v-if="isAdmin"
                  type="button"
                  class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-sm font-medium text-[#B24A5A] disabled:cursor-not-allowed disabled:opacity-60"
                  :disabled="isSubmitting"
                  @click.stop="handleDelete(question.uuid)"
                >
                  삭제
                </button>
                <div class="rounded-full border border-[#1A2A4F]/10 bg-white px-4 py-2 text-sm font-medium text-[#1A2A4F]/70">
                  자세히 보기
                </div>
              </div>
            </div>
          </article>

          <div v-if="!pageError && questions.length === 0" class="luxe-card-soft p-6 text-sm font-medium text-[#1A2A4F]/60">
            아직 등록된 질문이 없어요. 첫 질문을 남겨보세요.
          </div>
        </section>
      </template>
    </main>
  </div>
</template>
