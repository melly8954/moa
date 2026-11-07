package com.moa.common.guard;

/**
 * 시작 가드 규칙. dev·prod 프로필로 시작할 때 설정값을 검사한다 (local·test는 검사하지 않는다).
 * 루트 패키지 아래에 이 인터페이스를 구현한 클래스를 두면 StartupGuard가 찾아서 돌린다 (빈이 아니다, 인자 없는 생성자).
 * 헬퍼는 자기 설정 규칙을 이렇게 더한다.
 */
public interface GuardRule {

	void check(GuardContext context);
}
