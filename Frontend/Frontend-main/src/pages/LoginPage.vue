<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI, userAPI } from '../api/auth'
import { useAuthStore } from '../store/auth'
import { decodeJwtRole, decodeJwtUuid } from '../utils/jwt'

type LoginFieldKey = 'id' | 'password'

const router = useRouter()
const auth = useAuthStore()
const userId = ref('')
const password = ref('')
const showPassword = ref(false)
const submitError = ref('')
const isLoading = ref(false)
const fieldErrors = ref<Record<LoginFieldKey, string>>({
  id: '',
  password: '',
})

const errorTextClass = 'mt-2 text-sm font-bold text-[#D16A6A]'

const inputClass = (field: LoginFieldKey) =>
  [
    'w-full rounded-[22px] border bg-[#FFF2EF] px-4 py-4 focus:outline-none focus:ring-2',
    fieldErrors.value[field]
      ? 'border-[#D16A6A] ring-2 ring-[#FFD4D4] focus:border-[#D16A6A] focus:ring-[#FFD4D4]'
      : 'border-[#F7A5A5] focus:border-[#1A2A4F] focus:ring-[#FFDBB6]',
  ].join(' ')

watch(
  () => auth.state.isLoggedIn,
  async (loggedIn) => {
    if (auth.state.isAuthReady && loggedIn) {
      await router.replace('/')
    }
  },
  { immediate: true }
)

const resetFieldErrors = () => {
  fieldErrors.value = {
    id: '',
    password: '',
  }
}

const validateForm = () => {
  resetFieldErrors()
  submitError.value = ''

  if (!userId.value.trim()) {
    fieldErrors.value.id = '아이디를 입력해 주세요.'
    return false
  }

  if (!password.value) {
    fieldErrors.value.password = '비밀번호를 입력해 주세요.'
    return false
  }

  return true
}

const clearFieldError = (field: LoginFieldKey) => {
  if (fieldErrors.value[field]) {
    fieldErrors.value[field] = ''
  }
  submitError.value = ''
}

const handleLogin = async () => {
  if (!validateForm()) {
    return
  }

  isLoading.value = true
  submitError.value = ''

  try {
    const response = await authAPI.signIn({ id: userId.value.trim(), password: password.value })
    const accessToken = response.accessToken
    const userUuid =
      decodeJwtUuid(accessToken) ?? (await userAPI.getUuidById(userId.value.trim())).uuid

    if (!userUuid) {
      localStorage.removeItem('accessToken')
      throw new Error('유효하지 않은 토큰입니다.')
    }

    localStorage.setItem('accessToken', accessToken)
    const profile = await userAPI.getProfile(userUuid)

    auth.loginSuccess({
      user: {
        uuid: profile.uuid,
        user_id: profile.user_id ?? profile.id ?? '',
        nickname: profile.nickname,
        birth: profile.birth,
        role: decodeJwtRole(accessToken) ?? profile.role ?? 'user',
        is_locked: profile.is_locked,
        balance: profile.wallet?.balance ?? profile.point ?? 0,
      },
      accessToken,
    })

    await router.push('/')
  } catch (error: any) {
    console.error('login error:', error)
    localStorage.removeItem('accessToken')
    submitError.value =
      error.response?.data?.details ??
      error.response?.data?.message ??
      '로그인에 실패했습니다. 다시 시도해 주세요.'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-[linear-gradient(180deg,#fff8f5_0%,#fff2ef_100%)] px-4 py-10">
    <div class="mx-auto flex min-h-[calc(100vh-5rem)] max-w-6xl items-center justify-center">
      <section class="w-full max-w-xl rounded-[36px] border border-[#1A2A4F]/10 bg-white p-8 shadow-[0_18px_44px_rgba(26,42,79,0.08)] sm:p-10">
        <div class="text-center">
          <p class="text-sm font-bold uppercase tracking-[0.24em] text-[#1A2A4F]/55">Welcome Back</p>
          <h2 class="mt-3 text-3xl font-black text-[#1A2A4F]">로그인</h2>
          <p class="mt-2 text-sm leading-6 text-slate-600">
            이어서 학습하려면 계정 정보를 입력해 주세요.
          </p>
        </div>

        <form class="mt-8 space-y-5" novalidate @submit.prevent="handleLogin">
          <div>
            <label class="mb-2 block text-sm font-bold text-[#1A2A4F]">아이디</label>
            <input
              v-model="userId"
              type="text"
              placeholder="아이디를 입력하세요"
              :class="inputClass('id')"
              @input="clearFieldError('id')"
            >
            <p v-if="fieldErrors.id" :class="errorTextClass">{{ fieldErrors.id }}</p>
          </div>

          <div>
            <label class="mb-2 block text-sm font-bold text-[#1A2A4F]">비밀번호</label>
            <div class="relative">
              <input
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="비밀번호를 입력하세요"
                :class="[inputClass('password'), 'pr-20'].join(' ')"
                @input="clearFieldError('password')"
              >
              <button
                type="button"
                class="absolute right-4 top-1/2 -translate-y-1/2 text-sm font-bold text-[#1A2A4F]"
                @click="showPassword = !showPassword"
              >
                {{ showPassword ? '숨기기' : '보기' }}
              </button>
            </div>
            <p v-if="fieldErrors.password" :class="errorTextClass">{{ fieldErrors.password }}</p>
          </div>

          <p v-if="submitError" :class="errorTextClass">{{ submitError }}</p>

          <button
            type="submit"
            :disabled="isLoading"
            class="mt-2 w-full rounded-full bg-[#1A2A4F] px-6 py-4 text-base font-black text-white transition hover:bg-[#233868] disabled:bg-slate-400"
          >
            {{ isLoading ? '로그인 중...' : '학습 이어하기' }}
          </button>
        </form>

        <div class="mt-6 text-center text-sm text-slate-600">
          아직 계정이 없나요?
          <button type="button" class="ml-2 font-bold text-[#1A2A4F]" @click="router.push('/signup')">
            회원가입
          </button>
        </div>
      </section>
    </div>
  </div>
</template>
