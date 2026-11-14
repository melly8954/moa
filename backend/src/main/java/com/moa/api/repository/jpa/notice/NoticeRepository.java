package com.moa.api.repository.jpa.notice;

import com.moa.api.entity.notice.Notice;
import com.moa.common.repository.SoftDeleteRepository;

public interface NoticeRepository extends SoftDeleteRepository<Notice, Long> {
}
