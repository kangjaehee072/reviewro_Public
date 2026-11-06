package com.reviewro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SpringBootApplication
@EnableScheduling // 톡방 SSE 연결 유지용 heartbeat
public class ReviewRoApplication {

	public static void main(String[] args) {
		ApplicationHome home = new ApplicationHome(ReviewRoApplication.class);
		File source = home.getSource();
		File appHome = (source != null && source.isFile()) ? home.getDir() : new File(System.getProperty("user.dir"));
		System.setProperty("app.home", appHome.getPath().replace("\\","/"));

		createEnvIfMissing(appHome.toPath());
		printEnvSources(appHome.toPath());

		SpringApplication.run(ReviewRoApplication.class, args);
	}

	// .env 는 git 에 없어서 새로 받은 서버에는 없다.
	// 앱 홈(jar 로 띄우면 jar 가 있는 폴더, IDE 면 프로젝트 루트)에 없으면,
	// 앱 홈에 빈 틀을 만들고 어디에 만들었는지 알린다.
	// 이미 있으면, 틀(env.example)에 새로 생긴 항목 중 빠진 것을 빈 값으로 덧붙인다 (값이 있는 줄은 건드리지 않는다)
	private static void createEnvIfMissing(Path appHome) {
		// 읽는 .env 는 앱 홈의 것 하나뿐이다 (예전처럼 두 군데를 읽으면 빈 값이 진짜 값을 덮는 일이 생긴다)
		Path target = appHome.resolve(".env");
		if (Files.exists(target)) {
			addMissingKeys(target);
			return;
		}
		try (InputStream template = ReviewRoApplication.class.getResourceAsStream("/env.example")) {
			if (template == null) {
				return;
			}
			Files.copy(template, target);
			System.out.println("[ReviewRo] .env 가 없어서 빈 틀을 만들었습니다: " + target.toAbsolutePath()
					+ "  카카오 키 등을 채운 뒤 서버를 다시 시작하세요.");
		} catch (IOException e) {
			System.err.println("[ReviewRo] .env 를 만들지 못했습니다 (" + target.toAbsolutePath() + "): " + e.getMessage());
		}
	}

	// 틀에 있는데 .env 에 없는 키를 그 위 설명 주석과 함께 끝에 덧붙인다
	private static void addMissingKeys(Path env) {
		try (InputStream template = ReviewRoApplication.class.getResourceAsStream("/env.example")) {
			if (template == null) {
				return;
			}
			Set<String> present = new HashSet<>();
			for (String line : Files.readAllLines(env, StandardCharsets.UTF_8)) {
				String key = keyOf(line);
				if (key != null) {
					present.add(key);
				}
			}

			StringBuilder append = new StringBuilder();
			List<String> missing = new ArrayList<>();
			List<String> comments = new ArrayList<>();
			for (String line : new String(template.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
				String key = keyOf(line);
				if (key == null) {
					// 빈 줄이면 설명 묶음이 끊긴다
					if (line.isBlank()) comments.clear(); else comments.add(line);
					continue;
				}
				if (!present.contains(key)) {
					comments.forEach(c -> append.append(c).append(System.lineSeparator()));
					append.append(key).append('=').append(System.lineSeparator());
					missing.add(key);
				}
				comments.clear();
			}
			if (missing.isEmpty()) {
				return;
			}
			String head = System.lineSeparator() + "# (서버가 새 설정 항목을 덧붙였습니다)" + System.lineSeparator();
			Files.writeString(env, head + append, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
			System.out.println("[ReviewRo] .env 에 빠진 항목을 빈 값으로 추가했습니다 " + missing + ": " + env.toAbsolutePath());
		} catch (IOException e) {
			System.err.println("[ReviewRo] .env 에 새 항목을 추가하지 못했습니다 (" + env.toAbsolutePath() + "): " + e.getMessage());
		}
	}

	// 어느 .env 를 읽는지 시작할 때 알린다. 띄운 폴더에 다른 .env 가 있으면 읽지 않는다고 같이 알린다
	private static void printEnvSources(Path appHome) {
		Path home = appHome.resolve(".env").toAbsolutePath().normalize();
		Path work = Path.of(System.getProperty("user.dir")).resolve(".env").toAbsolutePath().normalize();
		System.out.println("[ReviewRo] 읽는 .env: " + home);
		if (!work.equals(home) && Files.exists(work)) {
			System.out.println("[ReviewRo] 주의: " + work + " 는 읽지 않습니다. 값은 위 파일에 넣으세요.");
		}
	}

	// "KEY=값" 줄이면 KEY, 주석·빈 줄이면 null
	private static String keyOf(String line) {
		String trimmed = line.trim();
		int eq = trimmed.indexOf('=');
		if (trimmed.isEmpty() || trimmed.startsWith("#") || eq <= 0) {
			return null;
		}
		return trimmed.substring(0, eq).trim();
	}

}
