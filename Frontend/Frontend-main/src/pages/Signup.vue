<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '../api/auth'

type FieldKey =
  | 'id'
  | 'password'
  | 'password_confirm'
  | 'nickname'
  | 'email'
  | 'birth'
  | 'profile_image'

const EMAIL_DOMAIN_CUSTOM = 'custom'

const router = useRouter()
const isLoading = ref(false)
const submitError = ref('')
const profileImage = ref<File | null>(null)
const profileImageInput = ref<HTMLInputElement | null>(null)
const emailLocalPart = ref('')
const emailDomain = ref('gmail.com')
const customEmailDomain = ref('')

const emailDomainOptions = [
  { label: 'gmail.com', value: 'gmail.com' },
  { label: 'naver.com', value: 'naver.com' },
  { label: 'daum.net', value: 'daum.net' },
  { label: 'kakao.com', value: 'kakao.com' },
  { label: '직접 입력', value: EMAIL_DOMAIN_CUSTOM },
]

const fieldErrors = ref<Record<FieldKey, string>>({
  id: '',
  password: '',
  password_confirm: '',
  nickname: '',
  email: '',
  birth: '',
  profile_image: '',
})

const formData = ref({
  id: '',
  email: '',
  password: '',
  password_confirm: '',
  birth: '',
  nickname: '',
})

const errorTextClass = 'mt-2 text-sm font-bold text-[#D16A6A]'

const inputClass = (field: FieldKey) =>
  [
    'w-full rounded-[22px] border bg-[#FFF2EF] px-4 py-4 focus:outline-none focus:ring-2',
    fieldErrors.value[field]
      ? 'border-[#D16A6A] ring-2 ring-[#FFD4D4] focus:border-[#D16A6A] focus:ring-[#FFD4D4]'
      : 'border-[#F7A5A5] focus:border-[#1A2A4F] focus:ring-[#FFDBB6]',
  ].join(' ')

const resetFieldErrors = () => {
  fieldErrors.value = {
    id: '',
    password: '',
    password_confirm: '',
    nickname: '',
    email: '',
    birth: '',
    profile_image: '',
  }
}

const setFieldError = (field: FieldKey, message: string) => {
  fieldErrors.value[field] = message
}

const isValidEmail = (email: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)

const buildEmail = () => {
  const local = emailLocalPart.value.trim()
  const domain =
    emailDomain.value === EMAIL_DOMAIN_CUSTOM
      ? customEmailDomain.value.trim()
      : emailDomain.value.trim()

  if (!local || !domain) {
    return ''
  }

  return `${local}@${domain}`
}

const validateForm = () => {
  resetFieldErrors()
  submitError.value = ''
  formData.value.email = buildEmail()

  const id = formData.value.id.trim()
  const nickname = formData.value.nickname.trim()
  const email = formData.value.email.trim()
  const password = formData.value.password
  const passwordConfirm = formData.value.password_confirm
  const birth = formData.value.birth

  if (!id) {
    setFieldError('id', '아이디를 입력해 주세요.')
    return false
  }

  if (!password) {
    setFieldError('password', '비밀번호를 입력해 주세요.')
    return false
  }

  if (password.length < 8) {
    setFieldError('password', '비밀번호는 8자 이상으로 입력해 주세요.')
    return false
  }

  if (!passwordConfirm) {
    setFieldError('password_confirm', '비밀번호 확인을 입력해 주세요.')
    return false
  }

  if (password !== passwordConfirm) {
    setFieldError('password_confirm', '비밀번호가 일치하지 않습니다.')
    return false
  }

  if (!nickname) {
    setFieldError('nickname', '닉네임을 입력해 주세요.')
    return false
  }

  if (!birth) {
    setFieldError('birth', '생년월일을 선택해 주세요.')
    return false
  }

  if (!emailLocalPart.value.trim()) {
    setFieldError('email', '이메일을 입력해 주세요.')
    return false
  }

  if (emailDomain.value === EMAIL_DOMAIN_CUSTOM && !customEmailDomain.value.trim()) {
    setFieldError('email', '이메일 도메인을 입력해 주세요.')
    return false
  }

  if (!email) {
    setFieldError('email', '이메일을 입력해 주세요.')
    return false
  }

  if (!isValidEmail(email)) {
    setFieldError('email', '이메일 형식이 올바르지 않습니다.')
    return false
  }

  return true
}

