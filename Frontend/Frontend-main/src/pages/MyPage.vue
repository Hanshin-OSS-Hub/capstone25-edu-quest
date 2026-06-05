<script setup lang="ts">
import axios from 'axios'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Navbar from '../components/Navbar.vue'
import PageHeader from '../components/PageHeader.vue'
import { userAPI, type UserProfile } from '../api/auth'
import { progressAPI } from '../api/learning'
import { wrongNoteAPI } from '../api/wrong_note'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const profile = ref<UserProfile | null>(null)
const isLoading = ref(true)
const error = ref('')
const solvedCount = ref(0)
const wrongCount = ref(0)
const clearedStageCount = ref(0)
const totalStageCount = ref(0)
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  newPasswordConfirm: '',
})
const passwordMessage = ref('')
const passwordError = ref('')
const isUpdatingPassword = ref(false)
const isPasswordModalOpen = ref(false)

type AttendanceEntry =
  | boolean
  | {
      attended?: boolean
      is_attended?: boolean
      checked?: boolean
      day?: string
      date?: string
    }

type ProfileWithAttendance = UserProfile & {
  attendance_rate?: number
  attendanceRate?: number
  weekly_attendance?: AttendanceEntry[]
  weeklyAttendance?: AttendanceEntry[]
  attendance?: {
    rate?: number
    attendance_rate?: number
    weekly?: AttendanceEntry[]
  }
}

const attendanceLabels = ['일', '월', '화', '수', '목', '금', '토']

const profileImageUrl = computed(
  () =>
    profile.value?.profile_image_url ??
    profile.value?.profile_url ??
    profile.value?.profile_image ??
    profile.value?.avatar_url ??
    profile.value?.profile
)

const displayName = computed(() => profile.value?.nickname ?? auth.state.user?.nickname ?? '학습자')
const displayUserId = computed(() => profile.value?.user_id ?? profile.value?.id ?? auth.state.user?.user_id ?? '-')
const displayBirth = computed(() => {
  const birth = profile.value?.birth?.trim()
  if (!birth) {
    return '미등록'
  }

  const matched = birth.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (matched) {
    const [, year, month, day] = matched
    return `${year}.${month}.${day}`
  }

  const parsed = new Date(birth)
  if (Number.isNaN(parsed.getTime())) {
    return birth
  }

  return parsed.toLocaleDateString('ko-KR')
})

const totalPoints = computed(() => profile.value?.wallet?.balance ?? profile.value?.point ?? 0)
const totalAttempts = computed(() => solvedCount.value + wrongCount.value)
const correctRate = computed(() => {
  if (totalAttempts.value === 0) {
    return 0
  }

  return Math.round((solvedCount.value / totalAttempts.value) * 100)
})
const incorrectRate = computed(() => 100 - correctRate.value)
const attendanceEntries = computed(() => {
  const profileWithAttendance = profile.value as ProfileWithAttendance | null
  const source =
    profileWithAttendance?.weekly_attendance ??
    profileWithAttendance?.weeklyAttendance ??
    profileWithAttendance?.attendance?.weekly

  if (!Array.isArray(source)) {
    return []
  }

  return source.slice(0, 7).map((entry, index) => {
    if (typeof entry === 'boolean') {
      return {
        label: attendanceLabels[index] ?? `${index + 1}`,
        attended: entry,
      }
    }

    return {
      label: entry.day ?? entry.date ?? attendanceLabels[index] ?? `${index + 1}`,
      attended: entry.attended ?? entry.is_attended ?? entry.checked ?? false,
    }
  })
})
const attendanceCount = computed(() =>
  attendanceEntries.value.length === 0 ? null : attendanceEntries.value.filter((entry) => entry.attended).length
)
const attendanceRate = computed(() => {
  const profileWithAttendance = profile.value as ProfileWithAttendance | null
  const profileRate =
    profileWithAttendance?.attendance_rate ??
    profileWithAttendance?.attendanceRate ??
    profileWithAttendance?.attendance?.rate ??
    profileWithAttendance?.attendance?.attendance_rate

  if (typeof profileRate === 'number') {
    return Math.round(profileRate)
  }

  if (attendanceCount.value === null || attendanceEntries.value.length === 0) {
    return null
  }

  return Math.round((attendanceCount.value / attendanceEntries.value.length) * 100)
})
const attendanceRateText = computed(() => (attendanceRate.value === null ? '-' : `${attendanceRate.value}%`))
const attendanceSummaryText = computed(() =>
  attendanceCount.value === null ? '출석 데이터가 없습니다' : `이번 주 출석 ${attendanceCount.value}일`
)
const progressRate = computed(() => {
  if (totalStageCount.value === 0) {
    return 0
  }

  return Math.round((clearedStageCount.value / totalStageCount.value) * 100)
})
const correctBarWidth = computed(() => `${correctRate.value}%`)
const wrongBarWidth = computed(() => `${incorrectRate.value}%`)
const progressBarWidth = computed(() => `${progressRate.value}%`)
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/

