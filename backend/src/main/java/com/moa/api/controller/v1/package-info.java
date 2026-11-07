/**
 * 컨트롤러: HTTP 요청·응답만 다룬다. Application만 부른다. 이름은 Api<리소스>Controller.
 * 도메인마다 하위 패키지를 둔다: controller/v1/<domain>/{request,response}
 * <ul>
 * <li>요청 DTO(record): <리소스><동작>Request. toCommand()로 Command를 만든다</li>
 * <li>응답 DTO(record): <리소스>GetResponse(단건), <리소스>ListResponse(목록·페이징 항목). from(Dto)로 만든다</li>
 * </ul>
 */
package com.moa.api.controller.v1;
