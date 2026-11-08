package com.dragonminez.common.wish;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

public final class WishFiles {

	private WishFiles() {}

	public static List<Path> migrate(Path oldDir, Path newDir) throws IOException {
		if (!Files.isDirectory(oldDir)) return List.of();
		Files.createDirectories(newDir);
		try (var files = Files.list(oldDir)) {
			List<Path> sources = files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
					.filter(path -> path.getFileName().toString().endsWith(".json"))
					.sorted()
					.toList();
			List<Path> copied = new java.util.ArrayList<>();
			for (Path source : sources) {
				Path target = newDir.resolve(source.getFileName());
				try {
					Files.copy(source, target);
					copied.add(target);
				} catch (java.nio.file.FileAlreadyExistsException ignored) {
					// Keep the server-wide wish configuration.
				}
			}
			return List.copyOf(copied);
		}
	}
}
