package ch.unibas.medizin.depot.dto;

import ch.unibas.medizin.depot.validation.ModeConstraint;
import ch.unibas.medizin.depot.validation.RealmConstraint;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AccessTokenRequestDto(@NotBlank String password,
                                    @NotBlank @RealmConstraint String realm,
                                    @NotBlank String subject,
                                    @NotBlank @ModeConstraint String mode,
                                    @NotNull @Future @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") @JsonDeserialize(using = LocalDateDeserializer.class) @JsonSerialize(using = LocalDateSerializer.class) LocalDate expirationDate) {
}
