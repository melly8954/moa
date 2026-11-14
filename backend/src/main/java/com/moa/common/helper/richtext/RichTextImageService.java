package com.moa.common.helper.richtext;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;

import lombok.extern.slf4j.Slf4j;

/**
 * 본문 이미지 올리기·보여 주기·글과 잇기 (선택 헬퍼 rich-text). 트랜잭션은 부르는 Application이 연다.
 *
 * <pre>{@code
 * // 글을 저장·고칠 때 (같은 트랜잭션): 본문에 쓰인 이미지는 USED, 빠진 이미지는 다시 TEMP
 * richTextImageService.attach("NOTICE", notice.getId(), notice.getContent());
 * // 글을 지울 때
 * richTextImageService.detach("NOTICE", notice.getId());
 * }</pre>
 */
@Slf4j
@Service
public class RichTextImageService {

	private static final Pattern PUBLIC_ID = Pattern
		.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
	private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd");

	private final RichTextImageRepository repository;
	private final RichTextProperties properties;
	private final Path root;

	public RichTextImageService(RichTextImageRepository repository, RichTextProperties properties) {
		this.repository = repository;
		this.properties = properties;
		this.root = Path.of(properties.imageDir()).toAbsolutePath().normalize();
	}

	/**
	 * 임시(TEMP)로 저장한다. 형식은 파일 내용으로 판별한다 (png, jpg, gif, webp).
	 *
	 * @throws ServiceException 빈 파일, 받지 않는 형식(SVG 등), 크기 초과면 V001
	 */
	public UploadedImage upload(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, "이미지 파일이 비어 있습니다.");
		}
		if (file.getSize() > properties.imageMaxSize().toBytes()) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, "이미지는 " + properties.imageMaxSize() + "까지 올릴 수 있습니다.");
		}
		ImageType type = ImageType.detect(header(file))
			.orElseThrow(() -> new ServiceException(ErrorCode.INVALID_INPUT, "png, jpg, gif, webp 이미지만 올릴 수 있습니다."));
		String publicId = UUID.randomUUID().toString();
		String path = LocalDate.now().format(DATE_DIR) + "/" + publicId + "." + type.extension();
		Path target = root.resolve(path);
		try (InputStream input = file.getInputStream()) {
			Files.createDirectories(target.getParent());
			Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ex) {
			throw new UncheckedIOException("본문 이미지를 저장하지 못했습니다: " + path, ex);
		}
		repository.save(new RichTextImage(publicId, path, type.contentType(), file.getSize()));
		return new UploadedImage(publicId, HtmlSanitizer.IMAGE_PATH_PREFIX + publicId);
	}

	/**
	 * @throws ServiceException 없거나 ID 형식이 틀리면 R001
	 */
	public StoredImage load(String publicId) {
		RichTextImage image = PUBLIC_ID.matcher(publicId).matches() ? repository.findByPublicId(publicId).orElse(null)
			: null;
		Path file = image == null ? null : root.resolve(image.getPath()).normalize();
		if (file == null || !file.startsWith(root) || !Files.isRegularFile(file)) {
			throw new ServiceException(ErrorCode.NOT_FOUND);
		}
		return new StoredImage(new FileSystemResource(file), image.getContentType());
	}

	/**
	 * 글 본문(HTML)에 쓰인 이미지를 이 글의 것(USED)으로 하고, 전에 이 글에 쓰였지만 빠진 이미지는 다시 임시(TEMP)로 돌린다.
	 * 다른 글이 쓰고 있는 이미지는 건드리지 않는다.
	 */
	public void attach(String ownerType, Long ownerId, String html) {
		Set<String> used = HtmlSanitizer.imageIds(html);
		for (RichTextImage image : repository.findAllByOwnerTypeAndOwnerId(ownerType, ownerId)) {
			if (!used.contains(image.getPublicId())) {
				image.release();
			}
		}
		for (RichTextImage image : repository.findAllByPublicIdIn(used)) {
			if (image.usableBy(ownerType, ownerId)) {
				image.use(ownerType, ownerId);
			}
		}
	}

	/** 글을 지울 때. 이 글의 이미지를 모두 임시로 돌린다 (보관 시간이 지나면 지워진다) */
	public void detach(String ownerType, Long ownerId) {
		repository.findAllByOwnerTypeAndOwnerId(ownerType, ownerId).forEach(RichTextImage::release);
	}

	/** 보관 시간이 지난 임시 이미지를 파일과 함께 지운다. 지운 수 */
	public int deleteExpiredTemps() {
		List<RichTextImage> expired = repository.findAllByStatusAndUpdatedAtBefore(RichTextImage.Status.TEMP,
			LocalDateTime.now().minus(properties.tempRetention()));
		Set<Long> deleted = new HashSet<>();
		for (RichTextImage image : expired) {
			try {
				Files.deleteIfExists(root.resolve(image.getPath()).normalize());
				deleted.add(image.getId());
			} catch (IOException ex) {
				log.warn("본문 이미지 파일을 지우지 못했습니다. 다음 정리 때 다시 합니다: {}", image.getPath(), ex);
			}
		}
		if (!deleted.isEmpty()) {
			repository.deleteAllByIdInBatch(deleted);
		}
		return deleted.size();
	}

	private static byte[] header(MultipartFile file) {
		try (InputStream input = file.getInputStream()) {
			return input.readNBytes(ImageType.HEADER_LENGTH);
		} catch (IOException ex) {
			throw new UncheckedIOException("이미지를 읽지 못했습니다.", ex);
		}
	}
}
