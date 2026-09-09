package vn.nguongocso.farm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProductFeedbackLookupCodeGeneratorTest {

    private final ProductFeedbackLookupCodeGenerator generator = new ProductFeedbackLookupCodeGenerator();

    @Test
    void generate_shouldCreateExpectedPublicFormatAndSha256Hash() {
        ProductFeedbackLookupCodeGenerator.GeneratedLookupCode generated = generator.generate();

        assertThat(generated.displayValue()).matches("PA-(?:[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{4}-){3}"
                + "[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{4}");
        assertThat(generated.hash()).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(generator.hash(generated.displayValue())).isEqualTo(generated.hash());
    }

    @Test
    void hash_shouldAcceptLowercaseCodeWithoutDashes() {
        assertThat(generator.hash("pa7k2m9q4xh8np3r5t"))
                .isEqualTo(generator.hash("PA-7K2M-9Q4X-H8NP-3R5T"));
    }

    @Test
    void hash_shouldRejectMalformedCode() {
        assertThatThrownBy(() -> generator.hash("PA-INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
