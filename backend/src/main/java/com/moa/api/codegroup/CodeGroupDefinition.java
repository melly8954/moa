package com.moa.api.codegroup;

/**
 * 코드 그룹 하나 (moa, specs/004-admin-codes FR-004). 그 그룹을 읽는 기능이 코드에 등록한다.
 * 관리 화면에서는 그룹을 더하거나 고치지 않고, 그룹 안의 항목만 다룬다.
 *
 * @param groupCode 대문자·숫자·밑줄 (예: GAME_GENRE)
 * @param name 화면에 보일 이름
 * @param description 무엇을 담는지 (없어도 된다)
 * @param usedBy 이 그룹을 쓰는 곳 (예: 게임 등록 화면의 장르)
 */
public record CodeGroupDefinition(String groupCode, String name, String description, String usedBy) {
}
