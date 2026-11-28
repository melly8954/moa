// npm run gen:api가 만든 파일이다. 손으로 고치지 않는다 (scripts/gen-api.mjs)

export interface paths {
    "/api/v1/users/me/profile": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 내 프로필
         * @description 프로필을 아직 만들지 않았으면 기본값. 관리자 계정은 A201
         */
        get: operations["getMyProfile"];
        /**
         * 프로필 저장
         * @description 닉네임·게임 정보를 뺀 모든 칸을 보낸 값으로 바꾼다. 칸 오류는 V001, 관리자 계정은 A201
         */
        put: operations["saveProfile"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
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
    "/api/v1/users/me/nickname": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 닉네임 바꾸기
         * @description 지금과 같으면 그대로. 규칙 V001, 변경 제한 기간 안 B202(다시 바꿀 수 있는 시각은 프로필 조회의 nicknameChangeableAt), 태그가 다 차면 B201, 관리자 계정은 A201
         */
        put: operations["changeNickname"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/notifications/{id}/read": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 알림 읽음
         * @description 이미 읽었으면 그대로. 내 알림이 아니거나 없으면 R001
         */
        put: operations["read"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/notifications/read-all": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /** 모두 읽음 */
        put: operations["readAll"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/notices/seen": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 공지 목록 확인
         * @description 지금(서버 시각)을 공지 목록을 마지막으로 연 시각으로 기록한다
         */
        put: operations["markSeen"];
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
         * @description 보낸 역할로 통째로 바꾼다. 다음 요청부터 바로 반영된다. 없는 사용자·역할 R001, 주거나 빼는 역할을 다룰 수 없으면 A005. moa: 대상이 회원이면 B001, 최고 관리자가 0명이 되면 B001. 최고 관리자 역할을 고르면 그 역할만 남는다. 사용자 관리 드로어도 쓴다
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
         * @description 이름. 없으면 R001, 다룰 수 없는 계정(최고 관리자 계정, 나보다 권한이 많은 관리자 계정) A005
         */
        put: operations["rename"];
        post?: never;
        /**
         * 사용자 지우기
         * @description 소프트 삭제, 모든 기기에서 로그아웃. 같은 아이디로 다시 만들 수 있다. 자기 자신 B001, 다룰 수 없는 계정(최고 관리자 계정, 나보다 권한이 많은 관리자 계정) A005, 최고 관리자가 0명이 되면 B001. 지운 계정의 역할도 뺀다
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
         * 계정 종류 바꾸기
         * @description moa: 회원(USER)·관리자 계정(ADMIN). 자기 자신 B001, 다룰 수 없는 계정(최고 관리자 계정, 나보다 권한이 많은 관리자 계정) A005, 최고 관리자가 0명이 되면 B001. 회원으로 바꾸면 역할을 모두 빼고, 바뀌면 모든 기기에서 로그아웃시킨다
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
         * @description 로그인·토큰 재발급을 막고 모든 기기에서 로그아웃시킨다. 자기 자신 B001, 다룰 수 없는 계정(최고 관리자 계정, 나보다 권한이 많은 관리자 계정) A005, 최고 관리자가 0명이 되면 B001
         */
        put: operations["lock"];
        post?: never;
        /**
         * 잠금 풀기
         * @description 다룰 수 없는 계정(최고 관리자 계정, 나보다 권한이 많은 관리자 계정) A005
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
         * @description 이름·설명·권한을 바꾼다 (코드는 그대로). 없으면 R001, 등록되지 않은 권한 V001, 다룰 수 없는 역할·권한 A005. moa: 최고 관리자 역할은 B001
         */
        put: operations["modify"];
        post?: never;
        /**
         * 역할 지우기
         * @description 소프트 삭제. 이 역할을 가진 사용자에게서도 빠진다. 없으면 R001, 다룰 수 없는 역할 A005. moa: 최고 관리자 역할은 B001
         */
        delete: operations["delete_1"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/popups/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 팝업 하나
         * @description 없으면 R001
         */
        get: operations["get_1"];
        /**
         * 팝업 고치기
         * @description 모든 칸을 바꾼다. 없으면 R001
         */
        put: operations["modify_1"];
        post?: never;
        /**
         * 팝업 지우기
         * @description 소프트 삭제. 본문 이미지는 임시로 돌아가 정리된다. 없으면 R001
         */
        delete: operations["delete_2"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/popups/enabled": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 여러 팝업 켜기·끄기
         * @description 하나라도 없으면 R001이고 아무것도 바꾸지 않는다
         */
        put: operations["setEnabled"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/notices/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 공지 하나
         * @description 없으면 R001
         */
        get: operations["get_2"];
        /**
         * 공지 고치기
         * @description 제목과 본문. 상태와 게시 시각은 그대로다. 없으면 R001
         */
        put: operations["modify_2"];
        post?: never;
        /**
         * 공지 지우기
         * @description 소프트 삭제. 본문 이미지는 임시로 돌아가 정리된다. 없으면 R001
         */
        delete: operations["delete_3"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/notices/{id}/pin": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 공지 고정·풀기
         * @description 없으면 R001
         */
        put: operations["pin"];
        post?: never;
        delete?: never;
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
         * @description 없는 메뉴 R001, 그 밖의 오류는 만들기와 같다. 자기 자신을 부모로, 하위 메뉴가 있는 메뉴의 대상 바꾸기 B001
         */
        put: operations["modify_3"];
        post?: never;
        /**
         * 메뉴 지우기
         * @description 소프트 삭제. 하위 메뉴가 있으면 B001
         */
        delete: operations["delete_4"];
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
         * @description 보낸 메뉴의 부모와 순서를 한 트랜잭션으로 바꾼다. 하나라도 틀리면 아무것도 바꾸지 않는다. 묶음 메뉴를 하위로, 대상이 다른 부모 아래로 옮기면 B001
         */
        put: operations["reorder"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/menus/enabled": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 사용 여부 한꺼번에 바꾸기
         * @description moa (specs/001-admin-menus FR-010): 한 트랜잭션. 없는 메뉴가 하나라도 있으면 R001, 아무것도 바꾸지 않는다
         */
        put: operations["setEnabled_1"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/code-groups/{groupCode}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * moa: 코드 그룹 바꾸기
         * @description 이름, 설명, 사용 여부. 그룹 코드는 바꾸지 않는다
         */
        put: operations["modifyGroup"];
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
         * @description 이름, 정렬 순서(moa: 비우면 그대로), 사용 여부
         */
        put: operations["modifyCode"];
        post?: never;
        /**
         * moa: 사용 안 함 코드 지우기
         * @description 사용 안 함 항목만 지운다(소프트 삭제). 사용 중이면 B001, 없으면 R001 (specs/004-admin-codes FR-008)
         */
        delete: operations["deleteCode"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/code-groups/{groupCode}/codes/order": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * moa: 그룹 안 순서 한꺼번에
         * @description 다른 그룹의 코드가 섞이면 R001, 아무것도 바꾸지 않는다
         */
        put: operations["reorder_1"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/code-groups/{groupCode}/codes/enabled": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * moa: 그룹 안 여러 항목 사용 여부
         * @description 하나라도 없으면 R001, 아무것도 바꾸지 않는다
         */
        put: operations["setEnabled_2"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/code-groups/order": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * moa: 그룹 순서 한꺼번에
         * @description 없는 그룹이 섞이면 R001, 아무것도 바꾸지 않는다 (specs/004-admin-codes FR-005)
         */
        put: operations["reorderGroups"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/banners/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 배너 하나
         * @description 없으면 R001
         */
        get: operations["get_3"];
        /**
         * 배너 고치기
         * @description 모든 칸을 바꾼다. 빠진 이미지는 정리된다. 없으면 R001
         */
        put: operations["modify_4"];
        post?: never;
        /**
         * 배너 지우기
         * @description 소프트 삭제. 이미지는 임시로 돌아가 정리된다. 없으면 R001
         */
        delete: operations["delete_5"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/banners/enabled": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * 여러 배너 켜기·끄기
         * @description 하나라도 없으면 R001이고 아무것도 바꾸지 않는다
         */
        put: operations["setEnabled_3"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/users/me/withdrawal": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 회원 탈퇴
         * @description 닉네임이 다르면 V001(칸 nickname), 관리자 계정은 A201. 성공하면 리프레시 쿠키를 지우고 완전 삭제 예정 시각을 준다. 이미 탈퇴한 요청이 겹치면 처음 시각으로 답한다
         */
        post: operations["withdraw"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/rich-text/images": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 본문 이미지 올리기
         * @description png, jpg, gif, webp만 (파일 내용으로 판별, SVG 안 됨). 크기 초과·형식 오류·빈 파일 V001. 올린 이미지는 임시이고, 글을 저장할 때 본문에 쓰이면 남는다
         */
        post: operations["upload"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/notifications/push-subscriptions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 이 브라우저 구독 저장
         * @description 같은 endpoint면 내 것으로 바꾼다
         */
        post: operations["subscribe"];
        /**
         * 이 브라우저 구독 지우기
         * @description 끄기와 로그아웃 전에 부른다. 없으면 그대로
         */
        delete: operations["unsubscribe"];
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
    "/api/v1/auth/recovery": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 복구 안내
         * @description 완전 삭제 예정 시각. 복구 쿠키가 없거나 맞지 않으면 A403, 유예가 지났으면 A402
         */
        get: operations["getRecovery"];
        put?: never;
        /**
         * 복구
         * @description 탈퇴 전 그대로 돌아오고 로그인과 같은 토큰을 준다. 복구 쿠키를 지운다. 쿠키가 없거나 맞지 않으면(이미 복구함·다시 탈퇴함) A403, 잠겼으면 A007, 유예가 지났으면 A402
         */
        post: operations["recover"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/auth/oauth/sign-up": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 가입
         * @description oauth_signup 쿠키의 소셜 계정으로 닉네임·약관 동의를 받아 회원을 만들고 로그인시킨다. 응답은 로그인과 같다. 성공하면 oauth_signup 쿠키를 지운다
         */
        post: operations["signUp"];
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
         * @description 최신순 페이징. 지운 사용자와 시스템 계정은 빠진다. moa: 역할, 최고 관리자, 요청한 사람이 다룰 수 없는 이유(protection)
         */
        get: operations["search"];
        put?: never;
        /**
         * 사용자 만들기
         * @description 임시 비밀번호로 만들고 응답에 한 번만 보인다. 아이디 형식 V001, 중복 R002. moa: 계정 종류 ADMIN(관리자 계정)도 사용자 관리 권한으로 만든다
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
         * @description 임시 비밀번호로 바꾸고 응답에 한 번만 보인다. 모든 기기에서 로그아웃시킨다. 비밀번호 없는 계정(소셜) B001, 다룰 수 없는 계정(최고 관리자 계정, 나보다 권한이 많은 관리자 계정) A005
         */
        post: operations["resetPassword"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/users/bulk-delete": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 한꺼번에 지우기
         * @description moa (specs/002-admin-users FR-006): 한 트랜잭션. 지우기와 같이 소프트 삭제, 역할 빼기, 모든 기기에서 로그아웃. 없는 계정 R001, 다룰 수 없는 계정(나보다 권한이 많은 관리자 계정) A005, 최고 관리자 계정·자기 자신 B001이 하나라도 있으면 아무것도 지우지 않는다
         */
        post: operations["deleteAll"];
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
         * @description 만든 순서대로. moa: 최고 관리자 역할이 맨 앞, 요청한 사람이 다룰 수 있는지(grantable·blockReason) (specs/003-admin-roles R18)
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
    "/api/v1/admin/roles/{id}/members": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 역할의 구성원
         * @description moa (FR-006): 아이디 순 쪽. keyword가 없으면 구성원만, 있으면 아이디·이름으로 관리자 계정과 구성원 여부. 지운 사용자·시스템 계정은 빠진다. 없는 역할 R001
         */
        get: operations["getMembers"];
        put?: never;
        /**
         * 구성원 더하기
         * @description moa (FR-006): 한 트랜잭션. 이미 구성원이면 그대로. 없는 역할·사용자 R001, 다룰 수 없는 역할 A005, 회원이나 (다른 역할에) 최고 관리자가 섞이면 B001 (아무도 더하지 않는다). 최고 관리자 역할에 더하면 그 계정의 다른 역할은 빠진다
         */
        post: operations["addMembers"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/roles/{id}/members/remove": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 구성원 빼기
         * @description moa (FR-006): 한 트랜잭션. 구성원이 아니면 그대로. 없는 역할·사용자 R001, 다룰 수 없는 역할 A005, 회원이 섞이거나 최고 관리자가 0명이 되면 B001 (아무도 빼지 않는다)
         */
        post: operations["removeMembers"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/roles/bulk-delete": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 한꺼번에 지우기
         * @description moa (specs/003-admin-roles FR-005): 한 트랜잭션. 없는 역할 R001, 다룰 수 없는 역할 A005, 최고 관리자 역할 B001이 하나라도 있으면 아무것도 지우지 않는다
         */
        post: operations["deleteAll_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/popups": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 팝업 목록
         * @description 순서, 노출 시작이 늦은 것 순 페이징. 상태를 보내지 않으면 끝난 팝업은 뺀다
         */
        get: operations["search_1"];
        put?: never;
        /**
         * 팝업 등록
         * @description 내용은 걸러 저장한다. 끝 일시가 시작보다 앞이면 V001
         */
        post: operations["create_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/popups/bulk-delete": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 여러 팝업 지우기
         * @description 하나라도 없으면 R001이고 아무것도 지우지 않는다
         */
        post: operations["deleteAll_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/notices": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 공지 목록
         * @description 임시 저장 포함, 고친 최신순 페이징
         */
        get: operations["search_2"];
        put?: never;
        /**
         * 공지 만들기
         * @description publish=true면 바로 게시, 아니면 임시 저장. 본문은 걸러 저장한다
         */
        post: operations["create_3"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/notices/{id}/publish": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 공지 게시
         * @description 임시 저장 공지를 게시한다. 이미 게시했으면 B001, 없으면 R001
         */
        post: operations["publish"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/notices/bulk-delete": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 한꺼번에 지우기
         * @description specs/006-notices FR-001: 한 트랜잭션. 공지마다 지우기와 같다. 없는(지운) 공지가 하나라도 있으면 R001이고 아무것도 지우지 않는다
         */
        post: operations["deleteAll_3"];
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
         * @description 등록되지 않았거나 대상이 다른 연결 화면·공개 범위 V001, 없는 부모 R001, 3단계가 되는 부모 B001
         */
        post: operations["create_4"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/menus/bulk-delete": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 한꺼번에 지우기
         * @description moa (FR-010): 소프트 삭제, 한 트랜잭션. 고른 것끼리 부모·자식이면 함께 지운다. 없는 메뉴 R001, 고르지 않은 하위 메뉴가 남는 묶음 B001 (아무것도 지우지 않는다)
         */
        post: operations["deleteAll_4"];
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
         * @description moa: 사용 중으로 만든다. 그룹 코드가 겹치면 R002 (specs/004-admin-codes FR-004)
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
         * @description 그룹이 없으면 R001, 같은 코드가 있으면 R002. moa: 코드를 비우면 자동 순번, 순서를 비우면 맨 뒤
         */
        post: operations["createCode"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/banners": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 배너 목록
         * @description 순서, 노출 시작이 늦은 것 순 페이징. 상태를 보내지 않으면 끝난 배너는 뺀다
         */
        get: operations["search_3"];
        put?: never;
        /**
         * 배너 등록
         * @description 이미지는 rich-text 이미지 올리기로 받은 주소. GIF, 없는 이미지, 끝 일시가 시작보다 앞이면 V001
         */
        post: operations["create_5"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/banners/bulk-delete": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * 여러 배너 지우기
         * @description 하나라도 없으면 R001이고 아무것도 지우지 않는다
         */
        post: operations["deleteAll_5"];
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
    "/api/v1/terms": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 현재 약관 버전
         * @description 필수 약관(SERVICE, PRIVACY)마다 현재 버전. 문안은 프론트가 버전으로 고른다
         */
        get: operations["current"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/rich-text/images/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 본문 이미지 보여 주기
         * @description 로그인 없이 연다. 없으면 R001
         */
        get: operations["show"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/profile/avatars": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 기본 아바타 목록
         * @description 로그인한 누구나 부를 수 있다
         */
        get: operations["getAvatars"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/notifications": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 내 알림 목록
         * @description 최신순 페이징. 벨 펼침은 size=10
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
    "/api/v1/notifications/unread-count": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** 안 읽은 알림 수 */
        get: operations["unreadCount"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/notifications/push/public-key": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 브라우저 알림 공개 키
         * @description 웹 푸시를 끈 서버면 publicKey가 없다
         */
        get: operations["pushPublicKey"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/notices": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 공지 목록
         * @description 게시한 공지만. 고정 먼저, 그다음 게시 최신순
         */
        get: operations["getPage_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/notices/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 공지 하나
         * @description 게시한 공지만. 임시 저장·지운 공지·없는 공지는 R001
         */
        get: operations["get_4"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/notices/new": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 새 공지 여부
         * @description 공지 목록을 마지막으로 연 뒤 게시된 공지가 있나. 연 적이 없으면 게시한 공지가 하나라도 있나
         */
        get: operations["hasNew"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/menus/user": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 사용자 메뉴
         * @description moa: 사용자 셸 메뉴 (대상 USER, specs/001-admin-menus FR-007). 로그인하지 않아도 부른다. 공개 범위가 PUBLIC이면 모두가, MEMBER면 로그인한 사용자만 본다
         */
        get: operations["user"];
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
         * @description 사용 중인 관리 메뉴 트리 (2단계, moa: 대상 ADMIN만). moa: 관리자 계정은 권한과 상관없이 모두 받고 메뉴마다 accessible(연결 화면의 권한이 있는가)을 받는다. 회원은 빈 목록이다. 하위 메뉴가 없는 묶음 메뉴는 빠진다 (specs/001-admin-menus FR-002, 2026-11-22)
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
         * @description 선택 상자 등에 쓴다. includeDisabled=true면 사용 안 함 코드도 enabled=false로 준다 (예전 데이터의 코드 이름을 보일 때). 그룹이 없으면 R001. moa: 그룹이 꺼져 있으면 빈 목록이다 (includeDisabled=true면 그대로 준다, specs/004-admin-codes FR-008)
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
    "/api/v1/auth/oauth/{provider}/callback": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 콜백
         * @description 제공자가 돌려보내는 주소. 결과에 따라 /auth/oauth/done, /sign-up/nickname, /sign-in?oauthError=… 로 보낸다
         */
        get: operations["callback"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/auth/oauth/{provider}/authorize": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 인가 시작
         * @description 브라우저가 이 주소로 이동한다. oauth_state 쿠키를 심고 제공자 인가 화면으로 보낸다. 제공자 설정이 없으면 /sign-in?oauthError=disabled로 보낸다
         */
        get: operations["authorize"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/auth/oauth/providers": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 제공자 목록
         * @description 설정이 있는 제공자만 준다. 로그인 화면은 이 목록의 단추만 보인다 (US3-AC3)
         */
        get: operations["providers"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/screens": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 연결 화면 목록
         * @description 메뉴에 걸 수 있는 화면. 관리 연결 화면 먼저, 그 안에서는 등록 순서
         */
        get: operations["getScreens"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/roles/permission-tree": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 권한 트리
         * @description moa: 역할 창에서 권한을 고르는 관리 메뉴 트리(꺼진 메뉴 포함)와 메뉴에 없는 관리 연결 화면 (specs/003-admin-roles FR-004)
         */
        get: operations["getPermissionTree"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/purge-logs": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * 파기 이력 목록
         * @description 최신순. 조건(target, from, to)은 모두 선택이고 함께 주면 AND. from이 to보다 늦으면 V001
         */
        get: operations["getPage_2"];
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
        get: operations["getPage_3"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/admin/code-groups/tree": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * moa: 공통코드 트리
         * @description 그룹마다(그룹 순서, 같으면 만든 순서) 항목(사용 안 함 포함, 정렬 순서대로)
         */
        get: operations["getTree"];
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
        /** @description 프로필 저장 요청. 모든 칸을 보낸다. null이나 빈 배열은 비움 */
        ProfileSaveRequest: {
            /**
             * @description 기본 아바타 키 (GET /api/v1/profile/avatars). null이면 default
             * @example cat
             */
            avatar?: string;
            /** @description 자기소개. 500자까지 */
            bio?: string;
            /**
             * Format: int32
             * @description 출생 연도. 1900~올해
             * @example 2000
             */
            birthYear?: number;
            /**
             * @description 성별: MALE, FEMALE, UNDISCLOSED
             * @example FEMALE
             */
            gender?: string;
            /** @description 나이·성별 공개. null이면 거짓 */
            ageGenderPublic?: boolean;
            /**
             * @description 디스코드 사용자 이름. 2~32자 영문 소문자·숫자·_·.
             * @example moa_user.01
             */
            discordTag?: string;
            /**
             * @description 디스코드 공개 범위: PRIVATE, FRIENDS, MEMBERS. null이면 PRIVATE
             * @example MEMBERS
             */
            discordVisibility?: string;
            /** @description 플레이 시간대 (PLAY_TIME 코드) */
            playTimes?: string[];
        };
        /** @description 플레이 가능 시간대 */
        PlayTime: {
            /**
             * @description 코드
             * @example WEEKDAY_EVENING
             */
            code: string;
            /**
             * @description 이름
             * @example 평일 저녁
             */
            name: string;
        };
        /** @description 내 프로필 */
        ProfileGetResponse: {
            /**
             * @description 닉네임 (tb_user.name)
             * @example 모아
             */
            nickname: string;
            /**
             * @description 닉네임 태그. 프로필이 아직 없으면 null
             * @example 0421
             */
            nicknameTag?: string;
            /**
             * @description 아바타 키. 목록에서 빠진 키면 default
             * @example default
             */
            avatar: string;
            /** @description 자기소개 */
            bio?: string;
            /**
             * Format: int32
             * @description 출생 연도
             * @example 2000
             */
            birthYear?: number;
            /**
             * Format: int32
             * @description 나이 (올해 − 출생 연도). 출생 연도가 없으면 null
             * @example 26
             */
            age?: number;
            /**
             * @description 성별
             * @enum {string}
             */
            gender?: "MALE" | "FEMALE" | "UNDISCLOSED";
            /** @description 나이·성별 공개 */
            ageGenderPublic: boolean;
            /** @description 디스코드 사용자 이름 */
            discordTag?: string;
            /**
             * @description 디스코드 공개 범위
             * @enum {string}
             */
            discordVisibility: "PRIVATE" | "FRIENDS" | "MEMBERS";
            /** @description 플레이 가능 시간대 (공통코드 순서, 꺼진 값도 이름과 함께) */
            playTimes: components["schemas"]["PlayTime"][];
            /**
             * Format: date-time
             * @description 다음에 닉네임을 바꿀 수 있는 시각. 지금 바꿀 수 있으면 null
             */
            nicknameChangeableAt?: string;
        };
        /** @description 내 비밀번호 바꾸기 요청 */
        PasswordChangeRequest: {
            /** @description 지금 비밀번호 (임시 비밀번호 포함) */
            currentPassword: string;
            /** @description 새 비밀번호. 8~100자 */
            newPassword: string;
        };
        /** @description 닉네임 바꾸기 요청 */
        NicknameChangeRequest: {
            /**
             * @description 새 닉네임. 2~12자 한글·영문·숫자·_
             * @example 모아
             */
            nickname: string;
        };
        /** @description 닉네임 바꾸기 결과 */
        NicknameChangeResponse: {
            /**
             * @description 닉네임
             * @example 모아
             */
            nickname: string;
            /**
             * @description 닉네임 태그
             * @example 0421
             */
            nicknameTag?: string;
            /**
             * Format: date-time
             * @description 다음에 닉네임을 바꿀 수 있는 시각. 지금 바꿀 수 있으면 null
             */
            nicknameChangeableAt?: string;
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
        /** @description 역할 */
        AccessRoleRef: {
            /**
             * Format: int64
             * @description 역할 ID
             */
            id: number;
            /**
             * @description 이름
             * @example 보안 담당
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
             * @description 계정 종류 (moa: USER 회원, ADMIN 관리자 계정)
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
            /** @description moa: 받은 역할 (specs/002-admin-users FR-006). 목록·하나 조회에서만 채우고 바꾸기 응답에서는 비어 있다. 회원은 비어 있다 */
            accessRoles: components["schemas"]["AccessRoleRef"][];
            /** @description moa: 최고 관리자 역할을 가졌나. 목록·하나 조회에서만 채운다 */
            superAdmin: boolean;
            /**
             * @description moa: 요청한 사람이 이 계정을 다룰 수 없는 이유 (research R10). 다룰 수 있으면 null. 목록·하나 조회에서만 채운다
             * @enum {string}
             */
            protection?: "FIXED" | "SUPER_ADMIN_ONLY" | "WIDER_PERMISSIONS";
            /** @description moa: 연결된 소셜 제공자 (specs/002-admin-users FR-004). 없으면 빈 목록. 목록·하나 조회에서만 채우고 바꾸기 응답에서는 비어 있다 */
            socialProviders: "GOOGLE"[];
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
             * @description 묶은 권한 키 (이름순). 최고 관리자 역할은 비고 모든 권한을 갖는다
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
            /** @description moa: 코드에 고정된 최고 관리자 역할인가. 바꾸거나 지울 수 없다 (specs/003-admin-roles FR-010) */
            fixed: boolean;
            /**
             * @description moa: 묶은 권한의 이름 (사용자 관리 드로어에 작게 보인다)
             * @example [
             *       "코드 관리"
             *     ]
             */
            permissionNames: string[];
            /** @description moa: 요청한 사람이 이 역할을 다루고 주고 뺄 수 있나 */
            grantable: boolean;
            /**
             * @description moa: 다룰 수 없는 이유. 다룰 수 있으면 null
             * @enum {string}
             */
            blockReason?: "FIXED" | "SUPER_ADMIN_ONLY" | "WIDER_PERMISSIONS";
        };
        /** @description 팝업 등록·고치기 (specs/016-banner-popup FR-004) */
        PopupSaveRequest: {
            /**
             * @description 제목
             * @example 12월 시즌 대회 열려요
             */
            title: string;
            /** @description 내용 HTML. 허용한 서식만 남기고 걸러 저장한다 (rich-text) */
            content: string;
            /**
             * @description 연결 주소. /로 시작하는 내부 주소나 http·https 주소
             * @example /notices/42
             */
            linkUrl?: string;
            /**
             * Format: date-time
             * @description 노출 시작 일시
             * @example 2026-12-01T10:00:00+09:00
             */
            startsAt: string;
            /**
             * Format: date-time
             * @description 노출 끝 일시. 비면 끌 때까지. 있으면 시작보다 뒤
             * @example 2026-12-10T23:59:00+09:00
             */
            endsAt?: string;
            /**
             * Format: int32
             * @description 순서 (작을수록 먼저)
             * @example 0
             */
            sortOrder: number;
            /** @description 켜짐 */
            enabled: boolean;
        };
        /** @description 팝업 (관리) */
        PopupAdminResponse: {
            /**
             * Format: int64
             * @description 팝업 ID
             */
            id: number;
            /** @description 제목 */
            title: string;
            /** @description 내용 HTML (저장 때 걸렀다) */
            content: string;
            /** @description 연결 주소 */
            linkUrl?: string;
            /**
             * Format: date-time
             * @description 노출 시작 일시
             */
            startsAt: string;
            /**
             * Format: date-time
             * @description 노출 끝 일시. 없으면 끌 때까지
             */
            endsAt?: string;
            /**
             * Format: int32
             * @description 순서
             */
            sortOrder: number;
            /** @description 켜짐 */
            enabled: boolean;
            /**
             * @description 상태
             * @enum {string}
             */
            status: "SCHEDULED" | "ACTIVE" | "ENDED" | "DISABLED";
            /**
             * Format: date-time
             * @description 등록한 시각
             */
            createdAt: string;
            /**
             * Format: date-time
             * @description 고친 시각
             */
            updatedAt: string;
        };
        /** @description 여러 팝업 켜기·끄기 */
        PopupEnabledRequest: {
            /**
             * @description 팝업 ID들
             * @example [
             *       1,
             *       2
             *     ]
             */
            ids: number[];
            /** @description 켜짐 */
            enabled: boolean;
        };
        /** @description 공지 고치기. 상태와 게시 시각은 그대로다 */
        NoticeModifyRequest: {
            /**
             * @description 제목
             * @example 점검 안내
             */
            title: string;
            /** @description 본문 HTML. 허용한 서식만 남기고 걸러 저장한다 (rich-text) */
            content: string;
        };
        /** @description 공지 (관리) */
        NoticeAdminResponse: {
            /**
             * Format: int64
             * @description 공지 ID
             */
            id: number;
            /** @description 제목 */
            title: string;
            /** @description 본문 HTML (저장 때 걸렀다) */
            content: string;
            /**
             * @description 상태
             * @enum {string}
             */
            status: "DRAFT" | "PUBLISHED";
            /** @description 상단 고정 */
            pinned: boolean;
            /**
             * Format: date-time
             * @description 게시 시각. 임시 저장이면 없다
             */
            publishedAt?: string;
            /**
             * Format: date-time
             * @description 만든 시각
             */
            createdAt: string;
            /**
             * Format: date-time
             * @description 고친 시각
             */
            updatedAt: string;
        };
        /** @description 공지 고정·풀기 */
        NoticePinRequest: {
            /** @description 상단 고정 */
            pinned: boolean;
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
             * @description moa: 가리키는 연결 화면 키 (GET /api/v1/admin/screens). 비우면 묶음 메뉴(1단계만). 등록되지 않았거나 대상이 다르면 V001
             * @example ADMIN_CODES
             */
            screenKey?: string;
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
             * @description moa: 메뉴 대상 (ADMIN 관리 셸, USER 사용자 셸). 비우면 그대로 둔다. 1단계 메뉴를 바꾸면 하위 메뉴도 바뀌고, 하위 메뉴는 부모를 따른다
             * @example ADMIN
             * @enum {string}
             */
            audience?: "ADMIN" | "USER";
            /**
             * @description moa: 사용자 화면 메뉴의 공개 범위 (PUBLIC 모두, MEMBER 로그인한 사용자). 사용자 화면 메뉴는 필수, 로그인이 필요한 연결 화면은 MEMBER만(V001). 관리 메뉴·묶음 메뉴는 버린다
             * @example PUBLIC
             * @enum {string}
             */
            visibility?: "PUBLIC" | "MEMBER";
        };
        /** @description 메뉴가 가리키는 연결 화면 */
        MenuScreen: {
            /**
             * @description 이름
             * @example 코드 관리
             */
            name: string;
            /**
             * @description 화면 경로
             * @example /admin/codes
             */
            path: string;
            /**
             * @description 관리 연결 화면의 권한 키. 사용자 연결 화면은 없다
             * @example CODE_MANAGE
             */
            permission?: string;
            /**
             * @description 관리 연결 화면의 권한 이름
             * @example 공통코드 관리
             */
            permissionName?: string;
            /** @description 로그인이 필요한가. 관리 연결 화면은 언제나 true */
            signInRequired: boolean;
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
             * @description moa: 메뉴 대상 (ADMIN 관리 셸, USER 사용자 셸)
             * @example ADMIN
             * @enum {string}
             */
            audience: "ADMIN" | "USER";
            /**
             * @description moa: 가리키는 연결 화면 키. 묶음 메뉴면 없다
             * @example ADMIN_CODES
             */
            screenKey?: string;
            /** @description moa: 가리키는 연결 화면. 묶음 메뉴이거나 없는 연결 화면이면 없다 */
            screen?: components["schemas"]["MenuScreen"];
            /** @description moa: 연결 화면 키가 있는데 등록된 연결 화면이 없다 (코드에서 빠졌다). 사이드바에 보이지 않는다 */
            missingScreen: boolean;
            /**
             * @description moa: 사용자 화면 메뉴의 공개 범위. 관리 메뉴·묶음 메뉴는 없다
             * @example PUBLIC
             * @enum {string}
             */
            visibility?: "PUBLIC" | "MEMBER";
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
        /** @description moa: 여러 메뉴의 사용 여부 한꺼번에 바꾸기 요청 */
        MenuEnabledRequest: {
            /**
             * @description 메뉴 ID들
             * @example [
             *       3,
             *       4
             *     ]
             */
            ids: number[];
            /** @description 사용 여부 */
            enabled: boolean;
        };
        /** @description moa: 코드 그룹 바꾸기 요청. 그룹 코드는 바꾸지 않는다 */
        CodeGroupModifyRequest: {
            /**
             * @description 이름
             * @example 이벤트 분류
             */
            name: string;
            /** @description 설명 */
            description?: string;
            /** @description 사용 여부. false면 화면용 목록이 빈 목록이다 */
            enabled: boolean;
        };
        /** @description 코드 그룹 (moa: specs/004-admin-codes FR-004·FR-008) */
        CodeGroupListResponse: {
            /**
             * @description 그룹 코드
             * @example GAME_GENRE
             */
            groupCode: string;
            /**
             * @description 이름
             * @example 게임 장르
             */
            name: string;
            /** @description 설명 */
            description?: string;
            /** @description moa: 사용 여부. false면 화면용 목록이 빈 목록이다 */
            enabled: boolean;
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
             * @description 정렬 순서. moa: 비우면 그대로
             * @example 10
             */
            sortOrder?: number;
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
        /** @description moa: 그룹 안 순서 바꾸기 한 줄 */
        CodeOrderRequest: {
            /**
             * @description 코드
             * @example 001
             */
            code: string;
            /**
             * Format: int32
             * @description 정렬 순서
             * @example 0
             */
            sortOrder: number;
        };
        /** @description moa: 한 그룹 안 여러 항목의 사용 여부 한꺼번에 바꾸기 요청 */
        CodeEnabledRequest: {
            /**
             * @description 코드들
             * @example [
             *       "001",
             *       "002"
             *     ]
             */
            codes: string[];
            /** @description 사용 여부 */
            enabled: boolean;
        };
        /** @description moa: 그룹 순서 바꾸기 한 줄 (specs/004-admin-codes FR-005) */
        CodeGroupOrderRequest: {
            /**
             * @description 그룹 코드
             * @example GAME_GENRE
             */
            groupCode: string;
            /**
             * Format: int32
             * @description 그룹 순서
             * @example 0
             */
            sortOrder: number;
        };
        /** @description 배너 등록·고치기 (specs/016-banner-popup FR-003) */
        BannerSaveRequest: {
            /**
             * @description 이름 (관리 목록에서 알아보는 이름)
             * @example 윈터컵 참가 신청
             */
            name: string;
            /** @description 넓은 화면 이미지 주소 (rich-text 이미지 올리기의 url). JPG·PNG·WebP */
            wideImageUrl: string;
            /** @description 좁은 화면 이미지 주소. 비면 넓은 화면 이미지를 쓴다 */
            narrowImageUrl?: string;
            /**
             * @description 대체 문구
             * @example moa 윈터컵 5인 팀 대회 참가 신청
             */
            altText: string;
            /**
             * @description 연결 주소. /로 시작하는 내부 주소나 http·https 주소
             * @example /notices/42
             */
            linkUrl?: string;
            /**
             * Format: date-time
             * @description 노출 시작 일시
             * @example 2026-12-01T10:00:00+09:00
             */
            startsAt: string;
            /**
             * Format: date-time
             * @description 노출 끝 일시. 비면 끌 때까지. 있으면 시작보다 뒤
             * @example 2026-12-10T23:59:00+09:00
             */
            endsAt?: string;
            /**
             * Format: int32
             * @description 순서 (작을수록 먼저)
             * @example 0
             */
            sortOrder: number;
            /** @description 켜짐 */
            enabled: boolean;
        };
        /** @description 배너 (관리) */
        BannerAdminResponse: {
            /**
             * Format: int64
             * @description 배너 ID
             */
            id: number;
            /** @description 이름 */
            name: string;
            /** @description 넓은 화면 이미지 주소 */
            wideImageUrl: string;
            /** @description 좁은 화면 이미지 주소. 없으면 넓은 화면 이미지를 쓴다 */
            narrowImageUrl?: string;
            /** @description 대체 문구 */
            altText: string;
            /** @description 연결 주소 */
            linkUrl?: string;
            /**
             * Format: date-time
             * @description 노출 시작 일시
             */
            startsAt: string;
            /**
             * Format: date-time
             * @description 노출 끝 일시. 없으면 끌 때까지
             */
            endsAt?: string;
            /**
             * Format: int32
             * @description 순서
             */
            sortOrder: number;
            /** @description 켜짐 */
            enabled: boolean;
            /**
             * @description 상태
             * @enum {string}
             */
            status: "SCHEDULED" | "ACTIVE" | "ENDED" | "DISABLED";
            /**
             * Format: date-time
             * @description 등록한 시각
             */
            createdAt: string;
            /**
             * Format: date-time
             * @description 고친 시각
             */
            updatedAt: string;
        };
        /** @description 여러 배너 켜기·끄기 */
        BannerEnabledRequest: {
            /**
             * @description 배너 ID들
             * @example [
             *       1,
             *       2
             *     ]
             */
            ids: number[];
            /** @description 켜짐 */
            enabled: boolean;
        };
        /** @description 회원 탈퇴 요청 */
        WithdrawalRequest: {
            /**
             * @description 확인용으로 입력한 지금 닉네임 (대소문자 구별)
             * @example 모아
             */
            nickname: string;
        };
        /** @description 회원 탈퇴 결과 */
        WithdrawalResponse: {
            /**
             * Format: date-time
             * @description 완전 삭제 예정 시각. 이때까지 다시 로그인해 복구할 수 있다
             */
            purgeAt: string;
        };
        /** @description 올린 본문 이미지 */
        RichTextImageUploadResponse: {
            /**
             * @description 이미지 ID
             * @example 3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c
             */
            id: string;
            /**
             * @description 본문 img src에 그대로 넣을 주소
             * @example /api/v1/rich-text/images/3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c
             */
            url: string;
        };
        /** @description 구독 키 (base64url) */
        Keys: {
            p256dh: string;
            auth: string;
        };
        /** @description 이 브라우저의 웹 푸시 구독 (PushSubscription.toJSON()) */
        PushSubscriptionRequest: {
            /** @description 브라우저 푸시 서비스 주소 */
            endpoint: string;
            /** @description 구독 키 */
            keys: components["schemas"]["Keys"];
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
        /** @description 동의한 약관 하나 */
        Agreement: {
            /**
             * @description 종류 (SERVICE, PRIVACY)
             * @example SERVICE
             */
            type: string;
            /**
             * @description 동의한 버전 (GET /api/v1/terms)
             * @example draft
             */
            version: string;
        };
        /** @description 소셜 가입 요청 */
        OAuthSignUpRequest: {
            /**
             * @description 닉네임. 2~12자 한글·영문·숫자·_ (잠정). 다른 회원과 겹쳐도 된다
             * @example 모아_01
             */
            nickname: string;
            /** @description 동의한 약관. SERVICE·PRIVACY 둘 다 현재 버전이어야 한다 */
            agreements: components["schemas"]["Agreement"][];
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
        /** @description moa: 사용자 ID 목록 (한꺼번에 지우기) */
        UserIdsRequest: {
            /**
             * @description 사용자 ID들
             * @example [
             *       1,
             *       2
             *     ]
             */
            ids: number[];
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
        /** @description moa: 구성원으로 더하거나 뺄 사용자 ID 목록 */
        RoleMembersRequest: {
            /**
             * @description 사용자 ID들
             * @example [
             *       10,
             *       11
             *     ]
             */
            userIds: number[];
        };
        /** @description moa: 역할 ID 목록 (한꺼번에 지우기) */
        RoleIdsRequest: {
            /**
             * @description 역할 ID들
             * @example [
             *       1,
             *       2
             *     ]
             */
            ids: number[];
        };
        /** @description 팝업 ID 목록 (한꺼번에 지우기) */
        PopupIdsRequest: {
            /**
             * @description 팝업 ID들
             * @example [
             *       1,
             *       2
             *     ]
             */
            ids: number[];
        };
        /** @description 공지 만들기 */
        NoticeCreateRequest: {
            /**
             * @description 제목
             * @example 점검 안내
             */
            title: string;
            /** @description 본문 HTML. 허용한 서식만 남기고 걸러 저장한다 (rich-text) */
            content: string;
            /** @description 상단 고정 */
            pinned: boolean;
            /** @description true면 바로 게시, false면 임시 저장 */
            publish: boolean;
        };
        /** @description 공지 ID 목록 (한꺼번에 지우기) */
        NoticeIdsRequest: {
            /**
             * @description 공지 ID들
             * @example [
             *       1,
             *       2
             *     ]
             */
            ids: number[];
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
             * @description moa: 가리키는 연결 화면 키 (GET /api/v1/admin/screens). 비우면 묶음 메뉴(1단계만). 등록되지 않았거나 대상이 다르면 V001
             * @example ADMIN_CODES
             */
            screenKey?: string;
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
             * @description moa: 메뉴 대상 (ADMIN 관리 셸, USER 사용자 셸). 비우면 ADMIN. 하위 메뉴는 보낸 값과 상관없이 부모를 따른다
             * @example ADMIN
             * @enum {string}
             */
            audience?: "ADMIN" | "USER";
            /**
             * @description moa: 사용자 화면 메뉴의 공개 범위 (PUBLIC 모두, MEMBER 로그인한 사용자). 사용자 화면 메뉴는 필수, 로그인이 필요한 연결 화면은 MEMBER만(V001). 관리 메뉴·묶음 메뉴는 버린다
             * @example PUBLIC
             * @enum {string}
             */
            visibility?: "PUBLIC" | "MEMBER";
        };
        /** @description moa: 메뉴 ID 목록 (한꺼번에 지우기) */
        MenuIdsRequest: {
            /**
             * @description 메뉴 ID들
             * @example [
             *       3,
             *       4
             *     ]
             */
            ids: number[];
        };
        /** @description 코드 그룹 만들기 요청. moa: 사용 중으로 만든다 */
        CodeGroupCreateRequest: {
            /**
             * @description 그룹 코드. 영문 대문자로 시작, 대문자·숫자·밑줄. 만든 뒤 바꾸지 않는다
             * @example EVENT_TYPE
             */
            groupCode: string;
            /**
             * @description 이름
             * @example 이벤트 분류
             */
            name: string;
            /** @description 설명 */
            description?: string;
        };
        /** @description 코드 만들기 요청 */
        CodeCreateRequest: {
            /**
             * @description 코드. 영문 대문자·숫자·밑줄. moa: 필수(자동 순번 없음, specs/004-admin-codes FR-006)
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
             * @description 정렬 순서. 작을수록 앞. moa: 비우면 그룹 맨 뒤
             * @example 10
             */
            sortOrder?: number;
        };
        /** @description 배너 ID 목록 (한꺼번에 지우기) */
        BannerIdsRequest: {
            /**
             * @description 배너 ID들
             * @example [
             *       1,
             *       2
             *     ]
             */
            ids: number[];
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
            /** @description moa: 비밀번호가 있는지. 없으면(소셜 회원) 비밀번호 변경을 숨긴다 (010 US2-AC3) */
            hasPassword: boolean;
            /**
             * @description moa: 계정 바 아바타 키. 프로필이 없거나 목록에서 빠진 키면 default (009 FR-014)
             * @example default
             */
            avatar: string;
            /** @description moa: 마이페이지를 쓸 수 있는지. 관리자 계정(ADMIN)이면 거짓 (009 FR-016) */
            mypageAllowed: boolean;
        };
        MyPermissionResponse: {
            permissions: string[];
        };
        /** @description 약관 하나 */
        Item: {
            /**
             * @description 종류 (SERVICE 이용약관, PRIVACY 개인정보 수집·이용)
             * @example SERVICE
             */
            type: string;
            /**
             * @description 현재 버전. 프론트가 이 버전의 문안을 고른다
             * @example draft
             */
            version: string;
            /** @description 필수 동의인지 */
            required: boolean;
        };
        /** @description 가입 때 동의받을 현재 약관 */
        TermsResponse: {
            terms: components["schemas"]["Item"][];
        };
        /** @description 기본 아바타 목록 */
        AvatarListResponse: {
            /** @description 아바타 키 (순서대로). 그림은 프론트 /avatars/<키>.svg */
            avatars: string[];
        };
        /** @description 개인 알림 */
        NotificationResponse: {
            /**
             * Format: int64
             * @description 알림 ID
             */
            id: number;
            /**
             * @description 종류
             * @enum {string}
             */
            type: "SYSTEM";
            /** @description 제목 */
            title: string;
            /**
             * @description 관련 화면 경로
             * @example /notices/7
             */
            link: string;
            /**
             * Format: date-time
             * @description 만든 시각
             */
            createdAt: string;
            /** @description 읽었나 */
            read: boolean;
        };
        /** @description 페이징 응답 */
        PageResponseNotificationResponse: {
            /** @description 항목 */
            content: components["schemas"]["NotificationResponse"][];
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
        /** @description 안 읽은 개인 알림 수 */
        UnreadCountResponse: {
            /**
             * Format: int64
             * @description 안 읽은 수
             */
            count: number;
        };
        /** @description 브라우저 알림(웹 푸시) VAPID 공개 키 */
        PushPublicKeyResponse: {
            /** @description base64url 공개 키. 서버에서 웹 푸시를 끄면 없다 */
            publicKey?: string;
        };
        /** @description 공지 목록 항목 */
        NoticeListResponse: {
            /**
             * Format: int64
             * @description 공지 ID
             */
            id: number;
            /**
             * Format: int64
             * @description 게시 순번. 처음 게시한 공지가 1, 지운 공지는 세지 않는다
             */
            number: number;
            /** @description 제목 */
            title: string;
            /** @description 상단 고정. 화면은 번호 대신 고정 표시를 보인다 */
            pinned: boolean;
            /**
             * Format: date-time
             * @description 게시 시각
             */
            publishedAt: string;
        };
        /** @description 페이징 응답 */
        PageResponseNoticeListResponse: {
            /** @description 항목 */
            content: components["schemas"]["NoticeListResponse"][];
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
        /** @description 공지 */
        NoticeResponse: {
            /**
             * Format: int64
             * @description 공지 ID
             */
            id: number;
            /** @description 제목 */
            title: string;
            /** @description 본문 HTML (저장 때 걸렀다) */
            content: string;
            /** @description 상단 고정 */
            pinned: boolean;
            /**
             * Format: date-time
             * @description 게시 시각
             */
            publishedAt: string;
        };
        /** @description 새 공지 여부 */
        NoticeNewResponse: {
            /** @description 공지 목록을 마지막으로 연 뒤 게시된 공지가 있다 */
            hasNew: boolean;
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
            /** @description moa: 들어갈 수 있는가(연결 화면의 권한이 있는가). 상위 메뉴는 하위 메뉴 중 하나라도 있으면 true */
            accessible: boolean;
            /** @description 하위 메뉴 */
            children: components["schemas"]["MyMenuResponse"][];
        };
        /** @description 탈퇴한 계정의 복구 안내 */
        RecoveryResponse: {
            /**
             * Format: date-time
             * @description 완전 삭제 예정 시각. 이때까지 복구할 수 있다
             */
            purgeAt: string;
        };
        /** @description 설정된 소셜 로그인 제공자 */
        OAuthProvidersResponse: {
            /**
             * @description 제공자 경로 값. 설정이 없으면 빈 배열
             * @example [
             *       "google"
             *     ]
             */
            providers: string[];
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
        /** @description 권한 */
        ScreenPermission: {
            /**
             * @description 권한 키
             * @example CODE_MANAGE
             */
            key: string;
            /**
             * @description 권한 이름
             * @example 공통코드 관리
             */
            name: string;
        };
        /** @description 연결 화면 (메뉴에 걸 수 있는 화면) */
        ScreenResponse: {
            /**
             * @description 연결 화면 키
             * @example ADMIN_CODES
             */
            key: string;
            /**
             * @description 이름
             * @example 코드 관리
             */
            name: string;
            /**
             * @description 화면 경로
             * @example /admin/codes
             */
            path: string;
            /**
             * @description 대상 (ADMIN 관리 셸, USER 사용자 셸)
             * @example ADMIN
             * @enum {string}
             */
            audience: "ADMIN" | "USER";
            /** @description 관리 연결 화면의 권한. 사용자 연결 화면은 없다 */
            permission?: components["schemas"]["ScreenPermission"];
            /** @description 로그인이 필요한가. 관리 연결 화면은 언제나 true */
            signInRequired: boolean;
        };
        /** @description 페이징 응답 */
        PageResponseRoleMemberResponse: {
            /** @description 항목 */
            content: components["schemas"]["RoleMemberResponse"][];
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
        /** @description moa: 역할 구성원의 한 줄 (specs/003-admin-roles FR-006). 관리자 계정만 보인다 */
        RoleMemberResponse: {
            /**
             * Format: int64
             * @description 사용자 ID
             */
            id: number;
            /**
             * @description 아이디
             * @example operator
             */
            loginId: string;
            /**
             * @description 이름
             * @example 김운영
             */
            name: string;
            /** @description 최고 관리자 역할을 가졌나. 다른 역할은 받지 않는다 */
            superAdmin: boolean;
            /** @description 이 역할의 구성원인가 */
            member: boolean;
        };
        /** @description 관리 메뉴 */
        Node: {
            /**
             * Format: int64
             * @description 메뉴 ID
             */
            menuId: number;
            /**
             * @description 이름
             * @example 코드 관리
             */
            name: string;
            /** @description 사용 여부 */
            enabled: boolean;
            /** @description 화면 메뉴의 권한. 묶음 메뉴는 없다 */
            permission?: components["schemas"]["PermissionRef"];
            /** @description 묶음 메뉴의 하위 메뉴 */
            children: components["schemas"]["Node"][];
        };
        /** @description 권한 */
        PermissionRef: {
            /**
             * @description 권한 키
             * @example CODE_MANAGE
             */
            key: string;
            /**
             * @description 권한 이름
             * @example 공통코드 관리
             */
            name: string;
        };
        /** @description moa: 역할 창에서 권한을 고르는 트리 (specs/003-admin-roles FR-004) */
        PermissionTreeResponse: {
            /** @description 관리 메뉴 트리. 메뉴 관리와 같은 순서·묶음이고 꺼진 메뉴도 들어 있다 */
            menus: components["schemas"]["Node"][];
            /** @description 어느 관리 메뉴도 가리키지 않는 관리 연결 화면 */
            unlisted: components["schemas"]["Unlisted"][];
        };
        /** @description 메뉴에 없는 관리 연결 화면 */
        Unlisted: {
            /**
             * @description 연결 화면 키
             * @example ADMIN_CODES
             */
            screenKey: string;
            /**
             * @description 이름
             * @example 코드 관리
             */
            name: string;
            /** @description 권한 */
            permission: components["schemas"]["PermissionRef"];
        };
        /** @description 파기 이력 검색 조건 */
        PurgeLogSearchRequest: {
            /**
             * @description 대상 종류. 없으면 전체
             * @enum {string}
             */
            target?: "LOGIN_HISTORY" | "NOTIFICATION" | "REFRESH_TOKEN" | "RICH_TEXT_IMAGE";
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
        /** @description 페이징 응답 */
        PageResponsePurgeLogListResponse: {
            /** @description 항목 */
            content: components["schemas"]["PurgeLogListResponse"][];
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
        /** @description 파기 이력 */
        PurgeLogListResponse: {
            /**
             * Format: int64
             * @description 파기 이력 ID
             */
            id: number;
            /**
             * Format: date-time
             * @description 파기 일시 (시간대 포함)
             */
            createdAt: string;
            /**
             * @description 대상 종류
             * @enum {string}
             */
            target: "LOGIN_HISTORY" | "NOTIFICATION" | "REFRESH_TOKEN" | "RICH_TEXT_IMAGE";
            /**
             * @description 방법
             * @enum {string}
             */
            method: "ROW_DELETE" | "COLUMN_ERASE";
            /**
             * Format: int32
             * @description 건수
             * @example 3
             */
            count: number;
            /**
             * @description 처리자
             * @enum {string}
             */
            actor: "SCHEDULER" | "PERSON";
        };
        /** @description 팝업 관리 검색 조건. 모두 선택 */
        PopupSearchRequest: {
            /**
             * @description 제목에 들어 있는 글자
             * @example 시즌
             */
            keyword?: string;
            /**
             * @description 상태. 비면 끝난 것만 빼고, ALL이면 모두
             * @enum {string}
             */
            status?: "SCHEDULED" | "ACTIVE" | "ENDED" | "DISABLED" | "ALL";
        };
        /** @description 페이징 응답 */
        PageResponsePopupAdminListResponse: {
            /** @description 항목 */
            content: components["schemas"]["PopupAdminListResponse"][];
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
        /** @description 팝업 관리 목록 항목 */
        PopupAdminListResponse: {
            /**
             * Format: int64
             * @description 팝업 ID
             */
            id: number;
            /** @description 제목 */
            title: string;
            /**
             * Format: date-time
             * @description 노출 시작 일시
             */
            startsAt: string;
            /**
             * Format: date-time
             * @description 노출 끝 일시. 없으면 끌 때까지
             */
            endsAt?: string;
            /**
             * Format: int32
             * @description 순서
             */
            sortOrder: number;
            /** @description 켜짐 */
            enabled: boolean;
            /**
             * @description 상태
             * @enum {string}
             */
            status: "SCHEDULED" | "ACTIVE" | "ENDED" | "DISABLED";
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
        /** @description 공지 관리 검색 조건. 모두 선택 */
        NoticeSearchRequest: {
            /**
             * @description 제목에 들어 있는 글자
             * @example 점검
             */
            keyword?: string;
            /**
             * @description 상태
             * @enum {string}
             */
            status?: "DRAFT" | "PUBLISHED";
        };
        /** @description 공지 관리 목록 항목 */
        NoticeAdminListResponse: {
            /**
             * Format: int64
             * @description 공지 ID
             */
            id: number;
            /** @description 제목 */
            title: string;
            /**
             * @description 상태
             * @enum {string}
             */
            status: "DRAFT" | "PUBLISHED";
            /** @description 상단 고정 */
            pinned: boolean;
            /**
             * Format: date-time
             * @description 게시 시각. 임시 저장이면 없다
             */
            publishedAt?: string;
            /**
             * Format: date-time
             * @description 고친 시각
             */
            updatedAt: string;
        };
        /** @description 페이징 응답 */
        PageResponseNoticeAdminListResponse: {
            /** @description 항목 */
            content: components["schemas"]["NoticeAdminListResponse"][];
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
        /** @description moa: 공통코드 트리 표의 그룹 하나 (specs/004-admin-codes FR-005) */
        CodeGroupTreeResponse: {
            /**
             * @description 그룹 코드
             * @example GAME_GENRE
             */
            groupCode: string;
            /**
             * @description 이름
             * @example 게임 장르
             */
            name: string;
            /** @description 설명 */
            description?: string;
            /** @description 사용 여부. false면 화면용 목록이 빈 목록이다 */
            enabled: boolean;
            /** @description 항목 (사용 안 함 포함, 정렬 순서대로) */
            codes: components["schemas"]["CodeListResponse"][];
        };
        /** @description 배너 관리 검색 조건. 모두 선택 */
        BannerSearchRequest: {
            /**
             * @description 이름에 들어 있는 글자
             * @example 윈터
             */
            keyword?: string;
            /**
             * @description 상태. 비면 끝난 것만 빼고, ALL이면 모두
             * @enum {string}
             */
            status?: "SCHEDULED" | "ACTIVE" | "ENDED" | "DISABLED" | "ALL";
        };
        /** @description 배너 관리 목록 항목 */
        BannerAdminListResponse: {
            /**
             * Format: int64
             * @description 배너 ID
             */
            id: number;
            /** @description 이름 */
            name: string;
            /** @description 넓은 화면 이미지 주소 */
            wideImageUrl: string;
            /**
             * Format: date-time
             * @description 노출 시작 일시
             */
            startsAt: string;
            /**
             * Format: date-time
             * @description 노출 끝 일시. 없으면 끌 때까지
             */
            endsAt?: string;
            /**
             * Format: int32
             * @description 순서
             */
            sortOrder: number;
            /** @description 켜짐 */
            enabled: boolean;
            /**
             * @description 상태
             * @enum {string}
             */
            status: "SCHEDULED" | "ACTIVE" | "ENDED" | "DISABLED";
        };
        /** @description 페이징 응답 */
        PageResponseBannerAdminListResponse: {
            /** @description 항목 */
            content: components["schemas"]["BannerAdminListResponse"][];
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
    getMyProfile: {
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
                    "*/*": components["schemas"]["ProfileGetResponse"];
                };
            };
        };
    };
    saveProfile: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ProfileSaveRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ProfileGetResponse"];
                };
            };
        };
    };
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
    changeNickname: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NicknameChangeRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["NicknameChangeResponse"];
                };
            };
        };
    };
    read: {
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
    readAll: {
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
                content?: never;
            };
        };
    };
    markSeen: {
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
    get_1: {
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
                    "*/*": components["schemas"]["PopupAdminResponse"];
                };
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
                "application/json": components["schemas"]["PopupSaveRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PopupAdminResponse"];
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
    setEnabled: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PopupEnabledRequest"];
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
    get_2: {
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
                    "*/*": components["schemas"]["NoticeAdminResponse"];
                };
            };
        };
    };
    modify_2: {
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
                "application/json": components["schemas"]["NoticeModifyRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["NoticeAdminResponse"];
                };
            };
        };
    };
    delete_3: {
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
    pin: {
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
                "application/json": components["schemas"]["NoticePinRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["NoticeAdminResponse"];
                };
            };
        };
    };
    modify_3: {
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
    delete_4: {
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
    setEnabled_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["MenuEnabledRequest"];
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
    modifyGroup: {
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
                "application/json": components["schemas"]["CodeGroupModifyRequest"];
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
    reorder_1: {
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
                "application/json": components["schemas"]["CodeOrderRequest"][];
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
    setEnabled_2: {
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
                "application/json": components["schemas"]["CodeEnabledRequest"];
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
    reorderGroups: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CodeGroupOrderRequest"][];
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
    get_3: {
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
                    "*/*": components["schemas"]["BannerAdminResponse"];
                };
            };
        };
    };
    modify_4: {
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
                "application/json": components["schemas"]["BannerSaveRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["BannerAdminResponse"];
                };
            };
        };
    };
    delete_5: {
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
    setEnabled_3: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["BannerEnabledRequest"];
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
    withdraw: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["WithdrawalRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["WithdrawalResponse"];
                };
            };
        };
    };
    upload: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: {
            content: {
                "multipart/form-data": {
                    /** Format: binary */
                    file: string;
                };
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["RichTextImageUploadResponse"];
                };
            };
        };
    };
    subscribe: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PushSubscriptionRequest"];
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
    unsubscribe: {
        parameters: {
            query: {
                endpoint: string;
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
    getRecovery: {
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
                    "*/*": components["schemas"]["RecoveryResponse"];
                };
            };
        };
    };
    recover: {
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
                    "*/*": components["schemas"]["AuthRefreshResponse"];
                };
            };
        };
    };
    signUp: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["OAuthSignUpRequest"];
            };
        };
        responses: {
            /** @description 가입·로그인 성공 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["AuthRefreshResponse"];
                };
            };
            /** @description V001 닉네임 규칙, A017 약관 미동의·버전 다름, A009 가입 대기 쿠키 없음·만료 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ErrorResponse"];
                };
            };
            /** @description A012 이미 가입된 소셜 계정, A015 이메일 중복, B201 닉네임 태그가 다 참 */
            409: {
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
    deleteAll: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UserIdsRequest"];
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
    getMembers: {
        parameters: {
            query?: {
                keyword?: string;
                page?: number;
                size?: number;
            };
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
                    "*/*": components["schemas"]["PageResponseRoleMemberResponse"];
                };
            };
        };
    };
    addMembers: {
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
                "application/json": components["schemas"]["RoleMembersRequest"];
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
    removeMembers: {
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
                "application/json": components["schemas"]["RoleMembersRequest"];
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
    deleteAll_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["RoleIdsRequest"];
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
    search_1: {
        parameters: {
            query: {
                request: components["schemas"]["PopupSearchRequest"];
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
                    "*/*": components["schemas"]["PageResponsePopupAdminListResponse"];
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
                "application/json": components["schemas"]["PopupSaveRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PopupAdminResponse"];
                };
            };
        };
    };
    deleteAll_2: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PopupIdsRequest"];
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
    search_2: {
        parameters: {
            query: {
                request: components["schemas"]["NoticeSearchRequest"];
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
                    "*/*": components["schemas"]["PageResponseNoticeAdminListResponse"];
                };
            };
        };
    };
    create_3: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NoticeCreateRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["NoticeAdminResponse"];
                };
            };
        };
    };
    publish: {
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
                    "*/*": components["schemas"]["NoticeAdminResponse"];
                };
            };
        };
    };
    deleteAll_3: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["NoticeIdsRequest"];
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
    create_4: {
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
    deleteAll_4: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["MenuIdsRequest"];
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
    search_3: {
        parameters: {
            query: {
                request: components["schemas"]["BannerSearchRequest"];
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
                    "*/*": components["schemas"]["PageResponseBannerAdminListResponse"];
                };
            };
        };
    };
    create_5: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["BannerSaveRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["BannerAdminResponse"];
                };
            };
        };
    };
    deleteAll_5: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["BannerIdsRequest"];
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
    current: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 현재 약관 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["TermsResponse"];
                };
            };
        };
    };
    show: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                id: string;
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
                    "*/*": string;
                };
            };
        };
    };
    getAvatars: {
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
                    "*/*": components["schemas"]["AvatarListResponse"];
                };
            };
        };
    };
    getPage: {
        parameters: {
            query?: {
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
                    "*/*": components["schemas"]["PageResponseNotificationResponse"];
                };
            };
        };
    };
    unreadCount: {
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
                    "*/*": components["schemas"]["UnreadCountResponse"];
                };
            };
        };
    };
    pushPublicKey: {
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
                    "*/*": components["schemas"]["PushPublicKeyResponse"];
                };
            };
        };
    };
    getPage_1: {
        parameters: {
            query?: {
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
                    "*/*": components["schemas"]["PageResponseNoticeListResponse"];
                };
            };
        };
    };
    get_4: {
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
                    "*/*": components["schemas"]["NoticeResponse"];
                };
            };
        };
    };
    hasNew: {
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
                    "*/*": components["schemas"]["NoticeNewResponse"];
                };
            };
        };
    };
    user: {
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
    callback: {
        parameters: {
            query?: {
                code?: string;
                state?: string;
                error?: string;
            };
            header?: never;
            path: {
                provider: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 결과 화면 */
            302: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    authorize: {
        parameters: {
            query?: {
                /** @description login (연결 link는 나중) */
                mode?: string;
                /** @description 로그인 뒤 돌아갈 같은 사이트 경로 */
                next?: string;
            };
            header?: never;
            path: {
                /**
                 * @description 제공자
                 * @example google
                 */
                provider: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 제공자 인가 화면 또는 로그인 화면 */
            302: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
    providers: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description 제공자 목록 */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["OAuthProvidersResponse"];
                };
            };
        };
    };
    getScreens: {
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
                    "*/*": components["schemas"]["ScreenResponse"][];
                };
            };
        };
    };
    getPermissionTree: {
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
                    "*/*": components["schemas"]["PermissionTreeResponse"];
                };
            };
        };
    };
    getPage_2: {
        parameters: {
            query: {
                search: components["schemas"]["PurgeLogSearchRequest"];
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
                    "*/*": components["schemas"]["PageResponsePurgeLogListResponse"];
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
    getPage_3: {
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
    getTree: {
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
                    "*/*": components["schemas"]["CodeGroupTreeResponse"][];
                };
            };
        };
    };
}
