# DB 테이블 규칙

moa가 정한 MariaDB 테이블·열·인덱스·마이그레이션 규칙이다. 원칙 문서 "기술 제약"이 이 문서를 가리킨다.
엔터티 부모 클래스, 소프트·하드 삭제 고르기, `ddl-auto: validate`는 키트 `springboot/README.md`를 따른다.
마이그레이션 번호는 키트(`V1_x`)와 달리 이 문서를 따른다.
도구가 만드는 테이블(`flyway_schema_history`)은 이 규칙 밖이다.

## 이름

- 모든 식별자는 소문자 snake_case. 단어는 `_` 하나로 잇는다
- 식별자는 64자를 넘지 않는다 (MariaDB 한도)
- 테이블은 `tb_<이름>`, 단수로 쓴다 (예: `tb_user`, `tb_notice_read`). 이름만 보고 테이블임을 알 수 있게 한다
  - 잇는 테이블(다대다)은 두 이름을 잇는다 (`tb_user_access_role`)
  - 컬렉션 테이블(엔터티가 값 목록을 따로 담는 표)은 주인 뒤에 값 이름을 붙인다 (`tb_access_role_permission`)
- 뷰는 `vw_<이름>`
- 추측성 줄임말을 쓰지 않는다 (`cd`, `nm`, `cnt` X). 널리 쓰는 말(`ip`, `os`)과 표준 이름(`p256dh`)은 된다
- 타입 이름을 열 이름으로 쓰지 않는다 (`datetime`, `text` X)
- MariaDB 예약어를 이름으로 쓰지 않는다 (`order`, `desc`, `system` X → `sort_order`, `description`, `system_group`). 역따옴표로 감싸서 피하지 않는다
- 같은 뜻의 열은 이미 있는 이름·타입·길이를 다시 쓴다 (`name`, `description`, `parent_id`, `sort_order`, `enabled`)

## 키

- PK는 `id BIGINT NOT NULL AUTO_INCREMENT`다
- 아래 표의 경우만 다른 PK를 쓴다. 테이블 `COMMENT` 괄호에 `PK: <경우>`를 적는다

  | 경우 | 알아보는 법 | PK |
  |---|---|---|
  | 1:1 테이블 | 다른 테이블의 행 하나에 많아야 한 행 | 그 행의 ID (`tb_notice_read.user_id`) |
  | 컬렉션 테이블 | 엔터티의 `@ElementCollection` | (주인 ID, 값) (`tb_access_role_permission`) |
  | 라이브러리 테이블 | 라이브러리가 DDL을 정한다 | 라이브러리 정의 그대로 (ShedLock의 `name`) |

- 표에 없으면 `id`를 쓴다. 다른 PK가 필요해 보이면 정하지 말고 `[NEEDS CLARIFICATION]`으로 묻는다. 정해지면 이 표에 한 줄 더한다
- 업무 값(아이디, 코드, 이메일)은 PK로 쓰지 않고 유니크(`uk_`)로 막는다
- 다른 행을 가리키는 열은 `<대상 단수>_id BIGINT` (예: `user_id`, `parent_id`)
- 주소에 드러나는 ID는 순번 대신 `public_id CHAR(36)`(UUID)에 유니크를 건다 (`tb_rich_text_image`)
- 외래 키 제약은 걸지 않는다. 지운 행(소프트 삭제)을 가리키는 행이 남고, 시드와 테스트 정리(`DatabaseCleaner`)가 순서를 따지지 않는다. 가리키는 행이 있는지는 서비스가 본다

## 열

| 무엇 | 규칙 |
|---|---|
| 시각 | `DATETIME(6)`, 이름은 `<동작>_at` (예: `published_at`, `read_at`). `TIMESTAMP`는 시간대 변환이 끼어 쓰지 않는다 |
| 참·거짓 | `BOOLEAN NOT NULL`, 이름은 형용사·과거분사 (`enabled`, `pinned`, `success`) |
| 순서 | `sort_order INT NOT NULL` |
| 상태값 | `VARCHAR`에 enum 이름 |
| 긴 본문 | `MEDIUMTEXT` |

- `DEFAULT`는 두지 않는다. 행을 넣는 코드가 늘 값을 준다. 단 기존 행이 있는 테이블에 `ADD COLUMN ... NOT NULL`로 칸을 더할 때는 기존 행을 채우려고 둔다
- 열의 정렬 규칙(`COLLATE`)은 테이블 기본을 따른다. 다르게 두면 그 이유를 열 `COMMENT`에 적는다

## 감사·삭제 열

엔터티 부모 클래스와 1:1로 맞춘다. 엔터티가 상속한 열은 쓰지 않더라도 둔다(`ddl-auto: validate`).

| 테이블 종류 | 열 |
|---|---|
| `SoftDeleteEntity` (기본) | `created_at`, `updated_at`, `created_by`, `updated_by`, `deleted_at`, `deleted_by` |
| `BaseEntity` (하드 삭제) | `created_at`, `updated_at`, `created_by`, `updated_by` |
| `BaseTimeEntity` (로그·토큰) | `created_at`, `updated_at` |
| 부모 없는 엔터티 (`@HardDelete`) | 필요한 시각 열만 |
| 컬렉션 테이블 | 없음 (주인 행과 함께 저장된다) |

- 마지막 두 종류는 감사 열이 없는 이유를 테이블 `COMMENT`에 적는다 (아래 "COMMENT")

## 인덱스·제약

