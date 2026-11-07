package com.moa.support.sample;

import java.util.List;

import org.springframework.stereotype.Component;

import com.moa.common.security.permission.PermissionDefinition;
import com.moa.common.security.permission.PermissionSource;

/** 골격 검증용 권한. 테스트에서만 쓴다 */
@Component
public class SamplePermissions implements PermissionSource {

	public static final String MANAGE = "SAMPLE_MANAGE";

	@Override
	public List<PermissionDefinition> permissions() {
		return List.of(new PermissionDefinition(MANAGE, "샘플 관리", "샘플"));
	}
}
