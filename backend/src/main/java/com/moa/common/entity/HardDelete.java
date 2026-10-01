package com.moa.common.entity;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 소프트 삭제를 쓰지 않는 엔터티 표시. SoftDeleteEntity를 상속하지 않는 엔터티는 반드시 붙인다.
 * 이유는 docs/design/conventions.md DB 절 하드 삭제 표에도 같은 내용으로 적는다.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface HardDelete {

	/** 하드 삭제를 쓰는 이유 */
	String reason();
}
