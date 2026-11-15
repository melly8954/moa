package com.moa.api.controller.v1.admin.role.response;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.role.PermissionTreeDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "moa: 역할 창에서 권한을 고르는 트리 (specs/003-admin-roles FR-004)")
public record PermissionTreeResponse(
	@Schema(description = "관리 메뉴 트리. 메뉴 관리와 같은 순서·묶음이고 꺼진 메뉴도 들어 있다") List<Node> menus,
	@Schema(description = "어느 관리 메뉴도 가리키지 않는 관리 프로그램") List<Unlisted> unlisted) {

	public static PermissionTreeResponse from(PermissionTreeDto tree) {
		return new PermissionTreeResponse(tree.menus().stream().map(Node::from).toList(),
			tree.unlisted().stream().map(Unlisted::from).toList());
	}

	@Schema(description = "관리 메뉴")
	public record Node(
		@Schema(description = "메뉴 ID") Long menuId,
		@Schema(description = "이름", example = "코드 관리") String name,
		@Schema(description = "사용 여부") boolean enabled,
		@Schema(description = "화면 메뉴의 권한. 묶음 메뉴는 없다") @Nullable PermissionRef permission,
		@Schema(description = "묶음 메뉴의 하위 메뉴") List<Node> children) {

		static Node from(PermissionTreeDto.Node node) {
			return new Node(node.menuId(), node.name(), node.enabled(),
				node.permission() == null ? null : new PermissionRef(node.permission(), node.permissionName()),
				node.children().stream().map(Node::from).toList());
		}
	}

	@Schema(description = "메뉴에 없는 관리 프로그램")
	public record Unlisted(
		@Schema(description = "프로그램 키", example = "ADMIN_CODES") String programKey,
		@Schema(description = "이름", example = "코드 관리") String name,
		@Schema(description = "권한") PermissionRef permission) {

		static Unlisted from(PermissionTreeDto.Unlisted unlisted) {
			return new Unlisted(unlisted.programKey(), unlisted.name(),
				new PermissionRef(unlisted.permission(), unlisted.permissionName()));
		}
	}

	@Schema(description = "권한")
	public record PermissionRef(
		@Schema(description = "권한 키", example = "CODE_MANAGE") String key,
		@Schema(description = "권한 이름", example = "공통코드 관리") String name) {
	}
}
