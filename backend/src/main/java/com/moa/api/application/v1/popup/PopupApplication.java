package com.moa.api.application.v1.popup;

import java.time.LocalDateTime;
import java.util.Collection;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.dto.marketing.ExposureStatusFilter;
import com.moa.api.dto.popup.PopupContent;
import com.moa.api.dto.popup.PopupDto;
import com.moa.api.entity.popup.Popup;
import com.moa.api.service.popup.PopupService;
import com.moa.common.audit.CurrentActor;
import com.moa.common.helper.richtext.RichTextImageService;

import lombok.RequiredArgsConstructor;

/**
 * 팝업 관리 (specs/016-banner-popup). 본문 이미지는 같은 트랜잭션에서 rich-text 헬퍼에 잇는다 (research R8, 공지와 같다).
 */
@Component
@RequiredArgsConstructor
public class PopupApplication {

	private final PopupService popupService;
	private final RichTextImageService richTextImageService;
	private final CurrentActor currentActor;

	@Transactional(readOnly = true)
	public Page<PopupDto> getAdminPage(@Nullable String keyword, @Nullable ExposureStatusFilter status,
		Pageable pageable) {
		return popupService.getAdminPage(keyword, status, LocalDateTime.now(), pageable);
	}

	@Transactional(readOnly = true)
	public PopupDto get(Long id) {
		return popupService.get(id, LocalDateTime.now());
	}

	@Transactional
	public PopupDto create(PopupContent content) {
		PopupDto popup = popupService.create(content, LocalDateTime.now());
		richTextImageService.attach(Popup.OWNER_TYPE, popup.id(), popup.content());
		return popup;
	}

	@Transactional
	public PopupDto modify(Long id, PopupContent content) {
		PopupDto popup = popupService.modify(id, content, LocalDateTime.now());
		richTextImageService.attach(Popup.OWNER_TYPE, popup.id(), popup.content());
		return popup;
	}

	@Transactional
	public void delete(Long id) {
		popupService.delete(id, currentActor.id());
		richTextImageService.detach(Popup.OWNER_TYPE, id);
	}

	@Transactional
	public void setEnabled(Collection<Long> ids, boolean enabled) {
		popupService.setEnabled(ids, enabled);
	}

	@Transactional
	public void deleteAll(Collection<Long> ids) {
		popupService.deleteAll(ids, currentActor.id())
			.forEach(id -> richTextImageService.detach(Popup.OWNER_TYPE, id));
	}
}
