/**
 * 엔터티: 기본은 SoftDeleteEntity 상속 + @SQLRestriction(SoftDeleteEntity.NOT_DELETED).
 * 하드 삭제는 BaseEntity(또는 BaseTimeEntity) 상속 + @HardDelete(reason).
 */
package com.moa.api.entity;
