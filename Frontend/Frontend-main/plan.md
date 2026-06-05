너는 기존 React 프론트엔드를 Vue 3로 안전하게 마이그레이션하는 시니어 풀스택 엔지니어다.

내 목표는 현재 React로 작성된 프론트엔드 코드를 Vue 3 기반으로 마이그레이션하는 것이다.
하지만 이번 작업의 최우선 목적은 “프레임워크 교체”가 아니라 “기능 보존”이다.
즉, 기존 프론트엔드의 사용자 흐름, 화면 동작, API 계약, 인증 처리, 상태 변화, 예외 처리, 응답 처리 방식을 유지한 상태에서
React -> Vue로만 옮겨야 한다.

또한 이번 작업은 아래 자료를 함께 참고해야 한다.
1. 현재 프론트엔드 React 코드
2. 백엔드 프로젝트 파일
3. ERD 파일
4. Postman 컬렉션

매우 중요:
- API 계약은 “DB 테이블 이름 추정”이 아니라 “Postman 컬렉션 + 실제 백엔드 구현 + 현재 프론트 코드”를 우선 기준으로 삼아라.
- ERD/DDL은 도메인 이해용으로 사용하되, 실제 API 계약이 Postman과 다르면 Postman 계약을 우선한다.
- 백엔드 코드에 구현이 안 보이더라도, Postman에 정의된 요청/응답 계약이 있으면 프론트는 그 계약을 우선 존중한다.
- 반대로, 프론트가 현재 Postman과 다르게 호출하고 있다면 즉시 멋대로 고치지 말고, 실제 사용 여부와 백엔드 구현 가능성을 함께 비교해서 조심스럽게 정리하라.
- API 명명 규칙이 일관되지 않더라도 임의로 정리하지 말고, 실제 계약을 그대로 유지하라.

====================================
0. 이번 작업의 절대 원칙
====================================

최우선 원칙:
1. 기존 기능을 절대 바꾸지 말 것
2. 기존 사용자 흐름을 절대 바꾸지 말 것
3. 기존 라우팅 URL 구조를 절대 바꾸지 말 것
4. 기존 화면 구성과 UI 동작을 최대한 동일하게 유지할 것
5. 기존 비즈니스 로직을 임의로 바꾸지 말 것
6. 기존 API 계약을 임의로 단순화/정규화/재설계하지 말 것
7. 기존 에러 처리 방식을 유지할 것
8. 기존 상태관리 의미를 유지할 것
9. 기존 권한/인증 흐름을 유지할 것
10. 확실하지 않은 부분은 삭제하지 말고 TODO로 남길 것

절대 금지:
- DB 테이블만 보고 endpoint를 임의 생성하는 것
- singular/plural, snake_case/camelCase가 이상하다고 임의 수정하는 것
- 성공 응답이 204 No Content인데 JSON 파싱을 강제하는 것
- multipart 계약을 JSON으로 멋대로 바꾸는 것
- refresh token 쿠키 기반 흐름을 localStorage 기반으로 멋대로 바꾸는 것
- 응답 필드명을 prettier하게 통일하는 것
- sort / page / size / is_asc / stage_number 같은 query 키를 멋대로 바꾸는 것
- create API와 update API의 요청 형식이 다르다고 통합하는 것
- 기존 화면을 Vue 스타일로 “재설계”하는 것
- 내가 요청하지 않은 추가 기능 구현

기술 기준:
- Vue 3
- Composition API
- Single File Component(.vue)
- Vue Router
- 현재 프론트 구조에 맞는 가장 단순하고 안정적인 상태관리 방식
- 기존 스타일 최대 유지
- 기존 API 모듈 구조 최대 유지
- 기존 asset / CSS / 레이아웃 최대 유지

====================================
1. 자료 우선순위 규칙
====================================

자료 해석 우선순위는 아래와 같다.

1순위: 현재 프론트엔드에서 실제 사용 중인 화면 동작과 API 사용 방식
2순위: Postman 컬렉션에 정의된 요청/응답 계약
3순위: 백엔드 코드에 실제 존재하는 controller / dto / security / validation 구현
4순위: ERD / DDL / migration SQL

