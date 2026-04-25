package ch.unibas.medizin.depot.dto;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

public class ExpirationDeserializer extends ValueDeserializer<Instant> {

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) {
        final var text = p.getValueAsString();
        if (text == null || text.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(text).toInstant();
        } catch (DateTimeParseException e) {
            // fall through to next format
        }

        try {
            return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            // fall through to next format
        }

        try {
            return LocalDate.parse(text).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException e) {
            return (Instant) ctxt.handleWeirdStringValue(Instant.class, text,
                    "expected ISO date (yyyy-MM-dd) or full ISO date-time");
        }
    }

}
