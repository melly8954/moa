package com.moa.api.repository.writer.banner;

import org.springframework.stereotype.Component;

import com.moa.api.entity.banner.Banner;
import com.moa.api.repository.jpa.banner.BannerRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BannerWriter {

	private final BannerRepository bannerRepository;

	public Banner save(Banner banner) {
		return bannerRepository.save(banner);
	}
}
