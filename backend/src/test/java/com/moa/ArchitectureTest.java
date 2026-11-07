package com.moa;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import java.util.List;
import java.util.Optional;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.entity.HardDelete;
import com.moa.common.entity.SoftDeleteEntity;
import com.moa.common.exception.ServiceException;
import com.tngtech.archunit.core.domain.Dependency;
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
 * 레이어 규칙과 삭제 규칙을 강제한다.
 *
 * <pre>
 * Controller·Scheduler → Application → Service → Reader/Writer → Repository
 * </pre>
 *
 * 트랜잭션 경계는 Application이다. Service는 하나의 기능만 하고 다른 Service를 부르지 않는다. 여러 Service(다른 도메인 포함)는
 * Application이 한 트랜잭션으로 조합한다. ServiceException은 Service에서만 던진다.
 *
 * 도메인은 레이어 루트 바로 아래 패키지 이름이다 (예: api.service.account → account). Service는 자기 도메인의 Reader·Writer만,
 * Reader·Writer는 자기 도메인의 Repository만 부른다. 레이어 루트에 바로 둔 클래스는 도메인 검사에서 뺀다.
 */
@AnalyzeClasses(packages = "com.moa", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	private static final String CONTROLLER = "..api.controller..";
	private static final String SCHEDULER = "..api.scheduler..";
	private static final String APPLICATION = "..api.application..";
	private static final String SERVICE = "..api.service..";
	private static final String READER = "..api.repository.reader..";
	private static final String WRITER = "..api.repository.writer..";
	private static final String REPOSITORY = "..api.repository.jpa..";
	private static final List<String> READER_WRITER_ROOTS = List.of("repository.reader", "repository.writer");
	private static final List<String> REPOSITORY_ROOTS = List.of("repository.jpa");

	@ArchTest
	static final ArchRule layers = layeredArchitecture().consideringOnlyDependenciesInLayers()
		.withOptionalLayers(true)
		.layer("Controller").definedBy(CONTROLLER)
		.layer("Scheduler").definedBy(SCHEDULER)
		.layer("Application").definedBy(APPLICATION)
		.layer("Service").definedBy(SERVICE)
		.layer("ReaderWriter").definedBy(READER, WRITER)
		.layer("Repository").definedBy(REPOSITORY)
		.whereLayer("Controller").mayNotBeAccessedByAnyLayer()
		.whereLayer("Scheduler").mayNotBeAccessedByAnyLayer()
		.whereLayer("Application").mayOnlyBeAccessedByLayers("Controller", "Scheduler")
		.whereLayer("Service").mayOnlyBeAccessedByLayers("Application")
		.whereLayer("ReaderWriter").mayOnlyBeAccessedByLayers("Service")
		.whereLayer("Repository").mayOnlyBeAccessedByLayers("ReaderWriter");

	@ArchTest
	static final ArchRule servicesUseOwnDomainReaderWriter = classes().that().resideInAPackage(SERVICE)
		.should(useOnlyOwnDomain("service", READER_WRITER_ROOTS))
		.allowEmptyShould(true)
		.because("다른 도메인은 Application이 그 도메인의 Service로 조합한다. Reader·Writer를 직접 부르지 않는다");

	@ArchTest
	static final ArchRule servicesDoNotCallServices = classes().that().resideInAPackage(SERVICE)
		.should(notCallOtherServices())
		.allowEmptyShould(true)
		.because("Service는 하나의 기능만 한다. 여러 Service의 조합은 Application이 한다");

	@ArchTest
	static final ArchRule transactionsOnlyInApplication = noMethods().that()
		.areDeclaredInClassesThat().resideInAnyPackage(CONTROLLER, SERVICE, READER, WRITER)
		.should().beAnnotatedWith(Transactional.class)
		.orShould().beAnnotatedWith(jakarta.transaction.Transactional.class)
		.allowEmptyShould(true)
		.because("트랜잭션 경계는 Application이다. 유스케이스 하나가 한 트랜잭션으로 묶인다");

	@ArchTest
	static final ArchRule transactionalClassesOnlyInApplication = noClasses().that()
		.resideInAnyPackage(CONTROLLER, SERVICE, READER, WRITER)
		.should().beAnnotatedWith(Transactional.class)
		.orShould().beAnnotatedWith(jakarta.transaction.Transactional.class)
		.allowEmptyShould(true)
		.because("트랜잭션 경계는 Application이다. 유스케이스 하나가 한 트랜잭션으로 묶인다");

	@ArchTest
	static final ArchRule serviceExceptionOnlyInService = noClasses().that()
		.resideInAnyPackage(CONTROLLER, APPLICATION, READER, WRITER, REPOSITORY)
		.should().accessClassesThat().areAssignableTo(ServiceException.class)
		.allowEmptyShould(true)
		.because("업무 오류는 Service에서 판단해 던진다. Reader는 Optional·빈 목록을 돌려주고 Service가 R001 등으로 바꾼다");

	@ArchTest
	static final ArchRule readerWritersUseOwnDomainRepository = classes().that().resideInAnyPackage(READER, WRITER)
		.should(useOnlyOwnDomain(null, REPOSITORY_ROOTS))
		.allowEmptyShould(true)
		.because("Reader·Writer는 자기 도메인의 Repository만 부른다");

	@ArchTest
	static final ArchRule controllersDoNotUseEntities = noClasses().that().resideInAPackage(CONTROLLER)
		.should().dependOnClassesThat().resideInAPackage("..api.entity..")
		.allowEmptyShould(true)
		.because("엔터티를 API로 내보내지 않는다. Dto와 Response로 바꾼다");

	@ArchTest
	static final ArchRule entitiesDeclareDeletePolicy = classes().that().areAnnotatedWith(Entity.class)
		.should(declareDeletePolicy())
		.allowEmptyShould(true)
		.because("소프트 삭제가 기본이다. 하드 삭제는 @HardDelete(reason)로 이유를 남긴다");

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

	/**
	 * 출발 클래스의 도메인과 다른 도메인의 대상 레이어 클래스에 의존하면 위반이다.
	 *
	 * @param sourceRoot 출발 레이어 루트. null이면 Reader·Writer 루트 중 맞는 것을 쓴다
	 * @param targetRoots 검사할 대상 레이어 루트
	 */
	private static ArchCondition<JavaClass> useOnlyOwnDomain(String sourceRoot, List<String> targetRoots) {
		List<String> sourceRoots = sourceRoot == null ? READER_WRITER_ROOTS : List.of(sourceRoot);
		return new ArchCondition<>("자기 도메인의 " + String.join("·", targetRoots) + "만 부른다") {
			@Override
			public void check(JavaClass javaClass, ConditionEvents events) {
				Optional<String> source = domainOf(javaClass.getPackageName(), sourceRoots);
				if (source.isEmpty()) {
					return;
				}
				javaClass.getDirectDependenciesFromSelf().stream()
					.map(Dependency::getTargetClass)
					.map(JavaClass::getBaseComponentType)
					.distinct()
					.forEach(target -> domainOf(target.getPackageName(), targetRoots)
						.filter(domain -> !domain.equals(source.get()))
						.ifPresent(domain -> events.add(SimpleConditionEvent.violated(javaClass, javaClass.getName()
							+ "(" + source.get() + ")가 다른 도메인의 " + target.getName() + "(" + domain + ")를 부릅니다"))));
			}
		};
	}

	/**
	 * Service 패키지의 다른 Service(이름이 Service로 끝나는 클래스)에 의존하면 위반이다. 자기 자신과 같은 패키지의 값 클래스는 괜찮다.
	 */
	private static ArchCondition<JavaClass> notCallOtherServices() {
		return new ArchCondition<>("다른 Service를 부르지 않는다") {
			@Override
			public void check(JavaClass javaClass, ConditionEvents events) {
				javaClass.getDirectDependenciesFromSelf().stream()
					.map(Dependency::getTargetClass)
					.map(JavaClass::getBaseComponentType)
					.filter(target -> !target.equals(javaClass))
					.filter(target -> target.getPackageName().contains(".api.service"))
					.filter(target -> target.getSimpleName().endsWith("Service"))
					.distinct()
					.forEach(target -> events.add(SimpleConditionEvent.violated(javaClass, javaClass.getName()
						+ "가 다른 Service " + target.getName() + "를 부릅니다")));
			}
		};
	}

	/**
	 * api.<루트>.<도메인>... 패키지에서 도메인을 꺼낸다. 루트에 바로 있으면 비어 있다.
	 */
	static Optional<String> domainOf(String packageName, List<String> roots) {
		for (String root : roots) {
			String marker = ".api." + root + ".";
			int index = packageName.indexOf(marker);
			if (index >= 0) {
				String rest = packageName.substring(index + marker.length());
				int dot = rest.indexOf('.');
				return Optional.of(dot < 0 ? rest : rest.substring(0, dot));
			}
		}
		return Optional.empty();
	}

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
