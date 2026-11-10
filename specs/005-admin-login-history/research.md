# Research: 접속 이력 (admin-login-history)

Technical Context에 미결은 없다. 아래는 코드를 확인해 정한 것이다.

## R1. 만들 것의 범위

- **Decision**: 보관 기간 기본값만 바꾸고, 기록·조회·정리는 키트를 그대로 쓴다
- **Rationale**: 기록(FR-001)과 조회, 매일 정리는 키트에 있다. 보관 기간은 `app.login-history.retention-days`로 열려 있고, 지금 기본값이 키트 기본 365일이다
- **Alternatives considered**: moa 전용 정리 작업. 키트와 같은 동작이라 만들 이유가 없다

## R2. 보관 기간을 바꾸는 곳

- **Decision**: `application.yaml`의 `${LOGIN_HISTORY_RETENTION_DAYS:365}`를 `:90`으로, `backend/.env.example`의 `LOGIN_HISTORY_RETENTION_DAYS=365`를 `90`으로 바꾼다
- **Rationale**: 환경 변수를 주지 않은 배포가 90일이 되고, `.env.example`을 복사해 쓰는 배포도 90일이 된다
- **환경 변수는 남긴다**: 운영에서 값을 덮을 수 있다. 다만 90일이 스펙이므로, 다른 값으로 바꾸려면 FR-002를 먼저 고친다
- **키트 코드의 기본값**: `LoginHistoryCleaner`의 `@Value(... :365)`는 설정이 없을 때만 쓰인다. `application.yaml`에 항상 값이 있으므로 고치지 않는다 (원칙 III)
- **이미 배포된 환경**: `.env`에 365가 들어 있으면 그 값이 이긴다. 운영 `.env`에서 이 변수를 지우거나 90으로 바꿔야 한다. 운영 배포가 아직 없으면 할 일이 없다
- **Alternatives considered**: 키트 `LoginHistoryCleaner`의 기본값 고치기. 키트 코드를 고치게 되고, 설정만으로 충분하다

## R3. US1-AC1을 확인하는 방법

- **Decision**: 실제 로그인 API로 한 아이디의 실패와 성공을 남기고(접속 IP와 User-Agent를 준다), 다른 아이디의 이력도 하나 남긴 뒤, 관리자가 `loginId`로 조회한다
- **Rationale**: 스토리 흐름 그대로다. 다른 아이디의 이력을 함께 두어야 "그 아이디로 찾는다"가 실제로 거르는지 확인된다
- **Alternatives considered**: 이력을 DB에 직접 넣기. 기록(FR-001)을 건너뛴다

## R4. FR-002를 확인하는 방법

- **Decision**: 89일 전과 91일 전 이력을 DB에 넣고 `LoginHistoryCleaner.deleteExpired()`를 직접 부른 뒤, 91일 전 것만 지워졌는지 본다
- **Rationale**: 이 기능에서 실제로 바뀌는 것이 보관 기간이다. AC는 아니지만 바꾼 값이 적용되는지 확인해야 한다. 정리 시각(매일 새벽)은 키트가 맡는다
- **Alternatives considered**: 설정 값만 읽어 90인지 보기. 정리 작업이 그 값을 쓰는지까지는 확인하지 못한다

## R5. 프록시 뒤의 접속 IP

- **Decision**: 이 기능에서는 다루지 않는다
- **Rationale**: 스펙 Assumptions가 배포 구성 때 맞추기로 했다. 키트는 `request.getRemoteAddr()`를 남기므로, 테스트는 요청의 접속 주소가 그대로 남는지만 본다
- **배포 때 참고**: 키트 `LoginHistoryRecorder` 주석은 프록시 뒤라면 `server.forward-headers-strategy=native`로 실제 IP를 받으라고 안내한다

## R6. 테스트 이름과 위치

- **Decision**: `AdminLoginHistoryAcceptanceTest` 한 클래스에 `@DisplayName("US1-AC1 ...")`, `@DisplayName("FR-002 ...")`로 둔다
- **Rationale**: 원칙 II의 형식이다. AC가 아닌 확인은 FR 번호로 이름을 붙여 스펙과 이어지게 한다
