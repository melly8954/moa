package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;
import com.moa.common.audit.SystemActor;
import com.moa.common.helper.richtext.RichTextImageCleaner;
import com.moa.common.helper.richtext.RichTextImageService;
import com.moa.support.AcceptanceTest;

/**
 * 본문 이미지 (선택 헬퍼 rich-text): 올리기, 보여 주기, 글과 잇기, 정리
 */
@TestPropertySource(properties = {"app.rich-text.image-dir=build/tmp/rich-text-images",
	"app.rich-text.image-max-size=1KB"})
class RichTextImageAcceptanceTest extends AcceptanceTest {

	private static final String IMAGES = "/api/v1/rich-text/images";
	private static final byte[] PNG = {(byte)0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D',
		'R'};

	@Autowired
	private RichTextImageService richTextImageService;

	@Autowired
	private RichTextImageCleaner richTextImageCleaner;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Test
	@DisplayName("본문 이미지: 올리면 주소를 받고, 그 주소는 로그인 없이 이미지 형식·nosniff·긴 캐시로 열린다")
	void uploadAndShow() {
		MvcTestResult uploaded = upload("a.png", PNG);
		assertThat(uploaded).hasStatus(201);
		String url = JsonPath.read(body(uploaded), "$.url");
		assertThat(url).startsWith("/api/v1/rich-text/images/");

		MvcTestResult shown = mvc.get().uri(url).exchange();
		assertThat(shown).hasStatus(200);
		assertThat(shown).hasContentType("image/png");
		assertThat(shown).hasHeader("X-Content-Type-Options", "nosniff");
		assertThat(shown.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("max-age=31536000");
		assertThat(mvc.get().uri(IMAGES + "/00000000-0000-0000-0000-000000000000")).hasStatus(404);
	}

	@Test
	@DisplayName("본문 이미지: 이름이 .png여도 내용이 이미지가 아니면, SVG면, 크기를 넘으면 V001. 올리기는 로그인 필요")
	void rejects() {
		assertThat(upload("a.png", "not image".getBytes())).hasStatus(400)
			.bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(upload("a.svg", "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>x</script></svg>".getBytes()))
			.hasStatus(400);
		byte[] large = new byte[2048];
		System.arraycopy(PNG, 0, large, 0, PNG.length);
		assertThat(upload("big.png", large)).hasStatus(400);
		assertThat(mvc.post().uri(IMAGES).multipart().file(new MockMultipartFile("file", "a.png", "image/png", PNG)))
			.hasStatus(401);
	}

	@Test
	@DisplayName("본문 이미지: 글에 쓰이면 남고, 글에서 빠지거나 글을 지우면 임시가 되어 보관 시간이 지나면 지워진다")
	void attachAndCleanup() {
		String used = id(upload("a.png", PNG));
		String dropped = id(upload("b.png", PNG));
		String never = id(upload("c.png", PNG));
		inTransaction(() -> richTextImageService.attach("NOTICE", 1L,
			"<p><img src=\"/api/v1/rich-text/images/" + used + "\"><img src=\"/api/v1/rich-text/images/" + dropped
				+ "\"></p>"));
		inTransaction(() -> richTextImageService.attach("NOTICE", 1L,
			"<p><img src=\"/api/v1/rich-text/images/" + used + "\"></p>"));

		jdbcTemplate.update("update rich_text_images set updated_at = now(6) - interval 2 day");
		SystemActor.run(richTextImageCleaner::deleteExpired);

		assertThat(mvc.get().uri(IMAGES + "/" + used)).hasStatus(200);
		assertThat(mvc.get().uri(IMAGES + "/" + dropped)).hasStatus(404);
		assertThat(mvc.get().uri(IMAGES + "/" + never)).hasStatus(404);

		inTransaction(() -> richTextImageService.detach("NOTICE", 1L));
		jdbcTemplate.update("update rich_text_images set updated_at = now(6) - interval 2 day");
		SystemActor.run(richTextImageCleaner::deleteExpired);
		assertThat(mvc.get().uri(IMAGES + "/" + used)).hasStatus(404);
	}

	private MvcTestResult upload(String name, byte[] content) {
		return mvc.post().uri(IMAGES).multipart()
			.file(new MockMultipartFile("file", name, "application/octet-stream", content))
			.header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER"))
			.exchange();
	}

	private void inTransaction(Runnable work) {
		SystemActor.run(() -> transactionTemplate.executeWithoutResult(status -> work.run()));
	}

	private String id(MvcTestResult result) {
		assertThat(result).hasStatus(201);
		return JsonPath.read(body(result), "$.id");
	}

	private String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString();
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
