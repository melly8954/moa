package com.moa.api.repository.jpa.menu;

import java.util.List;

import com.moa.api.entity.menu.Menu;
import com.moa.common.repository.SoftDeleteRepository;

public interface MenuRepository extends SoftDeleteRepository<Menu, Long> {

	List<Menu> findAllByOrderBySortOrderAscIdAsc();

	boolean existsByParentId(Long parentId);
}
