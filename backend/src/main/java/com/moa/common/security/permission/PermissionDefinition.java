package com.moa.common.security.permission;

/**
 * 권한 하나. 키는 코드가 쓰고, 이름과 묶음은 관리 화면이 보여 준다.
 *
 * @param key 대문자·숫자·밑줄 (예: CODE_MANAGE)
 * @param name 화면에 보일 이름 (예: 공통코드 관리)
 * @param group 화면에서 묶을 이름 (예: 공통코드)
 */
public record PermissionDefinition(String key, String name, String group) {
}
