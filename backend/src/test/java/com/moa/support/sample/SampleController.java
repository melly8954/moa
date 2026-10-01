package com.moa.support.sample;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.common.audit.CurrentActor;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.security.AuthPrincipal;

import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/** 골격 검증용 API. 테스트에서만 쓴다 (계층 규칙 대상 아님) */
@RestController
@RequestMapping("/test/items")
@RequiredArgsConstructor
public class SampleController {

	private final SampleItemRepository repository;
	private final CurrentActor currentActor;

	@PostMapping
	public ResponseEntity<SampleItemResponse> create(@Valid @RequestBody SampleItemRequest request) {
		return ResponseEntity.ok(SampleItemResponse.from(repository.save(new SampleItem(request.name()))));
	}

	@PermitAll
	@PostMapping("/public")
	public ResponseEntity<SampleItemResponse> createPublic(@Valid @RequestBody SampleItemRequest request) {
		return ResponseEntity.ok(SampleItemResponse.from(repository.save(new SampleItem(request.name()))));
	}

	@GetMapping("/{id}")
	public ResponseEntity<SampleItemResponse> get(@PathVariable Long id) {
		return ResponseEntity.ok(repository.findById(id)
			.map(SampleItemResponse::from)
			.orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND)));
	}

	@Transactional
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		SampleItem item = repository.findById(id).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		item.delete(currentActor.id());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	public ResponseEntity<Long> me(@AuthenticationPrincipal AuthPrincipal principal) {
		return ResponseEntity.ok(principal.userId());
	}

	public record SampleItemRequest(@NotBlank @Size(max = 100) String name) {
	}

	public record SampleItemResponse(Long id, String name, Long createdBy, Long updatedBy) {

		static SampleItemResponse from(SampleItem item) {
			return new SampleItemResponse(item.getId(), item.getName(), item.getCreatedBy(), item.getUpdatedBy());
		}
	}
}
