package org.example.eventpro.entity;

import jakarta.persistence.EntityManager;
import org.example.eventpro.controllers.BilletController;
import org.example.eventpro.dto.request.PaymentRequest;
import org.example.eventpro.enums.*;
import org.example.eventpro.exceptions.*;
import org.example.eventpro.repository.*;
import org.example.eventpro.service.PaiementService;
import org.example.eventpro.service.ReservationService;
import org.example.eventpro.dto.request.ReservationCreateRequest;
import org.example.eventpro.dto.request.LigneReservationRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:billets-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@Import(BilletTests.TimeConfig.class)
class BilletTests {
    @Autowired PaiementService service;
    @Autowired org.example.eventpro.service.BilletService billets;
    @Autowired BilletRepository billetRepository;
    @Autowired ReservationService reservationService;
    @Autowired BilletController controller;
    @Autowired PaiementRepository paiements;
    @Autowired ReservationRepository reservations;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired MutableClock clock;
    TransactionTemplate tx;
    MockMvc mvc;
    Long clientId, categoryId, reservationId;

    @TestConfiguration
    static class TimeConfig {
        @Bean @Primary MutableClock billetTestClock() { return new MutableClock(); }
    }
    static class MutableClock extends Clock {
        volatile Instant now = Instant.parse("2031-06-10T10:00:00Z");
        public ZoneId getZone() { return ZoneId.of("Europe/Paris"); }
        public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        public Instant instant() { return now; }
    }

    @BeforeEach void setup() {
        clock.now = Instant.parse("2031-06-10T10:00:00Z");
        tx = new TransactionTemplate(transactionManager);
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        tx.executeWithoutResult(status -> {
            Client client = new Client();
            client.setNom("Client"); client.setEmail(UUID.randomUUID() + "@test.com"); client.setMotDePasse("test-hash");
            em.persist(client); clientId = client.getId();
            Organisateur organizer = new Organisateur();
            organizer.setNom("Organisateur"); organizer.setEmail(UUID.randomUUID() + "@test.com"); organizer.setMotDePasse("test-hash");
            em.persist(organizer);
            Evenement event = new Evenement();
            event.setTitre("Concert"); event.setLieu("Paris"); event.setOrganisateur(organizer);
            event.setDateOuvertureVente(clock.instant().minusSeconds(3600));
            event.setDateClotureVente(clock.instant().plusSeconds(7200));
            event.setDateDebut(clock.instant().plusSeconds(10800)); event.setDateFin(clock.instant().plusSeconds(14400));
            event.setCapacite(10); event.setStatut(StatutEvenement.PUBLISHED); em.persist(event);
            CategorieBillet category = new CategorieBillet(); category.setNom("Standard"); em.persist(category);
            var configured = new CategorieBilletEvenement(category, new BigDecimal("100.00"), 10, 10);
            configured.setEvenement(event); em.persist(configured); categoryId = configured.getId();
        });
        reservationId = reserve();
    }



    Long reserve() {
        return reservationService.create(new ReservationCreateRequest(clientId,
                List.of(new LigneReservationRequest(categoryId, 1)), null)).id();
    }
    PaymentRequest payment() {
        return new PaymentRequest(reservationId, new BigDecimal("100.00"));
    }
    org.example.eventpro.dto.response.BilletResponseDTO paidTicket() {
        service.pay(payment());
        return billets.findByReservationId(reservationId).get(0);
    }

