// npm run gen:api가 만든 파일이다. 손으로 고치지 않는다 (scripts/gen-api.mjs)

export interface paths {
    "/api/v1/users/me/password": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 내 비밀번호 바꾸기
         * @description 바꾸면 비밀번호 변경 요구(passwordChangeRequired)가 풀린다. 현재 비밀번호가 틀리거나 새 비밀번호가 같으면 V001, 비밀번호가 없는 계정(소셜)은 B001
         */
        put: operations["changePassword"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/users/{userId}/roles": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 사용자의 역할
         * @description 없는 사용자 R001
         */
        get: operations["getRolesOfUser"];
        /**
         * 사용자의 역할 바꾸기
         * @description 보낸 역할로 통째로 바꾼다. 다음 요청부터 바로 반영된다. 없는 사용자·역할 R001, 주거나 빼는 역할을 다룰 수 없으면 A005
         */
        put: operations["replaceRolesOfUser"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/users/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 사용자 하나
         * @description 없으면 R001
         */
        get: operations["get"];
        /**
         * 사용자 정보 바꾸기
         * @description 이름. 없으면 R001, ADMIN이 아닌데 ADMIN 계정이면 A005
         */
        put: operations["rename"];
        post?: never;
        /**
         * 사용자 지우기
         * @description 소프트 삭제, 모든 기기에서 로그아웃. 같은 아이디로 다시 만들 수 있다. 자기 자신 B001, ADMIN이 아닌데 ADMIN 계정이면 A005
         */
        delete: operations["delete"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/users/{id}/role": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 시스템 역할 바꾸기
         * @description ADMIN만 (아니면 A005). 자기 자신은 B001. 다음 토큰 재발급부터 반영된다
         */
        put: operations["changeRole"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/users/{id}/lock": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 잠그기
         * @description 로그인·토큰 재발급을 막고 모든 기기에서 로그아웃시킨다. 자기 자신 B001, ADMIN이 아닌데 ADMIN 계정이면 A005
         */
        put: operations["lock"];
        post?: never;
        /**
         * 잠금 풀기
         * @description ADMIN이 아닌데 ADMIN 계정이면 A005
         */
        delete: operations["unlock"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/roles/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 역할 하나
         * @description 없으면 R001
         */
        get: operations["getRole"];
        /**
         * 역할 바꾸기
         * @description 이름·설명·권한을 바꾼다 (코드는 그대로). 없으면 R001, 등록되지 않은 권한 V001, 다룰 수 없는 역할·권한 A005
         */
        put: operations["modify"];
        post?: never;
        /**
         * 역할 지우기
         * @description 소프트 삭제. 이 역할을 가진 사용자에게서도 빠진다. 없으면 R001, 다룰 수 없는 역할 A005
         */
        delete: operations["delete_1"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/menus/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 메뉴 바꾸기
         * @description 없는 메뉴 R001, 그 밖의 오류는 만들기와 같다. 자기 자신을 부모로 B001
         */
        put: operations["modify_1"];
        post?: never;
        /**
         * 메뉴 지우기
         * @description 소프트 삭제. 하위 메뉴가 있으면 B001
         */
        delete: operations["delete_2"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/menus/order": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 순서 한 번에 바꾸기
         * @description 보낸 메뉴의 부모와 순서를 한 트랜잭션으로 바꾼다. 하나라도 틀리면 아무것도 바꾸지 않는다
         */
        put: operations["reorder"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/code-groups/{groupCode}/codes/{code}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 코드 바꾸기
         * @description 이름, 정렬 순서, 사용 여부
         */
        put: operations["modifyCode"];
        post?: never;
        /**
         * 코드 지우기
         * @description 소프트 삭제. 같은 코드로 다시 만들 수 있다
         */
        delete: operations["deleteCode"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/auth/token/refresh": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 토큰 재발급
         * @description 리프레시 토큰으로 액세스 토큰을 다시 받는다. 리프레시 토큰도 새 것으로 바뀐다
         */
        post: operations["refresh"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/auth/logout": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 로그아웃
         * @description 리프레시 토큰을 폐기한다. 토큰이 없거나 이미 폐기됐어도 성공한다
         */
        post: operations["logout"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/auth/login": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 로그인
         * @description 아이디·비밀번호로 로그인한다. 웹은 리프레시 토큰을 HttpOnly 쿠키로, 앱은 본문으로 받는다
         */
        post: operations["login"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/users": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 사용자 목록
         * @description 최신순 페이징. 지운 사용자와 시스템 계정은 빠진다
         */
        get: operations["search"];
        put?: never;
        /**
         * 사용자 만들기
         * @description 임시 비밀번호로 만들고 응답에 한 번만 보인다. 아이디 형식 V001, 중복 R002, ADMIN이 아닌데 ADMIN으로 만들면 A005
         */
        post: operations["create"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/users/{id}/password-reset": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 비밀번호 초기화
         * @description 임시 비밀번호로 바꾸고 응답에 한 번만 보인다. 모든 기기에서 로그아웃시킨다. 비밀번호 없는 계정(소셜) B001, ADMIN이 아닌데 ADMIN 계정이면 A005
         */
        post: operations["resetPassword"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/roles": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 역할 목록
         * @description 만든 순서대로
         */
        get: operations["getRoles"];
        put?: never;
        /**
         * 역할 만들기
         * @description 코드 형식·등록되지 않은 권한 V001, 내가 가지지 않은 권한 A005, 코드 중복 R002
         */
        post: operations["create_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/menus": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** 메뉴 트리 (사용 안 함 포함) */
        get: operations["getMenus"];
        put?: never;
        /**
         * 메뉴 만들기
         * @description 경로 형식·등록되지 않은 권한 V001, 경로 중복 R002, 없는 부모 R001, 3단계가 되는 부모 B001
         */
        post: operations["create_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/code-groups": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** 코드 그룹 목록 */
        get: operations["getGroups"];
        put?: never;
        /**
         * 코드 그룹 만들기
         * @description 같은 그룹 코드가 있으면 R002
         */
        post: operations["createGroup"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/code-groups/{groupCode}/codes": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** 그룹의 코드 목록 (사용 안 함 포함) */
        get: operations["getCodes"];
        put?: never;
        /**
         * 코드 만들기
         * @description 그룹이 없으면 R001, 같은 코드가 있으면 R002
         */
        post: operations["createCode"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/users/me": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 내 정보
         * @description 로그인한 사용자의 정보. 화면이 로그인 상태와 역할을 확인할 때 쓴다
         */
        get: operations["me"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/users/me/permissions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 내 권한
         * @description 로그인한 사용자의 권한 키. 화면이 버튼·메뉴를 숨길 때 쓴다 (API 보호는 서버가 따로 한다)
         */
        get: operations["myPermissions"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/menus/me": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 내 메뉴
         * @description 사용 중이고 내 권한으로 볼 수 있는 메뉴 트리 (2단계). 권한 키가 없는 메뉴는 모두가 본다. ADMIN은 모두 본다. 보이는 하위 메뉴가 없는 그룹은 빠진다
         */
        get: operations["me_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/codes/{groupCode}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 그룹의 코드 목록
         * @description 선택 상자 등에 쓴다. includeDisabled=true면 사용 안 함 코드도 enabled=false로 준다 (예전 데이터의 코드 이름을 보일 때). 지운 코드는 주지 않는다. 그룹이 없으면 R001
         */
        get: operations["getCodes_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/permissions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 권한 목록
         * @description 코드에 등록된 권한 전부. 역할에 묶을 수 있는 것이다
         */
        get: operations["getPermissions"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/login-histories": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 접속 이력 목록
         * @description 최신순. 조건(userId, loginId, success, from, to)은 모두 선택이고 함께 주면 AND. from이 to보다 늦으면 V001
         */
        get: operations["getPage"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        /** @description 내 비밀번호 바꾸기 요청 */
        PasswordChangeRequest: {
            /** @description 지금 비밀번호 (임시 비밀번호 포함) */
            currentPassword: string;
            /** @description 새 비밀번호. 8~100자 */
            newPassword: string;
        };
        /** @description 사용자 역할 바꾸기 요청 */
        UserRoleReplaceRequest: {
            /**
             * @description 줄 역할 ID 전부. 빈 목록이면 모두 뺀다
             * @example [
             *       1,
             *       2
             *     ]
             */
            roleIds: number[];
        };
        /** @description 사용자 정보 바꾸기 요청 */
        UserRenameRequest: {
            /**
             * @description 이름
             * @example 홍길동
             */
            name: string;
        };
        /** @description 사용자 (관리) */
        UserAdminResponse: {
            /**
             * Format: int64
             * @description 사용자 ID
             */
            id: number;
            /**
             * @description 로그인 아이디
             * @example hong
             */
            loginId: string;
            /**
             * @description 이름
             * @example 홍길동
             */
            name: string;
            /**
             * @description 시스템 역할
             * @example USER
             * @enum {string}
             */
            role: "USER" | "ADMIN";
            /** @description 잠김 여부 */
            locked: boolean;
            /**
             * Format: date-time
             * @description 잠근 시각 (시간대 포함). 잠기지 않았으면 없다
             */
            lockedAt?: string;
            /** @description 임시 비밀번호를 받아 아직 바꾸지 않았다 */
            passwordChangeRequired: boolean;
            /** @description 비밀번호로 로그인하는 계정인지. false(소셜)면 비밀번호 초기화를 할 수 없다 */
            hasPassword: boolean;
            /**
             * Format: date-time
             * @description 만든 시각 (시간대 포함)
             */
            createdAt: string;
        };
        /** @description 시스템 역할 바꾸기 요청 */
        UserRoleChangeRequest: {
            /**
             * @description 시스템 역할
             * @example ADMIN
             * @enum {string}
             */
            role: "USER" | "ADMIN";
        };
        /** @description 역할 바꾸기 요청. 코드는 바꾸지 않는다 */
        RoleModifyRequest: {
            /**
             * @description 이름
             * @example 운영 매니저
             */
            name: string;
            /** @description 설명 */
            description?: string;
            /**
             * @description 묶을 권한 키 (통째로 바꾼다)
             * @example [
             *       "CODE_MANAGE"
             *     ]
             */
            permissions: string[];
        };
        /** @description 역할 */
        RoleResponse: {
            /**
             * Format: int64
             * @description 역할 ID
             */
            id: number;
            /**
             * @description 역할 코드
             * @example MANAGER
             */
            code: string;
            /**
             * @description 이름
             * @example 운영 매니저
             */
            name: string;
            /** @description 설명 */
            description?: string;
            /**
             * @description 묶은 권한 키 (이름순)
             * @example [
             *       "CODE_MANAGE"
             *     ]
             */
            permissions: string[];
            /**
             * Format: int64
             * @description 이 역할을 가진 사용자 수
             * @example 3
             */
            userCount: number;
        };
        /** @description 메뉴 바꾸기 요청 */
        MenuModifyRequest: {
            /**
             * Format: int64
             * @description 부모 메뉴 ID. 없으면 1단계
             */
            parentId?: number;
            /**
             * @description 이름
             * @example 코드 관리
             */
            name: string;
            /**
             * @description 화면 경로 (/로 시작). 그룹이면 비운다
             * @example /admin/codes
             */
            path?: string;
            /**
             * @description 아이콘 이름
             * @example list
             */
            icon?: string;
            /**
             * Format: int32
             * @description 정렬 순서
             * @example 10
             */
            sortOrder: number;
            /** @description 사용 여부. false면 내 메뉴에서 빠진다 */
            enabled: boolean;
            /**
             * @description 볼 때 필요한 권한 키. 비우면 로그인한 사용자 모두가 본다. 등록되지 않은 키는 V001
             * @example CODE_MANAGE
             */
            permission?: string;
        };
        /** @description 메뉴 (관리) */
        MenuTreeResponse: {
            /**
             * Format: int64
             * @description 메뉴 ID
             */
            id: number;
            /**
             * Format: int64
             * @description 부모 메뉴 ID. 1단계면 없다
             */
            parentId?: number;
            /**
             * @description 이름
             * @example 코드 관리
             */
            name: string;
            /**
             * @description 화면 경로. 그룹이면 없다
             * @example /admin/codes
             */
            path?: string;
            /**
             * @description 아이콘 이름
             * @example list
             */
            icon?: string;
            /**
             * Format: int32
             * @description 정렬 순서
             * @example 10
             */
            sortOrder: number;
            /** @description 사용 여부 */
            enabled: boolean;
            /**
             * @description 볼 때 필요한 권한 키. 없으면 로그인한 사용자 모두
             * @example CODE_MANAGE
             */
            permission?: string;
            /** @description 하위 메뉴 */
            children: components["schemas"]["MenuTreeResponse"][];
        };
        /** @description 순서 바꾸기 한 줄 */
        MenuOrderRequest: {
            /**
             * Format: int64
             * @description 메뉴 ID
             */
            id: number;
            /**
             * Format: int64
             * @description 부모 메뉴 ID. 없으면 1단계
             */
            parentId?: number;
            /**
             * Format: int32
             * @description 정렬 순서
             * @example 10
             */
            sortOrder: number;
        };
        /** @description 코드 바꾸기 요청. 코드 값 자체는 바꾸지 않는다 */
        CodeModifyRequest: {
            /**
             * @description 이름
             * @example 결제 완료
             */
            name: string;
            /**
             * Format: int32
             * @description 정렬 순서
             * @example 10
             */
            sortOrder: number;
            /** @description 사용 여부. false면 화면 목록에서 빠진다 */
            enabled: boolean;
        };
        /** @description 코드 */
        CodeListResponse: {
            /**
             * @description 코드
             * @example PAID
             */
            code: string;
            /**
             * @description 이름
             * @example 결제 완료
             */
            name: string;
            /**
             * Format: int32
             * @description 정렬 순서
             * @example 10
             */
            sortOrder: number;
            /** @description 사용 여부 */
            enabled: boolean;
        };
        /** @description 오류 응답 */
        ErrorResponse: {
            /**
             * @description 에러 코드
             * @example V001
             */
            code: string;
            /** @description 기본 설명 문장. 로그·API 문서용이고 화면에 쓰지 않는다. 화면 문구는 code로 고른다 */
            message: string;
            /**
             * Format: date-time
             * @description 발생 시각
             */
            timestamp: string;
            /** @description 요청 경로 */
            path: string;
            /** @description 요청 추적 ID. 서버 로그와 같은 값 */
            traceId?: string;
            /** @description 필드별 검증 오류. V001일 때만 있다 */
            errors?: components["schemas"]["FieldError"][];
            /**
             * @description 화면 안내에 끼울 값. 값이 필요한 오류만 있다 (문장이 아닌 데이터, 시각은 ISO-8601)
             * @example {
             *       "lockedUntil": "2026-10-10T14:30:00+09:00"
             *     }
             */
            details?: {
                [key: string]: unknown;
            };
        };
        /** @description 필드별 검증 오류 */
        FieldError: {
            /**
             * @description 필드
             * @example email
             */
            field: string;
            /** @description 사유 */
            message: string;
        };
        /** @description 토큰 재발급·로그아웃 요청. 앱만 본문을 보낸다. 웹은 쿠키로 보내므로 본문이 없다 */
        AuthRefreshRequest: {
            /** @description 리프레시 토큰 (앱) */
            refreshToken?: string;
        };
        /** @description 토큰 발급 응답 */
        AuthRefreshResponse: {
            /** @description 액세스 토큰 */
            accessToken: string;
            /**
             * @description 토큰 유형
             * @example Bearer
             */
            tokenType: string;
            /**
             * Format: int64
             * @description 액세스 토큰 남은 시간(초)
             * @example 900
             */
            expiresIn: number;
            /** @description 리프레시 토큰. 앱(X-Client-Type: app)에만 준다 */
            refreshToken?: string;
        };
        /** @description 로그인 요청 */
        AuthLoginRequest: {
            /**
             * @description 로그인 아이디
             * @example user
             */
            loginId: string;
            /**
             * @description 비밀번호
             * @example user1234!
             */
            password: string;
        };
        /** @description 사용자 만들기 요청. 비밀번호는 서버가 임시로 만든다 */
        UserCreateRequest: {
            /**
             * @description 로그인 아이디. 영문·숫자·점·밑줄·하이픈 3~50자
             * @example hong
             */
            loginId: string;
            /**
             * @description 이름
             * @example 홍길동
             */
            name: string;
            /**
             * @description 시스템 역할. ADMIN은 ADMIN만 줄 수 있다
             * @example USER
             * @enum {string}
             */
            role: "USER" | "ADMIN";
        };
        /** @description 만든 사용자와 임시 비밀번호 */
        UserCreateResponse: {
            /** @description 만든 사용자 */
            user: components["schemas"]["UserAdminResponse"];
            /**
             * @description 임시 비밀번호. 이 응답에서만 보인다. 사용자는 다음 로그인 때 바꿔야 한다
             * @example aB3$kP9mQ2xZ
             */
            temporaryPassword: string;
        };
        /** @description 임시 비밀번호 */
        TemporaryPasswordResponse: {
            /**
             * @description 임시 비밀번호. 이 응답에서만 보인다. 사용자는 다음 로그인 때 바꿔야 한다
             * @example aB3$kP9mQ2xZ
             */
            temporaryPassword: string;
        };
        /** @description 역할 만들기 요청 */
        RoleCreateRequest: {
            /**
             * @description 역할 코드. 영문 대문자·숫자·밑줄, 만든 뒤 바꾸지 않는다
             * @example MANAGER
             */
            code: string;
            /**
             * @description 이름
             * @example 운영 매니저
             */
            name: string;
            /** @description 설명 */
            description?: string;
            /**
             * @description 묶을 권한 키. 등록되지 않은 키는 V001
             * @example [
             *       "CODE_MANAGE"
             *     ]
             */
            permissions: string[];
        };
        /** @description 메뉴 만들기 요청 */
        MenuCreateRequest: {
            /**
             * Format: int64
             * @description 부모 메뉴 ID. 없으면 1단계
             */
            parentId?: number;
            /**
             * @description 이름
             * @example 코드 관리
             */
            name: string;
            /**
             * @description 화면 경로 (/로 시작). 그룹이면 비운다
             * @example /admin/codes
             */
            path?: string;
            /**
             * @description 아이콘 이름
             * @example list
             */
            icon?: string;
            /**
             * Format: int32
             * @description 정렬 순서. 작을수록 앞
             * @example 10
             */
            sortOrder: number;
            /**
             * @description 볼 때 필요한 권한 키. 비우면 로그인한 사용자 모두가 본다. 등록되지 않은 키는 V001
             * @example CODE_MANAGE
             */
            permission?: string;
        };
        /** @description 코드 그룹 만들기 요청 */
        CodeGroupCreateRequest: {
            /**
             * @description 그룹 코드. 영문 대문자·숫자·밑줄
             * @example ORDER_STATUS
             */
            groupCode: string;
            /**
             * @description 이름
             * @example 주문 상태
             */
            name: string;
            /** @description 설명 */
            description?: string;
        };
        /** @description 코드 그룹 */
        CodeGroupListResponse: {
            /**
             * @description 그룹 코드
             * @example ORDER_STATUS
             */
            groupCode: string;
            /**
             * @description 이름
             * @example 주문 상태
             */
            name: string;
            /** @description 설명 */
            description?: string;
        };
        /** @description 코드 만들기 요청 */
        CodeCreateRequest: {
            /**
             * @description 코드. 영문 대문자·숫자·밑줄
             * @example PAID
             */
            code: string;
            /**
             * @description 이름
             * @example 결제 완료
             */
            name: string;
            /**
             * Format: int32
             * @description 정렬 순서. 작을수록 앞
             * @example 10
             */
            sortOrder: number;
        };
        /** @description 사용자 */
        UserGetResponse: {
            /**
             * Format: int64
             * @description 사용자 ID
             * @example 3
             */
            id: number;
            /**
             * @description 로그인 아이디
             * @example user
             */
            loginId: string;
            /**
             * @description 이름
             * @example 사용자
             */
            name: string;
            /**
             * @description 역할
             * @example USER
             * @enum {string}
             */
            role: "USER" | "ADMIN";
            /** @description 임시 비밀번호를 받아 바꿔야 한다. true면 화면이 비밀번호 변경으로 보낸다 */
            passwordChangeRequired: boolean;
        };
        MyPermissionResponse: {
            permissions: string[];
        };
        /** @description 내 메뉴 */
        MyMenuResponse: {
            /**
             * Format: int64
             * @description 메뉴 ID
             */
            id: number;
            /**
             * @description 이름
             * @example 코드 관리
             */
            name: string;
            /**
             * @description 화면 경로. 그룹이면 없다
             * @example /admin/codes
             */
            path?: string;
            /**
             * @description 아이콘 이름
             * @example list
             */
            icon?: string;
            /** @description 하위 메뉴 */
            children: components["schemas"]["MyMenuResponse"][];
        };
        /** @description 사용자 검색 조건. 모두 선택 */
        UserSearchRequest: {
            /**
             * @description 아이디나 이름에 들어 있는 글자
             * @example hong
             */
            keyword?: string;
            /**
             * @description 시스템 역할
             * @example USER
             * @enum {string}
             */
            role?: "USER" | "ADMIN";
            /** @description 잠김 여부 */
            locked?: boolean;
        };
        /** @description 페이징 응답 */
        PageResponseUserAdminResponse: {
            /** @description 항목 */
            content: components["schemas"]["UserAdminResponse"][];
            /**
             * Format: int32
             * @description 페이지 번호 (0부터)
             */
            page: number;
            /**
             * Format: int32
             * @description 페이지 크기
             */
            size: number;
            /**
             * Format: int64
             * @description 전체 항목 수
             */
            totalElements: number;
            /**
             * Format: int32
             * @description 전체 페이지 수
             */
            totalPages: number;
        };
        /** @description 권한 (코드에 등록된 것) */
        PermissionResponse: {
            /**
             * @description 권한 키
             * @example CODE_MANAGE
             */
            key: string;
            /**
             * @description 이름
             * @example 공통코드 관리
             */
            name: string;
            /**
             * @description 화면에서 묶을 이름
             * @example 공통코드
             */
            group: string;
        };
        /** @description 접속 이력 검색 조건 */
        LoginHistorySearchRequest: {
            /**
             * Format: int64
             * @description 사용자 ID. 그 사용자의 성공한 로그인만 (실패 이력에는 사용자 ID가 없다)
             */
            userId?: number;
            /**
             * @description 입력한 아이디. 정확히 일치
             * @example kim
             */
            loginId?: string;
            /** @description true면 성공만, false면 실패만. 없으면 전체 */
            success?: boolean;
            /**
             * Format: date
             * @description 시작일 (포함)
             * @example 2026-11-01
             */
            from?: string;
            /**
             * Format: date
             * @description 종료일 (포함, 그날 끝까지). from보다 이르면 V001
             * @example 2026-11-30
             */
            to?: string;
        };
        /** @description 접속 이력 */
        LoginHistoryListResponse: {
            /**
             * Format: int64
             * @description 이력 ID
             */
            id: number;
            /**
             * Format: int64
             * @description 사용자 ID. 실패하면 없다
             */
            userId?: number;
            /** @description 입력한 아이디 */
            loginId: string;
            /** @description 성공 여부 */
            success: boolean;
            /**
             * @description 실패 코드
             * @example A006
             */
            failureCode?: string;
            /** @description 접속 IP */
            ip?: string;
            /**
             * @description 기기 종류
             * @example PC
             */
            deviceType: string;
            /**
             * @description OS
             * @example Windows
             */
            os: string;
            /**
             * @description 브라우저
             * @example Chrome
             */
            browser: string;
            /**
             * Format: date-time
             * @description 시각 (시간대 포함)
             */
            createdAt: string;
        };
        /** @description 페이징 응답 */
        PageResponseLoginHistoryListResponse: {
            /** @description 항목 */
            content: components["schemas"]["LoginHistoryListResponse"][];
            /**
             * Format: int32
             * @description 페이지 번호 (0부터)
             */
            page: number;
            /**
             * Format: int32
             * @description 페이지 크기
             */
            size: number;
            /**
             * Format: int64
             * @description 전체 항목 수
             */
            totalElements: number;
            /**
             * Format: int32
             * @description 전체 페이지 수
             */
            totalPages: number;
        };
    };
    responses: never;
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    changePassword: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PasswordChangeRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getRolesOfUser: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                userId: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RoleResponse"][];
                };
            };
        };
    };
    replaceRolesOfUser: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                userId: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UserRoleReplaceRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    get: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["UserAdminResponse"];
                };
            };
        };
    };
    rename: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UserRenameRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["UserAdminResponse"];
                };
            };
        };
    };
    delete: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    changeRole: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UserRoleChangeRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["UserAdminResponse"];
                };
            };
        };
    };
    lock: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    unlock: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    getRole: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RoleResponse"];
                };
            };
        };
    };
    modify: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["RoleModifyRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RoleResponse"];
                };
            };
        };
    };
    delete_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    modify_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["MenuModifyRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["MenuTreeResponse"];
                };
            };
        };
    };
    delete_2: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    reorder: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["MenuOrderRequest"][];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    modifyCode: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                groupCode: string;
                code: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CodeModifyRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["CodeListResponse"];
                };
            };
        };
    };
    deleteCode: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                groupCode: string;
                code: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    refresh: {
        parameters: {
            query?: never;
            header?: {
                /** @description 앱이면 app. 리프레시 토큰을 본문으로 주고받는다 */
                "X-Client-Type"?: string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: {
            content: {
                "application/json": components["schemas"]["AuthRefreshRequest"];
            };
        };
        responses: {
            /** @description 재발급 성공 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["AuthRefreshResponse"];
                };
            };
            /** @description A003 무효·만료, A004 재사용 감지 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ErrorResponse"];
                };
            };
        };
    };
    logout: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: {
            content: {
                "application/json": components["schemas"]["AuthRefreshRequest"];
            };
        };
        responses: {
            /** @description 로그아웃 성공 */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    login: {
        parameters: {
            query?: never;
            header?: {
                /** @description 앱이면 app. 리프레시 토큰을 본문으로 받는다 */
                "X-Client-Type"?: string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["AuthLoginRequest"];
            };
        };
        responses: {
            /** @description 로그인 성공 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["AuthRefreshResponse"];
                };
            };
            /** @description A006 아이디 또는 비밀번호가 틀림 */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ErrorResponse"];
                };
            };
        };
    };
    search: {
        parameters: {
            query: {
                request: components["schemas"]["UserSearchRequest"];
                page?: number;
                size?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PageResponseUserAdminResponse"];
                };
            };
        };
    };
    create: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UserCreateRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["UserCreateResponse"];
                };
            };
        };
    };
    resetPassword: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: number;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["TemporaryPasswordResponse"];
                };
            };
        };
    };
    getRoles: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RoleResponse"][];
                };
            };
        };
    };
    create_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["RoleCreateRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RoleResponse"];
                };
            };
        };
    };
    getMenus: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["MenuTreeResponse"][];
                };
            };
        };
    };
    create_2: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["MenuCreateRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["MenuTreeResponse"];
                };
            };
        };
    };
    getGroups: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["CodeGroupListResponse"][];
                };
            };
        };
    };
    createGroup: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CodeGroupCreateRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["CodeGroupListResponse"];
                };
            };
        };
    };
    getCodes: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                groupCode: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["CodeListResponse"][];
                };
            };
        };
    };
    createCode: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                groupCode: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CodeCreateRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["CodeListResponse"];
                };
            };
        };
    };
    me: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["UserGetResponse"];
                };
            };
        };
    };
    myPermissions: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["MyPermissionResponse"];
                };
            };
        };
    };
    me_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["MyMenuResponse"][];
                };
            };
        };
    };
    getCodes_1: {
        parameters: {
            query?: {
                includeDisabled?: boolean;
            };
            header?: never;
            path: {
                groupCode: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["CodeListResponse"][];
                };
            };
        };
    };
    getPermissions: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PermissionResponse"][];
                };
            };
        };
    };
    getPage: {
        parameters: {
            query: {
                search: components["schemas"]["LoginHistorySearchRequest"];
                /** @description 페이지 (0부터) */
                page?: number;
                /** @description 페이지 크기 */
                size?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PageResponseLoginHistoryListResponse"];
                };
            };
        };
    };
}
