package com.moa;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;

import com.moa.common.entity.HardDelete;
import com.moa.common.entity.SoftDeleteEntity;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import jakarta.persistence.Entity;

/**
 * 레이어 규칙과 삭제 규칙을 강제한다 (harness-psw 4.7, 5.1). 규칙의 이유와 예외는 docs/design/conventions.md에 둔다.
 *
 * <pre>
 * Controller → Application → Service → Reader/Writer → Repository
 * </pre>
 */
@AnalyzeClasses(packages = "com.moa", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	private static final String CONTROLLER = "..api.controller..";
	private static final String APPLICATION = "..api.application..";
	private static final String SERVICE = "..api.service..";
	private static final String READER = "..api.repository.reader..";
	private static final String WRITER = "..api.repository.writer..";
	private static final String REPOSITORY = "..api.repository.jpa..";

	@ArchTest
	static final ArchRule layers = layeredArchitecture().consideringOnlyDependenciesInLayers()
		.withOptionalLayers(true)
		.layer("Controller").definedBy(CONTROLLER)
		.layer("Application").definedBy(APPLICATION)
		.layer("Service").definedBy(SERVICE)
		.layer("ReaderWriter").definedBy(READER, WRITER)
		.layer("Repository").definedBy(REPOSITORY)
		.whereLayer("Controller").mayNotBeAccessedByAnyLayer()
		.whereLayer("Application").mayOnlyBeAccessedByLayers("Controller")
		.whereLayer("Service").mayOnlyBeAccessedByLayers("Application")
		.whereLayer("ReaderWriter").mayOnlyBeAccessedByLayers("Service")
		.whereLayer("Repository").mayOnlyBeAccessedByLayers("ReaderWriter");

	@ArchTest
	static final ArchRule controllersDoNotUseEntities = noClasses().that().resideInAPackage(CONTROLLER)
		.should().dependOnClassesThat().resideInAPackage("..api.entity..")
		.allowEmptyShould(true)
		.because("엔터티를 API로 내보내지 않는다. Dto와 Response로 바꾼다");

	@ArchTest
	static final ArchRule entitiesDeclareDeletePolicy = classes().that().areAnnotatedWith(Entity.class)
		.should(declareDeletePolicy())
		.allowEmptyShould(true)
		.because("소프트 삭제가 기본이다. 하드 삭제는 @HardDelete(reason)로 이유를 남긴다 (harness-psw 4.7)");

	@ArchTest
	static final ArchRule softDeleteEntitiesHideDeletedRows = classes().that().areAnnotatedWith(Entity.class)
		.and().areAssignableTo(SoftDeleteEntity.class)
		.should().beAnnotatedWith(SQLRestriction.class)
		.allowEmptyShould(true)
		.because("삭제된 행은 조회에서 빠져야 한다. @SQLRestriction(SoftDeleteEntity.NOT_DELETED)을 붙인다");

	@ArchTest
	static final ArchRule softDeleteRepositoriesCannotDelete = classes().that().areInterfaces()
		.and().areAssignableTo(Repository.class)
		.should(notAllowPhysicalDeleteOfSoftDeleteEntities())
		.allowEmptyShould(true)
		.because("소프트 삭제 엔터티의 리포지토리는 SoftDeleteRepository를 상속한다. delete로 물리 삭제할 수 없게 한다");

	private static ArchCondition<JavaClass> declareDeletePolicy() {
		return new ArchCondition<>("SoftDeleteEntity를 상속하거나 @HardDelete를 붙인다") {
			@Override
			public void check(JavaClass javaClass, ConditionEvents events) {
				boolean soft = javaClass.isAssignableTo(SoftDeleteEntity.class);
				boolean hard = javaClass.isAnnotatedWith(HardDelete.class);
				if (soft == hard) {
					events.add(SimpleConditionEvent.violated(javaClass, javaClass.getName()
						+ (soft ? ": 소프트 삭제 엔터티에 @HardDelete가 붙었습니다" : ": 삭제 방식이 없습니다")));
				}
			}
		};
	}

	private static ArchCondition<JavaClass> notAllowPhysicalDeleteOfSoftDeleteEntities() {
		return new ArchCondition<>("소프트 삭제 엔터티에 delete 메서드를 열지 않는다") {
			@Override
			public void check(JavaClass javaClass, ConditionEvents events) {
				Class<?> type = javaClass.reflect();
				Class<?> domainType = ResolvableType.forClass(type).as(Repository.class).getGeneric(0).resolve();
				boolean softDelete = domainType != null && SoftDeleteEntity.class.isAssignableFrom(domainType);
				if (softDelete && CrudRepository.class.isAssignableFrom(type)) {
					events.add(SimpleConditionEvent.violated(javaClass, javaClass.getName()
						+ ": 소프트 삭제 엔터티의 리포지토리가 CrudRepository(JpaRepository)를 상속합니다"));
				}
			}
		};
	}
}
