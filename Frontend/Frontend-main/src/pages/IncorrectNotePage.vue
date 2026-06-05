<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '../components/PageHeader.vue'
import { wrongNoteAPI, type WrongNote } from '../api/wrong_note'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const incorrectNotes = ref<WrongNote[]>([])
const error = ref('')
const loadingFeedback = ref<Record<string, boolean>>({})
const deletingNote = ref<Record<string, boolean>>({})
const expandedFeedback = ref<Record<string, boolean>>({})
const feedbackErrors = ref<Record<string, string>>({})

const loadWrongNotes = async () => {
  if (!auth.state.user) {
    error.value = '로그인이 필요한 기능입니다.'
    return
  }

  const response = await wrongNoteAPI.getUserWrongNotes(auth.state.user.uuid, {
    page: 0,
    size: 50,
    sort: 'created_at',
    is_asc: false,
  })

  incorrectNotes.value = response.results
}

onMounted(async () => {
  await auth.restoreAuth(route.path)

  try {
    await loadWrongNotes()
  } catch (fetchError) {
    console.error(fetchError)
    error.value = '오답노트를 불러오지 못했습니다.'
  }
})

const requestAIFeedback = async (note: WrongNote) => {
  if (!note.uuid) {
    return
  }

  loadingFeedback.value[note.uuid] = true
  feedbackErrors.value[note.uuid] = ''

  try {
    const refreshed = await wrongNoteAPI.requestAiFeedback(note.uuid)
    note.feedback = refreshed.feedback
    note.is_reviewed = refreshed.is_reviewed
    note.isReviewed = refreshed.isReviewed
  } catch (requestError) {
    feedbackErrors.value[note.uuid] = resolveAiFeedbackErrorMessage(requestError)
    console.warn('AI 피드백 요청 실패:', feedbackErrors.value[note.uuid])
  } finally {
    loadingFeedback.value[note.uuid] = false
  }
}

const resolveAiFeedbackErrorMessage = (requestError: unknown) => {
  const error = requestError as {
    response?: {
      data?: {
        message?: string
        error?: string
        details?: {
          code?: string
        }
      }
    }
  }

  const data = error.response?.data
  const serverMessage = data?.message || data?.error || ''
  const errorCode = data?.details?.code || ''

  if (errorCode === 'AI_FEEDBACK_UNAVAILABLE') {
    return serverMessage || 'AI 피드백을 생성할 수 없습니다. API 키 또는 AI 서비스 상태를 확인해 주세요.'
  }

  return serverMessage || 'AI 피드백을 불러오지 못했습니다.'
}

const deleteWrongNote = async (note: WrongNote) => {
  if (!note.uuid) {
    return
  }

  if (!window.confirm('이 오답노트를 삭제할까요?')) {
    return
  }

  deletingNote.value[note.uuid] = true

  try {
    await wrongNoteAPI.deleteWrongNote(note.uuid)
    incorrectNotes.value = incorrectNotes.value.filter((item) => item.uuid !== note.uuid)
  } catch (deleteError) {
    console.error('오답노트 삭제 실패:', deleteError)
    alert('오답노트를 삭제하지 못했습니다.')
  } finally {
    deletingNote.value[note.uuid] = false
  }
}

const toggleFeedback = (note: WrongNote) => {
  if (!note.uuid) {
    return
  }

  expandedFeedback.value[note.uuid] = !expandedFeedback.value[note.uuid]
}
</script>