해석 원칙:
- 화면 동작 보존이 최우선이다.
- 프론트가 이미 특정 요청/응답 형태를 전제로 짜여 있으면 그 의미를 먼저 파악한다.
- Postman 계약이 있으면 endpoint, method, body mode, 응답 구조, 상태 코드를 그대로 따른다.
- DB/ERD는 “이 API가 어떤 도메인과 연결되는지”를 이해하는 참고자료이지, 프론트 계약을 새로 만드는 근거가 아니다.
- API와 ERD가 완전히 일치하지 않아도, 프론트는 실제 HTTP 계약을 우선 맞춘다.

====================================
2. 먼저 분석만 수행
====================================

코드를 바로 대규모 수정하지 말고, 먼저 아래 분석을 수행하라.

2-1. 프론트엔드 분석
- React 버전
- 빌드 도구(Vite/CRA/Webpack 등)
- TypeScript 사용 여부
- 라우팅 구조
- 상태관리 방식
- 공통 컴포넌트 구조
- 페이지 구조
- API 호출 모듈 구조
- axios/fetch wrapper 존재 여부
- interceptor 존재 여부
- localStorage/sessionStorage/cookie 사용 여부
- 인증 처리 방식
- refresh 흐름 존재 여부
- multipart/form-data 처리 방식
- 파일 업로드 처리 방식
- 폼 라이브러리 사용 여부
- 스타일링 방식
- 테스트 코드 유무
- 환경변수 사용 방식
- alias 사용 여부

2-2. 백엔드 분석
- Spring Boot 설정
- security 관련 구현 여부
- auth 관련 controller/service/dto 존재 여부
- stage/problem/submission/wrong-note/bookmark/note/community 관련 구현 여부
- 예외 응답 형식 구현 여부
- multipart 처리 관련 구현 여부
- 파일 업로드 관련 구현 여부

2-3. ERD/DDL 분석
- member / role / user_role
- stage / problem / submission / evaluation / hint
- wrong_note / bookmark / note
- community_post / community_answer
- wallet / wallet_history / reward_history
- file / reset_password_token
도메인 관계를 요약하라.

2-4. Postman 컬렉션 분석
각 endpoint에 대해 아래를 정리하라.
- method
- path
- path param
- query param
- body mode
- body schema
- auth 필요 여부
- 성공 status code
- 성공 body shape
- 실패 status code
- 실패 body shape
- 프론트에서 특별히 주의해야 할 점

====================================
3. 반드시 파악해야 하는 실제 API 계약
====================================

아래 계약은 매우 중요하므로, 프론트 마이그레이션 시 반드시 유지하라.

------------------------------------
3-A. 인증 / 사용자(identity)
------------------------------------

[회원가입]
- POST /api/v1/sign-up
- body mode: form-data
- field:
  - profileImage: file
  - profile: text(application/json)
- profile JSON에는 아래 정보가 들어간다.
  - id
  - email
  - password
  - password_valid
  - birth
  - nickname
- 성공: 201 Created
- 실패: 400 Bad Request
- 중요:
  - multipart 계약을 절대 바꾸지 말 것
  - profile은 단순 문자열이 아니라 JSON 텍스트라는 점을 유지할 것
  - 파일 첨부가 optional인지 required인지 현재 코드와 백엔드를 함께 확인할 것

[로그인]
- POST /api/v1/auth/sign-in
- body mode: raw JSON
- body:
  {
    "id": "id",
    "password": "password"
  }
- 성공: 200 OK
- 응답 body:
  {
    "accessToken": "new-access-token"
  }
- refresh token은 cookie로 내려오는 계약을 전제할 것
- 실패: 400 Bad Request
- 중요:
  - accessToken은 JSON body에서 읽는다
  - refresh token은 쿠키 흐름을 존중한다
  - 프론트에서 refresh token 값을 직접 저장/조회하는 식으로 재설계하지 말 것

[아이디 찾기]
- POST /api/v1/auth/find-id
- raw JSON
- body:
  {
    "email": "email"
  }
- 성공: 204 No Content
- 실패: 400
- 중요:
  - 성공 시 body가 없으므로 JSON 파싱 금지

[비밀번호 찾기]
- POST /api/v1/auth/find-password
- raw JSON
- body:
  {
    "email": "email",
    "id": "id"
  }
