package com.moa.api.repository.writer.popup;

import org.springframework.stereotype.Component;

import com.moa.api.entity.popup.Popup;
import com.moa.api.repository.jpa.popup.PopupRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PopupWriter {

	private final PopupRepository popupRepository;

	public Popup save(Popup popup) {
		return popupRepository.save(popup);
	}
}
