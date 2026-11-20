# DB 테이블 규칙

moa가 정한 MariaDB 테이블·열·인덱스 규칙이다. 원칙 문서 "기술 제약"이 이 문서를 가리킨다.
마이그레이션 위치·파일 이름·키트 번호(`V1_x`), 이미 적용된 파일을 고치지 않는 것, `ddl-auto: validate`,
엔터티 부모 클래스는 키트 `springboot/README.md`를 따르고 여기 다시 적지 않는다.

## 이름

- 모든 식별자는 소문자 snake_case. 단어는 `_` 하나로 잇는다
- 테이블은 `tb_<이름>`, 단수로 쓴다 (예: `tb_user`, `tb_notice_read`)
  - 이름만 보고 테이블임을 알 수 있게 한다 (뷰·인덱스·제약과 구별)
  - 잇는 테이블(다대다)은 두 이름을 잇는다 (예: `tb_user_access_role`)
- 뷰는 `vw_<이름>`
- 추측성 줄임말을 쓰지 않는다 (`cd`, `nm`, `cnt` X). 널리 쓰는 말(`ip`, `os`)과 표준 이름(`p256dh`)은 된다
- 타입 이름을 열 이름으로 쓰지 않는다 (`datetime`, `text` X)
- 예약어를 이름으로 쓰지 않는다 (`order`, `desc`, `system` X → `sort_order`, `description`, `system_group`). 역따옴표로 감싸서 피하지 않는다
- 같은 뜻의 열은 이미 있는 이름을 다시 쓴다 (`name`, `description`, `parent_id`, `sort_order`, `enabled`)

## 열

| 무엇 | 규칙 |
|---|---|
| PK | `id BIGINT NOT NULL AUTO_INCREMENT` 하나 |
| 다른 행을 가리키는 열 | `<대상 단수>_id BIGINT` (예: `user_id`, `parent_id`) |
| 시각 | `DATETIME(6)`, 이름은 `<동작>_at` (예: `published_at`, `read_at`). `TIMESTAMP`는 시간대 변환이 끼어 쓰지 않는다 |
| 참·거짓 | `BOOLEAN NOT NULL`, 이름은 형용사·과거분사 (`enabled`, `pinned`, `success`) |
| 순서 | `sort_order INT NOT NULL` |
| 상태값 | `VARCHAR`에 enum 이름. 값 목록은 `COMMENT`에 적는다 |
| 긴 본문 | `MEDIUMTEXT` |

- `DEFAULT`는 두지 않는다. 행을 넣는 코드가 늘 값을 준다. 단 `ALTER TABLE ... ADD COLUMN ... NOT NULL`로 기존 행을 채울 때는 둔다

## 감사·삭제 열

엔터티 부모 클래스와 1:1로 맞춘다. 엔터티가 상속한 열은 쓰지 않더라도 둔다(`ddl-auto: validate`).

| 엔터티 부모 | 열 |
|---|---|
| `SoftDeleteEntity` (기본) | `created_at`, `updated_at`, `created_by`, `updated_by`, `deleted_at`, `deleted_by` |
| `BaseEntity` (하드 삭제) | `created_at`, `updated_at`, `created_by`, `updated_by` |
| `BaseTimeEntity` (로그·토큰) | `created_at`, `updated_at` |
| 부모 없음 (알림·구독처럼 쓰고 버리는 행) | 필요한 시각 열만. 테이블 `COMMENT`에 이유를 적는다 |

## 인덱스·제약

- 이름을 반드시 붙인다. 이름 없는 제약은 마이그레이션 차이에서 알아볼 수 없다
  - 인덱스 `idx_<테이블>_<열>`, 유니크 `uk_<테이블>_<열>`
  - `<테이블>`은 `tb_`를 뺀 이름, `<열>`은 순서대로 줄이지 않고 다 쓴다 (`idx_notification_user_id_created_at`)
- 소프트 삭제 테이블의 업무 키 유니크는 활성 행에만 값이 있는 가상 열 `active_key`로 건다 (키트 README "엔터티·감사")
- 조회 조건·정렬에 쓰는 열은 인덱스가 있는지 본다. `<대상>_id` 열은 인덱스를 둔다 (PK·유니크의 앞 열이면 따로 두지 않는다)
- 외래 키 제약은 걸지 않는다. 지운 행(소프트 삭제)을 가리키는 행이 남고, 키트 시드와 테스트 정리(`DatabaseCleaner`)가 순서를 따지지 않는다. 가리키는 행이 있는지는 서비스가 본다

## 테이블 공통

- `ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci`
- 테이블마다 `COMMENT`를 한 줄 단다
- 새 테이블은 모든 열에 `COMMENT`를 단다. 감사 열(`created_at` 등)과 `id`는 뺀다

## 마이그레이션

- moa 번호는 `V2_<n>`이다. PR 올리기 전에 dev의 마지막 번호를 다시 보고 맞춘다
- 파일 머리 주석은 `-- moa: <무엇>. 스펙 specs/<번호>-<이름> (<FR·research 번호>)`로 시작한다
- 한 파일은 한 가지 변경이다. 같은 기능의 스키마 변경과 시드는 한 파일에 둔다
- 키트 헬퍼를 새로 들이거나 올려 이 규칙을 따르지 않는 테이블이 생기면, 같은 PR에서 moa 마이그레이션으로 이름을 바꾸고 엔터티의 `@Table`을 고친다. 고친 키트 파일은 키트로 옮길 목록에 올린다