- 성공: 204 No Content
- 실패: 400

[비밀번호 재설정]
- PUT /api/v1/auth/reset-password
- raw JSON
- body:
  {
    "token": "token",
    "new_password": "new_password"
  }
- 성공: 204 No Content
- 실패: 401 Unauthorized
- 중요:
  - token 검증 실패 케이스를 유지할 것

[토큰 재발급]
- POST /api/v1/auth/refresh
- Authorization 헤더 + Cookie를 사용하는 계약
- 성공: 200 OK
- body:
  {
    "accessToken": "new-access-token"
  }
- 응답 헤더에 Set-Cookie(refreshToken=...; HttpOnly; Secure; SameSite=Strict; Path=/auth/refresh; Max-Age=1209600)가 포함될 수 있음
- 실패: 401 Unauthorized
- 중요:
  - 프론트에서 refresh endpoint 요청 시 credentials/cookie 전송이 유지되어야 한다
  - access token 갱신 후 재시도 로직이 기존에 있으면 그대로 유지
  - refresh token을 자바스크립트에서 직접 읽으려는 방향으로 바꾸지 말 것

[로그아웃]
- POST /api/v1/auth/logout
- Authorization 필요
- 성공: 204 No Content

[유저 프로필 조회]
- GET /api/v1/users/{uuid}
- Authorization 필요
- 성공: 200
- body 예시:
  {
    "uuid": "...",
    "id": "id",
    "birth": "2000-01-01",
    "nickname": "name",
    "point": 100,
    "role": "user",
    "is_locked": false,
    "profile": "url"
  }
- 실패: 404
- 중요:
  - ERD상 member, wallet, role 등이 합쳐진 응답일 수 있으므로 DB 구조로 되돌려 분해하지 말 것
  - 프론트는 실제 응답 필드를 그대로 사용하라

[유저 목록]
- GET /api/v1/users?page&size&sort&is_asc
- Authorization 필요
- 성공: 200
- paging 응답 구조 유지
- sort 설명: created_at, balance

[아이디로 UUID 조회]
- GET /api/v1/users/{id}/uuid
- 성공: 200
- body:
  {
    "uuid": "uuid_value_here"
  }

[프로필 수정]
- PUT /api/v1/users/{uuid}
- Authorization 필요
- body mode: form-data
- field:
  - profileImage: file
  - profile: text(application/json)
- profile 설명:
  - email
  - password
  - nickname
- 성공: 204
- 중요:
  - 생성과 수정 모두 multipart이지만 내부 JSON 구조가 다를 수 있으므로 같은 DTO로 단순화하지 말 것

[역할 변경]
- PUT /api/v1/users/{uuid}/role
- raw JSON
- body:
  {
    "role_uuid": "..."
  }
- 성공: 204

[계정 잠금]
- PUT /api/v1/users/{uuid}/lock
- raw JSON
- body:
  {
    "is_locked": true
  }
- 성공: 204

[권한 목록]
- GET /api/v1/users/roles
- 성공: 200
- body:
  {
    "results": [
      {
        "uuid": "...",
        "name": "admin"
      }
    ]
  }

------------------------------------
3-B. 학습(learning)
------------------------------------

[스테이지 생성]
- POST /api/v1/stages
- raw JSON
- body:
  {
    "title": "title",
    "number": 1,
    "reward": 100
  }
- 성공: 201

[스테이지 수정]
- PUT /api/v1/stages/{uuid}
- raw JSON
- body:
  {
    "title": "title",
    "number": 1,
    "reward": 100
  }
- 성공: 204

[스테이지 삭제]
- DELETE /api/v1/stages/{uuid}
- 성공: 204

[스테이지 단건 조회]
- GET /api/v1/stages/{uuid}
- 성공: 200
- body:
  {
    "uuid": "...",
    "title": "title",
    "number": 1,
    "reward": 100
  }

[스테이지 목록]
- GET /api/v1/stages?page&size&sort&is_asc
- 성공: 200
- sort 설명: number
- paging 응답 구조 유지

