package com.moa.common.guard;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

/**
 * 시작 가드. 설정 파일을 읽은 직후, 빈(DB 연결, Flyway 포함)을 만들기 전에 돈다.
 *
 * <ol>
 *   <li>프로필이 local·dev·prod·test 중 정확히 하나인지 본다. 없으면 막는다 (기본 프로필이 없다)</li>
 *   <li>dev·prod면 GuardRule을 모두 돌린다. 위반을 모아 한 번에 보여 주고 시작을 실패시킨다</li>
 * </ol>
 *
 * 등록: META-INF/spring.factories (스프링 빈이 아니다).
 */
public class StartupGuard implements EnvironmentPostProcessor, Ordered {

	static final String LOCAL = "local";
	static final String DEV = "dev";
	static final String PROD = "prod";
	static final String TEST = "test";
	private static final Set<String> PROFILES = Set.of(LOCAL, DEV, PROD, TEST);

	private final Log log;

	public StartupGuard(DeferredLogFactory logFactory) {
		this.log = logFactory.getLog(StartupGuard.class);
	}

	/** 설정 파일(application*.yaml)을 읽은 뒤에 돈다 */
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String profile = profile(environment);
		if (LOCAL.equals(profile) || TEST.equals(profile)) {
			return;
		}
		GuardContext context = new GuardContext(profile, environment);
		rules().forEach(rule -> rule.check(context));
		context.warnings().forEach(warning -> log.warn("[시작 가드 " + profile + "] " + warning));
		if (!context.errors().isEmpty()) {
			// 실패하면 로그 시스템이 뜨기 전에 끝나 경고 로그가 나오지 않으므로 메시지에 함께 싣는다
			String warnings = context.warnings().isEmpty() ? ""
				: "\n경고:\n - " + String.join("\n - ", context.warnings());
			throw new IllegalStateException("시작할 수 없습니다 (" + profile + "):\n - "
				+ String.join("\n - ", context.errors()) + warnings);
		}
	}

	static String profile(ConfigurableEnvironment environment) {
		List<String> known = Arrays.stream(environment.getActiveProfiles()).filter(PROFILES::contains).toList();
		if (known.size() != 1) {
			throw new IllegalStateException("프로필을 하나 정해야 합니다: SPRING_PROFILES_ACTIVE=local|dev|prod (지금: "
				+ Arrays.toString(environment.getActiveProfiles()) + ")");
		}
		return known.get(0);
	}

	/** 루트 패키지(이 클래스의 두 단계 위) 아래 GuardRule 구현을 찾는다 */
	static List<GuardRule> rules() {
		String guardPackage = ClassUtils.getPackageName(StartupGuard.class);
		String rootPackage = guardPackage.substring(0, guardPackage.lastIndexOf(".common."));
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AssignableTypeFilter(GuardRule.class));
		return scanner.findCandidateComponents(rootPackage).stream()
			.map(BeanDefinition::getBeanClassName)
			.sorted()
			.map(StartupGuard::instantiate)
			.collect(Collectors.toList());
	}

	private static GuardRule instantiate(String className) {
		try {
			return (GuardRule)ClassUtils.forName(className, StartupGuard.class.getClassLoader())
				.getDeclaredConstructor()
				.newInstance();
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("시작 가드 규칙을 만들지 못했습니다: " + className, ex);
		}
	}
}
