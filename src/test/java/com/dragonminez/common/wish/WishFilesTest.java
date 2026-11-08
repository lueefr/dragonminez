package com.dragonminez.common.wish;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WishFilesTest {

	@TempDir Path root;

	@Test
	void migrateWishes() throws Exception {
		Path oldDir = root.resolve("world/dragonminez/wishes");
		Path newDir = root.resolve("config/dragonminez/wishes");
		Files.createDirectories(oldDir);
		Files.writeString(oldDir.resolve("shenron.json"), "[{\"type\":\"rewards\"}]");
		Files.writeString(oldDir.resolve("porunga.json"), "{\"wishes\":[]}");
		Files.writeString(oldDir.resolve("porunga_daima.json"), "{\"wishes\":[]}");
		Files.writeString(oldDir.resolve("custom.json"), "{\"wishes\":[]}");
		Files.writeString(oldDir.resolve("notes.txt"), "leave alone");

		assertEquals(4, WishFiles.migrate(oldDir, newDir).size());
		for (String name : new String[]{"shenron.json", "porunga.json", "porunga_daima.json", "custom.json"}) {
			assertEquals(Files.readString(oldDir.resolve(name)), Files.readString(newDir.resolve(name)));
		}
		assertFalse(Files.exists(newDir.resolve("notes.txt")));
	}

	@Test
	void preserveCustom() throws Exception {
		Path oldDir = root.resolve("world/dragonminez/wishes");
		Path newDir = root.resolve("config/dragonminez/wishes");
		Files.createDirectories(oldDir);
		Files.createDirectories(newDir);
		Files.writeString(oldDir.resolve("shenron.json"), "legacy");
		Files.writeString(newDir.resolve("shenron.json"), "server custom");

		assertTrue(WishFiles.migrate(oldDir, newDir).isEmpty());
		assertEquals("server custom", Files.readString(newDir.resolve("shenron.json")));
		assertEquals("legacy", Files.readString(oldDir.resolve("shenron.json")));
	}

	@Test
	void repeatMigration() throws Exception {
		Path oldDir = root.resolve("world/dragonminez/wishes");
		Path newDir = root.resolve("config/dragonminez/wishes");
		Files.createDirectories(oldDir);
		Files.writeString(oldDir.resolve("porunga.json"), "legacy");

		assertEquals(1, WishFiles.migrate(oldDir, newDir).size());
		Files.writeString(newDir.resolve("porunga.json"), "edited");
		assertTrue(WishFiles.migrate(oldDir, newDir).isEmpty());
		assertEquals("edited", Files.readString(newDir.resolve("porunga.json")));
	}

	@Test
	void missingWorldFolder() throws Exception {
		Path oldDir = root.resolve("world/dragonminez/wishes");
		Path newDir = root.resolve("config/dragonminez/wishes");

		assertTrue(WishFiles.migrate(oldDir, newDir).isEmpty());
		assertFalse(Files.exists(newDir));
	}
}
