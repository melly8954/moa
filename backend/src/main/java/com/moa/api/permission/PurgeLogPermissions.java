package com.moa.api.permission;

import java.util.List;

import org.springframework.stereotype.Component;

import com.moa.common.security.permission.PermissionDefinition;
import com.moa.common.security.permission.PermissionSource;

/** 개인정보 파기 기록의 권한 (moa, specs/013-purge-log FR-008) */
@Component
public class PurgeLogPermissions implements PermissionSource {

	public static final String READ = "PURGE_LOG_READ";

	@Override
	public List<PermissionDefinition> permissions() {
		return List.of(new PermissionDefinition(READ, "파기 기록 조회", "파기 기록"));
	}
}
