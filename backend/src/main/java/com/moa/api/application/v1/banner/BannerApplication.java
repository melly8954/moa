package com.moa.api.application.v1.banner;

import java.time.LocalDateTime;
import java.util.Collection;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.dto.banner.BannerContent;
import com.moa.api.dto.banner.BannerDto;
import com.moa.api.dto.marketing.ExposureStatusFilter;
import com.moa.api.entity.banner.Banner;
import com.moa.api.service.banner.BannerService;
import com.moa.common.audit.CurrentActor;
import com.moa.common.helper.richtext.RichTextImageService;

import lombok.RequiredArgsConstructor;

/**
 * 배너 관리 (specs/016-banner-popup). 이미지는 같은 트랜잭션에서 rich-text 헬퍼에 잇는다 (research R1).
 * 헬퍼의 attach가 HTML만 받으므로 두 이미지 주소로 짧은 img 조각을 만든다. 빠진 이미지는 임시로 돌아가 정리된다.
 */
@Component
@RequiredArgsConstructor
public class BannerApplication {

	private final BannerService bannerService;
	private final RichTextImageService richTextImageService;
	private final CurrentActor currentActor;

	@Transactional(readOnly = true)
	public Page<BannerDto> getAdminPage(@Nullable String keyword, @Nullable ExposureStatusFilter status,
		Pageable pageable) {
		return bannerService.getAdminPage(keyword, status, LocalDateTime.now(), pageable);
	}

	@Transactional(readOnly = true)
	public BannerDto get(Long id) {
		return bannerService.get(id, LocalDateTime.now());
	}

	@Transactional
	public BannerDto create(BannerContent content) {
		BannerDto banner = bannerService.create(content, LocalDateTime.now());
		attachImages(banner);
		return banner;
	}

	@Transactional
	public BannerDto modify(Long id, BannerContent content) {
		BannerDto banner = bannerService.modify(id, content, LocalDateTime.now());
		attachImages(banner);
		return banner;
	}

	@Transactional
	public void delete(Long id) {
		bannerService.delete(id, currentActor.id());
		richTextImageService.detach(Banner.OWNER_TYPE, id);
	}

	@Transactional
	public void setEnabled(Collection<Long> ids, boolean enabled) {
		bannerService.setEnabled(ids, enabled);
	}

	@Transactional
	public void deleteAll(Collection<Long> ids) {
		bannerService.deleteAll(ids, currentActor.id())
			.forEach(id -> richTextImageService.detach(Banner.OWNER_TYPE, id));
	}

	private void attachImages(BannerDto banner) {
		StringBuilder html = new StringBuilder("<img src=\"").append(banner.wideImageUrl()).append("\">");
		if (banner.narrowImageUrl() != null) {
			html.append("<img src=\"").append(banner.narrowImageUrl()).append("\">");
		}
		richTextImageService.attach(Banner.OWNER_TYPE, banner.id(), html.toString());
	}
}
