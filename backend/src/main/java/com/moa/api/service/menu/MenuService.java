package com.moa.api.service.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moa.api.dto.menu.MenuDto;
import com.moa.api.dto.menu.MenuOrderDto;
import com.moa.api.entity.menu.Menu;
import com.moa.api.repository.reader.menu.MenuReader;
import com.moa.api.repository.writer.menu.MenuWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.security.permission.PermissionRegistry;

import lombok.RequiredArgsConstructor;

/**
 * 메뉴 권한 (선택 헬퍼 menu). 깊이는 2단계(그룹 → 메뉴)까지다.
 */
@Service
@RequiredArgsConstructor
public class MenuService {

	private final MenuReader menuReader;
	private final MenuWriter menuWriter;
	private final PermissionRegistry permissionRegistry;

	/** 전체 트리 (사용 안 함 포함) */
	public List<MenuDto> getTree() {
		return tree(menuReader.findAll(), menu -> true);
	}

	/**
	 * 사용 중이고 이 권한으로 볼 수 있는 메뉴 트리. 권한 키가 없는(null) 메뉴는 모두가 본다.
	 * 경로가 없는 1단계 메뉴(그룹)는 보이는 자식이 없으면 뺀다.
	 */
	public List<MenuDto> getVisibleTree(Set<String> permissions) {
		List<MenuDto> visible = tree(menuReader.findAll(), menu -> menu.isEnabled()
			&& (menu.getPermission() == null || permissions.contains(menu.getPermission())));
		return visible.stream().filter(menu -> menu.path() != null || !menu.children().isEmpty()).toList();
	}

	/**
	 * @throws ServiceException 경로 형식이 틀리거나 등록되지 않은 권한이면 V001, 경로가 겹치면 R002, 부모가 없으면 R001, 3단계가 되면 B001
	 */
	public MenuDto create(Long parentId, String name, String path, String icon, int sortOrder, String permission) {
		String normalizedPath = checkPath(path, null);
		String normalizedPermission = checkPermission(permission);
		checkParent(null, parentId, false);
		return MenuDto.of(
			menuWriter.save(new Menu(parentId, name, normalizedPath, icon, sortOrder, normalizedPermission)),
			List.of());
	}

	/**
	 * @throws ServiceException 메뉴·부모가 없으면 R001, 경로·권한 V001, 경로 중복 R002, 3단계·자기 부모면 B001
	 */
	public MenuDto modify(Long id, Long parentId, String name, String path, String icon, int sortOrder,
		boolean enabled, String permission) {
		Menu menu = require(id);
		String normalizedPath = checkPath(path, id);
		String normalizedPermission = checkPermission(permission);
		checkParent(id, parentId, menuReader.hasChildren(id));
		menu.modify(parentId, name, normalizedPath, icon, sortOrder, enabled, normalizedPermission);
		return MenuDto.of(menu, List.of());
	}

	/**
	 * @throws ServiceException 없으면 R001, 하위 메뉴가 있으면 B001
	 */
	public void delete(Long id, Long actorId) {
		Menu menu = require(id);
		if (menuReader.hasChildren(id)) {
			throw new ServiceException(ErrorCode.INVALID_STATE, "하위 메뉴가 있어 지울 수 없습니다.");
		}
		menu.delete(actorId);
	}

	/**
	 * 보낸 메뉴의 부모와 순서를 한 번에 바꾼다. 바꾼 뒤의 트리 전체를 검사하고, 틀리면 아무것도 반영하지 않는다
	 * (호출하는 Application의 트랜잭션이 롤백한다).
	 *
	 * @throws ServiceException 없는 메뉴·부모 R001, 3단계·자기 부모 B001
	 */
	public void reorder(List<MenuOrderDto> orders) {
		Map<Long, Menu> menus = menuReader.findAll().stream()
			.collect(Collectors.toMap(Menu::getId, menu -> menu));
		for (MenuOrderDto order : orders) {
			Menu menu = menus.get(order.id());
			if (menu == null) {
				throw new ServiceException(ErrorCode.NOT_FOUND, "없는 메뉴입니다: " + order.id());
			}
			menu.move(order.parentId(), order.sortOrder());
		}
		for (Menu menu : menus.values()) {
			if (menu.getParentId() == null) {
				continue;
			}
			Menu parent = menus.get(menu.getParentId());
			if (parent == null) {
				throw new ServiceException(ErrorCode.NOT_FOUND, "없는 부모 메뉴입니다: " + menu.getParentId());
			}
			if (parent.getId().equals(menu.getId()) || parent.getParentId() != null) {
				throw new ServiceException(ErrorCode.INVALID_STATE, "메뉴는 2단계까지만 둘 수 있습니다: " + menu.getId());
			}
		}
	}

	private Menu require(Long id) {
		return menuReader.find(id).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}

	/** 경로는 비울 수 있다(그룹). 있으면 /로 시작하는 내부 경로이고, 지우지 않은 메뉴 사이에서 겹치지 않는다 */
	private String checkPath(String path, Long excludeId) {
		if (path == null || path.isBlank()) {
			return null;
		}
		String trimmed = path.trim();
		if (!trimmed.startsWith("/") || trimmed.startsWith("//") || trimmed.contains("://")) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, "메뉴 경로는 /로 시작하는 내부 경로여야 합니다: " + trimmed);
		}
		if (menuReader.existsPath(trimmed, excludeId)) {
			throw new ServiceException(ErrorCode.DUPLICATE_RESOURCE, "이미 쓰는 메뉴 경로입니다: " + trimmed);
		}
		return trimmed;
	}

	/** 부모는 1단계 메뉴여야 하고 자기 자신일 수 없다. 자식이 있는 메뉴는 부모를 가질 수 없다 (3단계가 된다) */
	private void checkParent(Long id, Long parentId, boolean hasChildren) {
		if (parentId == null) {
			return;
		}
		if (parentId.equals(id)) {
			throw new ServiceException(ErrorCode.INVALID_STATE, "자기 자신을 부모로 둘 수 없습니다.");
		}
		Menu parent = require(parentId);
		if (parent.getParentId() != null || hasChildren) {
			throw new ServiceException(ErrorCode.INVALID_STATE, "메뉴는 2단계까지만 둘 수 있습니다.");
		}
	}

	/** 권한은 비울 수 있다(모두가 본다). 있으면 등록된 권한 키여야 한다 */
	private String checkPermission(String permission) {
		if (permission == null || permission.isBlank()) {
			return null;
		}
		String trimmed = permission.trim();
		if (!permissionRegistry.contains(trimmed)) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, "등록되지 않은 권한입니다: " + trimmed);
		}
		return trimmed;
	}

	/** 1단계와 그 자식으로 묶는다. 조건에 맞지 않는 1단계 메뉴는 자식과 함께 뺀다 */
	private List<MenuDto> tree(List<Menu> menus, Predicate<Menu> include) {
		Map<Long, List<Menu>> children = new HashMap<>();
		menus.stream().filter(menu -> menu.getParentId() != null)
			.forEach(menu -> children.computeIfAbsent(menu.getParentId(), key -> new ArrayList<>()).add(menu));
		return menus.stream()
			.filter(menu -> menu.getParentId() == null && include.test(menu))
			.map(top -> MenuDto.of(top, children.getOrDefault(top.getId(), List.of()).stream()
				.filter(include)
				.map(child -> MenuDto.of(child, List.of()))
				.toList()))
			.toList();
	}
}
