package com.moa.api.service.menu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moa.api.dto.menu.MenuAudience;
import com.moa.api.dto.menu.MenuDto;
import com.moa.api.dto.menu.MenuOrderDto;
import com.moa.api.dto.menu.MenuVisibility;
import com.moa.api.dto.menu.ProgramDto;
import com.moa.api.entity.menu.Menu;
import com.moa.api.program.ProgramDefinition;
import com.moa.api.program.ProgramRegistry;
import com.moa.api.repository.reader.menu.MenuReader;
import com.moa.api.repository.writer.menu.MenuWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.security.permission.PermissionRegistry;

import lombok.RequiredArgsConstructor;

/**
 * 메뉴 권한 (선택 헬퍼 menu). 깊이는 2단계(묶음 → 화면 메뉴)까지다.
 * 화면 메뉴는 등록된 프로그램을 가리키고, 경로와 권한은 프로그램을 따른다 (moa, specs/001-admin-menus FR-008·FR-009).
 */
@Service
@RequiredArgsConstructor
public class MenuService {

	private final MenuReader menuReader;
	private final MenuWriter menuWriter;
	private final ProgramRegistry programRegistry;
	private final PermissionRegistry permissionRegistry;

	/** 등록된 프로그램 전부. 관리 프로그램 먼저 */
	public List<ProgramDto> getPrograms() {
		return programRegistry.all().stream().map(this::program).toList();
	}

	/** 전체 트리 (사용 안 함 포함) */
	public List<MenuDto> getTree() {
		return tree(menuReader.findAll(), menu -> true);
	}

	/**
	 * 사용 중이고 볼 수 있는 이 대상의 메뉴 트리 (moa, specs/001-admin-menus FR-002·FR-007).
	 * 관리 메뉴는 프로그램의 권한을 가졌을 때, 사용자 메뉴는 공개 범위가 PUBLIC이거나 로그인했을 때 보인다.
	 * 없는 프로그램을 가리키는 메뉴는 보이지 않고, 보이는 하위 메뉴가 없는 묶음 메뉴는 뺀다.
	 *
	 * @param permissions 내 권한 (ADMIN은 등록된 모든 권한)
	 */
	public List<MenuDto> getVisibleTree(MenuAudience audience, Set<String> permissions, boolean signedIn) {
		List<MenuDto> visible = tree(menuReader.findAll(), menu -> menu.isEnabled()
			&& menu.getAudience() == audience
			&& isVisible(menu, permissions, signedIn));
		return visible.stream().filter(menu -> menu.programKey() != null || !menu.children().isEmpty()).toList();
	}

	/**
	 * 하위 메뉴의 대상은 보낸 값과 상관없이 부모를 따른다. 1단계 메뉴의 대상을 비우면 관리 메뉴(ADMIN)다.
	 *
	 * @throws ServiceException 프로그램·공개 범위가 틀리면 V001, 부모가 없으면 R001, 3단계가 되면 B001
	 */
	public MenuDto create(Long parentId, String name, String programKey, String icon, int sortOrder,
		MenuAudience audience, MenuVisibility visibility) {
		checkParent(null, parentId, false);
		MenuAudience resolved = parentId != null ? require(parentId).getAudience()
			: audience != null ? audience : MenuAudience.ADMIN;
		ProgramDefinition program = checkProgram(programKey, resolved, parentId != null);
		MenuVisibility checkedVisibility = checkVisibility(program, resolved, visibility);
		Menu saved = menuWriter.save(
			new Menu(parentId, name, key(program), icon, sortOrder, resolved, checkedVisibility));
		return MenuDto.of(saved, program(program), List.of());
	}

