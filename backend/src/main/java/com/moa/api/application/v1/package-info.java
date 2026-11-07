/**
 * 애플리케이션: 유스케이스 진입점이자 트랜잭션 경계(@Transactional). Command·Query를 받아 Service를 조합하고 Dto를 돌려준다. 이름은 <리소스>Application.
 * 도메인마다 하위 패키지를 둔다: application/v1/<domain>/{command,query}
 * <ul>
 * <li>변경 요청 객체(record): <동작><리소스>Command (예: RegisterCodeCommand)</li>
 * <li>조회 조건 객체(record): <리소스>Query</li>
 * </ul>
 */
package com.moa.api.application.v1;
