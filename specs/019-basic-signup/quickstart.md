# Quickstart: 기본 회원가입·로그인 (019)

## 준비

- 백엔드 `backend/`에서 `.\gradlew.bat bootRun`(local 프로필). local은 가짜 발송이라 메일이 실제로 가지 않는다. 받은 코드는 `GET http://localhost:8080/api/local/mail-outbox`로 본다
- 프론트 `frontend/`에서 `npm run dev` (http://localhost:3000)
- 실제 Gmail로 보내 보려면 `backend/.env.local`에 `MAIL_SENDER=smtp`, `MAIL_FROM`, `MAIL_USERNAME`, `MAIL_PASSWORD`(Gmail 앱 비밀번호: 구글 계정 → 보안 → 2단계 인증 → 앱 비밀번호)를 넣는다. 값은 커밋하지 않는다

## 수동 확인

| # | 할 일 | 기대 | AC |
|---|---|---|---|
| 1 | 로그인 화면 | "회원가입", "아이디·비밀번호 찾기" 링크 | US1-AC1 |
| 2 | `/sign-up`에서 새 이메일로 코드 받기 → 우편함의 코드 입력 | 계정 정보 화면, 이메일 읽기만 | US1-AC2·AC3 |
| 3 | 아이디·비밀번호·닉네임·약관 → 가입 | 로그인된 첫 화면, `닉네임#태그` | US1-AC4 |
| 4 | 로그아웃 → 3의 아이디·비밀번호로 로그인 | 로그인됨 | US1-AC5 |
| 5 | 3의 이메일로 다시 코드 받기 | 화면은 2와 같고, 우편함에는 코드가 아닌 "이미 가입돼 있다" 안내 | US5-AC1·AC6 |
| 6 | 소셜(구글) 회원의 이메일로 코드 받기 | 5와 같다 | US5-AC2 |
| 7 | 60초 안에 다시 받기, 1시간에 여섯 번 받기 | B504 남은 초, B505 | US2-AC2·AC3 |
| 8 | 틀린 코드 5번 → 맞는 코드 | 다섯째부터 "새 코드를 받아 주세요" | US2-AC5 |
| 9 | `/sign-in/find`에서 3의 이메일 → 코드 → 새 비밀번호 | 아이디가 보이고, 저장 뒤 새 비밀번호로만 로그인. 다른 브라우저의 로그인이 끊김 | US3-AC1·AC3·AC4 |
| 10 | `/sign-in/find`에서 없는 이메일 | 9와 같은 안내, 우편함에 메일 없음 | US3-AC2 |
| 11 | 3의 아이디로 틀린 비밀번호 5번 → 맞는 비밀번호 | 다섯째부터 잠금 안내와 풀리는 시각, 맞는 비밀번호도 막힘 | US4-AC2·AC3 |
| 12 | 11 뒤 9로 재설정 | 바로 로그인됨 | US3-AC5 |
| 13 | 없는 아이디로 5번 | 11과 같은 안내 | US4-AC6 |
| 14 | 관리자 `admin`으로 틀린 비밀번호 5번 | 같은 잠금 (spec FR-015 결정) | FR-015 |
| 15 | 관리자 접속 이력 | 11의 다섯째는 "비밀번호 불일치", 잠금 중 시도는 "임시 잠금" | US4-AC8 |
| 16 | 3의 회원 탈퇴 → 같은 아이디로 가입, 같은 이메일로 코드 받기 | 아이디는 "이미 쓰는 아이디", 이메일은 5와 같다 | US6-AC1·AC2 |
| 17 | 16의 회원을 31일 앞당겨 파기 (018 quickstart 방법) | `tb_user_email`·`tb_email_verification`에 그 회원 행 없음. 같은 아이디·이메일로 다시 가입됨 | US6-AC4·AC5 |
| 18 | 백엔드 로그를 `@`·6자리 숫자로 찾기 | 이메일 원문·코드 없음 | US6-AC6 |
| 19 | **마이그레이션 전** 로컬 DB에서 세 쿼리 (research R2 "마이그레이션 전 확인"): 겹침 `… GROUP BY email COLLATE utf8mb4_bin HAVING COUNT(DISTINCT user_id) > 1`, 정규화 안 된 값 `… WHERE email COLLATE utf8mb4_bin <> LOWER(TRIM(email)) COLLATE utf8mb4_bin`, ASCII 밖 값 `… WHERE email REGEXP '[^ -~]'` | 셋 다 0. 0이 아니면 백필 전에 멈추고 수를 PR에 적는다 | FR-005 |
| 20 | 마이그레이션 뒤 `SELECT COUNT(*) FROM tb_email_claim`과 `SELECT COUNT(DISTINCT email COLLATE utf8mb4_bin) FROM tb_user_social_account`, 그리고 구글 회원 하나의 이메일로 이메일 가입 코드를 받아 우편함에 안내 메일이 오는지 | 수가 같고, 안내 메일이 온다 (백필 해시 = 앱 해시) | FR-005, US5-AC6 |
| 21 | 구글 회원의 이메일로 이메일 가입 계정 만들기를 동시에 두 번 (또는 2와 구글 가입을 같은 이메일로 동시에) | 회원은 하나만 생긴다 | SC-002, US5-AC5 |
| 22 | `SPRING_PROFILES_ACTIVE=dev`로 띄워 `GET /api/local/mail-outbox` | 열리지 않는다 (자동 테스트로도 본다) | FR-019 |

## 자동 검사

- 로컬: 백엔드 `.\gradlew.bat build`, 프론트 `npm run check` (CLAUDE.md "로컬 검사")
- 인수 테스트(`dbTest`)와 화면 검사(`test:e2e`)는 PR CI가 돌린다. 가짜 발송기의 우편함으로 코드를 읽는다