    @Test void paymentAutomaticallyGeneratesCorrectQuantityAndRelations() {
        Long secondCategory = tx.execute(s -> {
            CategorieBillet general = new CategorieBillet(); general.setNom("VIP"); em.persist(general);
            var configured = new CategorieBilletEvenement(general, new BigDecimal("200"), 10, 10);
            configured.setEvenement(em.find(CategorieBilletEvenement.class, categoryId).getEvenement());
            em.persist(configured); return configured.getId();
        });
        var reservation = reservationService.create(new ReservationCreateRequest(clientId,
                List.of(new LigneReservationRequest(categoryId, 2), new LigneReservationRequest(secondCategory, 1)), null));
        service.pay(new PaymentRequest(reservation.id(), new BigDecimal("400")));
        var tickets = billets.findByReservationId(reservation.id());
        assertThat(tickets).hasSize(3);
        assertThat(tickets.stream().map(t -> t.code()).distinct().count()).isEqualTo(3);
        assertThat(tickets).allSatisfy(t -> {
            assertThat(t.statut()).isEqualTo(StatutBillet.VALID);
            assertThat(t.dateControle()).isNull();
            assertThat(t.dateEmission()).isEqualTo(LocalDateTime.now(clock));
            assertThat(t.reservationId()).isEqualTo(reservation.id());
            assertThat(t.code()).startsWith("TKT-");
        });
        for (var line : reservation.lignes()) {
            var matching = tickets.stream().filter(t -> t.ligneReservationId().equals(line.id())).toList();
            assertThat(matching).hasSize(line.quantite());
            Long expectedCategory = tx.execute(s -> em.find(CategorieBilletEvenement.class,
                    line.categorieBilletEvenementId()).getCategorieBillet().getId());
            assertThat(matching).allSatisfy(t -> assertThat(t.categorieBilletId()).isEqualTo(expectedCategory));
        }
    }

    @Test void failedAndUnpaidReservationsHaveNoTickets() {
        assertThat(billets.findByReservationId(reservationId)).isEmpty();
        assertThatThrownBy(() -> billets.generateForReservation(reservationId)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.pay(new PaymentRequest(reservationId, BigDecimal.ONE))).isInstanceOf(BusinessException.class);
        assertThat(billets.findByReservationId(reservationId)).isEmpty();
        assertThatThrownBy(() -> billets.generateForReservation(reservationId)).isInstanceOf(BusinessException.class);
    }

    @Test void confirmationAloneWithoutSuccessfulPaymentCannotMintTickets() {
        tx.executeWithoutResult(s -> em.find(Reservation.class, reservationId).setStatut(StatutReservation.CONFIRMED));
        assertThatThrownBy(() -> billets.generateForReservation(reservationId)).isInstanceOf(BusinessException.class);
        assertThat(billets.findByReservationId(reservationId)).isEmpty();
    }

    @Test void replayAndRegenerationDoNotDuplicateOrResetUsedTickets() {
        var payment = payment();
        service.pay(payment);
        var first = billets.findByReservationId(reservationId).get(0);
        billets.controler(first.code(), first.evenementId());
        service.pay(payment);
        var generated = billets.generateForReservation(reservationId);
        assertThat(generated).hasSize(1);
        assertThat(generated.get(0).id()).isEqualTo(first.id());
        assertThat(generated.get(0).statut()).isEqualTo(StatutBillet.USED);
    }

    @Test void generationFailureRollsBackPaymentAndConfirmation() {
        // Deliberately corrupt a fixture to force failure during ticket generation.
        tx.executeWithoutResult(s -> em.find(Reservation.class, reservationId).getLignesReservation().get(0).setQuantite(0));
        assertThatThrownBy(() -> service.pay(payment())).isInstanceOf(BusinessException.class);
        assertThat(paiements.findByReservationId(reservationId)).isEmpty();
        assertThat(reservations.findById(reservationId).orElseThrow().getStatut()).isEqualTo(StatutReservation.PENDING_PAYMENT);
        assertThat(billets.findByReservationId(reservationId)).isEmpty();
    }

    @Test void verificationDoesNotConsumeTicketAndControlOnlySucceedsOnce() {
        var ticket = paidTicket();
        assertThat(billets.verifier(ticket.code(), ticket.evenementId()).statut()).isEqualTo(StatutBillet.VALID);
        assertThat(billets.findById(ticket.id()).dateControle()).isNull();
        var used = billets.controler(ticket.code(), ticket.evenementId());
        assertThat(used.statut()).isEqualTo(StatutBillet.USED);
        assertThat(used.dateControle()).isEqualTo(LocalDateTime.now(clock));
        clock.now = clock.now.plusSeconds(10);
        assertThatThrownBy(() -> billets.controler(ticket.code(), ticket.evenementId())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> billets.verifier(ticket.code(), ticket.evenementId())).isInstanceOf(BusinessException.class);
        assertThat(billets.findByCode(ticket.code()).dateControle()).isEqualTo(used.dateControle());
    }

