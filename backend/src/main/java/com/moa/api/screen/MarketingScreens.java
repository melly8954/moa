package com.moa.api.screen;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.moa.api.permission.BannerPermissions;
import com.moa.api.permission.PopupPermissions;

/** 마케팅의 연결 화면 (specs/016-banner-popup FR-002). 이번에는 관리 화면만 있다 (FR-011) */
@Component
@Order(70)
public class MarketingScreens implements ScreenSource {

	@Override
	public List<ScreenDefinition> screens() {
		return List.of(
			ScreenDefinition.admin("ADMIN_BANNERS", "배너 관리", "/admin/banners", BannerPermissions.MANAGE),
			ScreenDefinition.admin("ADMIN_POPUPS", "팝업 관리", "/admin/popups", PopupPermissions.MANAGE));
	}
}
