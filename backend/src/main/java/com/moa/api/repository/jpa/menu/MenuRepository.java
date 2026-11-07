package com.moa.api.repository.jpa.menu;

import java.util.List;

import com.moa.api.entity.menu.Menu;
import com.moa.common.repository.SoftDeleteRepository;

public interface MenuRepository extends SoftDeleteRepository<Menu, Long> {

	List<Menu> findAllByOrderBySortOrderAscIdAsc();

	boolean existsByPath(String path);

	boolean existsByPathAndIdNot(String path, Long id);

	boolean existsByParentId(Long parentId);
}