const getApiErrorMessage = (requestError: unknown, fallback: string) => {
  if (!axios.isAxiosError(requestError)) {
    return fallback
  }

  const responseData = requestError.response?.data as { message?: string; error?: string } | undefined
  return responseData?.message ?? responseData?.error ?? fallback
}

const clearPasswordFeedback = () => {
  passwordMessage.value = ''
  passwordError.value = ''
}

const resetPasswordForm = () => {
  passwordForm.value = {
    currentPassword: '',
    newPassword: '',
    newPasswordConfirm: '',
  }
}

const openPasswordModal = () => {
  clearPasswordFeedback()
  resetPasswordForm()
  isPasswordModalOpen.value = true
}

const closeAccountModal = () => {
  if (isUpdatingPassword.value) {
    return
  }

  isPasswordModalOpen.value = false
  clearPasswordFeedback()
  resetPasswordForm()
}

const handlePasswordUpdate = async () => {
  clearPasswordFeedback()

  const currentPassword = passwordForm.value.currentPassword
  const newPassword = passwordForm.value.newPassword
  const newPasswordConfirm = passwordForm.value.newPasswordConfirm

  if (!currentPassword) {
    passwordError.value = '현재 비밀번호를 입력해 주세요.'
    return
  }

  if (!passwordPattern.test(newPassword)) {
    passwordError.value = '새 비밀번호는 8자 이상이며 영문, 숫자, 특수문자를 모두 포함해야 합니다.'
    return
  }

  if (newPassword !== newPasswordConfirm) {
    passwordError.value = '새 비밀번호 확인이 일치하지 않습니다.'
    return
  }

  isUpdatingPassword.value = true
  try {
    await userAPI.updatePassword({
      currentPassword,
      newPassword,
      newPasswordConfirm,
    })

    passwordMessage.value = '비밀번호가 변경되었습니다. 다시 로그인해 주세요.'
    resetPasswordForm()

    window.setTimeout(() => {
      auth.logout()
      void router.push('/login')
    }, 900)
  } catch (requestError) {
    passwordError.value = getApiErrorMessage(requestError, '비밀번호 변경에 실패했습니다.')
  } finally {
    isUpdatingPassword.value = false
  }
}

onMounted(async () => {
  await auth.restoreAuth(route.path)
  if (!auth.state.accessToken || !auth.state.user) {
    await router.push('/login')
    return
  }

  try {
    const [profileResponse, progressResponse, wrongNotesResponse] = await Promise.all([
      userAPI.getProfile(auth.state.user.uuid),
      progressAPI.getProgress(auth.state.user.uuid),
      wrongNoteAPI.getUserWrongNotes(auth.state.user.uuid, {
        page: 0,
        size: 100,
        sort: 'created_at',
        is_asc: false,
      }),
    ])

    profile.value = profileResponse
    solvedCount.value = progressResponse.results.reduce((sum, stage) => sum + stage.clear.length, 0)
    wrongCount.value = wrongNotesResponse.results.length
    totalStageCount.value = progressResponse.results.length
    clearedStageCount.value = progressResponse.results.filter((stage) => {
      const total = stage.totalQuestionCount ?? stage.total_question_count ?? 0
      return total > 0 && stage.clear.length === total
    }).length
  } catch (loadError) {
    console.error(loadError)
    error.value = '마이페이지 정보를 불러오지 못했습니다.'
  } finally {
    isLoading.value = false
  }
})
</script>

