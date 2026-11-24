package com.moa.api.permission;

import java.util.List;

import org.springframework.stereotype.Component;

import com.moa.common.security.permission.PermissionDefinition;
import com.moa.common.security.permission.PermissionSource;

/** 팝업 관리의 권한 (specs/016-banner-popup FR-002) */
@Component
public class PopupPermissions implements PermissionSource {

	public static final String MANAGE = "POPUP_MANAGE";

	@Override
	public List<PermissionDefinition> permissions() {
		return List.of(new PermissionDefinition(MANAGE, "팝업 관리", "마케팅"));
	}
}
