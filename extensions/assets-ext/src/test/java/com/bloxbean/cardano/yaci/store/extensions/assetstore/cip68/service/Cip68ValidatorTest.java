package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.FieldCategory;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What CIP-68 requires of each label is reported as validation errors, never as a rejection: the token is indexed and
 * served either way (ADR 0006).
 */
@DisplayName("Cip68Validator")
class Cip68ValidatorTest {

    private static ParsedCip68Datum datum(String name, String description, String image) {
        return new ParsedCip68Datum(null, description, null, name, null, null, 1L, image, null, null);
    }

    private static List<String> fields(List<ValidationError> errors) {
        return errors.stream().map(ValidationError::fieldName).toList();
    }

    @Test
    void fungibleTokenNeedsNameAndDescription() {
        assertThat(Cip68Validator.validate(datum("name", "desc", null), Cip68Constants.LABEL_FT)).isEmpty();
    }

    @Test
    void fungibleTokenWithoutDescriptionHasAMandatoryError() {
        // 333: CIP-68 declares `description : bounded_bytes`, without a `?`
        List<ValidationError> errors = Cip68Validator.validate(datum("name", null, null), Cip68Constants.LABEL_FT);

        assertThat(errors).singleElement().satisfies(e -> {
            assertThat(e.cip()).isEqualTo("CIP-68");
            assertThat(e.subStandard()).isEqualTo("333");
            assertThat(e.fieldName()).isEqualTo("description");
            assertThat(e.fieldCategory()).isEqualTo(FieldCategory.MANDATORY);
            assertThat(e.code()).isEqualTo("missing_value");
            assertThat(e.error()).isEqualTo("description field is missing a mandatory value");
        });
    }

    @Test
    void nftAndRftWithoutDescriptionHaveNoError() {
        // 222 and 444: CIP-68 declares `? description : bounded_bytes`
        assertThat(Cip68Validator.validate(datum("name", null, "ipfs://Qm"), Cip68Constants.LABEL_NFT)).isEmpty();
        assertThat(Cip68Validator.validate(datum("name", null, "ipfs://Qm"), Cip68Constants.LABEL_RFT)).isEmpty();
    }

    @Test
    void nftAndRftWithoutImageHaveAMandatoryErrorForTheirLabel() {
        assertThat(Cip68Validator.validate(datum("name", "desc", null), Cip68Constants.LABEL_NFT))
                .singleElement().satisfies(e -> {
                    assertThat(e.fieldName()).isEqualTo("image");
                    assertThat(e.subStandard()).isEqualTo("222");
                    assertThat(e.fieldCategory()).isEqualTo(FieldCategory.MANDATORY);
                });
        assertThat(Cip68Validator.validate(datum("name", "desc", null), Cip68Constants.LABEL_RFT))
                .singleElement().satisfies(e -> assertThat(e.subStandard()).isEqualTo("444"));
    }

    @Test
    void emptyOrBlankImageIsMissing() {
        assertThat(fields(Cip68Validator.validate(datum("name", "desc", ""), Cip68Constants.LABEL_NFT))).containsExactly("image");
        assertThat(fields(Cip68Validator.validate(datum("name", "desc", "  "), Cip68Constants.LABEL_RFT))).containsExactly("image");
    }

    @Test
    void fungibleTokenNeedsNoImage() {
        // 333 has `logo`, which is optional
        assertThat(Cip68Validator.validate(datum("name", "desc", null), Cip68Constants.LABEL_FT)).isEmpty();
    }

    @Test
    void imageSchemesCip68AllowHaveNoError() {
        for (String image : new String[]{"https://example.com/a.png", "ipfs://Qm", "ar://abc", "data:image/png;base64,AAAA",
                "IPFS://Qm", "HTTPS://example.com/a.png"}) {
            assertThat(Cip68Validator.validate(datum("name", null, image), Cip68Constants.LABEL_NFT)).as(image).isEmpty();
            assertThat(Cip68Validator.validate(datum("name", null, image), Cip68Constants.LABEL_RFT)).as(image).isEmpty();
        }
    }

