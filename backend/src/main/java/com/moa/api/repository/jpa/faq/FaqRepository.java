package com.moa.api.repository.jpa.faq;

import java.util.Collection;
import java.util.List;

import com.moa.api.entity.faq.Faq;
import com.moa.common.repository.SoftDeleteRepository;

public interface FaqRepository extends SoftDeleteRepository<Faq, Long> {

	List<Faq> findAllByIdIn(Collection<Long> ids);
}
