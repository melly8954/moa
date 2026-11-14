package com.moa.support;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpServer;

/**
 * 웹 푸시를 받아 두는 가짜 푸시 서버 (specs/007-notifications research R11). 브라우저 구독의 endpoint를 이 서버 주소로
 * 저장하면, 서버가 보낸 웹 푸시 요청이 여기에 쌓인다. 암호화된 본문은 풀지 않고 요청 경로와 헤더만 본다.
 */
public final class FakePushServer implements AutoCloseable {

	private final HttpServer server;
	private final BlockingQueue<String> requests = new LinkedBlockingQueue<>();
	private volatile int status = 201;

	public FakePushServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath() + " "
				+ exchange.getRequestHeaders().getFirst("Content-Encoding"));
			exchange.getRequestBody().readAllBytes();
			exchange.sendResponseHeaders(status, -1);
			exchange.close();
		});
		server.start();
	}

	/** 구독 endpoint로 쓸 주소. path로 브라우저를 구분한다 */
	public String endpoint(String path) {
		return "http://127.0.0.1:" + server.getAddress().getPort() + "/" + path;
	}

	/** 다음 요청부터 돌려줄 상태 (만료 흉내는 410) */
	public void respondWith(int status) {
		this.status = status;
	}

	/** 요청 하나를 기다린다. 5초 안에 오지 않으면 null */
	public String next() throws InterruptedException {
		return requests.poll(5, TimeUnit.SECONDS);
	}

	/** 일정 시간 동안 요청이 오지 않았나 */
	public boolean quiet() throws InterruptedException {
		return requests.poll(RealtimeAcceptanceTest.QUIET_MILLIS, TimeUnit.MILLISECONDS) == null;
	}

	@Override
	public void close() {
		server.stop(0);
	}
}
