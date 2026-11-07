package org.example.eventpro.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import java.nio.charset.StandardCharsets;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Size(max = 255)
    private String nom;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 8)
    private String motDePasse;
    @AssertTrue(message = "Le mot de passe ne doit pas dépasser 72 octets UTF-8")
    public boolean isMotDePasseCompatibleBCrypt() {
        return motDePasse == null || motDePasse.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
