package com.moa.common.helper.richtext;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RichTextImageRepository extends JpaRepository<RichTextImage, Long> {

	Optional<RichTextImage> findByPublicId(String publicId);

	List<RichTextImage> findAllByPublicIdIn(Collection<String> publicIds);

	List<RichTextImage> findAllByOwnerTypeAndOwnerId(String ownerType, Long ownerId);

	List<RichTextImage> findAllByStatusAndUpdatedAtBefore(RichTextImage.Status status, LocalDateTime before);
}
