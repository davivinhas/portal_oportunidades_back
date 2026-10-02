package com.example.portal_oportunidades_back.opportunity.query;

import com.example.portal_oportunidades_back.exception.BadRequestException;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static com.example.portal_oportunidades_back.opportunity.OpportunityFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpportunityCursorCodecTest {
    private final OpportunityCursorCodec codec = new OpportunityCursorCodec();
    private final OpportunityFilter filter = new OpportunityFilter(
            " Java ", OpportunityModality.SCIENTIFIC_INITIATION, null, 7L);

    @Test
    void roundTripsAndAcceptsEquivalentNormalizedFilters() {
        String token = codec.encode(new OpportunityCursor(NOW, 123), filter);
        var decoded = codec.decode(token,
                new OpportunityFilter("java", OpportunityModality.SCIENTIFIC_INITIATION, null, 7L));
        assertThat(decoded).isEqualTo(new OpportunityCursor(NOW, 123));
        assertThat(token).doesNotContain("+", "/", "=");
    }

    @Test
    void rejectsChangedFilters() {
        String token = codec.encode(new OpportunityCursor(NOW, 123), filter);
        assertThatThrownBy(() -> codec.decode(token,
                new OpportunityFilter("other", OpportunityModality.SCIENTIFIC_INITIATION, null, 7L)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsMalformedOversizedAndUnsupportedTokens() {
        assertThat(codec.decode(null, filter)).isNull();
        for (String token : new String[]{"", "%%%", "a".repeat(1025)}) {
            assertThatThrownBy(() -> codec.decode(token, filter)).isInstanceOf(BadRequestException.class);
        }
        String valid = codec.encode(new OpportunityCursor(NOW, 123), filter);
        String payload = new String(Base64.getUrlDecoder().decode(valid), StandardCharsets.UTF_8);
        String unsupported = Base64.getUrlEncoder().withoutPadding().encodeToString(
                payload.replaceFirst("1\n", "2\n").getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> codec.decode(unsupported, filter)).isInstanceOf(BadRequestException.class);
    }
}
