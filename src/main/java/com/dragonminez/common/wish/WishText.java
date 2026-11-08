package com.dragonminez.common.wish;

public final class WishText {

	private WishText() {}

	public static String key(String id, String field) {
		int separator = id == null ? -1 : id.indexOf(':');
		if (separator < 1 || separator == id.length() - 1) {
			throw new IllegalArgumentException("Wish id must have a namespace and path");
		}
		if (!"name".equals(field) && !"desc".equals(field)) {
			throw new IllegalArgumentException("Unknown wish translation field: " + field);
		}
		return "wish." + id.substring(0, separator) + "."
				+ id.substring(separator + 1).replace('/', '.') + "." + field;
	}

	public static String fallback(String id, String text) {
		if (text != null && !text.isBlank()) return text;
		if (id == null || id.isBlank()) return "";
		String path = id.substring(id.indexOf(':') + 1);
		return path.substring(path.lastIndexOf('/') + 1).replace('_', ' ');
	}
}
