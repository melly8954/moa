package com.moa.api.permission;

import java.util.List;

import org.springframework.stereotype.Component;

import com.moa.common.security.permission.PermissionDefinition;
import com.moa.common.security.permission.PermissionSource;

/** 공지사항의 권한 (specs/006-notices) */
@Component
public class NoticePermissions implements PermissionSource {

	public static final String MANAGE = "NOTICE_MANAGE";

	@Override
	public List<PermissionDefinition> permissions() {
		return List.of(new PermissionDefinition(MANAGE, "공지 관리", "공지"));
	}
}
