# Data Model: 회원 탈퇴 (member-withdrawal)

마이그레이션은 하나다: `V<다음 번호>__member_withdrawal.sql`. 번호는 적지 않고 PR 직전에 dev를 병합해 마지막 번호 + 1로 정한다 (`docs/db.md` "번호 겹침"). 순서 조건: 009의 `tb_user_game_profile`·`tb_user_profile_play_time` 다음.
새 테이블은 없다. 재가입 제한 기록은 만들지 않는다 (FR-015).

## 회원 — `tb_user` (칸 둘을 더함)

| 칸 | 형식 | 규칙 |
|---|---|---|
| `withdrawn_at` | DATETIME(6) NULL | COMMENT `'탈퇴한 시각 (NULL: 탈퇴 안 함)'`. 복구하면 비운다. 파기해도 남긴다 (FR-018) |
| `purged_at` | DATETIME(6) NULL | COMMENT `'개인정보 파기 시각 (NULL: 파기 안 함)'`. 한 번 채우면 바꾸지 않는다 |

- 탈퇴할 때 `tb_push_subscription`의 그 회원 행을 모두 지운다 (FR-021). 파기할 때의 삭제는 남은 것이 있을 때를 위한 것이다
- 인덱스 `idx_user_purged_at_withdrawn_at` (`purged_at`, `withdrawn_at`), `idx_user_purged_at_deleted_at` (`purged_at`, `deleted_at`): 파기 대상 찾기 (스스로 탈퇴, 관리자가 지움)
- `uk_user_login_id_active_key`와 `active_key` 식은 그대로 둔다. 유예 중에는 `deleted_at`이 NULL이라 아이디가 유니크에 남는다 ([research.md](./research.md) R1)
- 엔터티 `User`: `@SQLRestriction("deleted_at IS NULL AND withdrawn_at IS NULL")` ([research.md](./research.md) R8). `withdraw(now)`(이미 탈퇴했으면 그대로), `restore()`, `isWithdrawn()`. 파기는 엔터티가 아니라 native UPDATE다
- `UserRepository`: `findByLoginId`(native, `deleted_at is null`만), `findIncludingWithdrawn(id)`, `findForUpdateIncludingWithdrawn(id)`(native), `existsWithdrawn(id)`(native), 파기 대상 조회·잠금·덮기(native)
- `tb_purge_log.target`의 COMMENT에 `MEMBER`를 더한다(`MODIFY COLUMN`, `VARCHAR(30) NOT NULL` 그대로, `docs/db.md` "COMMENT")

### 상태 변화

```text
[활동 중] --탈퇴(닉네임 확인)--> [유예 중] --30일 안 로그인·복구--> [활동 중]
                                     └--30일 지남--> [파기 대기] --매일 정리--> [파기됨]
```

| 상태 | `withdrawn_at` | `purged_at` | `deleted_at` | 로그인 | 요청 | 아이디·이메일·소셜 |
|---|---|---|---|---|---|---|
| 활동 중 | NULL | NULL | NULL | 됨 | 됨 | 쓰는 중 |
| 유예 중 | 값 | NULL | NULL | A401, 복구 안내 | A002 | 붙잡음 |
| 파기 대기 | 값 (30일 지남) | NULL | NULL | A402 | A002 | 붙잡음 |
| 파기됨 | 값 | 값 | 값 | 없는 아이디 (A006), 소셜은 새 가입 | - | 풀림 |
| 관리자가 지움 | NULL | NULL | 값 | 지금과 같음 | 지금과 같음 | 지금과 같음 |
| 관리자가 지움, 30일 지나 파기됨 | NULL | 값 | 값 | 없는 아이디 (A006), 소셜은 새 가입 | - | 풀림 |

- 유예 중·파기 대기인 회원은 기본 조회에 나오지 않는다. 관리자 목록에도 나오지 않아 관리자가 지울 수 없다 (FR-008)

## 파기할 때 (FR-013·FR-019, 한 회원 = 한 트랜잭션)

대상: 탈퇴한 지 30일이 지난 회원, 관리자가 지운 지 30일이 지난 회원(`USER`). `purged_at`이 빈 것만 ([research.md](./research.md) R6)

| 테이블 | 하는 일 |
|---|---|
| `tb_user_profile_play_time`, `tb_user_profile`, `tb_user_game_profile`, `tb_user_social_account`, `tb_user_terms_agreement` | 그 회원의 행 DELETE |
| `tb_notification`, `tb_push_subscription`, `tb_notice_read`, `tb_refresh_token` | 그 회원의 행 DELETE |
| `tb_user` | `login_id` = `withdrawn_<id>`, `password` = NULL, `name` = `탈퇴한 회원`, `purged_at` = 지금, `deleted_at`·`deleted_by`는 비었을 때만 지금·시스템 계정, `updated_at` = 지금, `updated_by` = 시스템 계정 |
| `tb_purge_log` | `MEMBER`, `COLUMN_ERASE`, 1건, `SCHEDULER`, `target_user_id` = 그 회원 (`PersonalDataPurgedEvent`) |
| `tb_login_history` | 그대로 (보관 기간 365일) |
| 파티·글·메시지, 1:1 문의 | 그대로 (각 기능) |

- `tb_user`의 새 칸이 생기면(개인정보) 이 표와 파기 코드에 함께 더한다. 빠뜨리지 않게 인수 테스트가 파기 뒤 회원 행의 모든 문자열 칸에 원래 값이 없는지 본다
- 회원이 혼자 쓰는 새 테이블을 만드는 기능은 `docs/privacy.md` 보관 기간 표에 줄을 더하고 "파기됨" 이벤트로 지운다 (FR-017)

## 파기 대상 종류 — `PurgeTarget`

- `MEMBER`("회원")를 더한다. `tb_purge_log.target`은 VARCHAR(30)이라 타입은 그대로고 COMMENT만 고친다

## 저장하지 않는 상태 (서명 쿠키)

| 이름 | 내용 | 수명·범위 |
|---|---|---|
| `withdrawal_recovery` | 회원 ID, 탈퇴 시각 | 10분, Path `/api/v1/auth/recovery`, HttpOnly, `SameSite=Strict`. 복구하면 지움 |

## 설정

| 키 | 기본 | 뜻 |
|---|---|---|
| `app.withdrawal.grace-days` | 30 | 유예 기간 (`docs/privacy.md`와 같은 값) |
| `app.withdrawal.cleanup-cron` | `0 20 5 * * *` | 파기 정리 시각 |
| `app.withdrawal.cleanup-zone` | `Asia/Seoul` | 정리 시각의 시간대. 서버 시간대와 상관없이 이 시각으로 돈다 |

## 이벤트

| 이름 | 언제 | 내용 |
|---|---|---|
| `MemberWithdrawnEvent` | 탈퇴가 저장될 때 | 회원 ID, 탈퇴한 때 |
| `MemberRestoredEvent` | 복구가 저장될 때 | 회원 ID, 복구한 때 |
| `MemberPurgedEvent` | 파기가 저장될 때 | 회원 ID, 파기한 때 |

이번에 받는 쪽은 없다. 붙이는 기능은 `@TransactionalEventListener(AFTER_COMMIT)`로 받는다 ([research.md](./research.md) R7).
