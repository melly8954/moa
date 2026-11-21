# Quickstart: 개인정보 파기 기록 (purge-log)

## 자동 확인

- backend/에서 `./gradlew build` (빠른 검사), frontend/에서 `npm run check`
- 인수 테스트 `PurgeLogAcceptanceTest`(US1~US3)와 바뀐 `AdminMenusAcceptanceTest`·`AdminRolesAcceptanceTest`는 PR CI의 `db-test`가 돌린다 (로컬에서 돌리지 않는다)

## 화면 확인 (사람)

1. backend/에서 `./gradlew bootRun`, frontend/에서 `npm run dev`
2. 로컬 DB에 보관 기간이 지난 데이터를 만든다. 예: 접속 이력 하나의 `created_at`을 400일 전으로 바꾼다
3. 정리 시각을 기다리지 않으려면 `app.login-history.cleanup-cron`을 1분 뒤로 주고 다시 띄운다
4. 시험 계정 `admin`으로 로그인해 관리 메뉴 "운영 → 파기 기록"을 연다: 대상 "접속 이력", 건수 1, 방법 "행 삭제", 처리자 "정리기"가 보인다
5. 기간과 대상 종류를 바꿔 찾는다: 조건에 맞는 기록만 최신순으로 보인다
6. 역할 관리에서 "파기 기록"만 체크한 역할을 운영자에게 주고 그 운영자로 들어간다: 관리 메뉴에 파기 기록만 보인다

## 운영 배포 때 확인

- 마이그레이션이 `tb_purge_log`와 "파기 기록" 메뉴를 만든다. 메뉴 관리에서 위치를 바꿨던 환경이면 새 메뉴가 운영 맨 끝에 붙는다
- 첫 파기 기록은 배포한 다음 날 새벽 정리 뒤에 생긴다