- 이름을 반드시 붙인다. 이름 없는 제약은 마이그레이션 차이에서 알아볼 수 없다
  - 인덱스 `idx_<테이블>_<열>`, 유니크 `uk_<테이블>_<열>`
  - `<테이블>`은 `tb_`를 뺀 이름, `<열>`은 순서대로 줄이지 않고 다 쓴다 (`idx_notification_user_id_created_at`)
  - 64자를 넘으면 쓰임새로 짧게 이름 짓고(`idx_notice_public_list`) 인덱스 `COMMENT`에 열을 적는다
- 소프트 삭제 테이블의 업무 키 유니크는 활성 행에만 값이 있는 가상 열 `active_key`로 건다 (키트 README "엔터티·감사")
- 리포지토리 쿼리의 조건(`where`)·정렬(`order by`)에 쓰는 열은 인덱스가 있는지 본다. `<대상>_id` 열은 인덱스를 둔다 (PK·유니크의 앞 열이면 따로 두지 않는다)

## COMMENT

- 모든 테이블과 열에 단다. `id`, 감사 열, `active_key`는 뺀다
- 한국어 명사형으로 짧게 쓴다. 문장으로 풀어 쓰지 않는다
  - O `'공지 제목'`, `'잠근 시각'` / X `'공지의 제목을 저장한다'`, `'값이 있으면 로그인이 막힌다'`
  - 열 이름을 옮기기만 한 말은 쓰지 않는다 (`name` → `'이름'` X, `'메뉴 이름'` O)
- 덧붙일 것은 괄호 하나에 단답으로 넣는다
  - 상태값은 값 이름을 나열한다: `'공지 상태 (DRAFT, PUBLISHED)'`. 뜻은 코드 enum을 본다
  - 요일·월처럼 잘 알려진 순서는 처음과 끝만 적는다: `'요일 (MONDAY, ... SUNDAY)'`
  - NULL의 뜻, 단위, 형식: `'읽은 시각 (NULL: 안 읽음)'`, `'파일 크기 (바이트)'`, `'화면 경로 (/로 시작)'`
  - 둘 이상이면 `;`로 나눈다: `'공개 범위 (PUBLIC, MEMBER; NULL: 관리 메뉴)'`
- 감사 열이 없거나 PK가 `id`가 아닌 테이블은 이유를 괄호에 넣는다: `'공지 확인 기록 (PK: 1:1 테이블; 감사 열 없음: 사용자당 1행 덮어쓰기)'`
- 다른 정렬 규칙을 쓴 열은 이유를 괄호에 넣는다: `'닉네임 비교 키 (utf8mb4_bin: 대소문자 구별)'`
- 뜻이 바뀌면 `COMMENT`도 같은 마이그레이션에서 고친다. `MODIFY COLUMN`은 열 정의 전체를 다시 쓰므로 타입·NULL 여부·기본값을 그대로 옮겨 적는다

## 엔터티

- `@Table(name = "tb_…")`을 늘 적는다. 기본 이름 규칙은 `tb_`를 붙이지 않는다
- 인덱스·제약은 마이그레이션에만 둔다. 엔터티에 `@Index`·`uniqueConstraints`를 적지 않는다 (`validate`가 보지 않아 두 곳이 어긋나도 모른다)

## 마이그레이션

- Flyway는 모든 프로필(local, dev, prod)에서 앱이 뜰 때 돈다. 로컬 DB만 지우고 다시 만들 수 있고, 개발·운영 DB는 지우지 않는다
- 위치는 `src/main/resources/db/migration/`, 이름은 `V<자연수>__<설명>.sql`이다. 번호는 1, 2, 3…으로 늘린다 (`V2_1`·`V1_7` 같은 점 번호를 쓰지 않는다)
- `V1__schema.sql`은 처음 스키마 전체, `V2__seed.sql`은 모든 환경에 들어가는 처음 데이터(시스템 계정, 메뉴 등)다. 둘은 고치지 않는다
- 그 뒤 변경은 `V3`부터 하나씩 더한다. 한 파일은 한 가지 변경이고, 그 변경에 필요한 데이터도 같은 파일에 둔다
- 파일 머리 주석은 `-- moa: <무엇>.`으로 시작하고 근거를 적는다. 스펙이 있으면 `스펙 specs/<번호>-<이름> (<FR·research 번호>)`, 없으면 근거 문서 (예: `docs/db.md`)
- dev에 들어간 파일은 고치지 않고 새 파일을 더한다. 아직 dev에 들어가지 않은 내 브랜치의 파일은 고쳐도 된다
- 로컬 전용 데이터(시험 계정 등)는 `db/localdata/R__<설명>.sql`에 둔다. local·test 프로필에서만 돌고 운영에 들어가지 않는다
- 테스트 전용 마이그레이션은 `src/test/resources/db/testmigration/`에 `V900`부터 둔다. 테이블은 이 문서의 이름 규칙을 따른다
- 마이그레이션을 바꾸면 `bootRun`으로 한 번 띄워 Flyway 적용과 `ddl-auto: validate` 통과를 본다

### 번호 겹침 (여럿이 함께 작업할 때)

- 스펙에는 번호를 적지 않는다 (`V<다음 번호>__<설명>.sql`)
- 번호는 PR 직전에 dev를 병합하고 마지막 번호 + 1로 정한다
- CI `migration-numbers`가 이름 형식, 겹친 번호, 빈 번호를 막는다. 막히면 dev를 다시 병합하고 내 파일 번호를 올린다
- 로컬 Flyway가 checksum이나 없는 파일 때문에 멈추면 로컬 DB를 지우고 다시 띄운다

### 키트 헬퍼

- 키트 헬퍼를 새로 들이면 키트의 `V1_x` 파일을 그대로 두지 않는다. 내용을 다음 번호 파일로 옮겨 적고 이 규칙(이름, COMMENT)에 맞춘 뒤 엔터티의 `@Table`을 고친다. 고친 키트 파일은 키트로 옮길 목록에 올린다
