package com.dragonminez.common.wish;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WishTextTest {

	@Test
	void translationKeys() {
		assertEquals("wish.dragonminez.shenron.senzu.name",
				WishText.key("dragonminez:shenron/senzu", "name"));
		assertEquals("wish.custom.revive.desc",
				WishText.key("custom:revive", "desc"));
		assertEquals("wish.example.dragon.wish.name",
				WishText.key("example:dragon/wish", "name"));
	}

	@Test
	void labelFallbacks() {
		assertEquals("Sementes dos Deuses",
				WishText.fallback("custom:shenron_senzu", "Sementes dos Deuses"));
		assertEquals("shenron senzu",
				WishText.fallback("custom:shenron_senzu", null));
		assertEquals("senzu",
				WishText.fallback("dragonminez:shenron/senzu", ""));
		assertEquals("", WishText.fallback(null, null));
	}

	@Test
	void invalidKeys() {
		assertThrows(IllegalArgumentException.class, () -> WishText.key("missing_namespace", "name"));
		assertThrows(IllegalArgumentException.class, () -> WishText.key(":bad", "name"));
		assertThrows(IllegalArgumentException.class, () -> WishText.key("custom:", "name"));
		assertThrows(IllegalArgumentException.class, () -> WishText.key("custom:valid", "other"));
	}
}
