package com.moa.api.application.v1.menu;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.application.v1.menu.command.CreateMenuCommand;
import com.moa.api.application.v1.menu.command.ModifyMenuCommand;
import com.moa.api.dto.menu.MenuDto;
import com.moa.api.dto.menu.MenuOrderDto;
import com.moa.api.service.menu.MenuService;
import com.moa.common.audit.CurrentActor;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.permission.PermissionGuard;

import lombok.RequiredArgsConstructor;

/**
 * 메뉴 권한 (선택 헬퍼 menu). 내 메뉴는 캐시하지 않는다 (메뉴 표가 작고, 관리자가 고치면 바로 보여야 한다).
 */
@Component
@RequiredArgsConstructor
public class MenuApplication {

	private final MenuService menuService;
	private final CurrentActor currentActor;
	private final PermissionGuard permissionGuard;

	@Transactional(readOnly = true)
	public List<MenuDto> getMyMenus(AuthPrincipal principal) {
		return menuService.getVisibleTree(permissionGuard.permissions(principal));
	}

	@Transactional(readOnly = true)
	public List<MenuDto> getAllMenus() {
		return menuService.getTree();
	}

	@Transactional
	public MenuDto create(CreateMenuCommand command) {
		return menuService.create(command.parentId(), command.name(), command.path(), command.icon(),
			command.sortOrder(), command.permission());
	}

	@Transactional
	public MenuDto modify(ModifyMenuCommand command) {
		return menuService.modify(command.id(), command.parentId(), command.name(), command.path(), command.icon(),
			command.sortOrder(), command.enabled(), command.permission());
	}

	@Transactional
	public void delete(Long id) {
		menuService.delete(id, currentActor.id());
	}

	@Transactional
	public void reorder(List<MenuOrderDto> orders) {
		menuService.reorder(orders);
	}
}
