# Quickstart: 구글 소셜 로그인 (010) 확인 안내

구현 뒤 기능이 스펙대로 도는지 확인하는 순서다. 구현 코드는 tasks 단계에서 정한다.

## 준비

1. 구글 클라우드 콘솔에서 OAuth 클라이언트(웹 애플리케이션)를 만들고, 승인된 리디렉션 URI에 `http://localhost:3000/api/v1/auth/oauth/google/callback`을 넣는다
2. `backend/.env.local`에 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`를 넣는다 (이름은 `backend/.env.example` 참고. 값은 레포에 넣지 않는다)
3. 백엔드 `backend/`에서 `.\gradlew.bat bootRun`, 프론트 `frontend/`에서 `npm run dev`
4. 칼럼 추가 뒤 프론트 타입: `npm run gen:api`

## 수동 확인 (스펙 AC와 대응)

| 순서 | 할 일 | 기대 결과 | AC |
|---|---|---|---|
| 1 | 설정 없이 백엔드를 띄우고 `/sign-in` | 구글 단추 없음, 아이디 로그인 됨 | US3-AC3 |
| 2 | 설정을 넣고 `/sign-in` → 구글로 계속하기 → 처음 쓰는 구글 계정 | 닉네임 화면 → 넣으면 `/`, 닉네임이 보임 | US1-AC1·AC3 |
| 3 | 2를 닉네임 화면에서 닫고 다시 구글 로그인 | 닉네임 화면부터 다시, 사용자 관리에 회원 없음 | US1-AC5 |
| 4 | 로그아웃 뒤 같은 구글 계정으로 다시 | 닉네임 없이 바로 로그인, 회원 수 그대로 | US2-AC1 |
| 5 | 구글 회원으로 계정 메뉴 | "비밀번호 변경" 없음 | US2-AC3 |
| 6 | `admin`으로 사용자 관리 | 그 회원이 `USER`, 비밀번호 없음, `ADMIN` 주기 막힘 | US1-AC2·AC6 |
| 7 | `user`(비밀번호 있음)로 설정 → 구글 연결 → 로그아웃 → 구글 로그인 | `user` 계정으로 로그인 | US5-AC1·AC2 |
| 8 | 7에서 해제 | 연결 안 됨, 그 구글로 로그인하면 닉네임 화면(새 회원) | US5-AC5 |
| 9 | 구글 회원으로 설정 → 해제 | 막힘 안내 | US5-AC6 |
| 10 | `admin`으로 설정 | 구글 연결 줄 없음 | US5-AC7 |
| 11 | 구글 화면에서 취소 | 로그인 화면, 안내 없음 | US3-AC1 |
| 12 | `admin`으로 접속 이력 | 구글 로그인 성공·실패가 보임 (표시 방식은 미정) | US4-AC1 |

## 자동 검사

- 빠른 검사: 백엔드 `.\gradlew.bat build`, 프론트 `npm run check`
- 인수 테스트(`*GoogleLoginAcceptanceTest`, 원칙 II: AC마다 하나, 이름에 AC 번호)는 구글을 실제로 부르지 않도록 공급자 클라이언트(R2 경계)를 테스트용 가짜로 바꿔 돈다. 로컬에서는 돌리지 않고 PR CI로 본다 (CLAUDE.md)
- e2e는 핵심 흐름(US1 가입)만, 공급자 가짜를 쓴다
