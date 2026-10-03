package com.moa.api.repository.jpa;

import com.moa.api.entity.MemberSocialAccount;
import com.moa.common.repository.SoftDeleteRepository;

public interface MemberSocialAccountRepository extends SoftDeleteRepository<MemberSocialAccount, Long> {
}
