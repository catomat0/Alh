package com.github.catomat0.aoploghelper.logging.mask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort masker used when logging method arguments, return values and exception messages.
 * <p>
 * Two independent layers:
 * <ul>
 *   <li><b>Keyword layer</b> — {@link Map} entries and {@code key=value} / {@code "key":"value"}
 *       string pairs whose key contains any configured keyword are replaced with {@code "***"}.</li>
 *   <li><b>PII layer</b> — regex-based masking for e-mail, phone numbers, credit cards and
 *       Korean resident-registration numbers, applied to any string content.</li>
 * </ul>
 * On top of those, {@link #sanitizeForLog(String)} also strips control characters
 * ({@code \n}, {@code \r}, {@code \t}, ESC) to defeat log-line injection.
 * <p>
 * This is NOT a security boundary — do not rely on it to sanitize data leaving the process.
 */
public class SensitiveMasker {

    private static final String MASK = "***";

    private final Set<String> keywords;
    private final Pattern kvPattern;
    private final Pattern jsonPattern;
    private final boolean piiEnabled;
    private final List<PiiRule> piiRules;

    public SensitiveMasker(Collection<String> keywords) {
        this(keywords, true);
    }

    public SensitiveMasker(Collection<String> keywords, boolean piiEnabled) {
        this.keywords = new HashSet<>();
        for (String k : keywords) {
            if (k != null && !k.isBlank()) {
                this.keywords.add(k.toLowerCase(Locale.ROOT));
            }
        }
        String joined = this.keywords.stream().map(Pattern::quote).reduce((a, b) -> a + "|" + b).orElse("");
        this.kvPattern = joined.isEmpty()
                ? null
                : Pattern.compile("(?i)\\b(" + joined + ")\\s*=\\s*([^,\\s)\\]}]+)");
        this.jsonPattern = joined.isEmpty()
                ? null
                : Pattern.compile("(?i)\"(" + joined + ")\"\\s*:\\s*\"([^\"]*)\"");
        this.piiEnabled = piiEnabled;
        this.piiRules = piiEnabled ? defaultPiiRules() : List.of();
    }

    public Object mask(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence cs) {
            return maskString(cs.toString());
        }
        if (value instanceof Map<?, ?> map) {
            return maskMap(map);
        }
        if (value instanceof Collection<?> collection) {
            return maskCollection(collection);
        }
        if (value.getClass().isArray()) {
            return maskString(String.valueOf(value));
        }
        return value;
    }

    /**
     * Runs full masking and then strips control characters so the result is safe to embed in a
     * single log line. Use this on any string that mixes user-controlled content into the log.
     */
    public String sanitizeForLog(String input) {
        if (input == null) {
            return "";
        }
        return stripControlChars(maskString(input));
    }

    public List<Object> maskArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return List.of();
        }
        return java.util.Arrays.stream(args).map(this::mask).toList();
    }

    private Object maskCollection(Collection<?> collection) {
        return collection.stream().map(this::mask).toList();
    }

    private Object maskMap(Map<?, ?> map) {
        Map<Object, Object> out = new LinkedHashMap<>(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            if (key != null && isSensitiveKey(key.toString())) {
                out.put(key, MASK);
            } else {
                out.put(key, mask(entry.getValue()));
            }
        }
        return out;
    }

    private String maskString(String input) {
        String result = input;
        if (kvPattern != null) {
            Matcher m = kvPattern.matcher(result);
            result = m.replaceAll(match -> match.group(1) + "=" + MASK);
        }
        if (jsonPattern != null) {
            Matcher m = jsonPattern.matcher(result);
            result = m.replaceAll(match -> "\"" + match.group(1) + "\":\"" + MASK + "\"");
        }
        for (PiiRule rule : piiRules) {
            Matcher m = rule.pattern.matcher(result);
            result = m.replaceAll(match -> rule.replacement.apply(match));
        }
        return result;
    }

    private boolean isSensitiveKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        for (String kw : keywords) {
            if (lower.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private static String stripControlChars(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n') out.append("\\n");
            else if (c == '\r') out.append("\\r");
            else if (c == '\t') out.append("\\t");
            else if (c == 0x1B) out.append("\\e");
            else if (c < 0x20 || c == 0x7F) out.append('?');
            else out.append(c);
        }
        return out.toString();
    }

    boolean isPiiEnabled() {
        return piiEnabled;
    }

    private static List<PiiRule> defaultPiiRules() {
        List<PiiRule> rules = new ArrayList<>();

        // Email — keep first char and TLD: alice@example.com -> a***@***.com
        rules.add(new PiiRule(
                Pattern.compile("([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*@[A-Za-z0-9.-]+\\.([A-Za-z]{2,})"),
                m -> m.group(1) + "***@***." + m.group(2)
        ));

        // Korean resident registration number (주민등록번호) — full mask
        rules.add(new PiiRule(
                Pattern.compile("\\b\\d{6}-?[1-4]\\d{6}\\b"),
                m -> "******-*******"
        ));

        // Credit card (13–19 digits, optional separators) — keep last 4
        rules.add(new PiiRule(
                Pattern.compile("\\b(?:\\d[ -]?){12,18}\\d\\b"),
                m -> {
                    String digits = m.group().replaceAll("[ -]", "");
                    if (digits.length() < 13 || digits.length() > 19) return m.group();
                    String tail = digits.substring(digits.length() - 4);
                    return "****-****-****-" + tail;
                }
        ));

        // Korean mobile number — 010-1234-5678 / 01012345678
        rules.add(new PiiRule(
                Pattern.compile("\\b01[016789][ -]?\\d{3,4}[ -]?\\d{4}\\b"),
                m -> {
                    String digits = m.group().replaceAll("[ -]", "");
                    return digits.substring(0, 3) + "-****-" + digits.substring(digits.length() - 4);
                }
        ));

        return rules;
    }

    private record PiiRule(Pattern pattern, Function<MatchResult, String> replacement) {
    }
}
