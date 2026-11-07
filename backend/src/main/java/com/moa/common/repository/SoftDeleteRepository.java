package com.moa.common.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import com.moa.common.entity.SoftDeleteEntity;

/**
 * 소프트 삭제 엔터티의 리포지토리 기반. JpaRepository와 달리 delete 메서드가 없다.
 * 삭제는 엔터티의 delete(actorId)로 한다.
 */
@NoRepositoryBean
public interface SoftDeleteRepository<T extends SoftDeleteEntity, I> extends Repository<T, I> {

	<S extends T> S save(S entity);

	<S extends T> List<S> saveAll(Iterable<S> entities);

	Optional<T> findById(I id);

	boolean existsById(I id);

	List<T> findAll(Sort sort);

	Page<T> findAll(Pageable pageable);

	long count();

	void flush();
}