[문제 생성]
- POST /api/v1/problems
- raw JSON
- body:
  {
    "stage_uuid": "uuid",
    "type": "basic",
    "number": 1,
    "summary": "문제 설명",
    "example": "print('Hello')",
    "expectedOutput": "Hello",
    "block": {
      "answer": [1,2,3,4],
      "blocks": [
        {"order":1,"code":"p"},
        {"order":2,"code":"rint"},
        {"order":3,"code":"('"},
        {"order":4,"code":"Hello')"}
      ]
    },
    "hints": [
      {"level":1,"point":10,"content":"hint"},
      {"level":2,"point":20,"content":"hint"},
      {"level":3,"point":30,"content":"hint"}
    ]
  }
- 성공: 201
- 중요:
  - stage_uuid / expectedOutput / block / hints 구조를 정확히 유지할 것
  - block.answer는 배열, block.blocks는 순서와 code를 갖는 객체 배열이다

[문제 수정]
- PUT /api/v1/problems/{uuid}
- body mode: form-data
- field:
  - stage
  - type
  - summary
  - ex
  - output
  - blocks (application/json)
  - hints (application/json)
- 성공: 204
- 매우 중요:
  - 문제 생성과 문제 수정의 요청 형식과 필드명이 서로 다르다
  - create는 raw JSON + stage_uuid + example + expectedOutput + block
  - update는 form-data + stage + ex + output + blocks
  - 이 차이를 임의 통합하지 말고 그대로 유지하라

[문제 삭제]
- DELETE /api/v1/problems/{uuid}
- 성공: 204

[문제 단건 조회]
- GET /api/v1/problems/{uuid}
- 성공: 200
- 응답 예시:
  {
    "uuid": "...",
    "stage": "stage1",
    "type": "basic",
    "number": 1,
    "summary": "This is a sample problem",
    "blocks": [
      {"number":1,"code":"..."},
      {"number":2,"code":"..."}
    ],
    "hints": [1,2,3]
  }
- 중요:
  - create 시 보낸 구조와 read 응답 구조가 동일하지 않을 수 있다
  - 프론트는 생성 DTO와 조회 응답 DTO를 분리해서 처리하라

[스테이지별 문제 조회]
- GET /api/v1/problems?stage_number
- query:
  - stage_number
- 성공: 200
- body:
  {
    "results": [
      {
        "uuid": "...",
        "stage": "stage1",
        "type": "basic",
        "number": 1
      }
    ]
  }

[문제 목록]
- GET /api/v1/problems?page&size&sort&is_asc
- sort 설명: stage, number, type
- 성공: 200
- paging 응답 구조 유지

------------------------------------
3-C. 제출 / 힌트(submission)
------------------------------------

[문제 제출]
- POST /api/v1/problems/{uuid}/submissions
- Authorization 필요
- raw JSON
- body는 두 형태를 모두 지원해야 한다.
  1)
  {
    "answer": "answer"
  }
  2)
  {
    "answer": [4,3,1,2]
  }
- 성공: 201
- 응답:
  {
    "result": true
  }
- 매우 중요:
  - answer 타입이 string 또는 array일 수 있다
  - 프론트 검증/직렬화 로직에서 이 둘을 모두 지원해야 한다
  - 기존 게임 로직이 문자열형 답과 블록 배열형 답을 나눠 처리하고 있다면 그대로 보존하라

[힌트 조회]
- GET /api/v1/problems/{uuid}/hint?level
- Authorization 필요
- query:
  - level
- 성공: 200
- 응답:
  {
    "hint": "hint"
  }
- 중요:
  - 힌트 레벨 선택과 포인트 차감 흐름이 현재 프론트에 있다면 보존하라

------------------------------------
3-D. 진행도(progress)
------------------------------------

[진행도 조회]
- GET /api/v1/users/{uuid}/progress
- Authorization 필요
- 성공: 200
- 응답:
  {
    "results": [
      {
        "stage": "stage1",
        "total_question_count": 10,
        "clear": [1,2,3]
      }
    ]
  }
- 중요:
  - clear 배열을 이용한 UI 표시가 있다면 유지할 것

------------------------------------
3-E. 오답노트(wrongNote)
------------------------------------

