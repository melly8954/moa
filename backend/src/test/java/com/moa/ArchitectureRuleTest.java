package com.moa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.EvaluationResult;

/**
 * ArchitectureTest의 도메인·트랜잭션·예외 규칙이 위반을 실제로 잡는지 표본 클래스(support.arch)로 확인한다.
 * 골격 main 코드에는 Reader·Writer가 없어 ArchitectureTest만으로는 규칙이 비어 있어도 통과한다.
 */
class ArchitectureRuleTest {

	private static final JavaClasses SAMPLES = new ClassFileImporter().importPackages("com.moa.support.arch");

	@Test
	@DisplayName("Service가 다른 도메인의 Writer를 부르면 위반이고, 같은 도메인과 도메인 없는 클래스는 통과한다")
	void serviceCrossDomain() {
		EvaluationResult result = ArchitectureTest.servicesUseOwnDomainReaderWriter.evaluate(SAMPLES);

		assertThat(result.getFailureReport().getDetails())
			.singleElement()
			.asString()
			.contains("CrossDomainService(alpha)", "BetaWriter(beta)");
	}

	@Test
	@DisplayName("Writer가 다른 도메인의 Repository를 부르면 위반이다")
	void readerWriterCrossDomain() {
		EvaluationResult result = ArchitectureTest.readerWritersUseOwnDomainRepository.evaluate(SAMPLES);

		assertThat(result.getFailureReport().getDetails())
			.singleElement()
			.asString()
			.contains("CrossDomainWriter(alpha)", "BetaRepository(beta)");
	}

	@Test
	@DisplayName("Service가 다른 Service를 부르면 위반이다")
	void serviceCallsService() {
		EvaluationResult result = ArchitectureTest.servicesDoNotCallServices.evaluate(SAMPLES);

		assertThat(result.getFailureReport().getDetails())
			.singleElement()
			.asString()
			.contains("ChainedService", "SameDomainService");
	}

	@Test
	@DisplayName("Application이 아닌 레이어에 @Transactional을 붙이면 위반이다")
	void transactionOutsideApplication() {
		EvaluationResult result = ArchitectureTest.transactionsOnlyInApplication.evaluate(SAMPLES);

		assertThat(result.getFailureReport().getDetails())
			.singleElement()
			.asString()
			.contains("TransactionalService.run()");
	}

	@Test
	@DisplayName("Service가 아닌 레이어에서 ServiceException을 던지면 위반이다")
	void serviceExceptionOutsideService() {
		EvaluationResult result = ArchitectureTest.serviceExceptionOnlyInService.evaluate(SAMPLES);

		assertThat(result.getFailureReport().getDetails())
			.isNotEmpty()
			.allSatisfy(detail -> assertThat(detail).contains("ThrowingWriter"));
	}

	@Test
	@DisplayName("도메인은 레이어 루트 바로 아래 패키지 이름이다")
	void domainOf() {
		List<String> roots = List.of("repository.reader", "repository.writer");

		assertThat(ArchitectureTest.domainOf("a.b.api.repository.reader.account.sub", roots)).hasValue("account");
		assertThat(ArchitectureTest.domainOf("a.b.api.repository.writer.audit", roots)).hasValue("audit");
		assertThat(ArchitectureTest.domainOf("a.b.api.repository.reader", roots)).isEqualTo(Optional.empty());
		assertThat(ArchitectureTest.domainOf("a.b.api.service.account", roots)).isEmpty();
	}
}