    @Test
    void imageWithAnotherSchemeHasAMandatoryErrorThatQuotesIt() {
        for (String image : new String[]{"iagon://675816de7e3fc1.26175981_Wine", "http://example.com/a.png", "ftp://x/y",
                "image IPFS here", "Qm123", "ipfs:", "ipfs"}) {
            assertThat(Cip68Validator.validate(datum("name", "desc", image), Cip68Constants.LABEL_NFT))
                    .as(image).singleElement().satisfies(e -> {
                        assertThat(e.code()).isEqualTo("invalid_uri_scheme");
                        assertThat(e.fieldCategory()).isEqualTo(FieldCategory.MANDATORY);
                        assertThat(e.error()).contains("not a URI").contains("https, ipfs, ar, data");
                    });
        }
    }

    @Test
    void aLongInvalidImageIsAbbreviatedInTheError() {
        List<ValidationError> errors = Cip68Validator.validate(datum("name", "desc", "iagon://" + "x".repeat(500)),
                Cip68Constants.LABEL_NFT);

        assertThat(errors).singleElement().satisfies(e -> assertThat(e.error()).contains("iagon://").hasSizeLessThan(250));
    }

    @Test
    void nameIsRequiredForEveryLabelAndTheErrorNamesTheLabel() {
        for (int label : new int[]{Cip68Constants.LABEL_NFT, Cip68Constants.LABEL_FT, Cip68Constants.LABEL_RFT}) {
            assertThat(Cip68Validator.validate(datum(null, "desc", "ipfs://Qm"), label))
                    .as("label %d without name", label)
                    .singleElement().satisfies(e -> {
                        assertThat(e.fieldName()).isEqualTo("name");
                        assertThat(e.subStandard()).isEqualTo(String.valueOf(label));
                        assertThat(e.fieldCategory()).isEqualTo(FieldCategory.MANDATORY);
                    });
        }
    }

    @Test
    void aNameOfTheWrongTypeIsNotReportedAsMissingToo() {
        ValidationError wrongType = ValidationError.mandatory("CIP-68", null, "name", "wrong_type", "name is not a byte string, left out");
        ParsedCip68Datum parsed = new ParsedCip68Datum(null, "desc", null, null, null, null, 1L, null, null, null, List.of(wrongType));

        assertThat(Cip68Validator.validate(parsed, Cip68Constants.LABEL_FT)).isEmpty();
    }

    private static ParsedCip68Datum ft(String logo) {
        return new ParsedCip68Datum(null, "desc", logo, "name", null, null, 1L, null, null, null);
    }

    @Test
    void aMissingBlankOrAllowedLogoHasNothingToReport() {
        for (String logo : new String[]{null, "", "  ", "https://example.com/l.png", "ipfs://Qm", "ar://tx", "data:image/png;base64,AAAA", "IPFS://Qm"}) {
            assertThat(Cip68Validator.validateLogo(ft(logo))).as(String.valueOf(logo)).isNull();
        }
    }

    @Test
    void aLogoWithAnotherSchemeIsAnOptionalErrorAndTheDatumIsStillValid() {
        for (String logo : new String[]{"iagon://6911e6dd275fee62fb8917ba", "http://example.com/l.png", "iVBORw0KGgoAAAANSUhEUgAA", "logo here"}) {
            assertThat(Cip68Validator.validate(ft(logo), Cip68Constants.LABEL_FT)).as(logo).isEmpty();
            assertThat(Cip68Validator.validateLogo(ft(logo))).as(logo).satisfies(e -> {
                assertThat(e.fieldName()).isEqualTo("logo");
                assertThat(e.fieldCategory()).isEqualTo(FieldCategory.OPTIONAL);
                assertThat(e.code()).isEqualTo("invalid_uri_scheme");
                assertThat(e.error()).contains("not a URI").contains("https, ipfs, ar, data");
            });
        }
    }
}
