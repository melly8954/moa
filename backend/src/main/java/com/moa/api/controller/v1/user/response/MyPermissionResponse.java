package com.moa.api.controller.v1.user.response;

import java.util.List;

/**
 * 내 권한
 *
 * @param permissions 권한 키. 등록 순서대로
 */
public record MyPermissionResponse(List<String> permissions) {
}
