package org.example.eventpro.entity;

import jakarta.persistence.EntityManager;
import org.example.eventpro.controllers.PaymentController;
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

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:payments-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@Import(PaiementTests.TimeConfig.class)
class PaiementTests {
    @Autowired PaiementService service;
    @Autowired ReservationService reservationService;
    @Autowired PaymentController controller;
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
        @Bean @Primary MutableClock paymentTestClock() { return new MutableClock(); }
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
    PaymentRequest request() {
        return new PaymentRequest(reservationId, new BigDecimal("100.00"));
    }

    @Test void successConfirmsReservationAndPreservesStockAfterDeadline() {
        var result = service.pay(request());
        assertThat(result.created()).isTrue();
        assertThat(result.paiement().statut()).isEqualTo(StatutPaiement.SUCCESS);
        assertThat(result.paiement().datePaiement()).isEqualTo(clock.instant());
        assertThat(reservations.findById(reservationId).orElseThrow().getStatut()).isEqualTo(StatutReservation.CONFIRMED);
        clock.now = clock.now.plusSeconds(900);
        assertThat(reservationService.findById(reservationId).statut()).isEqualTo(StatutReservation.CONFIRMED);
        assertThatThrownBy(() -> reservationService.cancel(reservationId)).isInstanceOf(BusinessException.class);
    }

    @Test void identicalReplayReturnsOriginalPaymentEvenAfterExpirationDate() {
        var request = request();
        var original = service.pay(request);
        clock.now = clock.now.plusSeconds(901);
        var replay = service.pay(new PaymentRequest(reservationId, new BigDecimal("100")));
        assertThat(replay.created()).isFalse();
        assertThat(replay.paiement()).isEqualTo(original.paiement());
    }

    @Test void changedAmountOnPaidReservationIsRejected() {
        service.pay(request());
        assertThatThrownBy(() -> service.pay(new PaymentRequest(reservationId, BigDecimal.ONE)))
                .isInstanceOf(BusinessException.class);
    }

    @Test void wrongAmountDoesNotCreatePaymentOrConfirmReservation() {
        assertThatThrownBy(() -> service.pay(new PaymentRequest(reservationId, new BigDecimal("99.99"))))
                .isInstanceOf(BusinessException.class);
        assertThat(paiements.findByReservationId(reservationId)).isEmpty();
        assertThat(reservations.findById(reservationId).orElseThrow().getStatut()).isEqualTo(StatutReservation.PENDING_PAYMENT);
    }

    @Test void exactExpirationPersistsExpiredAndRejectsPayment() {
        clock.now = reservations.findById(reservationId).orElseThrow().getDateExpiration();
        assertThatThrownBy(() -> service.pay(request())).isInstanceOf(BusinessException.class);
        assertThat(paiements.findByReservationId(reservationId)).isEmpty();
        assertThat(reservations.findById(reservationId).orElseThrow().getStatut()).isEqualTo(StatutReservation.EXPIRED);
    }

    @Test void cancelledAndRefundedReservationsCannotBePaid() {
        reservationService.cancel(reservationId);
        assertThatThrownBy(() -> service.pay(request())).isInstanceOf(BusinessException.class);
        tx.executeWithoutResult(s -> em.find(Reservation.class, reservationId).setStatut(StatutReservation.REFUNDED));
        assertThatThrownBy(() -> service.pay(request())).isInstanceOf(BusinessException.class);
    }

    @Test void zeroAmountReservationCanBeConfirmed() {
        tx.executeWithoutResult(s -> em.find(Reservation.class, reservationId).setMontantTotal(BigDecimal.ZERO));
        var result = service.pay(new PaymentRequest(reservationId, BigDecimal.ZERO));
        assertThat(result.paiement().statut()).isEqualTo(StatutPaiement.SUCCESS);
        assertThat(result.paiement().montant()).isZero();
    }

    @Test void concurrentIdenticalRequestsReturnSamePayment() throws Exception {
        var request = request();
        var results = race(request, request);
        assertThat(results).allMatch(r -> r instanceof PaiementService.PaymentResult);
        var first = (PaiementService.PaymentResult) results.get(0);
        var second = (PaiementService.PaymentResult) results.get(1);
        assertThat(first.paiement().id()).isEqualTo(second.paiement().id());
        assertThat(first.created()).isNotEqualTo(second.created());
    }

    List<Object> race(PaymentRequest first, PaymentRequest second) throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            List<Future<Object>> results = new ArrayList<>();
            for (var r : List.of(first, second)) results.add(pool.submit(() -> {
                start.await();
                try { return service.pay(r); } catch (BusinessException ex) { return ex; }
            }));
            start.countDown();
            return List.of(results.get(0).get(15, TimeUnit.SECONDS), results.get(1).get(15, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
    }

    String body() {
        return "{\"reservationId\":" + reservationId + ",\"montant\":100.00}";
    }

    @Test void httpSuccessReplayReadsAndMissingResources() throws Exception {
        String body = body();
        mvc.perform(post("/api/paiements").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("statut").value("SUCCESS"))
                .andExpect(jsonPath("paymentToken").doesNotExist()).andExpect(jsonPath("tokenFingerprint").doesNotExist());
        mvc.perform(post("/api/paiements").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        Long id = paiements.findByReservationId(reservationId).orElseThrow().getId();
        mvc.perform(get("/api/paiements/" + id)).andExpect(status().isOk());
        mvc.perform(get("/api/paiements/reservation/" + reservationId)).andExpect(status().isOk());
        mvc.perform(get("/api/paiements/0")).andExpect(status().isNotFound());
        mvc.perform(get("/api/paiements/reservation/0")).andExpect(status().isNotFound());
        mvc.perform(post("/api/paiements").contentType(MediaType.APPLICATION_JSON)
                .content(body().replace("\"reservationId\":" + reservationId, "\"reservationId\":999999999")))
                .andExpect(status().isNotFound());
    }

    @Test void httpValidationAndPaymentByAmount() throws Exception {
        String body = body();
        for (String invalid : List.of("{}", body.replace("100.00", "-1"), body.replace("100.00", "100.001"),
                body.replace("\"reservationId\":" + reservationId, "\"reservationId\":0"))) {
            mvc.perform(post("/api/paiements").contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/paiements").contentType(MediaType.APPLICATION_JSON).content(body.replace("100.00", "99")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/paiements/reservation/" + reservationId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"montant\":100}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("statut").value("SUCCESS"));
    }
}