const clearFieldError = (field: FieldKey) => {
  if (fieldErrors.value[field]) {
    fieldErrors.value[field] = ''
  }
  submitError.value = ''
}

const handleEmailDomainChange = () => {
  if (emailDomain.value !== EMAIL_DOMAIN_CUSTOM) {
    customEmailDomain.value = ''
  }
  clearFieldError('email')
}

const handleFileChange = (event: Event) => {
  profileImage.value = (event.target as HTMLInputElement).files?.[0] ?? null
  clearFieldError('profile_image')
  submitError.value = ''
}

const clearProfileImage = () => {
  profileImage.value = null
  fieldErrors.value.profile_image = ''
  if (profileImageInput.value) {
    profileImageInput.value.value = ''
  }
}

const handleSignup = async () => {
  if (!validateForm()) {
    return
  }

  isLoading.value = true
  submitError.value = ''

  try {
    const payload = new FormData()
    payload.append(
      'profile',
      new Blob(
        [
          JSON.stringify({
            id: formData.value.id.trim(),
            email: formData.value.email.trim(),
            password: formData.value.password,
            password_valid: formData.value.password_confirm,
            birth: formData.value.birth,
            nickname: formData.value.nickname.trim(),
          }),
        ],
        { type: 'application/json' }
      )
    )

    if (profileImage.value) {
      payload.append('profileImage', profileImage.value)
    }

    await authAPI.signUp(payload)
    await router.push('/login')
  } catch (error: any) {
    submitError.value =
      error.response?.data?.details ??
      error.response?.data?.message ??
      '회원가입에 실패했습니다. 다시 시도해 주세요.'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-[linear-gradient(180deg,#fff8f5_0%,#fff2ef_100%)] px-4 py-10">
    <div class="mx-auto flex min-h-[calc(100vh-5rem)] max-w-6xl items-center justify-center">
      <section
        class="mx-auto w-full max-w-xl rounded-[36px] border border-[#1A2A4F]/10 bg-white p-8 shadow-[0_18px_44px_rgba(26,42,79,0.08)] sm:p-10"
      >
        <div class="text-center">
          <p class="text-sm font-bold uppercase tracking-[0.24em] text-[#1A2A4F]/55">Create Account</p>
          <h2 class="mt-3 text-3xl font-black text-[#1A2A4F]">회원가입</h2>
        </div>

        <form class="mt-8 space-y-5" novalidate @submit.prevent="handleSignup">
          <div class="space-y-4">
            <div>
              <label class="mb-2 block text-sm font-bold text-[#1A2A4F]">아이디</label>
              <input
                v-model="formData.id"
                type="text"
                placeholder="아이디를 입력하세요"
                :class="inputClass('id')"
                @input="clearFieldError('id')"
              >
              <p v-if="fieldErrors.id" :class="errorTextClass">{{ fieldErrors.id }}</p>
            </div>

            <div>
              <label class="mb-2 block text-sm font-bold text-[#1A2A4F]">비밀번호</label>
              <input
                v-model="formData.password"
                type="password"
                placeholder="비밀번호를 입력하세요"
                :class="inputClass('password')"
                @input="clearFieldError('password')"
              >
              <p v-if="fieldErrors.password" :class="errorTextClass">{{ fieldErrors.password }}</p>
            </div>

            <div>
              <label class="mb-2 block text-sm font-bold text-[#1A2A4F]">비밀번호 확인</label>
              <input
                v-model="formData.password_confirm"
                type="password"
                placeholder="비밀번호를 다시 입력하세요"
                :class="inputClass('password_confirm')"
                @input="clearFieldError('password_confirm')"
              >
              <p v-if="fieldErrors.password_confirm" :class="errorTextClass">
                {{ fieldErrors.password_confirm }}
              </p>
            </div>
          </div>

          <div class="grid gap-4 sm:grid-cols-2">
            <div>
              <label class="mb-2 block text-sm font-bold text-[#1A2A4F]">닉네임</label>
              <input
                v-model="formData.nickname"
                type="text"
                placeholder="닉네임"
                :class="inputClass('nickname')"
                @input="clearFieldError('nickname')"
              >
              <p v-if="fieldErrors.nickname" :class="errorTextClass">{{ fieldErrors.nickname }}</p>
            </div>

            <div>
              <label class="mb-2 block text-sm font-bold text-[#1A2A4F]">생년월일</label>
              <input
                v-model="formData.birth"
                type="date"
                :class="inputClass('birth')"
                @input="clearFieldError('birth')"
              >
              <p v-if="fieldErrors.birth" :class="errorTextClass">{{ fieldErrors.birth }}</p>
            </div>
          </div>

          <div>
            <label class="mb-2 block text-sm font-bold text-[#1A2A4F]">이메일</label>
            <div class="space-y-3">
              <div class="grid grid-cols-[minmax(0,1.15fr)_auto_minmax(0,1fr)] items-center gap-2">
                <input
                  v-model="emailLocalPart"
                  type="text"
                  placeholder="이메일 아이디"
                  :class="inputClass('email')"
                  @input="clearFieldError('email')"
                >
                <span class="text-base font-black text-[#1A2A4F]">@</span>
                <select
                  v-model="emailDomain"
                  :class="inputClass('email')"
                  @change="handleEmailDomainChange"
                >
                  <option
                    v-for="option in emailDomainOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </option>
                </select>
              </div>
              <input
                v-if="emailDomain === EMAIL_DOMAIN_CUSTOM"
                v-model="customEmailDomain"
                type="text"
                placeholder="도메인을 직접 입력하세요"
                :class="inputClass('email')"
                @input="clearFieldError('email')"
              >
            </div>
            <p v-if="fieldErrors.email" :class="errorTextClass">{{ fieldErrors.email }}</p>
          </div>

          <div>
            <div class="mb-2 flex items-center justify-between">
              <label class="block text-sm font-bold text-[#1A2A4F]">프로필 사진 선택</label>
              <button
                v-if="profileImage"
                type="button"
                class="text-sm font-bold text-[#D16A6A] transition hover:text-[#B94F4F]"
                @click="clearProfileImage"
              >
                삭제
              </button>
            </div>
            <label
              class="flex cursor-pointer items-center justify-between rounded-[22px] border border-[#F7A5A5] bg-[#FFF2EF] px-4 py-4 text-[#1A2A4F] transition hover:border-[#1A2A4F] hover:bg-[#FFF7F2]"
            >
              <span class="truncate pr-4 text-sm sm:text-base">
                {{ profileImage ? profileImage.name : '선택하지 않아도 가입할 수 있어요' }}
              </span>
              <span class="rounded-full bg-[#FFDBB6] px-4 py-2 text-sm font-bold text-[#1A2A4F]">
                파일 선택
              </span>
              <input
                ref="profileImageInput"
                type="file"
                accept="image/*"
                class="hidden"
                @change="handleFileChange"
              >
            </label>
            <p v-if="fieldErrors.profile_image" :class="errorTextClass">{{ fieldErrors.profile_image }}</p>
          </div>

          <p v-if="submitError" :class="errorTextClass">{{ submitError }}</p>

          <button
            type="submit"
            :disabled="isLoading"
            class="mt-2 w-full rounded-full bg-[#1A2A4F] px-6 py-4 text-base font-black text-white transition hover:bg-[#233868] disabled:bg-slate-400"
          >
            {{ isLoading ? '가입 중...' : '회원가입' }}
          </button>
        </form>

        <div class="mt-6 text-center text-sm text-slate-600">
          이미 계정이 있으신가요?
          <button
            type="button"
            class="ml-2 font-bold text-[#1A2A4F]"
            @click="router.push('/login')"
          >
            로그인
          </button>
        </div>
      </section>
    </div>
  </div>
</template>
