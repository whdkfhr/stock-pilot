# Frontend Phase 1 — 기반 + 인증

> 상태: ✅ 완료 (develop 병합) · 스택: Vue 3 + Vite + TypeScript + Pinia + Vue Router + axios

## 목표
프론트엔드 기반(디자인 시스템·레이아웃·API 클라이언트·인증 상태)을 세우고,
**회원가입 → 로그인 → 홈**까지 실제로 클릭해 테스트할 수 있게 한다.

## 디자인
토스증권 스타일 — 모바일 우선(폭 480px), 넓은 여백, 파란 포인트(#3182F6),
둥근 카드, Pretendard 폰트. 토큰은 `src/styles/tokens.css`.
국장 관습(상승=빨강, 하락=파랑) 색을 토큰에 포함.

## 구현
- **API 클라이언트** (`src/api/client.ts`): axios 인스턴스 `baseURL=/api`,
  요청 인터셉터로 JWT 부착, 401 응답 시 토큰 폐기 + 로그인 이동.
- **인증 스토어** (`src/stores/auth.ts`, Pinia): login/signup/logout/fetchMe,
  토큰 localStorage 영속, 가입 직후 자동 로그인.
- **라우팅** (`src/router/index.ts`): `requiresAuth`/`guestOnly` 메타 가드.
- **화면**: `LoginView`, `SignupView`(성향·기간 세그먼트 선택), `HomeView`(프로필 카드 + 예정 기능).
- **UI 컴포넌트**: `BaseButton`, `BaseInput`, `BaseCard`, `BaseSegmented`, `AppHeader`.

## 연동한 API
`POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/users/me`

## 개발 환경 (CORS 없이)
Vite dev 프록시가 `/api`·`/actuator`를 `localhost:8080`으로 넘긴다(`vite.config.ts`).
```bash
# 1) 백엔드
./gradlew bootRun
# 2) 프론트 (frontend/)
npm install && npm run dev   # http://localhost:5173
```

## 검증
- `npm run build`(vue-tsc 타입체크 + vite 빌드) 통과.
- 라이브: 프록시 경유 signup(201) → login(토큰) → /me(200) → 미인증 401 확인.

## 다음 (Phase 2 — 홈)
인기 랭킹 TOP-N · 관심종목 요약 · 실시간 시세 카드.
