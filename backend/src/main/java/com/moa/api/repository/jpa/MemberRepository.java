package com.moa.api.repository.jpa;

import com.moa.api.entity.Member;
import com.moa.common.repository.SoftDeleteRepository;

public interface MemberRepository extends SoftDeleteRepository<Member, Long> {
}
