<script setup lang="ts">
import PageHeader from '../components/PageHeader.vue'

const policySections = [
  {
    title: '1. 수집하는 개인정보 항목',
    body: [
      'EduQuest는 회원가입, 학습 서비스 제공, 고객 문의 대응을 위해 아이디, 비밀번호, 닉네임, 이메일 주소, 생년월일, 프로필 이미지를 수집할 수 있습니다.',
      '서비스 이용 과정에서 학습 기록, 북마크, 오답노트, 접속 로그, 기기 정보가 자동으로 생성되어 수집될 수 있습니다.',
    ],
  },
  {
    title: '2. 개인정보의 이용 목적',
    body: [
      '회원 식별, 본인 확인, 계정 관리, 학습 진도 제공, 맞춤형 콘텐츠 제공, 서비스 품질 개선을 위해 개인정보를 이용합니다.',
      '부정 이용 방지, 보안 관리, 공지사항 전달 및 문의 응대를 위한 목적으로도 활용됩니다.',
    ],
  },
  {
    title: '3. 개인정보의 보관 및 이용 기간',
    body: [
      '회원 탈퇴 시 관련 법령 또는 내부 정책에 따라 보관이 필요한 경우를 제외하고 지체 없이 파기합니다.',
      '전자상거래, 소비자 보호, 세무 관련 법령 등에서 정한 보존 기간이 있는 경우 해당 기간 동안 안전하게 보관합니다.',
    ],
  },
  {
    title: '4. 개인정보의 제3자 제공 및 처리 위탁',
    body: [
      'EduQuest는 원칙적으로 이용자의 개인정보를 외부에 제공하지 않습니다.',
      '서비스 운영에 필요한 경우 최소한의 범위에서 처리 업무를 위탁할 수 있으며, 이 경우 관련 내용을 사전에 공개하거나 안내합니다.',
    ],
  },
  {
    title: '5. 이용자의 권리',
    body: [
      '이용자는 언제든지 본인의 개인정보 조회, 수정, 삭제, 처리 정지 요청을 할 수 있습니다.',
      '권리 행사는 서비스 내 계정 관리 기능 또는 운영자 문의를 통해 요청할 수 있으며, 관련 법령에 따라 필요한 절차를 거쳐 처리됩니다.',
    ],
  },
  {
    title: '6. 개인정보 보호를 위한 조치',
    body: [
      '개인정보 접근 권한 최소화, 인증 정보 보호, 접근 통제, 로그 관리 등 기술적·관리적 보호 조치를 적용합니다.',
      '개인정보를 안전하게 처리하기 위해 권한 관리와 보안 점검을 지속적으로 수행합니다.',
    ],
  },
  {
    title: '7. 문의처',
    body: [
      '개인정보 보호와 관련된 문의, 불만 처리, 피해 구제 요청은 EduQuest 운영팀으로 접수할 수 있습니다.',
      '본 방침은 서비스 운영 정책에 따라 변경될 수 있으며, 중요한 변경 사항은 서비스 내 공지로 안내합니다.',
    ],
  },
]
</script>

<template>
  <div class="min-h-screen bg-[#FFF2EF]">
    <PageHeader
      title="개인정보처리방침"
      subtitle="EduQuest가 어떤 개인정보를 수집하고 어떻게 보호하는지 안내합니다."
      back-link="/"
    />

    <main class="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      <section class="luxe-panel overflow-hidden p-6 sm:p-8">
        <div class="rounded-[24px] border border-[#1A2A4F]/8 bg-[#FFF8F4] px-5 py-4 text-sm leading-6 text-[#1A2A4F]/72">
          본 개인정보처리방침은 2026년 5월 22일부터 적용됩니다.
        </div>

        <div class="mt-6 space-y-4">
          <article
            v-for="section in policySections"
            :key="section.title"
            class="rounded-[28px] border border-[#1A2A4F]/8 bg-white/92 p-6 shadow-[0_12px_28px_rgba(15,23,42,0.04)]"
          >
            <h2 class="text-xl font-black text-[#1A2A4F]">{{ section.title }}</h2>
            <div class="mt-4 space-y-3 text-sm leading-7 text-[#1A2A4F]/76 sm:text-base">
              <p v-for="paragraph in section.body" :key="paragraph">
                {{ paragraph }}
              </p>
            </div>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>
