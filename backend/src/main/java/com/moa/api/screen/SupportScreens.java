package com.moa.api.screen;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.moa.api.permission.FaqPermissions;
import com.moa.api.permission.InquiryPermissions;

/**
 * 고객센터의 연결 화면 (specs/017-customer-center FR-002, FR-014). 사용자 FAQ는 로그인 없이 연다.
 * 내 문의·문의 쓰기·문의 상세는 메뉴로 열지 않으므로 등록하지 않는다
 */
@Component
@Order(65)
public class SupportScreens implements ScreenSource {

	@Override
	public List<ScreenDefinition> screens() {
		return List.of(
			ScreenDefinition.admin("ADMIN_FAQS", "FAQ 관리", "/admin/faqs", FaqPermissions.MANAGE),
			ScreenDefinition.admin("ADMIN_INQUIRIES", "문의 관리", "/admin/inquiries", InquiryPermissions.MANAGE),
			ScreenDefinition.user("SUPPORT", "고객센터", "/support", false));
	}
}
