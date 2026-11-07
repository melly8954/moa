package com.moa.support.arch.api.repository.writer.alpha;

import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;

public class ThrowingWriter {

	public void save() {
		throw new ServiceException(ErrorCode.NOT_FOUND);
	}
}
