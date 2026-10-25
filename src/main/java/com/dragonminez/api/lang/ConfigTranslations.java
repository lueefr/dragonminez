package com.dragonminez.api.lang;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Config names are overlaid on vanilla translations; no resource pack is generated. */
public final class ConfigTranslations {
    public static final ConfigTranslations INSTANCE = new ConfigTranslations();
    private volatile Map<String, Map<String, String>> local = Map.of();
    private volatile Map<String, Map<String, String>> remote = Map.of();
    private volatile long revision;

    public synchronized void reload(Path languageDirectory) throws IOException {
        Files.createDirectories(languageDirectory);
        Map<String, Map<String, String>> next = new TreeMap<>();
        try (var files = Files.list(languageDirectory)) {
            for (Path file : files.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json")).sorted().toList()) {
                String locale = file.getFileName().toString().replaceFirst("\\.json$", "");
                requireLocale(locale);
                next.put(locale, parse(Files.readString(file, StandardCharsets.UTF_8)));
            }
        }
        local = Map.copyOf(next);
        revision++;
    }
    public static String requireLocale(String locale) {
        if (locale == null || !locale.matches("[a-z]{2,3}(?:_[a-z0-9]{2,8})?")) throw new IllegalArgumentException("Invalid language code: " + locale);
        return locale;
    }
    public static Map<String, String> parse(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) throw new IllegalArgumentException("Translation file must be a JSON object");
        Map<String, String> values = new TreeMap<>();
        root.getAsJsonObject().entrySet().forEach(entry -> {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Translation must be a string: " + entry.getKey());
            }
            values.put(entry.getKey(), entry.getValue().getAsString());
        });
        return Map.copyOf(values);
    }
    public synchronized void replaceRemote(Map<String, Map<String, String>> translations) {
        Map<String, Map<String, String>> next = new TreeMap<>();
        translations.forEach((locale, values) -> next.put(requireLocale(locale), Map.copyOf(values)));
        remote = Map.copyOf(next);
        revision++;
    }
    public void clearRemote() { replaceRemote(Map.of()); }
    public long revision() { return revision; }
    public Map<String, Map<String, String>> localSnapshot() { return local; }

    /** Null tells the language hook to continue with Minecraft's normal lookup. */
    public String find(String locale, String key) {
        String value = lookup(remote, locale, key);
        if (value == null) value = lookup(local, locale, key);
        if (value == null) value = lookup(remote, "en_us", key);
        if (value == null) value = lookup(local, "en_us", key);
        return value;
    }
    public String translate(String locale, String key) {
        String value = find(locale, key);
        if(value!=null)return value;
        value=bundled(locale).get(key);
        if(value==null)value=bundled("en_us").get(key);
        return value==null?key:value;
    }
    private static final Map<String,Map<String,String>> BUNDLED=new java.util.concurrent.ConcurrentHashMap<>();
    private static Map<String,String> bundled(String locale){return BUNDLED.computeIfAbsent(locale,id->{
        try(var stream=ConfigTranslations.class.getResourceAsStream("/assets/dragonminez/lang/"+id+".json")){
            return stream==null?Map.of():parse(new String(stream.readAllBytes(),StandardCharsets.UTF_8));
        }catch(IOException|RuntimeException ex){return Map.of();}
    });}
    /** Preserve translation keys across the wire; the receiving client selects its own locale. */
    public static net.minecraft.network.chat.MutableComponent text(String key) {
        return key==null||key.isEmpty()?net.minecraft.network.chat.Component.empty():net.minecraft.network.chat.Component.translatable(key);
    }
    private static String lookup(Map<String, Map<String, String>> translations, String locale, String key) {
        return translations.getOrDefault(locale, Map.of()).get(key);
    }
}
