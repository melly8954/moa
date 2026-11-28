package com.moa.api.permission;

import java.util.List;

import org.springframework.stereotype.Component;

import com.moa.common.security.permission.PermissionDefinition;
import com.moa.common.security.permission.PermissionSource;

/** 1:1 문의 관리의 권한 (specs/017-customer-center FR-002) */
@Component
public class InquiryPermissions implements PermissionSource {

	public static final String MANAGE = "INQUIRY_MANAGE";

	@Override
	public List<PermissionDefinition> permissions() {
		return List.of(new PermissionDefinition(MANAGE, "문의 관리", "고객센터"));
	}
}
