package com.moa.api.service.banner;

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

import com.moa.api.dto.banner.BannerContent;
import com.moa.api.dto.banner.BannerDto;
import com.moa.api.dto.marketing.ExposureStatusFilter;
import com.moa.api.entity.banner.Banner;
import com.moa.api.repository.reader.banner.BannerReader;
import com.moa.api.repository.writer.banner.BannerWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.helper.richtext.HtmlSanitizer;
import com.moa.common.helper.richtext.RichTextImageService;
import com.moa.common.response.ErrorResponse.FieldError;

import lombok.RequiredArgsConstructor;

/**
 * 배너 (specs/016-banner-popup). 이미지는 rich-text 헬퍼로 올린 이미지 주소다. JPG·PNG·WebP만 받는다 (research R1, R10).
 * 이미지의 주인은 확인하지 않는다 (research R10).
 */
@Service
@RequiredArgsConstructor
public class BannerService {

	private static final String GIF = "image/gif";

	private final BannerReader bannerReader;
	private final BannerWriter bannerWriter;
	private final RichTextImageService richTextImageService;

	public Page<BannerDto> getAdminPage(@Nullable String keyword, @Nullable ExposureStatusFilter status,
		LocalDateTime now, Pageable pageable) {
		return bannerReader.findAdminPage(keyword, status, now, pageable).map(banner -> BannerDto.from(banner, now));
	}

	/**
	 * @throws ServiceException 없으면 R001
	 */
	public BannerDto get(Long id, LocalDateTime now) {
		return BannerDto.from(require(id), now);
	}

	/**
	 * @throws ServiceException 칸이 올바르지 않으면 V001 (끝 일시, 이미지)
	 */
	public BannerDto create(BannerContent content, LocalDateTime now) {
		validate(content);
		return BannerDto.from(bannerWriter.save(new Banner(content)), now);
	}

	/**
	 * @throws ServiceException 없으면 R001, 칸이 올바르지 않으면 V001
	 */
	public BannerDto modify(Long id, BannerContent content, LocalDateTime now) {
		Banner banner = require(id);
		validate(content);
		banner.modify(content);
		return BannerDto.from(banner, now);
	}

	/**
	 * @throws ServiceException 없으면 R001
	 */
	public void delete(Long id, Long actorId) {
		require(id).delete(actorId);
	}

	/**
	 * 여러 배너를 한 번에 켜거나 끈다 (FR-008). 하나라도 없으면 아무것도 바꾸지 않는다 (Application의 트랜잭션이 롤백한다).
	 * 이미 그 값인 배너는 그대로 둔다.
	 *
	 * @throws ServiceException 없는 배너 R001
	 */
	public void setEnabled(Collection<Long> ids, boolean enabled) {
		requireAll(ids).forEach(banner -> banner.changeEnabled(enabled));
	}

	/**
	 * 여러 배너를 한 번에 지운다 (FR-008). 하나라도 없으면 아무것도 지우지 않는다.
	 *
	 * @return 지운 배너 ID (이미지를 풀 때 쓴다)
	 * @throws ServiceException 없는 배너 R001
	 */
	public List<Long> deleteAll(Collection<Long> ids, Long actorId) {
		List<Banner> targets = requireAll(ids);
		targets.forEach(banner -> banner.delete(actorId));
		return targets.stream().map(Banner::getId).toList();
	}

	private Banner require(Long id) {
		return bannerReader.findById(id).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}

	/** 보낸 ID가 모두 있어야 한다 (겹친 ID는 하나로 본다) */
	private List<Banner> requireAll(Collection<Long> ids) {
		Map<Long, Banner> banners = bannerReader.findAllByIdIn(ids).stream()
			.collect(Collectors.toMap(Banner::getId, Function.identity()));
		List<Banner> found = new ArrayList<>();
		for (Long id : new LinkedHashSet<>(ids)) {
			Banner banner = banners.get(id);
			if (banner == null) {
				throw new ServiceException(ErrorCode.NOT_FOUND, "없는 배너입니다: " + id);
			}
			found.add(banner);
		}
		return found;
	}

	private void validate(BannerContent content) {
		List<FieldError> errors = new ArrayList<>();
		if (content.endsAt() != null && !content.endsAt().isAfter(content.startsAt())) {
			errors.add(new FieldError("endsAt", "노출 끝 일시는 시작 일시보다 뒤여야 합니다."));
		}
		checkImage("wideImageUrl", content.wideImageUrl(), errors);
		if (content.narrowImageUrl() != null) {
			checkImage("narrowImageUrl", content.narrowImageUrl(), errors);
		}
		if (!errors.isEmpty()) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), errors);
		}
	}

	/** rich-text 이미지 주소이고, 저장된 이미지이고, GIF가 아니어야 한다 */
	private void checkImage(String field, String url, List<FieldError> errors) {
		if (!url.startsWith(HtmlSanitizer.IMAGE_PATH_PREFIX)) {
			errors.add(new FieldError(field, "올린 이미지의 주소가 아닙니다."));
			return;
		}
		String contentType;
		try {
			contentType = richTextImageService.load(url.substring(HtmlSanitizer.IMAGE_PATH_PREFIX.length()))
				.contentType();
		} catch (ServiceException e) {
			errors.add(new FieldError(field, "이미지를 찾을 수 없습니다. 다시 올려 주세요."));
			return;
		}
		if (GIF.equals(contentType)) {
			errors.add(new FieldError(field, "JPG, PNG, WebP 이미지만 쓸 수 있습니다."));
		}
	}
}
