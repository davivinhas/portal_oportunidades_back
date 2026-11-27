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
        String rank = cursor.rank() == null ? "" : Double.toString(cursor.rank());
        String payload = String.join("\n", "2", rank, cursor.createdAt().toString(),
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
            if (parts.length != 5 || !parts[0].equals("2")
                    || !parts[4].equals(fingerprint(filter))) throw invalidCursor();
            Double rank = parts[1].isEmpty() ? null : Double.valueOf(parts[1]);
            Instant createdAt = Instant.parse(parts[2]);
            long id = Long.parseLong(parts[3]);
            if (id < 1 || createdAt.getNano() % 1000 != 0
                    || rank != null && (!Double.isFinite(rank) || rank < 0)
                    || createdAt.isBefore(Instant.parse("0001-01-01T00:00:00Z"))
                    || createdAt.isAfter(Instant.parse("9999-12-31T23:59:59.999999Z"))) {
                throw invalidCursor();
            }
            return new OpportunityCursor(rank, createdAt, id);
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw invalidCursor();
        }
    }

    private String fingerprint(OpportunityFilter filter) {
        String query = filter.query() == null ? "" : filter.query();
        String canonical = query.length() + ":" + query + "\n"
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