<template>
  <div class="min-h-screen bg-[#FFF2EF] py-10">
    <Navbar />
    <PageHeader
      title="마이페이지"
      subtitle="학습 현황과 계정 정보를 한눈에 확인해 보세요."
      @back="router.back()"
    />

    <main class="mx-auto mt-6 max-w-7xl px-4 sm:px-6 lg:px-8">
      <div
        v-if="error"
        class="luxe-card mb-6 p-5 text-sm font-medium text-[#1A2A4F]"
      >
        {{ error }}
      </div>

      <div
        v-if="isLoading"
        class="luxe-panel p-10 text-center"
      >
        <div class="mx-auto mb-4 h-14 w-14 animate-spin rounded-full border-2 border-[#FFDBB6] border-b-[#1A2A4F]" />
        <p class="font-medium text-[#1A2A4F]">마이페이지를 불러오는 중입니다.</p>
      </div>

      <div v-else class="space-y-6">
        <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <article class="luxe-card p-5">
            <p class="text-sm font-medium text-[#1A2A4F]/55">보유 코인</p>
            <p class="mt-3 text-3xl font-semibold text-[#1A2A4F]">{{ totalPoints }}</p>
          </article>
          <article class="luxe-card p-5">
            <p class="text-sm font-medium text-[#1A2A4F]/55">해결한 문제</p>
            <p class="mt-3 text-3xl font-semibold text-[#1A2A4F]">{{ solvedCount }}</p>
          </article>
          <article class="luxe-card p-5">
            <p class="text-sm font-medium text-[#1A2A4F]/55">오답 노트</p>
            <p class="mt-3 text-3xl font-semibold text-[#1A2A4F]">{{ wrongCount }}</p>
          </article>
          <article class="luxe-card p-5">
            <p class="text-sm font-medium text-[#1A2A4F]/55">스테이지 진행률</p>
            <p class="mt-3 text-3xl font-semibold text-[#1A2A4F]">{{ progressRate }}%</p>
          </article>
        </section>

        <section class="grid gap-6 xl:grid-cols-[0.88fr_1.12fr]">
          <article class="luxe-panel p-8">
            <div class="flex flex-col items-center text-center">
              <div class="flex h-32 w-32 items-center justify-center overflow-hidden rounded-full border border-[#1A2A4F]/10 bg-[#FFF6EC] text-4xl font-semibold text-[#1A2A4F] shadow-[0_12px_30px_rgba(15,23,42,0.08)]">
                <img v-if="profileImageUrl" :src="profileImageUrl" alt="프로필 이미지" class="h-full w-full object-cover">
                <span v-else>{{ displayName[0] }}</span>
              </div>
              <h2 class="mt-5 text-3xl font-semibold tracking-[-0.03em] text-[#1A2A4F]">{{ displayName }}</h2>
              <p class="mt-2 luxe-pill bg-white px-4 py-2 text-sm font-medium text-[#1A2A4F]">
                {{ profile?.role ?? 'user' }}
              </p>
            </div>

            <div class="mt-8 grid gap-3">
              <div class="luxe-card-soft flex items-center justify-between gap-4 p-4">
                <span class="text-sm font-medium text-[#1A2A4F]/55">아이디</span>
                <span class="text-right font-medium text-[#1A2A4F]">{{ displayUserId }}</span>
              </div>
              <div class="luxe-card-soft flex items-center justify-between gap-4 p-4">
                <span class="text-sm font-medium text-[#1A2A4F]/55">생년월일</span>
                <span class="text-right font-medium text-[#1A2A4F]">{{ displayBirth }}</span>
              </div>
              <div class="luxe-card-soft p-4">
                <div class="mb-4">
                  <p class="text-sm font-medium text-[#1A2A4F]/55">Account Settings</p>
                  <h3 class="mt-1 text-xl font-semibold text-[#1A2A4F]">계정 설정</h3>
                </div>

                <div class="grid gap-3">
                  <div class="rounded-[18px] border border-[#1A2A4F]/8 bg-white/70 p-4">
                    <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                      <div>
                        <p class="font-semibold text-[#1A2A4F]">비밀번호 변경</p>
                        <p class="mt-1 text-sm leading-6 text-[#1A2A4F]/60">
                          계정 보안을 위해 비밀번호를 변경할 수 있습니다.
                        </p>
                      </div>
                      <button
                        type="button"
                        class="luxe-button-soft shrink-0 cursor-pointer rounded-full px-4 py-2 text-sm font-medium"
                        @click="openPasswordModal"
                      >
                        변경하기
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </article>

          <div class="grid gap-6">
            <article class="luxe-panel p-8">
              <div class="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                <div>
                  <p class="text-sm font-medium text-[#1A2A4F]/55">Learning Summary</p>
                  <h3 class="mt-1 text-2xl font-semibold text-[#1A2A4F]">학습 요약</h3>
                </div>
                <p class="luxe-pill bg-[#FFF6EC] px-4 py-2 text-sm font-medium text-[#1A2A4F]">
                  완료 스테이지 {{ clearedStageCount }} / {{ totalStageCount }}
                </p>
              </div>

              <div class="mt-6 grid gap-5 lg:grid-cols-2">
                <div class="luxe-card-soft p-5">
                  <div class="flex items-center justify-between">
                    <p class="text-sm font-medium text-[#1A2A4F]/55">정답률</p>
                    <p class="text-2xl font-semibold text-[#1A2A4F]">{{ correctRate }}%</p>
                  </div>
                  <div class="mt-4 space-y-4">
                    <div>
                      <div class="mb-2 flex items-center justify-between text-sm font-medium text-[#1A2A4F]">
                        <span>맞힌 문제</span>
                        <span>{{ solvedCount }}</span>
                      </div>
                      <div class="h-3 rounded-full bg-[#F3E7E1]">
                        <div class="h-3 rounded-full bg-[linear-gradient(90deg,#ffcfaa,#f3ab9b)]" :style="{ width: correctBarWidth }" />
                      </div>
                    </div>
                    <div>
                      <div class="mb-2 flex items-center justify-between text-sm font-medium text-[#1A2A4F]">
                        <span>틀린 문제</span>
                        <span>{{ wrongCount }}</span>
                      </div>
                      <div class="h-3 rounded-full bg-[#F3E7E1]">
                        <div class="h-3 rounded-full bg-[linear-gradient(90deg,#f4b6b6,#e59797)]" :style="{ width: wrongBarWidth }" />
                      </div>
                    </div>
                  </div>
                </div>

                <div class="luxe-card-soft p-5">
                  <div class="flex items-center justify-between">
                    <p class="text-sm font-medium text-[#1A2A4F]/55">전체 진행률</p>
                    <p class="text-2xl font-semibold text-[#1A2A4F]">{{ progressRate }}%</p>
                  </div>
                  <div class="mt-4">
                    <div class="mb-2 flex items-center justify-between text-sm font-medium text-[#1A2A4F]">
                      <span>완료한 스테이지</span>
                      <span>{{ clearedStageCount }} / {{ totalStageCount }}</span>
                    </div>
                    <div class="h-3 rounded-full bg-[#F3E7E1]">
                      <div class="h-3 rounded-full bg-[linear-gradient(90deg,#ffd8b2,#f3b98a)]" :style="{ width: progressBarWidth }" />
                    </div>
                    <div class="mt-6 grid grid-cols-2 gap-3">
                      <div class="rounded-[18px] border border-[#1A2A4F]/8 bg-white/70 p-4">
                        <p class="text-xs font-medium uppercase tracking-[0.16em] text-[#1A2A4F]/45">총 시도</p>
                        <p class="mt-2 text-2xl font-semibold text-[#1A2A4F]">{{ totalAttempts }}</p>
                      </div>
                      <div class="rounded-[18px] border border-[#1A2A4F]/8 bg-white/70 p-4">
                        <p class="text-xs font-medium uppercase tracking-[0.16em] text-[#1A2A4F]/45">출석률</p>
                        <p class="mt-2 text-2xl font-semibold text-[#1A2A4F]">{{ attendanceRateText }}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </article>

            <article class="luxe-panel p-8">
              <div class="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                <div>
                  <p class="text-sm font-medium text-[#1A2A4F]/55">Weekly Activity</p>
                  <h3 class="mt-1 text-2xl font-semibold text-[#1A2A4F]">주간 활동</h3>
                </div>
                <p class="text-sm font-medium text-[#1A2A4F]/55">{{ attendanceSummaryText }}</p>
              </div>

              <div
                v-if="attendanceEntries.length === 0"
                class="mt-6 rounded-[24px] border border-dashed border-[#1A2A4F]/14 bg-[#FFF8F4] p-6 text-sm font-medium text-[#1A2A4F]/65"
              >
                아직 출석 데이터가 없습니다.
              </div>

              <div v-else class="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-4 lg:grid-cols-7">
                <div
                  v-for="entry in attendanceEntries"
                  :key="entry.label"
                  class="rounded-[22px] border border-[#1A2A4F]/8 p-4 text-center shadow-[0_8px_20px_rgba(15,23,42,0.04)]"
                  :class="entry.attended ? 'bg-white' : 'bg-[#FFF8F4]'"
                >
                  <div
                    class="mx-auto flex h-12 w-12 items-center justify-center rounded-full text-sm font-semibold"
                    :class="entry.attended ? 'bg-[#FFF1E3] text-[#1A2A4F]' : 'bg-white text-[#1A2A4F]/45'"
                  >
                    {{ entry.attended ? 'ON' : 'OFF' }}
                  </div>
                  <p class="mt-3 text-sm font-medium text-[#1A2A4F]">{{ entry.label }}</p>
                </div>
              </div>
            </article>
          </div>
        </section>

      </div>
    </main>

    <div
      v-if="isPasswordModalOpen"
      class="fixed inset-0 z-50 flex items-center justify-center bg-[#1A2A4F]/45 px-4 py-6 backdrop-blur-sm"
      @click.self="closeAccountModal"
    >
      <form
        class="luxe-panel w-full max-w-lg p-6 sm:p-7"
        @submit.prevent="handlePasswordUpdate"
      >
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="text-sm font-medium text-[#1A2A4F]/55">Account Settings</p>
            <h3 class="mt-1 text-2xl font-semibold text-[#1A2A4F]">비밀번호 변경</h3>
          </div>
          <button
            type="button"
            class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-sm font-medium"
            @click="closeAccountModal"
          >
            취소
          </button>
        </div>

        <div class="mt-6 grid gap-4">
          <label class="block">
            <span class="mb-2 block text-sm font-medium text-[#1A2A4F]">현재 비밀번호</span>
            <input
              v-model="passwordForm.currentPassword"
              type="password"
              autocomplete="current-password"
              class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F]"
              placeholder="현재 비밀번호"
              @input="clearPasswordFeedback"
            >
          </label>

          <label class="block">
            <span class="mb-2 block text-sm font-medium text-[#1A2A4F]">새 비밀번호</span>
            <input
              v-model="passwordForm.newPassword"
              type="password"
              autocomplete="new-password"
              class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F]"
              placeholder="NewPassword123!"
              @input="clearPasswordFeedback"
            >
          </label>

          <label class="block">
            <span class="mb-2 block text-sm font-medium text-[#1A2A4F]">새 비밀번호 확인</span>
            <input
              v-model="passwordForm.newPasswordConfirm"
              type="password"
              autocomplete="new-password"
              class="luxe-input w-full rounded-[18px] px-4 py-3 text-[#1A2A4F]"
              placeholder="NewPassword123!"
              @input="clearPasswordFeedback"
            >
          </label>
        </div>

        <p v-if="passwordError" class="mt-4 text-sm font-medium text-[#B24A5A]">{{ passwordError }}</p>
        <p v-if="passwordMessage" class="mt-4 text-sm font-medium text-[#1A2A4F]">{{ passwordMessage }}</p>

        <div class="mt-6 flex justify-end gap-3">
          <button
            type="button"
            class="luxe-button-soft cursor-pointer rounded-full px-5 py-3 text-sm font-medium"
            @click="closeAccountModal"
          >
            취소
          </button>
          <button
            type="submit"
            :disabled="isUpdatingPassword"
            class="luxe-button-accent cursor-pointer rounded-full px-5 py-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-60"
          >
            {{ isUpdatingPassword ? '변경 중...' : '변경 완료' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