<template>
  <div class="min-h-screen bg-[#FFF2EF]">
    <PageHeader
      title="오답노트"
      subtitle="틀린 답안과 AI 피드백을 다시 확인하면서 취약한 부분을 복습해 보세요."
      back-link="/home"
    />

    <main class="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <div
        v-if="error"
        class="luxe-card mb-6 p-6 text-sm font-medium text-[#1A2A4F]"
      >
        {{ error }}
      </div>

      <section v-else-if="incorrectNotes.length" class="grid grid-cols-1 gap-5">
        <article
          v-for="note in incorrectNotes"
          :key="note.uuid"
          class="luxe-card grid gap-5 p-5 transition duration-300 hover:translate-y-[-2px] lg:grid-cols-[minmax(0,0.92fr)_minmax(360px,1.08fr)] lg:items-stretch"
        >
          <div class="flex min-w-0 flex-col gap-4">
            <div class="flex items-start justify-between gap-4">
              <h2 class="min-w-0 text-2xl font-black leading-snug text-[#1A2A4F]">
                {{ note.problem_summary || note.problemSummary || '오답 기록' }}
              </h2>
              <span
                class="luxe-pill shrink-0 px-3 py-1 text-xs font-medium tracking-[0.14em] text-[#1A2A4F]"
              >
                {{ note.is_reviewed ? 'REVIEWED' : 'PENDING' }}
              </span>
            </div>

            <div class="rounded-[22px] bg-[#1A2A4F] p-4 font-mono text-sm leading-relaxed text-gray-100">
              <p class="mb-2 text-xs font-black uppercase tracking-[0.16em] text-white/55">제출한 답안</p>
              <pre class="max-h-40 overflow-auto whitespace-pre-wrap break-words font-mono">{{ note.wrong_answer || '제출한 코드가 없습니다.' }}</pre>
            </div>

            <div class="mt-auto flex flex-wrap items-end justify-between gap-3">
              <div class="flex flex-wrap gap-2">
                <button
                  v-if="note.problem_uuid || note.problemUuid"
                  class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-sm font-medium"
                  @click="router.push('/game?problem=' + (note.problem_uuid || note.problemUuid) + '&from=wrong-note')"
                >
                  다시 풀기
                </button>
                <button
                  :disabled="deletingNote[note.uuid]"
                  class="cursor-pointer rounded-full border border-[#1A2A4F]/15 bg-white/70 px-4 py-2 text-sm font-medium text-[#1A2A4F] transition hover:bg-white disabled:cursor-not-allowed disabled:opacity-60"
                  @click="deleteWrongNote(note)"
                >
                  {{ deletingNote[note.uuid] ? '삭제 중...' : '삭제' }}
                </button>
              </div>
              <p class="text-right text-xs font-medium text-slate-400">
                마지막 시도: {{ note.last_submitted_at ?? note.created_at ?? '-' }}
              </p>
            </div>
          </div>

          <div class="flex min-w-0 flex-col rounded-[24px] border border-[#1A2A4F]/10 bg-[#FFF8F4] p-4 text-sm text-[#1A2A4F]">
            <div class="mb-3 flex items-center justify-between gap-3">
              <p class="text-base font-black">AI 튜터</p>
              <div class="flex shrink-0 items-center gap-2">
                <button
                  v-if="note.feedback"
                  class="cursor-pointer rounded-full border border-[#1A2A4F]/15 bg-white/70 px-4 py-2 text-xs font-bold text-[#1A2A4F] transition hover:bg-white"
                  @click="toggleFeedback(note)"
                >
                  {{ expandedFeedback[note.uuid] ? '접기' : '펼치기' }}
                </button>
                <button
                  v-else
                  :disabled="loadingFeedback[note.uuid]"
                  class="luxe-button-accent cursor-pointer rounded-full px-4 py-2 text-xs font-medium disabled:cursor-not-allowed disabled:opacity-60"
                  @click="requestAIFeedback(note)"
                >
                  <span v-if="loadingFeedback[note.uuid]" class="flex items-center gap-2">
                    <span class="h-3 w-3 animate-spin rounded-full border-2 border-[#1A2A4F] border-t-transparent"></span>
                    분석 중...
                  </span>
                  <span v-else>피드백 받기</span>
                </button>
              </div>
            </div>

            <div v-if="note.feedback" class="relative">
              <pre
                class="overflow-hidden whitespace-pre-wrap break-words font-sans font-medium leading-relaxed transition-[max-height] duration-300"
                :class="expandedFeedback[note.uuid] ? 'max-h-[720px] overflow-auto pr-2' : 'max-h-32'"
              >{{ note.feedback }}</pre>
              <div
                v-if="!expandedFeedback[note.uuid]"
                class="pointer-events-none absolute inset-x-0 bottom-0 h-12 bg-gradient-to-t from-[#FFF8F4] to-transparent"
              ></div>
            </div>

            <div
              v-else-if="loadingFeedback[note.uuid]"
              class="flex flex-1 items-center justify-center py-8 text-center font-medium text-[#1A2A4F]/60 animate-pulse"
            >
              AI 튜터가 오답을 분석하고 있습니다...
            </div>
            <div
              v-else-if="feedbackErrors[note.uuid]"
              class="flex flex-1 items-center justify-center rounded-2xl bg-red-50 px-4 py-8 text-center font-bold text-red-700"
            >
              {{ feedbackErrors[note.uuid] }}
            </div>
            <div v-else class="flex flex-1 items-center justify-center py-8 text-center font-medium text-[#1A2A4F]/40">
              버튼을 눌러 AI 피드백을 받아 보세요.
            </div>
          </div>
        </article>
      </section>

      <section
        v-else
        class="luxe-card p-8 text-center text-sm font-medium text-[#1A2A4F]/60"
      >
        아직 저장된 오답노트가 없습니다.
      </section>
    </main>
  </div>
</template>
