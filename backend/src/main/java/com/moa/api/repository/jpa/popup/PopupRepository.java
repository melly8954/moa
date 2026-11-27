package com.moa.api.repository.jpa.popup;

import java.util.Collection;
import java.util.List;

import com.moa.api.entity.popup.Popup;
import com.moa.common.repository.SoftDeleteRepository;

public interface PopupRepository extends SoftDeleteRepository<Popup, Long> {

	List<Popup> findAllByIdIn(Collection<Long> ids);
}
