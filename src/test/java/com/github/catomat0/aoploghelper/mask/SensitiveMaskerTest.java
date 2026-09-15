package com.github.catomat0.aoploghelper.mask;

import com.github.catomat0.aoploghelper.logging.mask.SensitiveMasker;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveMaskerTest {

    private final SensitiveMasker masker = new SensitiveMasker(
            List.of("password", "token", "authorization"));

    @Test
    void masksSensitiveMapEntries() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("username", "alice");
        input.put("password", "s3cret");
        input.put("accessToken", "xyz");

        @SuppressWarnings("unchecked")
        Map<String, Object> out = (Map<String, Object>) masker.mask(input);

        assertThat(out.get("username")).isEqualTo("alice");
        assertThat(out.get("password")).isEqualTo("***");
        assertThat(out.get("accessToken")).isEqualTo("***");
    }

    @Test
    void masksKeyValueStrings() {
        String rendered = (String) masker.mask("User(id=1, password=abc, name=alice)");
        assertThat(rendered).contains("password=***");
        assertThat(rendered).contains("name=alice");
    }

    @Test
    void masksJsonStrings() {
        String rendered = (String) masker.mask("{\"authorization\":\"Bearer xxx\",\"role\":\"admin\"}");
        assertThat(rendered).contains("\"authorization\":\"***\"");
        assertThat(rendered).contains("\"role\":\"admin\"");
    }

    @Test
    void leavesNonSensitivePrimitivesUntouched() {
        assertThat(masker.mask(42)).isEqualTo(42);
        assertThat(masker.mask(true)).isEqualTo(true);
        assertThat(masker.mask(null)).isNull();
    }

    @Test
    void masksEmailAddresses() {
        String s = (String) masker.mask("contact alice@example.com about it");
        assertThat(s).contains("a***@***.com");
        assertThat(s).doesNotContain("alice@example.com");
    }

    @Test
    void masksKoreanMobilePhone() {
        String s = (String) masker.mask("phone: 010-1234-5678, back: 01098765432");
        assertThat(s).contains("010-****-5678");
        assertThat(s).contains("010-****-5432");
    }

    @Test
    void masksCreditCard() {
        String s = (String) masker.mask("card 4111-1111-1111-1234 charged");
        assertThat(s).contains("****-****-****-1234");
    }

    @Test
    void masksKoreanResidentNumber() {
        String s = (String) masker.mask("RRN 901231-1234567 registered");
        assertThat(s).contains("******-*******");
        assertThat(s).doesNotContain("901231-1234567");
    }

    @Test
    void keepsNumericUserIdsUntouched() {
        // Plain user ids must not be masked
        assertThat(masker.mask("userId=42")).isEqualTo("userId=42");
        assertThat(masker.mask("[id=100, name=alice]")).isEqualTo("[id=100, name=alice]");
    }

    @Test
    void sanitizeForLogStripsControlChars() {
        String out = masker.sanitizeForLog("line1\r\nERROR fake\tadmin");
        assertThat(out).doesNotContain("\n").doesNotContain("\r").doesNotContain("\t");
        assertThat(out).contains("\\r\\n").contains("\\t");
    }

    @Test
    void pointyKeywordsAreEscaped() {
        SensitiveMasker withMetaChars = new SensitiveMasker(List.of(".*", "password"));
        String out = (String) withMetaChars.mask("password=abc, name=alice");
        assertThat(out).contains("password=***");
        // '.*' as a literal keyword should not match everything
        assertThat(out).contains("name=alice");
    }

    @Test
    void piiDisabledSwitchesOff() {
        SensitiveMasker off = new SensitiveMasker(List.of("password"), false);
        String out = (String) off.mask("email alice@example.com");
        assertThat(out).contains("alice@example.com");
    }
}
