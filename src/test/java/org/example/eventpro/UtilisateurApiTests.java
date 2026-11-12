package org.example.eventpro;

import org.example.eventpro.controllers.UtilisateurController;
import org.example.eventpro.entity.*;
import org.example.eventpro.exceptions.GlobalExceptionHandler;
import org.example.eventpro.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class UtilisateurApiTests {
    @Autowired UtilisateurController controller;
    @Autowired UtilisateurRepository repository;
    @Autowired PasswordEncoder encoder;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    String registerBody(String email) {
        return "{\"nom\":\" Test Client \",\"email\":\"" + email
                + "\",\"motDePasse\":\"Secret123!\"}";
    }

    @Test
    void listsOnlyTheRequestedAccountType() throws Exception {
        var routes = java.util.Map.of("register", "clients", "admin", "admins",
                "agent", "agents", "organisateur", "organisateurs");
        for (String creation : routes.keySet()) {
            mvc.perform(post("/api/utilisateurs/" + creation)
                    .contentType(MediaType.APPLICATION_JSON).content(registerBody(creation + "@example.com")))
                    .andExpect(status().isCreated());
        }
        for (var route : routes.entrySet()) {
            mvc.perform(get("/api/utilisateurs/" + route.getValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].email").value(route.getKey() + "@example.com"))
                    .andExpect(jsonPath("$[0].motDePasse").doesNotExist());
        }
    }

    @Test
    void eachActorEndpointPersistsItsOwnSubtype() throws Exception {
        var actors = java.util.Map.of("organisateur", Organisateur.class,
                "admin", Administrateur.class, "agent", AgentControle.class);
        for (var actor : actors.entrySet()) {
            String path = "/api/utilisateurs/" + actor.getKey();
            String email = UUID.randomUUID() + "@example.com";
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(registerBody(email)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("motDePasse").doesNotExist());
            var user = repository.findByEmail(email).orElseThrow();
            assertThat(user).isInstanceOf(actor.getValue());
            assertThat(encoder.matches("Secret123!", user.getMotDePasse())).isTrue();
            mvc.perform(post("/api/utilisateurs/register").contentType(MediaType.APPLICATION_JSON)
                    .content(registerBody(email))).andExpect(status().isConflict());
        }
    }

    @Test
    void registerReadUpdateDelete() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/utilisateurs/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email.toUpperCase())))
                .andExpect(status().isCreated()).andExpect(jsonPath("email").value(email))
                .andExpect(jsonPath("motDePasse").doesNotExist());
        var user = repository.findByEmail(email).orElseThrow();
        Long id = user.getId();
        assertThat(user).isInstanceOf(Client.class);
        String hash = user.getMotDePasse();
        assertThat(hash).isNotEqualTo("Secret123!");
        assertThat(encoder.matches("Secret123!", hash)).isTrue();
        mvc.perform(get("/api/utilisateurs/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("nom").value("Test Client"));
        mvc.perform(get("/api/utilisateurs")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].motDePasse").isEmpty());
        mvc.perform(put("/api/utilisateurs/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Nouveau nom\",\"email\":\"" + email.toUpperCase() + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("email").value(email));
        assertThat(repository.findById(id).orElseThrow().getMotDePasse()).isEqualTo(hash);
        mvc.perform(delete("/api/utilisateurs/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/utilisateurs/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void duplicateEmailOnRegisterAndUpdateIsConflict() throws Exception {
        String first = UUID.randomUUID() + "@example.com";
        String second = UUID.randomUUID() + "@example.com";
        for (String email : new String[]{first, second}) {
            mvc.perform(post("/api/utilisateurs/register").contentType(MediaType.APPLICATION_JSON)
                    .content(registerBody(email))).andExpect(status().isCreated());
        }
        mvc.perform(post("/api/utilisateurs/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(first.toUpperCase()))).andExpect(status().isConflict());
        Long id = repository.findByEmail(second).orElseThrow().getId();
        mvc.perform(put("/api/utilisateurs/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Test\",\"email\":\"" + first.toUpperCase() + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidRegistrationIsBadRequest() throws Exception {
        for (String body : new String[]{
                "{\"nom\":\" \",\"email\":\"invalide\",\"motDePasse\":\"court\"}",
                "{\"nom\":\"Test\",\"email\":\"test@example.com\",\"motDePasse\":\"" + "é".repeat(37) + "\"}",
                "{}"}) {
            mvc.perform(post("/api/utilisateurs/register").contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void missingUserIsNotFound() throws Exception {
        mvc.perform(get("/api/utilisateurs/0")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/utilisateurs/0")).andExpect(status().isNotFound());
        mvc.perform(put("/api/utilisateurs/0").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nom\":\"Test\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isNotFound());
    }
}
