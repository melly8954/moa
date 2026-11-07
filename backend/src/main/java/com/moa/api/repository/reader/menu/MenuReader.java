package com.moa.api.repository.reader.menu;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.moa.api.entity.menu.Menu;
import com.moa.api.repository.jpa.menu.MenuRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MenuReader {

	private final MenuRepository menuRepository;

	/** 지우지 않은 메뉴 전부, 정렬 순서대로 (sort_order, id) */
	public List<Menu> findAll() {
		return menuRepository.findAllByOrderBySortOrderAscIdAsc();
	}

	public Optional<Menu> find(Long id) {
		return menuRepository.findById(id);
	}

	/** excludeId가 있으면 그 메뉴는 빼고 본다 (바꾸기) */
	public boolean existsPath(String path, Long excludeId) {
		return excludeId == null ? menuRepository.existsByPath(path)
			: menuRepository.existsByPathAndIdNot(path, excludeId);
	}

	public boolean hasChildren(Long id) {
		return menuRepository.existsByParentId(id);
	}
}
