<template>
  <main class="min-h-screen bg-[#faf7f2] text-slate-900">
    <section class="mx-auto max-w-6xl px-6 py-20">
      <!-- 상단 소개 박스 -->
      <div class="rounded-[2rem] bg-white px-8 py-14 shadow-sm md:px-14">
        <p class="text-sm font-semibold tracking-[0.35em] text-slate-400">
          SERVICE INTRODUCTION
        </p>

        <div class="mt-6 grid gap-10 md:grid-cols-[1.4fr_0.8fr] md:items-center">
          <div>
            <h1 class="text-4xl font-bold leading-tight text-slate-900 md:text-5xl">
              반복하며 성장하는<br />
              AI 코딩 학습 서비스
            </h1>

            <p class="mt-6 text-base leading-8 text-slate-600 md:text-lg">
              EduQuest는 초등학교 4학년부터 중학교 3학년까지의 학생을 위한
              게임형 코딩 학습 서비스입니다. 학생은 Unity 기반 학습 게임에서
              코딩 문제를 풀고, 틀린 문제를 다시 복습하며, AI 튜터의 도움을 통해
              부족한 개념을 반복적으로 익힐 수 있습니다.
            </p>
          </div>

          <!-- 대상 강조 카드 -->
          <div class="rounded-3xl bg-[#f5efe6] p-7">
            <p class="text-sm font-semibold text-slate-500">
              이용 대상
            </p>

            <p class="mt-4 text-3xl font-bold text-slate-900">
              초4 ~ 중3
            </p>

            <p class="mt-4 text-sm leading-6 text-slate-600">
              코딩을 처음 배우는 학생도 문제 풀이, 오답 확인,
              AI 튜터 피드백, 재도전을 반복하며 기초 개념을 차근차근 익힐 수 있습니다.
            </p>
          </div>
        </div>
      </div>

      <!-- 서비스 설명 -->
      <div class="mt-10 rounded-[2rem] bg-white px-8 py-12 shadow-sm md:px-14">
        <h2 class="text-2xl font-bold text-slate-900">
          EduQuest는 어떤 서비스인가요?
        </h2>

        <p class="mt-5 text-base leading-8 text-slate-600">
          EduQuest는 단순히 한 번 문제를 풀고 끝나는 서비스가 아니라,
          학습자가 문제를 풀고, 결과를 확인하고, 틀린 문제를 다시 복습하는
          반복학습 구조를 중심으로 설계된 코딩 학습 플랫폼입니다.
        </p>

        <p class="mt-4 text-base leading-8 text-slate-600">
          학생은 스테이지별 문제를 해결하면서 학습을 진행하고, 틀린 문제는
          오답 노트에 저장해 다시 확인할 수 있습니다. 또한 AI 튜터를 통해
          틀린 이유와 보완해야 할 개념을 확인하며, 같은 실수를 줄일 수 있도록
          도움을 받을 수 있습니다.
        </p>

        <p class="mt-4 text-base leading-8 text-slate-600">
          필요한 문제는 북마크하여 나중에 다시 풀어볼 수 있고,
          학습 중 궁금한 점은 커뮤니티를 통해 질문할 수 있습니다.
          EduQuest는 학생이 스스로 다시 도전하고 성장할 수 있는 학습 경험을 제공합니다.
        </p>
      </div>

      <!-- 핵심 기능 카드 -->
      <div class="mt-10 grid gap-5 md:grid-cols-3 lg:grid-cols-5">
        <div class="rounded-3xl bg-white p-6 shadow-sm">
          <div class="text-3xl">🔁</div>
          <h3 class="mt-4 text-lg font-bold text-slate-900">
            반복학습
          </h3>
          <p class="mt-3 text-sm leading-6 text-slate-600">
            문제 풀이, 결과 확인, 복습 과정을 반복하며 코딩 개념을 익힙니다.
          </p>
        </div>

        <div class="rounded-3xl bg-white p-6 shadow-sm">
          <div class="text-3xl">🤖</div>
          <h3 class="mt-4 text-lg font-bold text-slate-900">
            AI 튜터
          </h3>
          <p class="mt-3 text-sm leading-6 text-slate-600">
            틀린 문제에 대해 부족한 개념과 다시 학습할 방향을 안내합니다.
          </p>
        </div>

        <div class="rounded-3xl bg-white p-6 shadow-sm">
          <div class="text-3xl">🎮</div>
          <h3 class="mt-4 text-lg font-bold text-slate-900">
            게임형 학습
          </h3>
          <p class="mt-3 text-sm leading-6 text-slate-600">
            Unity 기반 학습 게임을 통해 미션을 해결하듯 코딩 문제를 풉니다.
          </p>
        </div>

        <div class="rounded-3xl bg-white p-6 shadow-sm">
          <div class="text-3xl">📝</div>
          <h3 class="mt-4 text-lg font-bold text-slate-900">
            오답 복습
          </h3>
          <p class="mt-3 text-sm leading-6 text-slate-600">
            틀린 문제를 오답 노트에 저장하고 다시 확인하며 부족한 부분을 보완합니다.
          </p>
        </div>

        <div class="rounded-3xl bg-white p-6 shadow-sm">
          <div class="text-3xl">⭐</div>
          <h3 class="mt-4 text-lg font-bold text-slate-900">
            북마크 재학습
          </h3>
          <p class="mt-3 text-sm leading-6 text-slate-600">
            다시 보고 싶은 문제를 저장해두고 필요할 때 반복해서 학습할 수 있습니다.
          </p>
        </div>
      </div>

      <!-- 마무리 강조 -->
      <div class="mt-10 rounded-[2rem] bg-slate-900 px-8 py-10 text-white shadow-sm md:px-14">
        <h2 class="text-2xl font-bold">
          EduQuest의 핵심은 AI 튜터와 반복학습을 통한 성장입니다
        </h2>

        <p class="mt-5 text-base leading-8 text-slate-300">
          한 번에 정답을 맞히는 것보다 중요한 것은 틀린 문제를 다시 보고,
          부족한 개념을 확인하며, 스스로 다시 도전하는 과정입니다.
          EduQuest는 AI 튜터와 오답 복습 기능을 통해 학생들이 코딩에 익숙해지고
          자신감을 가질 수 있도록 돕습니다.
        </p>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
</script>