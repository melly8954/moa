package com.moa.common.legal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.moa.common.terms.TermsProperties;

import tools.jackson.databind.json.JsonMapper;

/**
 * 공개 문서 문안 (specs/012-terms-privacy research R1·R6). 시작할 때 문안 파일을 한 번 읽어 들고 있다.
 *
 * <p>
 * 위치 아래 문서 폴더(terms, privacy)마다 버전 파일 한 쌍을 둔다: &lt;버전&gt;.json(version, effectiveDate, title)과
 * &lt;버전&gt;.html(장마다 &lt;section id&gt;&lt;h2&gt;). 배포 jar 안에서도 찾히게 리소스 패턴(classpath*:)으로 찾는다.
 * 문안이 규칙을 어기면 IllegalStateException으로 시작을 막는다 (FR-008).
 */
@Component
public class LegalCatalog {

	private static final Pattern VERSION = Pattern.compile("^[a-z0-9][a-z0-9.-]*$");
	private static final Pattern DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
	/** 본문에 넣지 않는 것: 스크립트, 틀, 스타일, 이벤트 속성, javascript: 주소 */
	private static final Pattern FORBIDDEN = Pattern
		.compile("(?i)<\\s*(script|iframe|style)|\\son[a-z]+\\s*=|javascript:");
	private static final JsonMapper JSON = JsonMapper.builder().build();
	private static final Comparator<LegalVersion> LATEST_FIRST = Comparator
		.comparing(LegalVersion::effectiveDate).reversed();

	private final Map<LegalDocument, List<LegalVersion>> versions;

	@Autowired
	public LegalCatalog(LegalProperties legalProperties, TermsProperties termsProperties) {
		this(read(new PathMatchingResourcePatternResolver(), legalProperties.location(),
			LocalDate.now(ZoneId.of(legalProperties.zone())), termsProperties.serviceVersion()));
	}

	private LegalCatalog(Map<LegalDocument, List<LegalVersion>> versions) {
		this.versions = versions;
	}

	/**
	 * 위치와 오늘을 정해 읽는다 (테스트).
	 *
	 * @param location 예: classpath:legal/
	 * @param today 시행된 버전이 있는지 볼 날
	 * @param serviceVersion 가입 때 동의받는 이용약관 버전 (app.terms.service-version)
	 * @throws IllegalStateException 문안이 규칙을 어기면
	 */
	public static LegalCatalog load(ResourcePatternResolver resolver, String location, LocalDate today,
		String serviceVersion) {
		return new LegalCatalog(read(resolver, location, today, serviceVersion));
	}

	/** 문서의 버전들. 시행일 최근순 */
	public List<LegalVersion> versions(LegalDocument document) {
		return versions.getOrDefault(document, List.of());
	}

	private static Map<LegalDocument, List<LegalVersion>> read(ResourcePatternResolver resolver, String location,
		LocalDate today, String serviceVersion) {
		String base = location.replaceFirst("^classpath\\*?:", "");
		if (!base.endsWith("/")) {
			base = base + "/";
		}
		Resource[] metas;
		try {
			metas = resolver.getResources("classpath*:" + base + "*/*.json");
		} catch (IOException e) {
			throw new UncheckedIOException("공개 문서 문안을 찾지 못했습니다: " + location, e);
		}
		Map<LegalDocument, List<LegalVersion>> result = new EnumMap<>(LegalDocument.class);
		for (Resource meta : metas) {
			LegalVersion version = readVersion(meta);
			result.computeIfAbsent(version.document(), key -> new ArrayList<>()).add(version);
		}
		for (LegalDocument document : LegalDocument.values()) {
			List<LegalVersion> list = result.computeIfAbsent(document, key -> new ArrayList<>());
			check(document, list, today);
			list.sort(LATEST_FIRST);
			result.put(document, List.copyOf(list));
		}
		boolean hasServiceVersion = result.get(LegalDocument.TERMS).stream()
			.anyMatch(version -> version.version().equals(serviceVersion));
		if (!hasServiceVersion) {
			throw fail("이용약관에 가입 동의 버전(app.terms.service-version=" + serviceVersion + ")이 없습니다");
		}
		return result;
	}

	private static LegalVersion readVersion(Resource meta) {
		String path = pathOf(meta);
		String[] parts = path.split("/");
		if (parts.length < 2) {
			throw fail("문서 폴더 안에 있지 않습니다: " + path);
		}
		String folder = parts[parts.length - 2];
		String fileName = parts[parts.length - 1];
		LegalDocument document = LegalDocument.fromKey(folder)
			.orElseThrow(() -> fail("모르는 문서 폴더입니다: " + folder + " (" + path + ")"));
		Meta values = readMeta(meta, path);
		String version = required(values.version(), "version", path);
		if (!fileName.equals(version + ".json")) {
			throw fail("파일 이름과 version이 다릅니다: " + path);
		}
		if (!VERSION.matcher(version).matches()) {
			throw fail("version은 영문 소문자·숫자·-·.만 씁니다: " + path);
		}
		LocalDate effectiveDate = parseDate(required(values.effectiveDate(), "effectiveDate", path), path);
		String title = required(values.title(), "title", path);
		String htmlPath = path.substring(0, path.length() - ".json".length()) + ".html";
		String html = readHtml(meta, version, htmlPath);
		return new LegalVersion(document, version, effectiveDate, title, sections(html, htmlPath));
	}

