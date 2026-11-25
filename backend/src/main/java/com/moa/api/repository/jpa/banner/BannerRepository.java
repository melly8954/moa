package com.moa.api.repository.jpa.banner;

import java.util.Collection;
import java.util.List;

import com.moa.api.entity.banner.Banner;
import com.moa.common.repository.SoftDeleteRepository;

public interface BannerRepository extends SoftDeleteRepository<Banner, Long> {

	List<Banner> findAllByIdIn(Collection<Long> ids);
}