	/**
	 * 하위 메뉴의 대상은 부모를 따른다. 대상을 비우면 그대로 둔다. 하위 메뉴가 있는 메뉴는 대상을 바꿀 수 없다
	 * (하위 메뉴의 프로그램 대상이 메뉴 대상이다, FR-009).
	 *
	 * @throws ServiceException 메뉴·부모가 없으면 R001, 프로그램·공개 범위 V001,
	 *     3단계·자기 부모·하위 메뉴가 있는 메뉴의 대상 바꾸기 B001
	 */
	public MenuDto modify(Long id, Long parentId, String name, String programKey, String icon, int sortOrder,
		boolean enabled, MenuAudience audience, MenuVisibility visibility) {
		Menu menu = require(id);
		boolean hasChildren = menuReader.hasChildren(id);
		checkParent(id, parentId, hasChildren);
		MenuAudience resolved = parentId != null ? require(parentId).getAudience()
			: audience != null ? audience : menu.getAudience();
		if (resolved != menu.getAudience() && hasChildren) {
			throw new ServiceException(ErrorCode.INVALID_STATE, "하위 메뉴가 있는 메뉴는 대상을 바꿀 수 없습니다.");
		}
		ProgramDefinition program = checkProgram(programKey, resolved, parentId != null);
		MenuVisibility checkedVisibility = checkVisibility(program, resolved, visibility);
		menu.modify(parentId, name, key(program), icon, sortOrder, enabled, resolved, checkedVisibility);
		return MenuDto.of(menu, program(program), List.of());
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
	 * 여러 메뉴의 사용 여부를 한 번에 바꾼다 (moa, specs/001-admin-menus FR-010). 하나라도 없으면 아무것도 바꾸지 않는다
	 * (호출하는 Application의 트랜잭션이 롤백한다).
	 *
	 * @throws ServiceException 없는 메뉴 R001
	 */
	public void setEnabled(Collection<Long> ids, boolean enabled) {
		requireAll(ids).forEach(menu -> menu.changeEnabled(enabled));
	}

	/**
	 * 여러 메뉴를 한 번에 지운다 (moa, FR-010). 고른 것끼리 부모·자식이면 함께 지울 수 있다.
	 * 고르지 않은 하위 메뉴가 남는 묶음이 하나라도 있으면 아무것도 지우지 않는다.
	 *
	 * @throws ServiceException 없는 메뉴 R001, 고르지 않은 하위 메뉴가 있는 묶음 B001
	 */
	public void deleteAll(Collection<Long> ids, Long actorId) {
		List<Menu> targets = requireAll(ids);
		Set<Long> chosen = targets.stream().map(Menu::getId).collect(Collectors.toSet());
		boolean leavesChildren = menuReader.findAll().stream()
			.anyMatch(menu -> menu.getParentId() != null && chosen.contains(menu.getParentId())
				&& !chosen.contains(menu.getId()));
		if (leavesChildren) {
			throw new ServiceException(ErrorCode.INVALID_STATE, "하위 메뉴가 있는 묶음은 하위 메뉴와 함께 지우거나 하위 메뉴를 먼저 옮기세요.");
		}
		targets.forEach(menu -> menu.delete(actorId));
	}

	/**
	 * 보낸 메뉴의 부모와 순서를 한 번에 바꾼다. 바꾼 뒤의 트리 전체를 검사하고, 틀리면 아무것도 반영하지 않는다
	 * (호출하는 Application의 트랜잭션이 롤백한다).
	 *
	 * @throws ServiceException 없는 메뉴·부모 R001, 3단계·자기 부모·묶음 메뉴를 하위로·대상이 다른 부모 아래로 B001
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
			if (menu.getProgramKey() == null) {
				throw new ServiceException(ErrorCode.INVALID_STATE, "묶음 메뉴는 하위 메뉴가 될 수 없습니다: " + menu.getId());
			}
			// 메뉴 대상은 프로그램의 대상이라 다른 대상의 부모 아래로는 옮기지 않는다 (FR-009)
			if (parent.getAudience() != menu.getAudience()) {
				throw new ServiceException(ErrorCode.INVALID_STATE, "대상이 다른 메뉴 아래로 옮길 수 없습니다: " + menu.getId());
			}
		}
	}

	private Menu require(Long id) {
		return menuReader.find(id).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}

	/** 보낸 ID가 모두 있어야 한다 (겹친 ID는 하나로 본다) */
	private List<Menu> requireAll(Collection<Long> ids) {
		Map<Long, Menu> menus = menuReader.findAll().stream()
			.collect(Collectors.toMap(Menu::getId, menu -> menu));
		List<Menu> found = new ArrayList<>();
		for (Long id : new LinkedHashSet<>(ids)) {
			Menu menu = menus.get(id);
			if (menu == null) {
				throw new ServiceException(ErrorCode.NOT_FOUND, "없는 메뉴입니다: " + id);
			}
			found.add(menu);
		}
		return found;
	}

	private boolean isVisible(Menu menu, Set<String> permissions, boolean signedIn) {
		if (menu.getProgramKey() == null) {
			return true; // 묶음 메뉴: 보이는 하위 메뉴가 있을 때만 남는다
		}
		return programRegistry.find(menu.getProgramKey())
			.map(program -> menu.getAudience() == MenuAudience.ADMIN
				? permissions.contains(program.permission())
				: menu.getVisibility() == MenuVisibility.PUBLIC || signedIn)
			.orElse(false);
	}

	/**
	 * 프로그램은 비울 수 있다(묶음 메뉴, 1단계만). 있으면 등록된 프로그램이고 메뉴와 대상이 같다.
	 * 없는 프로그램(MISSING 등)은 받지 않는다. 다른 프로그램으로 바꾸거나 지운다.
	 */
	private ProgramDefinition checkProgram(String programKey, MenuAudience audience, boolean child) {
		if (programKey == null || programKey.isBlank()) {
			if (child) {
				throw new ServiceException(ErrorCode.INVALID_INPUT, "하위 메뉴는 프로그램을 골라야 합니다.");
			}
			return null;
		}
		ProgramDefinition program = programRegistry.find(programKey.trim())
			.orElseThrow(() -> new ServiceException(ErrorCode.INVALID_INPUT, "등록되지 않은 프로그램입니다: " + programKey));
		if (program.audience() != audience) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, "메뉴와 대상이 다른 프로그램입니다: " + program.key());
		}
		return program;
	}

	/**
	 * 사용자 화면 메뉴는 공개 범위가 있어야 하고, 로그인이 필요한 프로그램이면 MEMBER만 된다.
	 * 관리 메뉴와 묶음 메뉴는 공개 범위를 갖지 않는다 (보낸 값은 버린다).
	 */
	private MenuVisibility checkVisibility(ProgramDefinition program, MenuAudience audience,
		MenuVisibility visibility) {
		if (program == null || audience == MenuAudience.ADMIN) {
			return null;
		}
		if (visibility == null) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, "사용자 메뉴는 공개 범위를 골라야 합니다.");
		}
		if (program.signInRequired() && visibility == MenuVisibility.PUBLIC) {
			throw new ServiceException(ErrorCode.INVALID_INPUT,
				"로그인이 필요한 프로그램은 회원에게만 보일 수 있습니다: " + program.key());
		}
		return visibility;
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

	private String key(ProgramDefinition program) {
		return program == null ? null : program.key();
	}

	/** 1단계와 그 자식으로 묶는다. 조건에 맞지 않는 1단계 메뉴는 자식과 함께 뺀다 */
	private List<MenuDto> tree(List<Menu> menus, Predicate<Menu> include) {
		Map<Long, List<Menu>> children = new HashMap<>();
		menus.stream().filter(menu -> menu.getParentId() != null)
			.forEach(menu -> children.computeIfAbsent(menu.getParentId(), key -> new ArrayList<>()).add(menu));
		return menus.stream()
			.filter(menu -> menu.getParentId() == null && include.test(menu))
			.map(top -> dto(top, children.getOrDefault(top.getId(), List.of()).stream()
				.filter(include)
				.map(child -> dto(child, List.of()))
				.toList()))
			.toList();
	}

	private MenuDto dto(Menu menu, List<MenuDto> children) {
		return MenuDto.of(menu, programRegistry.find(menu.getProgramKey()).map(this::program).orElse(null),
			children);
	}

	private ProgramDto program(ProgramDefinition program) {
		return program == null ? null : ProgramDto.of(program, permissionRegistry);
	}
}
