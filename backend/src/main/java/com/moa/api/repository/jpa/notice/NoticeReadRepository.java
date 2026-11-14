package com.moa.api.repository.jpa.notice;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moa.api.entity.notice.NoticeRead;

public interface NoticeReadRepository extends JpaRepository<NoticeRead, Long> {
}
