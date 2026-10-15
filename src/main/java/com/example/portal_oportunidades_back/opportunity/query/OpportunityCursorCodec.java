package com.example.portal_oportunidades_back.opportunity.query;

import com.example.portal_oportunidades_back.exception.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class OpportunityCursorCodec {
    private static final int MAX_LENGTH = 1024;

    public String encode(OpportunityCursor cursor, OpportunityFilter filter) {
        String payload = String.join("\n", "1", cursor.createdAt().toString(),
                Long.toString(cursor.id()), fingerprint(filter));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public OpportunityCursor decode(String token, OpportunityFilter filter) {
        if (token == null) return null;
        if (token.isBlank() || token.length() > MAX_LENGTH) throw invalidCursor();
        try {
            String payload = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = payload.split("\n", -1);
            if (parts.length != 4 || !parts[0].equals("1")
                    || !parts[3].equals(fingerprint(filter))) throw invalidCursor();
            Instant createdAt = Instant.parse(parts[1]);
            long id = Long.parseLong(parts[2]);
            if (id < 1 || createdAt.getNano() % 1000 != 0
                    || createdAt.isBefore(Instant.parse("0001-01-01T00:00:00Z"))
                    || createdAt.isAfter(Instant.parse("9999-12-31T23:59:59.999999Z"))) {
                throw invalidCursor();
            }
            return new OpportunityCursor(createdAt, id);
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw invalidCursor();
        }
    }

    private String fingerprint(OpportunityFilter filter) {
        String title = filter.title() == null ? "" : filter.title();
        String canonical = title.length() + ":" + title + "\n"
                + (filter.modality() == null ? "" : filter.modality().name()) + "\n"
                + (filter.status() == null ? "" : filter.status().name()) + "\n"
                + (filter.recruiterId() == null ? "" : filter.recruiterId().toString());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private BadRequestException invalidCursor() {
        return new BadRequestException("Invalid cursor or cursor does not match the filters");
    }
}