[오답노트 단건]
- GET /api/v1/wrong-notes/{uuid}
- 성공: 200
- 응답 예시:
  {
    "uuid": "...",
    "user_uuid": "...",
    "problem_uuid": "...",
    "wrong_answer": "wrong",
    "correct_answer": "correct",
    "ai_explanation": "explain",
    "is_reviewed": true,
    "created_at": "2020-01-01",
    "reviewed_at": "2023-01-01"
  }

[내 오답노트 목록]
- GET /api/v1/wrong-notes/users/{uuid}?page&size&sort&is_asc
- sort 설명: problem_number, created_at
- 성공: 200
- paging 구조 유지

[전체 오답노트 목록]
- GET /api/v1/wrong-notes?page&size&sort&is_asc
- 성공: 200

[오답노트 삭제]
- DELETE /api/v1/wrong-notes/{uuid}
- 성공: 204

중요:
- ERD/DDL과 응답 필드가 완전히 일치하지 않을 수 있어도, 프론트는 API 응답 계약을 우선하라
- ai_explanation, correct_answer, reviewed_at 같은 화면 필드가 있으면 보존하라

------------------------------------
3-F. 북마크(bookmark)
------------------------------------

[북마크 추가]
- POST /api/v1/problems/{uuid}/bookmark
- 성공: 201

[북마크 삭제]
- DELETE /api/v1/problems/{uuid}/bookmark
- 성공: 204

[북마크 목록]
- GET /api/v1/users/{uuid}/bookmarks?page&size&sort&is_asc
- 성공: 200
- paging 구조 유지

------------------------------------
3-G. 노트(notes)
------------------------------------

[노트 생성]
- POST /api/v1/notes
- raw JSON
- body:
  {
    "title": "title",
    "content": "content"
  }
- 성공: 201

[노트 수정]
- PUT /api/v1/notes/{uuid}
- raw JSON
- body:
  {
    "title": "title",
    "content": "content"
  }
- 성공: 204

[노트 삭제]
- DELETE /api/v1/notes/{uuid}
- 성공: 204

[노트 단건]
- GET /api/v1/notes/{uuid}
- 성공: 200

[노트 목록]
- GET /api/v1/notes?page&size&sort&is_asc
- sort 설명: created_at, updated_at, title
- 성공: 200

------------------------------------
3-H. 커뮤니티(community)
------------------------------------

[질문 생성]
- POST /api/v1/questions
- raw JSON
- body:
  {
    "title": "title",
    "content": "content"
  }
- 성공: 201

[질문 삭제]
- DELETE /api/v1/questions/{uuid}
- 성공: 204

[답변 생성]
- POST /api/v1/question/{uuid}/answers
- raw JSON
- body:
  {
    "content": "content"
  }
- 성공: 201
- 매우 중요:
  - 여기 path는 /question/ 단수형이다
  - 임의로 /questions/로 고치지 말 것

[답변 삭제]
- DELETE /api/v1/answers/{uuid}
- 성공: 204

[답변 채택]
- POST /api/v1/answers/{uuid}/adopt
- 성공: 201

[질문 목록]
- GET /api/v1/questions?page&size&sort&is_asc
- sort 설명: created_at, title
- 성공: 200
- 응답의 user 객체 구조 유지

[질문 단건]
- GET /api/v1/questions/{uuid}
- 성공: 200
- 응답에 user, is_adopt, adopted_answer 포함 가능

[답변 목록]
- GET /api/v1/questions/{uuid}/answers?page&size&is_asc
- 성공: 200
- 매우 중요:
  - 답변 생성은 /question/{uuid}/answers
  - 답변 목록은 /questions/{uuid}/answers
  - 이 불일치를 “수정”하지 말고 그대로 유지할 것

====================================
4. 응답/에러 처리 규칙
====================================

프론트에서 반드시 아래를 지켜라.

1. 201/204 성공 응답 중 body가 없는 경우가 많다
- 성공 후 무조건 response.data를 참조하는 코드가 있으면 안전하게 수정하라
- 하지만 UX 흐름은 바꾸지 말 것

2. 에러 응답은 아래 형식을 가질 수 있다
{
  "code": 400,
  "message": "Bad Request",
  "details": "..."
}
- 기존 에러 파서/토스트/알림 로직이 있다면 유지하라
- details 우선 표시 여부를 현재 UI에 맞춰 유지하라

