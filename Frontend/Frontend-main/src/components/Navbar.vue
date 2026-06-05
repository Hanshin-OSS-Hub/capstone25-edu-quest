<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '../api/auth'
import { useAuthStore } from '../store/auth'
import { decodeJwtRole } from '../utils/jwt'

const router = useRouter()
const auth = useAuthStore()
const isLoggedIn = computed(() => auth.state.isLoggedIn)
const normalizedRole = computed(() => {
  const tokenRole = auth.state.accessToken ? decodeJwtRole(auth.state.accessToken) : null
  const role = tokenRole ?? auth.state.user?.role ?? ''
  return role.trim().toLowerCase()
})
const isAdmin = computed(() => {
  return normalizedRole.value === 'admin' || normalizedRole.value === 'admine' || normalizedRole.value === 'role_admin'
})

const handleLogout = async () => {
  try {
    await authAPI.logout()
  } catch (error) {
    console.error('logout failed:', error)
  } finally {
    auth.logout()
    await router.push('/')
  }
}
</script>

<template>
  <nav class="sticky top-0 z-50 border-b border-[#1A2A4F]/8 bg-white/72 backdrop-blur-xl">
    <div class="mx-auto max-w-7xl px-4 py-4 sm:px-6 lg:px-8">
      <div class="flex flex-col gap-4 lg:grid lg:grid-cols-[1fr_auto_1fr] lg:items-center">
        <div class="flex items-center justify-between gap-4">
          <RouterLink to="/" class="flex items-center gap-3">
            <img src="/logo-transparent.png" alt="EduQuest logo" class="h-11 w-11 object-contain" />
            <div>
              <p class="text-2xl font-semibold tracking-[-0.03em] text-[#1A2A4F]">EduQuest</p>
            </div>
          </RouterLink>

          <div class="flex items-center gap-2 lg:hidden">
            <RouterLink
              v-if="!isLoggedIn"
              to="/login"
              class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-sm font-medium"
            >
              로그인
            </RouterLink>
            <button
              v-else
              type="button"
              class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-sm font-medium"
              @click="handleLogout"
            >
              로그아웃
            </button>
          </div>
        </div>

        <div class="flex justify-center">
          <div class="flex flex-wrap items-center justify-center gap-2 rounded-full border border-[#1A2A4F]/8 bg-white/70 px-2 py-2 shadow-[0_8px_24px_rgba(15,23,42,0.04)] backdrop-blur">
            <RouterLink to="/stage" class="rounded-full px-4 py-2 text-sm font-medium text-[#1A2A4F] transition hover:bg-[#FFF8F4]">
              스테이지
            </RouterLink>
            <RouterLink to="/progress" class="rounded-full px-4 py-2 text-sm font-medium text-[#1A2A4F] transition hover:bg-[#FFF8F4]">
              학습 현황
            </RouterLink>
            <RouterLink to="/incorrect-note" class="rounded-full px-4 py-2 text-sm font-medium text-[#1A2A4F] transition hover:bg-[#FFF8F4]">
              오답 노트
            </RouterLink>
            <RouterLink to="/community" class="rounded-full px-4 py-2 text-sm font-medium text-[#1A2A4F] transition hover:bg-[#FFF8F4]">
              커뮤니티
            </RouterLink>
            <RouterLink to="/notice" class="rounded-full px-4 py-2 text-sm font-medium text-[#1A2A4F] transition hover:bg-[#FFF8F4]">
              공지사항
            </RouterLink>
            <RouterLink
              v-if="isAdmin"
              to="/admin"
              class="rounded-full px-4 py-2 text-sm font-medium text-[#1A2A4F] transition hover:bg-[#FFF8F4]"
            >
              관리자
            </RouterLink>
          </div>
        </div>

        <div class="hidden justify-end lg:flex">
          <div class="flex items-center gap-2">
            <template v-if="!isLoggedIn">
              <RouterLink
                to="/login"
                class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-sm font-medium transition hover:bg-white"
              >
                로그인
              </RouterLink>
              <RouterLink
                to="/signup"
                class="luxe-button-accent cursor-pointer rounded-full px-4 py-2 text-sm font-medium transition hover:translate-y-[-1px]"
              >
                회원가입
              </RouterLink>
            </template>
            <template v-else>
              <RouterLink
                to="/mypage"
                class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-sm font-medium"
              >
                마이페이지
              </RouterLink>
              <button
                type="button"
                class="luxe-button-soft cursor-pointer rounded-full px-4 py-2 text-sm font-medium"
                @click="handleLogout"
              >
                로그아웃
              </button>
            </template>
          </div>
        </div>
      </div>
    </div>
  </nav>
</template>