    @Test void wrongEventDoesNotConsumeTicket() {
        var ticket = paidTicket();
        assertThatThrownBy(() -> billets.controler(ticket.code(), ticket.evenementId() + 1000)).isInstanceOf(BusinessException.class);
        assertThat(billets.findById(ticket.id()).statut()).isEqualTo(StatutBillet.VALID);
    }

    @Test void cancelledTicketIsRejected() {
        var ticket = paidTicket();
        tx.executeWithoutResult(s -> em.find(Billet.class, ticket.id()).setStatut(StatutBillet.CANCELLED));
        assertThatThrownBy(() -> billets.controler(ticket.code(), ticket.evenementId())).isInstanceOf(BusinessException.class);
        assertThat(billets.findById(ticket.id()).dateControle()).isNull();
    }

    @Test void refundedReservationIsRejected() {
        var ticket = paidTicket();
        tx.executeWithoutResult(s -> em.find(Reservation.class, reservationId).setStatut(StatutReservation.REFUNDED));
        assertThatThrownBy(() -> billets.controler(ticket.code(), ticket.evenementId())).isInstanceOf(BusinessException.class);
    }

    @Test void cancelledOrFinishedEventIsRejected() {
        var ticket = paidTicket();
        tx.executeWithoutResult(s -> em.find(Evenement.class, ticket.evenementId()).setStatut(StatutEvenement.CANCELLED));
        assertThatThrownBy(() -> billets.controler(ticket.code(), ticket.evenementId())).isInstanceOf(BusinessException.class);
        tx.executeWithoutResult(s -> {
            var event = em.find(Evenement.class, ticket.evenementId());
            event.setStatut(StatutEvenement.PUBLISHED); event.setDateFin(clock.instant());
        });
        assertThatThrownBy(() -> billets.controler(ticket.code(), ticket.evenementId())).isInstanceOf(BusinessException.class);
    }

    @Test void concurrentScansHaveOnlyOneWinner() throws Exception {
        var ticket = paidTicket();
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            Callable<Boolean> scan = () -> {
                start.await();
                try { billets.controler(ticket.code(), ticket.evenementId()); return true; }
                catch (BusinessException ex) { return false; }
            };
            var first = pool.submit(scan); var second = pool.submit(scan); start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(billets.findById(ticket.id()).statut()).isEqualTo(StatutBillet.USED);
        } finally { pool.shutdownNow(); }
    }

    @Test void concurrentGenerationDoesNotDuplicateTickets() throws Exception {
        var ticket = paidTicket();
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            Callable<Long> generate = () -> { start.await(); return billets.generateForReservation(reservationId).get(0).id(); };
            var first = pool.submit(generate); var second = pool.submit(generate); start.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo(ticket.id());
            assertThat(second.get(15, TimeUnit.SECONDS)).isEqualTo(ticket.id());
            assertThat(billets.findByReservationId(reservationId)).hasSize(1);
        } finally { pool.shutdownNow(); }
    }

    @Test void httpReadsValidationCheckInAndErrorCodes() throws Exception {
        var ticket = paidTicket();
        String body = "{\"evenementId\":" + ticket.evenementId() + "}";
        mvc.perform(get("/api/billets/" + ticket.id())).andExpect(status().isOk());
        mvc.perform(get("/api/billets/code/" + ticket.code())).andExpect(status().isOk());
        mvc.perform(get("/api/billets/reservation/" + reservationId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(post("/api/billets/" + ticket.code() + "/verifier").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("statut").value("VALID"));
        mvc.perform(post("/api/billets/" + ticket.code() + "/controler").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/billets/" + ticket.code() + "/controler").contentType(MediaType.APPLICATION_JSON).content("{\"evenementId\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/billets/" + ticket.code() + "/controler").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("statut").value("USED")).andExpect(jsonPath("dateControle").isNotEmpty());
        mvc.perform(post("/api/billets/" + ticket.code() + "/controler").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/billets/0")).andExpect(status().isNotFound());
        mvc.perform(get("/api/billets/code/UNKNOWN")).andExpect(status().isNotFound());
        mvc.perform(get("/api/billets/reservation/0")).andExpect(status().isNotFound());
        mvc.perform(post("/api/billets/UNKNOWN/controler").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }
}
