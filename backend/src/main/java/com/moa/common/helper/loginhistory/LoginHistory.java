package com.moa.common.helper.loginhistory;

import com.moa.common.entity.BaseTimeEntity;
import com.moa.common.entity.HardDelete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 접속 이력 (선택 헬퍼 login-history).
 */
@Getter
@Entity
@Table(name = "login_histories")
@HardDelete(reason = "보관 기간(app.login-history.retention-days)이 지난 이력은 지운다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginHistory extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "login_id", nullable = false, length = 50)
	private String loginId;

	@Column(nullable = false)
	private boolean success;

	@Column(name = "failure_code", length = 10)
	private String failureCode;

	@Column(length = 45)
	private String ip;

	@Column(name = "user_agent", length = 500)
	private String userAgent;

	@Column(name = "device_type", nullable = false, length = 10)
	private String deviceType;

	@Column(nullable = false, length = 30)
	private String os;

	@Column(nullable = false, length = 30)
	private String browser;

	public LoginHistory(Long userId, String loginId, boolean success, String failureCode, String ip,
		String userAgent) {
		DeviceInfo device = DeviceInfo.parse(userAgent);
		this.userId = userId;
		this.loginId = truncate(loginId, 50);
		this.success = success;
		this.failureCode = failureCode;
		this.ip = ip;
		this.userAgent = truncate(userAgent, 500);
		this.deviceType = device.deviceType();
		this.os = device.os();
		this.browser = device.browser();
	}

	private static String truncate(String value, int max) {
		return value == null || value.length() <= max ? value : value.substring(0, max);
	}
}
