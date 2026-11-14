package com.moa.api.controller.v1.notification;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.notification.NotificationApplication;
import com.moa.api.controller.v1.notification.request.PushSubscriptionRequest;
import com.moa.api.controller.v1.notification.response.NotificationResponse;
import com.moa.api.controller.v1.notification.response.PushPublicKeyResponse;
import com.moa.api.controller.v1.notification.response.UnreadCountResponse;
import com.moa.common.response.PageResponse;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * 내 알림함과 브라우저 알림 구독 (specs/007-notifications). 로그인한 사용자 누구나, 늘 자기 것만.
 */
@Tag(name = "알림 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class ApiNotificationController {

	private final NotificationApplication notificationApplication;

	@Operation(summary = "내 알림 목록", description = "최신순 페이징. 벨 펼침은 size=10")
	@GetMapping
	public ResponseEntity<PageResponse<NotificationResponse>> getPage(@CurrentUser AuthPrincipal user,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ResponseEntity.ok(PageResponse.from(
			notificationApplication.getPage(user.userId(), PageRequest.of(page, size)), NotificationResponse::from));
	}

	@Operation(summary = "안 읽은 알림 수")
	@GetMapping("/unread-count")
	public ResponseEntity<UnreadCountResponse> unreadCount(@CurrentUser AuthPrincipal user) {
		return ResponseEntity.ok(new UnreadCountResponse(notificationApplication.countUnread(user.userId())));
	}

	@Operation(summary = "알림 읽음", description = "이미 읽었으면 그대로. 내 알림이 아니거나 없으면 R001")
	@PutMapping("/{id}/read")
	public ResponseEntity<Void> read(@CurrentUser AuthPrincipal user, @PathVariable Long id) {
		notificationApplication.read(user.userId(), id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "모두 읽음")
	@PutMapping("/read-all")
	public ResponseEntity<Void> readAll(@CurrentUser AuthPrincipal user) {
		notificationApplication.readAll(user.userId());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "브라우저 알림 공개 키", description = "웹 푸시를 끈 서버면 publicKey가 없다")
	@GetMapping("/push/public-key")
	public ResponseEntity<PushPublicKeyResponse> pushPublicKey() {
		return ResponseEntity.ok(new PushPublicKeyResponse(notificationApplication.pushPublicKey()));
	}

	@Operation(summary = "이 브라우저 구독 저장", description = "같은 endpoint면 내 것으로 바꾼다")
	@PostMapping("/push-subscriptions")
	public ResponseEntity<Void> subscribe(@CurrentUser AuthPrincipal user,
		@Valid @RequestBody PushSubscriptionRequest request) {
		notificationApplication.subscribe(user.userId(), request.endpoint(), request.keys().p256dh(),
			request.keys().auth());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "이 브라우저 구독 지우기", description = "끄기와 로그아웃 전에 부른다. 없으면 그대로")
	@DeleteMapping("/push-subscriptions")
	public ResponseEntity<Void> unsubscribe(@CurrentUser AuthPrincipal user, @RequestParam String endpoint) {
		notificationApplication.unsubscribe(user.userId(), endpoint);
		return ResponseEntity.noContent().build();
	}
}
