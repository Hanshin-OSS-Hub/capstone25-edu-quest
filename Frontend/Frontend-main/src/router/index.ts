import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../pages/HomePage.vue'
import LandingPage from '../pages/LandingPage.vue'
import LoginPage from '../pages/LoginPage.vue'
import SignupPage from '../pages/Signup.vue'
import GamePage from '../pages/GamePage.vue'
import ProgressPage from '../pages/Progress.vue'
import CommunityPage from '../pages/CommunityPage.vue'
import MyPage from '../pages/MyPage.vue'
import ReviewPage from '../pages/ReviewPage.vue'
import NoticePage from '../pages/NoticePage.vue'
import IncorrectNotePage from '../pages/IncorrectNotePage.vue'
import BookmarkPage from '../pages/BookmarkPage.vue'
import StagePage from '../pages/StagePage.vue'
import AdminPage from '../pages/AdminPage.vue'
import ServiceIntroPage from '../pages/ServiceIntroPage.vue'
import PrivacyPolicyPage from '../pages/PrivacyPolicyPage.vue'
import TermsOfServicePage from '../pages/TermsOfServicePage.vue'
import { useAuthStore } from '../store/auth'

const isAdminRole = (role?: string | null) => {
  const normalized = role?.trim().toLowerCase()
  return normalized === 'admin' || normalized === 'admine' || normalized === 'role_admin'
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: LandingPage, meta: { guestLanding: true } },
    { path: '/home', component: HomePage, meta: { requiresAuth: true } },
    { path: '/login', component: LoginPage, meta: { guestOnly: true } },
    { path: '/signup', component: SignupPage, meta: { guestOnly: true } },
    { path: '/game', component: GamePage, meta: { requiresAuth: true } },
    { path: '/progress', component: ProgressPage, meta: { requiresAuth: true } },
    { path: '/community', component: CommunityPage, meta: { requiresAuth: true } },
    { path: '/mypage', component: MyPage, meta: { requiresAuth: true } },
    { path: '/review', component: ReviewPage, meta: { requiresAuth: true } },
    { path: '/notice', component: NoticePage, meta: { requiresAuth: true } },
    { path: '/terms', component: TermsOfServicePage },
    { path: '/privacy', component: PrivacyPolicyPage },
    { path: '/incorrect-note', component: IncorrectNotePage, meta: { requiresAuth: true } },
    { path: '/bookmark', component: BookmarkPage, meta: { requiresAuth: true } },
    { path: '/stage', component: StagePage, meta: { requiresAuth: true } },
    { path: '/service-intro', component: ServiceIntroPage },
    { path: '/unity', redirect: '/game' },
    {
      path: '/admin',
      component: AdminPage,
      meta: { requiresAdmin: true },
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await auth.restoreAuth(to.path)

  if (to.meta.guestLanding && auth.state.isLoggedIn) {
    return '/home'
  }

  if (to.meta.guestOnly && auth.state.isLoggedIn) {
    return '/home'
  }

  if (to.meta.requiresAuth && !auth.state.isLoggedIn) {
    return '/login'
  }

  if (to.meta.requiresAdmin) {
    if (!auth.state.isLoggedIn) {
      return '/login'
    }

    const role = auth.state.user?.role
    if (!isAdminRole(role)) {
      return '/home'
    }
  }

  return true
})

export default router
