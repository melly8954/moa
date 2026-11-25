package com.moa.api.service.popup;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moa.api.dto.marketing.ExposureStatusFilter;
import com.moa.api.dto.popup.PopupContent;
import com.moa.api.dto.popup.PopupDto;
import com.moa.api.entity.popup.Popup;
import com.moa.api.repository.reader.popup.PopupReader;
import com.moa.api.repository.writer.popup.PopupWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.response.ErrorResponse.FieldError;

import lombok.RequiredArgsConstructor;

/**
 * 팝업 (specs/016-banner-popup). 내용의 이미지는 rich-text 헬퍼가 맡는다 (research R8).
 */
@Service
@RequiredArgsConstructor
public class PopupService {

	private final PopupReader popupReader;
	private final PopupWriter popupWriter;

	public Page<PopupDto> getAdminPage(@Nullable String keyword, @Nullable ExposureStatusFilter status,
		LocalDateTime now, Pageable pageable) {
		return popupReader.findAdminPage(keyword, status, now, pageable).map(popup -> PopupDto.from(popup, now));
	}

	/**
	 * @throws ServiceException 없으면 R001
	 */
	public PopupDto get(Long id, LocalDateTime now) {
		return PopupDto.from(require(id), now);
	}

	/**
	 * @throws ServiceException 끝 일시가 시작보다 앞이면 V001
	 */
	public PopupDto create(PopupContent content, LocalDateTime now) {
		validate(content);
		return PopupDto.from(popupWriter.save(new Popup(content)), now);
	}

	/**
	 * @throws ServiceException 없으면 R001, 끝 일시가 시작보다 앞이면 V001
	 */
	public PopupDto modify(Long id, PopupContent content, LocalDateTime now) {
		Popup popup = require(id);
		validate(content);
		popup.modify(content);
		return PopupDto.from(popup, now);
	}

	/**
	 * @throws ServiceException 없으면 R001
	 */
	public void delete(Long id, Long actorId) {
		require(id).delete(actorId);
	}

	/**
	 * 여러 팝업을 한 번에 켜거나 끈다 (FR-008). 하나라도 없으면 아무것도 바꾸지 않는다. 이미 그 값인 팝업은 그대로 둔다.
	 *
	 * @throws ServiceException 없는 팝업 R001
	 */
	public void setEnabled(Collection<Long> ids, boolean enabled) {
		requireAll(ids).forEach(popup -> popup.changeEnabled(enabled));
	}

	/**
	 * 여러 팝업을 한 번에 지운다 (FR-008). 하나라도 없으면 아무것도 지우지 않는다.
	 *
	 * @return 지운 팝업 ID (본문 이미지를 풀 때 쓴다)
	 * @throws ServiceException 없는 팝업 R001
	 */
	public List<Long> deleteAll(Collection<Long> ids, Long actorId) {
		List<Popup> targets = requireAll(ids);
		targets.forEach(popup -> popup.delete(actorId));
		return targets.stream().map(Popup::getId).toList();
	}

	private Popup require(Long id) {
		return popupReader.findById(id).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}

	/** 보낸 ID가 모두 있어야 한다 (겹친 ID는 하나로 본다) */
	private List<Popup> requireAll(Collection<Long> ids) {
		Map<Long, Popup> popups = popupReader.findAllByIdIn(ids).stream()
			.collect(Collectors.toMap(Popup::getId, Function.identity()));
		List<Popup> found = new ArrayList<>();
		for (Long id : new LinkedHashSet<>(ids)) {
			Popup popup = popups.get(id);
			if (popup == null) {
				throw new ServiceException(ErrorCode.NOT_FOUND, "없는 팝업입니다: " + id);
			}
			found.add(popup);
		}
		return found;
	}

	private void validate(PopupContent content) {
		if (content.endsAt() != null && !content.endsAt().isAfter(content.startsAt())) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(),
				List.of(new FieldError("endsAt", "노출 끝 일시는 시작 일시보다 뒤여야 합니다.")));
		}
	}
}