3. Authorization 헤더는 Bearer access token 규칙을 유지하라

4. refresh token은 쿠키 기반 흐름을 존중하라
- JS 저장소 중심으로 재설계하지 말 것
- 필요 시 withCredentials / credentials 포함 여부를 현재 구현과 Postman 계약을 함께 보고 맞춰라

5. multipart/form-data 요청 시
- Content-Type을 수동으로 깨뜨리지 말 것
- FormData 직렬화 시 profile 같은 JSON 텍스트 필드를 정확히 유지할 것

====================================
5. 프론트-백엔드 정합성 매핑을 먼저 작성
====================================

실제 마이그레이션 전에 반드시 아래 표를 먼저 만들어라.

기능별 정합성 표 항목:
- 기능명
- 관련 프론트 페이지/컴포넌트
- 현재 React에서 호출하는 API
- Postman 기준 API 계약
- 관련 백엔드 도메인(ERD 기준)
- 인증 필요 여부
- body mode
- 요청 필드
- 응답 필드
- 상태 코드
- 특이사항
- 마이그레이션 위험도
- TODO 여부

특히 아래를 우선 매핑하라:
- 회원가입
- 로그인/로그아웃
- 토큰 갱신
- 프로필 조회/수정
- 유저 목록/권한 변경/잠금
- 스테이지 목록/생성/수정
- 문제 목록/단건/생성/수정
- 문제 제출
- 힌트 조회
- 진행도 조회
- 오답노트 조회/목록
- 북마크 추가/삭제/목록
- 노트 CRUD
- 질문/답변/채택

====================================
6. 실제 마이그레이션 전략
====================================

이제 React -> Vue 마이그레이션 전략을 세워라.

6-1. 엔트리 포인트
- React root -> Vue createApp
- 앱 시작 구조만 바꾸고 전체 동작은 보존

6-2. 라우터
- React Router -> Vue Router
- URL 구조 절대 유지
- 기존 페이지 전환 흐름 절대 유지
- 인증 가드가 있으면 동일 의미 유지

6-3. 상태관리
- Context/store/provider 의미 유지
- access token 저장 방식 유지
- user 정보 캐싱 방식 유지
- 게임 상태, 문제 풀이 상태, 커뮤니티 상태가 있으면 분리 기준 유지

6-4. API 레이어
- 기존 API 함수명 최대 유지
- endpoint path, method, query key, body mode를 Postman 계약에 맞춤
- createProblem과 changeProblem의 body mode 차이 유지
- sign-up/changeProfile의 multipart 구조 유지
- submitProblem의 answer union(string | array) 유지
- community의 singular/plural path 차이 유지

6-5. 공통 컴포넌트
- 버튼, 입력창, 모달, 테이블, 카드, 페이지네이션, 탭, 폼 컴포넌트 우선 변환
- props/emits/slot으로 옮기되 동작 유지

6-6. 페이지 컴포넌트
- 화면 단위로 하나씩 변환
- 목록 / 상세 / 생성 / 수정 / 삭제 흐름 유지
- 기존 loading/error/empty 상태 유지

6-7. React hook -> Vue 대응
- useState -> ref/reactive
- useEffect -> onMounted/watch/watchEffect/onUnmounted
- useMemo -> computed
- useCallback -> 필요 시 일반 함수 또는 별도 참조 유지
- useRef -> ref/template ref
- custom hook -> composable
- 하지만 “의미가 같은지”를 먼저 검증하고 바꿔라

====================================
7. 구현 시 세부 지침
====================================

1. 함수명, 변수명, 도메인명 최대 유지
2. API 함수명 최대 유지
3. route name/path 최대 유지
4. query key 이름 절대 임의 변경 금지
5. payload key 이름 절대 임의 변경 금지
6. 응답 필드명 절대 임의 변경 금지
7. 204 응답에서 JSON 파싱 시도 금지
8. 파일 업로드 UI 흐름 유지
9. 페이지네이션 UI와 sort UI 유지
10. 권한 분기 유지
11. 관리자 기능/일반 사용자 기능 분기 유지
12. localStorage/sessionStorage/cookie 사용 방식 유지
13. accessToken 주입 방식 유지
14. refresh 실패 시 로그아웃/재로그인 유도 흐름이 있으면 유지
15. FormData 조립 방식에서 JSON 문자열 직렬화 누락 금지
16. GET 요청에 body가 있더라도, 현재 프론트가 실제로 query를 쓰는지 먼저 확인하고 기존 의미를 유지하라
17. 기존 버그처럼 보이는 동작도 섣불리 고치지 말고 TODO로 분리하라
18. Postman 계약과 프론트 코드가 어긋나면 그 차이를 먼저 보고서로 정리한 뒤 조심스럽게 수정하라

