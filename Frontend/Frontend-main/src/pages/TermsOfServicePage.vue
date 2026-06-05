<script setup lang="ts">
import PageHeader from '../components/PageHeader.vue'

const termsSections = [
  {
    title: '1. 목적',
    body: [
      '본 약관은 EduQuest가 제공하는 학습 및 커뮤니티 서비스의 이용 조건, 절차, 권리, 의무 및 책임 사항을 규정하는 것을 목적으로 합니다.',
    ],
  },
  {
    title: '2. 서비스 내용',
    body: [
      'EduQuest는 코딩 학습, 문제 풀이, 진도 관리, 오답노트, 북마크, 커뮤니티 등 학습 지원 기능을 제공합니다.',
      '서비스 내용은 운영 정책과 기술 환경에 따라 변경될 수 있으며, 중요한 변경이 있는 경우 사전에 안내합니다.',
    ],
  },
  {
    title: '3. 회원가입 및 계정 관리',
    body: [
      '이용자는 정확한 정보를 입력해 회원가입을 진행해야 하며, 타인의 정보를 도용해서는 안 됩니다.',
      '계정과 비밀번호 관리 책임은 이용자에게 있으며, 무단 사용이 의심되는 경우 즉시 운영팀에 알려야 합니다.',
    ],
  },
  {
    title: '4. 이용자의 의무',
    body: [
      '이용자는 관계 법령, 본 약관, 서비스 내 안내 사항을 준수해야 합니다.',
      '서비스 운영을 방해하거나 타인의 권리를 침해하는 행위, 비정상적인 접근, 부정한 방법의 포인트 획득 등은 금지됩니다.',
    ],
  },
  {
    title: '5. 서비스 이용 제한',
    body: [
      '약관 위반, 불법 행위, 보안 위협, 타 이용자 피해 유발 행위가 확인될 경우 서비스 이용이 제한될 수 있습니다.',
      '필요한 경우 게시물 삭제, 계정 정지, 접근 제한 등의 조치가 이루어질 수 있습니다.',
    ],
  },
  {
    title: '6. 지식재산권',
    body: [
      '서비스에 포함된 콘텐츠, 디자인, 상표, 코드 및 운영 자료에 대한 권리는 EduQuest 또는 정당한 권리자에게 있습니다.',
      '이용자는 서비스에서 제공되는 자료를 운영자의 사전 동의 없이 복제, 배포, 상업적으로 이용할 수 없습니다.',
    ],
  },
  {
    title: '7. 책임의 제한',
    body: [
      'EduQuest는 천재지변, 시스템 장애, 외부 공격, 이용자 귀책 사유로 인한 서비스 중단 또는 손해에 대해 관련 법령 범위 내에서 책임을 부담합니다.',
      '무료로 제공되는 서비스의 특성상, 운영자는 특별한 사정이 없는 한 간접 손해나 기대 이익 상실에 대해 책임지지 않습니다.',
    ],
  },
  {
    title: '8. 약관의 변경',
    body: [
      '본 약관은 관련 법령과 서비스 정책에 따라 변경될 수 있습니다.',
      '변경된 약관은 시행일과 변경 사유를 함께 공지하며, 계속 이용하는 경우 변경 약관에 동의한 것으로 봅니다.',
    ],
  },
]
</script>

<template>
  <div class="min-h-screen bg-[#FFF2EF]">
    <PageHeader
      title="이용약관"
      subtitle="EduQuest 서비스 이용 시 적용되는 기본 약관입니다."
      back-link="/"
    />

    <main class="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      <section class="luxe-panel overflow-hidden p-6 sm:p-8">
        <div class="rounded-[24px] border border-[#1A2A4F]/8 bg-[#FFF8F4] px-5 py-4 text-sm leading-6 text-[#1A2A4F]/72">
          본 이용약관은 2026년 5월 22일부터 적용됩니다.
        </div>

        <div class="mt-6 space-y-4">
          <article
            v-for="section in termsSections"
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
