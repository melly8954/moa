package com.moa.api.repository.writer.menu;

import org.springframework.stereotype.Component;

import com.moa.api.entity.menu.Menu;
import com.moa.api.repository.jpa.menu.MenuRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MenuWriter {

	private final MenuRepository menuRepository;

	public Menu save(Menu menu) {
		return menuRepository.save(menu);
	}
}