	private static Meta readMeta(Resource meta, String path) {
		try (InputStream in = meta.getInputStream()) {
			return JSON.readValue(in, Meta.class);
		} catch (IOException | RuntimeException e) {
			throw new IllegalStateException("공개 문서 문안 오류: 메타 파일을 읽지 못했습니다: " + path, e);
		}
	}

	private static String readHtml(Resource meta, String version, String htmlPath) {
		Resource html;
		try {
			html = meta.createRelative(version + ".html");
		} catch (IOException e) {
			throw new IllegalStateException("공개 문서 문안 오류: 본문 파일을 찾지 못했습니다: " + htmlPath, e);
		}
		if (!html.exists()) {
			throw fail("짝 본문 파일(.html)이 없습니다: " + htmlPath);
		}
		try (InputStream in = html.getInputStream()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("공개 문서 문안 오류: 본문 파일을 읽지 못했습니다: " + htmlPath, e);
		}
	}

	/** 본문 맨 위는 &lt;section id&gt;&lt;h2&gt;로만 이루어진다. 장 밖에 글을 두지 않는다 */
	private static List<LegalSection> sections(String html, String path) {
		if (FORBIDDEN.matcher(html).find()) {
			throw fail("본문에 스크립트·틀·스타일·이벤트 속성·javascript: 주소를 넣지 않습니다: " + path);
		}
		Document parsed = Jsoup.parseBodyFragment(html);
		parsed.outputSettings().prettyPrint(false);
		List<LegalSection> sections = new ArrayList<>();
		Set<String> ids = new HashSet<>();
		for (Node node : parsed.body().childNodes()) {
			if (node instanceof Comment || node instanceof TextNode text && text.isBlank()) {
				continue;
			}
			if (!(node instanceof Element section) || !section.normalName().equals("section")) {
				throw fail("장 밖에 글이 있습니다. 본문 맨 위에는 <section>만 둡니다: " + path);
			}
			String id = section.id();
			if (id.isBlank()) {
				throw fail("id가 없는 장이 있습니다: " + path);
			}
			if (!ids.add(id)) {
				throw fail("장 id가 겹칩니다: " + id + " (" + path + ")");
			}
			Element heading = section.firstElementChild();
			if (heading == null || !heading.normalName().equals("h2") || heading.text().isBlank()) {
				throw fail("장(" + id + ")의 첫 요소가 제목(h2)이 아닙니다: " + path);
			}
			Element body = section.clone();
			body.firstElementChild().remove();
			sections.add(new LegalSection(id, heading.text(), body.html().trim()));
		}
		if (sections.isEmpty()) {
			throw fail("장이 없습니다: " + path);
		}
		return List.copyOf(sections);
	}

	private static void check(LegalDocument document, List<LegalVersion> list, LocalDate today) {
		Set<String> names = new HashSet<>();
		Set<LocalDate> dates = new HashSet<>();
		for (LegalVersion version : list) {
			if (!names.add(version.version())) {
				throw fail(document.key() + " 버전 이름이 겹칩니다: " + version.version());
			}
			if (!dates.add(version.effectiveDate())) {
				throw fail(document.key() + " 시행일이 겹칩니다: " + version.effectiveDate());
			}
		}
		if (list.stream().noneMatch(version -> !version.effectiveDate().isAfter(today))) {
			throw fail(document.key() + "에 시행된 버전(시행일 " + today + " 이전)이 없습니다");
		}
	}

	private static String pathOf(Resource resource) {
		try {
			return resource.getURI().toString();
		} catch (IOException e) {
			throw new UncheckedIOException("공개 문서 문안 주소를 읽지 못했습니다: " + resource, e);
		}
	}

	private static String required(@Nullable String value, String name, String path) {
		if (value == null || value.isBlank()) {
			throw fail(name + "가 없습니다: " + path);
		}
		return value;
	}

	private static LocalDate parseDate(String value, String path) {
		if (!DATE.matcher(value).matches()) {
			throw fail("effectiveDate는 YYYY-MM-DD 꼴입니다: " + path);
		}
		try {
			return LocalDate.parse(value);
		} catch (DateTimeParseException e) {
			throw new IllegalStateException("공개 문서 문안 오류: 없는 날짜입니다: " + value + " (" + path + ")", e);
		}
	}

	private static IllegalStateException fail(String message) {
		return new IllegalStateException("공개 문서 문안 오류: " + message);
	}

	/** 메타 파일 (&lt;버전&gt;.json) */
	private record Meta(@Nullable String version, @Nullable String effectiveDate, @Nullable String title) {
	}
}