====================================
8. 검증 단계
====================================

각 단계 후 반드시 검증하라.

필수 검증:
1. 프로젝트 실행 여부
2. 빌드 성공 여부
3. 라우팅 정상 여부
4. 로그인 성공 여부
5. access token 저장/주입 여부
6. refresh 흐름 여부
7. logout 후 정리 여부
8. 회원가입 multipart 전송 여부
9. 프로필 수정 multipart 전송 여부
10. stage/problem 목록 조회 여부
11. createProblem 요청 body 구조 정확성
12. changeProblem form-data 구조 정확성
13. submitProblem에서 string/array 답안 모두 처리되는지
14. hint query(level) 동작 여부
15. progress 표시 여부
16. wrong-notes 목록/상세 표시 여부
17. bookmarks 목록/추가/삭제 여부
18. notes CRUD 여부
19. questions/answers/adopt 흐름 여부
20. 204 응답에서 프론트 오류가 안 나는지
21. standardized error response(code/message/details) 처리 유지 여부
22. 스타일 깨짐 여부
23. 콘솔 에러 여부
24. import/type 오류 여부

가능하면 수행:
- lint
- test
- build
- 타입 체크

====================================
9. 최종 산출물 형식
====================================

최종 결과는 아래 형식으로 정리하라.

1. 작업 개요
- 어떤 React 구조를 어떤 Vue 구조로 옮겼는지
- 전체 범위 요약

2. 분석 결과
- 프론트 구조 요약
- 백엔드 구조 요약
- ERD/도메인 요약
- Postman 계약 요약

3. 기능별 정합성 표
- 각 기능의 프론트/백엔드/Postman 대응 상태

4. 변경 파일 목록
- 수정 파일
- 생성 파일
- 삭제 파일

5. 핵심 변환 내역
- 엔트리 포인트
- 라우터
- 상태관리
- API 레이어
- 공통 컴포넌트
- 페이지 컴포넌트
- 폼 처리
- 인증 처리
- 에러 처리

6. 그대로 유지한 계약
- path
- query key
- body mode
- payload key
- response shape
- 상태 코드 처리 방식

7. 확인이 필요한 항목
- TODO
- Postman과 현재 프론트가 달랐던 지점
- 백엔드 실제 구현 확인 필요 항목
- 수동 테스트 필요 항목

8. 검증 결과
- 실행
- 빌드
- 인증
- 파일 업로드
- 문제 제출
- 커뮤니티
- 오답노트
- 북마크
- 노트
- 남은 이슈

====================================
10. 마지막 실행 원칙
====================================

끝까지 아래 원칙을 유지하라.

- 프레임워크 교체보다 기능 보존이 우선이다
- API 계약 정리는 이번 작업 목적이 아니다
- 불일치가 있어도 우선은 실제 계약을 보존한다
- create/update/read의 DTO 차이를 함부로 합치지 않는다
- auth 흐름을 함부로 단순화하지 않는다
- 쿠키 기반 refresh 흐름을 JS 저장소 중심으로 바꾸지 않는다
- endpoint singular/plural 차이를 임의 수정하지 않는다
- 204/201 empty body 응답을 안전하게 처리한다
- TODO를 숨기지 않는다
- 대규모 재설계보다 작은 검증 가능한 변경을 우선한다

이제 먼저
1. 프론트/백엔드/ERD/Postman을 모두 분석하고
2. 기능별 정합성 표를 작성한 뒤
3. React -> Vue 마이그레이션을 단계적으로 진행하라.

각 단계마다
- 무엇을 바꿨는지
- 왜 바꿨는지
- 어떤 API 계약을 기준으로 했는지
- 무엇을 검증했는지
를 명확하게 기록하라.