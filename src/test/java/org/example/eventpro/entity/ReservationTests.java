package org.example.eventpro.entity;

import jakarta.persistence.EntityManager;
import org.example.eventpro.controllers.ReservationController;
import org.example.eventpro.controllers.CategorieBilletEvenementController;
import org.example.eventpro.dto.request.*;
import org.example.eventpro.dto.response.ReservationResponseDTO;
import org.example.eventpro.enums.*;
import org.example.eventpro.exceptions.*;
import org.example.eventpro.repository.*;
import org.example.eventpro.service.ReservationService;
import org.example.eventpro.service.impl.ReservationExpirationJob;
import org.example.eventpro.service.impl.ReservationLifecycle;
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
import static org.example.eventpro.enums.StatutReservation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:reservations-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@Import(ReservationTests.TimeConfig.class)
class ReservationTests {
    @Autowired ReservationService service;
    @Autowired ReservationController controller;
    @Autowired org.example.eventpro.controllers.EvenementController evenementController;
    @Autowired CategorieBilletEvenementController categoriesController;
    @Autowired ReservationRepository reservations;
    @Autowired PromotionRepository promotions;
    @Autowired ReservationLifecycle lifecycle;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired MutableClock clock;
    TransactionTemplate tx;
    MockMvc mvc;
    Long clientId, otherClientId, eventId, categoryId, secondCategoryId, promoId;
    String promoCode;

    @TestConfiguration
    static class TimeConfig {
        @Bean @Primary MutableClock testClock() { return new MutableClock(); }
    }
    static class MutableClock extends Clock {
        volatile Instant now = Instant.parse("2030-06-10T10:00:00Z");
        public ZoneId getZone() { return ZoneId.of("Europe/Paris"); }
        public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        public Instant instant() { return now; }
    }

    @BeforeEach
    void setup() {
        clock.now = Instant.parse("2030-06-10T10:00:00Z");
        tx = new TransactionTemplate(transactionManager);
        mvc = MockMvcBuilders.standaloneSetup(controller, categoriesController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        tx.executeWithoutResult(status -> {
            Client client = new Client();
            client.setNom("Client"); client.setEmail(UUID.randomUUID() + "@test.com"); client.setMotDePasse("test-hash");
            em.persist(client); clientId = client.getId();
            Client other = new Client();
            other.setNom("Other"); other.setEmail(UUID.randomUUID() + "@test.com"); other.setMotDePasse("test-hash");
            em.persist(other); otherClientId = other.getId();
            Organisateur organizer = new Organisateur();
            organizer.setNom("Organisateur"); organizer.setEmail(UUID.randomUUID() + "@test.com"); organizer.setMotDePasse("test-hash");
            em.persist(organizer);
            Evenement event = new Evenement();
            event.setTitre("Concert"); event.setLieu("Paris"); event.setOrganisateur(organizer);
            event.setDateOuvertureVente(clock.instant().minusSeconds(3600));
            event.setDateClotureVente(clock.instant().plusSeconds(7200));
            event.setDateDebut(clock.instant().plusSeconds(10800));
            event.setDateFin(clock.instant().plusSeconds(14400));
            event.setCapacite(100); event.setStatut(StatutEvenement.PUBLISHED);
            em.persist(event); eventId = event.getId();
            categoryId = addCategory(event, "Standard", "100.00", 5, 3).getId();
            secondCategoryId = addCategory(event, "VIP", "200.00", 5, 3).getId();
            Promotion promo = new Promotion();
            promoCode = "PROMO-" + UUID.randomUUID();
            promo.setCode(promoCode); promo.setEvenement(event); promo.setTypeReduction(TypeReduction.PERCENTAGE);
            promo.setValeurReduction(new BigDecimal("10")); promo.setDateDebut(LocalDate.now(clock));
            promo.setDateFin(LocalDate.now(clock)); promo.setLimiteUtilisation(1);
            em.persist(promo); promoId = promo.getId();
        });
    }

    CategorieBilletEvenement addCategory(Evenement event, String name, String price, int stock, int quota) {
        CategorieBillet category = new CategorieBillet(); category.setNom(name); em.persist(category);
        var configured = new CategorieBilletEvenement(category, new BigDecimal(price), stock, quota);
        configured.setEvenement(event); em.persist(configured); return configured;
    }
    ReservationCreateRequest request(Long client, int quantity, String promo) {
        return new ReservationCreateRequest(client, List.of(new LigneReservationRequest(categoryId, quantity)), promo);
    }
    void editCategory(int stock, int quota) {
        tx.executeWithoutResult(s -> {
            var c = em.find(CategorieBilletEvenement.class, categoryId);
            c.setQuantiteTotale(stock); c.setQuotaMaxParClient(quota);
        });
    }
    void setStatus(Long id, StatutReservation state) {
        tx.executeWithoutResult(s -> em.find(Reservation.class, id).setStatut(state));
    }

    @Test void manageEventCategoriesAndReserveUsingReturnedId() throws Exception {
        String path = "/api/categories-billets-evenements";
        String creation = """
                {"evenementId":%d,"nom":"Balcon","prixUnitaire":90,"quantiteTotale":8,"quotaMaxParClient":3}
                """.formatted(eventId);
        var result = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(creation))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nom").value("Balcon"))
                .andReturn();
        Number id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        String item = path + "/" + id;
        mvc.perform(get(path).param("evenementId", eventId.toString())).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get(item)).andExpect(status().isOk()).andExpect(jsonPath("$.quantiteTotale").value(8));
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(creation.replace("Balcon", "BALCON")))
                .andExpect(status().isConflict());
        String update = """
                {"prixUnitaire":95,"quantiteTotale":6,"quotaMaxParClient":3}
                """;
        mvc.perform(put(item).contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk()).andExpect(jsonPath("$.prixUnitaire").value(95));
        var reservation = service.create(new ReservationCreateRequest(clientId,
                List.of(new LigneReservationRequest(id.longValue(), 2)), null));
        assertThat(reservation.montantTotal()).isEqualByComparingTo("190.00");
        mvc.perform(put(item).contentType(MediaType.APPLICATION_JSON).content(update.replace(":6", ":1")))
                .andExpect(status().isConflict());
        mvc.perform(delete(item)).andExpect(status().isConflict());
        mvc.perform(delete(path + "/" + secondCategoryId)).andExpect(status().isNoContent());
        mvc.perform(get(path + "/" + secondCategoryId)).andExpect(status().isNotFound());
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
    }

    @Test void eventStatusCanBePublishedAndSurvivesAnOrdinaryEdit() throws Exception {
        var api = MockMvcBuilders.standaloneSetup(evenementController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        String path = "/api/evenements/" + eventId;
        tx.executeWithoutResult(s -> em.find(Evenement.class, eventId).setStatut(StatutEvenement.DRAFT));
        api.perform(patch(path + "/statut").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"PUBLISHED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("PUBLISHED"));
        String edit = """
                {"titre":"Concert","lieu":"Paris","capacite":100,"dateDebut":"2030-06-12T12:00:00Z",
                 "dateFin":"2030-06-12T20:00:00Z","dateOuvertureVente":"2030-06-09T09:00:00Z",
                 "dateClotureVente":"2030-06-12T11:00:00Z"}
                """.trim();
        api.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(edit))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("PUBLISHED"));
        api.perform(put(path).contentType(MediaType.APPLICATION_JSON)
                        .content(edit.replace("}", ",\"statut\":\"CANCELLED\"}")))
                .andExpect(status().isOk());
        api.perform(get(path)).andExpect(jsonPath("$.statut").value("CANCELLED"));
        api.perform(patch(path + "/statut").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test void createsLinesAndSnapshotsPricesWithPromotion() {
        var result = service.create(new ReservationCreateRequest(clientId,
                List.of(new LigneReservationRequest(categoryId, 2), new LigneReservationRequest(secondCategoryId, 1)), promoCode));
        assertThat(result.montantTotal()).isEqualByComparingTo("360.00");
        assertThat(result.lignes()).hasSize(2);
        assertThat(result.lignes().get(0).sousTotal()).isEqualByComparingTo("200");
        assertThat(result.statut()).isEqualTo(PENDING_PAYMENT);
        assertThat(result.reference()).startsWith("RES-");
        assertThat(result.dateExpiration()).isEqualTo(clock.instant().plusSeconds(900));
        assertThat(promotions.findById(promoId).orElseThrow().getNombreUtilisations()).isEqualTo(1);
        tx.executeWithoutResult(s -> em.find(CategorieBilletEvenement.class, categoryId).setPrixUnitaire(new BigDecimal("999")));
        assertThat(service.findById(result.id()).lignes().get(0).prixUnitaire()).isEqualByComparingTo("100");
        assertThat(service.findByReference(result.reference()).id()).isEqualTo(result.id());
        assertThat(service.findByClientId(clientId)).hasSize(1);
    }

    @Test void fixedDiscountCannotProduceNegativeTotal() {
        tx.executeWithoutResult(s -> {
            var p = em.find(Promotion.class, promoId);
            p.setTypeReduction(TypeReduction.FIXED_AMOUNT); p.setValeurReduction(new BigDecimal("500"));
        });
        assertThat(service.create(request(clientId, 1, promoCode)).montantTotal()).isEqualByComparingTo("0.00");
    }

    @Test void roundsPercentageToCents() {
        tx.executeWithoutResult(s -> {
            em.find(CategorieBilletEvenement.class, categoryId).setPrixUnitaire(new BigDecimal("10.05"));
            em.find(Promotion.class, promoId).setValeurReduction(new BigDecimal("15"));
        });
        assertThat(service.create(request(clientId, 1, promoCode)).montantTotal()).isEqualByComparingTo("8.54");
    }

    @Test void rejectsInsufficientStockAndCumulativeQuota() {
        service.create(request(clientId, 3, null));
        assertThatThrownBy(() -> service.create(request(clientId, 1, null))).isInstanceOf(QuotaDepasseException.class);
        assertThatThrownBy(() -> service.create(request(otherClientId, 3, null))).isInstanceOf(StockInsuffisantException.class);
        assertThat(service.findByClientId(otherClientId)).isEmpty();
    }

    @Test void cancellationReleasesStockAndPromotionOnlyOnce() {
        editCategory(1, 1);
        var first = service.create(request(clientId, 1, promoCode));
        service.cancel(first.id()); service.cancel(first.id());
        assertThat(service.findById(first.id()).statut()).isEqualTo(CANCELLED);
        assertThat(promotions.findById(promoId).orElseThrow().getNombreUtilisations()).isZero();
        service.create(request(otherClientId, 1, promoCode));
        assertThatThrownBy(() -> service.create(request(clientId, 1, null))).isInstanceOf(StockInsuffisantException.class);
    }

    @Test void expirationReleasesStockAndPromotionAtExactDeadlineWithoutWaitingForJob() {
        editCategory(1, 1);
        var first = service.create(request(clientId, 1, promoCode));
        clock.now = first.dateExpiration();
        var second = service.create(request(otherClientId, 1, promoCode));
        assertThat(service.findById(first.id()).statut()).isEqualTo(EXPIRED);
        assertThat(service.findByReference(first.reference()).statut()).isEqualTo(EXPIRED);
        assertThat(service.findByClientId(clientId).get(0).statut()).isEqualTo(EXPIRED);
        assertThat(promotions.findById(promoId).orElseThrow().getNombreUtilisations()).isEqualTo(1);
        service.cancel(first.id());
        assertThat(service.findById(second.id()).statut()).isEqualTo(PENDING_PAYMENT);
    }

    @Test void expirationJobPersistsExpiredStatus() {
        var result = service.create(request(clientId, 1, promoCode));
        clock.now = result.dateExpiration();
        new ReservationExpirationJob(reservations, lifecycle, clock).expireReservations();
        assertThat(reservations.findById(result.id()).orElseThrow().getStatut()).isEqualTo(EXPIRED);
        assertThat(promotions.findById(promoId).orElseThrow().getNombreUtilisations()).isZero();
    }

    @Test void confirmedReservationKeepsStockAndCannotBeCancelledOrExpired() {
        editCategory(1, 1);
        var result = service.create(request(clientId, 1, null));
        setStatus(result.id(), CONFIRMED);
        clock.now = result.dateExpiration();
        lifecycle.expire(result.id());
        assertThat(service.findById(result.id()).statut()).isEqualTo(CONFIRMED);
        assertThatThrownBy(() -> service.cancel(result.id())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create(request(otherClientId, 1, null))).isInstanceOf(StockInsuffisantException.class);
    }

    @Test void invalidPromotionsRollbackReservation() {
        assertThatThrownBy(() -> service.create(request(clientId, 1, "UNKNOWN"))).isInstanceOf(PromotionInvalideException.class);
        tx.executeWithoutResult(s -> em.find(Promotion.class, promoId).setDateFin(LocalDate.now(clock).minusDays(1)));
        assertThatThrownBy(() -> service.create(request(clientId, 1, promoCode))).isInstanceOf(PromotionInvalideException.class);
        tx.executeWithoutResult(s -> {
            var p = em.find(Promotion.class, promoId);
            p.setDateFin(LocalDate.now(clock).plusDays(2)); p.setDateDebut(LocalDate.now(clock).plusDays(1));
        });
        assertThatThrownBy(() -> service.create(request(clientId, 1, promoCode))).isInstanceOf(PromotionInvalideException.class);
        assertThat(service.findByClientId(clientId)).isEmpty();
        assertThat(promotions.findById(promoId).orElseThrow().getNombreUtilisations()).isZero();
    }

    @Test void promotionLimitAppliesAcrossDifferentCategories() {
        service.create(request(clientId, 1, promoCode));
        assertThatThrownBy(() -> service.create(new ReservationCreateRequest(otherClientId,
                List.of(new LigneReservationRequest(secondCategoryId, 1)), promoCode)))
                .isInstanceOf(PromotionInvalideException.class);
        assertThat(service.findByClientId(otherClientId)).isEmpty();
    }

    @Test void rejectsDuplicateCategoryAndMixedEvents() {
        assertThatThrownBy(() -> service.create(new ReservationCreateRequest(clientId,
                List.of(new LigneReservationRequest(categoryId, 1), new LigneReservationRequest(categoryId, 1)), null)))
                .isInstanceOf(BusinessException.class);
        tx.executeWithoutResult(s -> {
            Evenement original = em.find(Evenement.class, eventId);
            Evenement other = new Evenement();
            other.setTitre("Other"); other.setLieu("Paris"); other.setOrganisateur(original.getOrganisateur());
            other.setDateDebut(original.getDateDebut()); other.setDateFin(original.getDateFin());
            other.setDateOuvertureVente(original.getDateOuvertureVente()); other.setDateClotureVente(original.getDateClotureVente());
            other.setCapacite(100); other.setStatut(StatutEvenement.PUBLISHED); em.persist(other);
            em.find(CategorieBilletEvenement.class, secondCategoryId).setEvenement(other);
        });
        assertThatThrownBy(() -> service.create(new ReservationCreateRequest(clientId,
                List.of(new LigneReservationRequest(categoryId, 1), new LigneReservationRequest(secondCategoryId, 1)), null)))
                .isInstanceOf(BusinessException.class);
        assertThat(service.findByClientId(clientId)).isEmpty();
    }

    @Test void checksSalesWindowAndCapsDeadlineAtClosing() {
        tx.executeWithoutResult(s -> em.find(Evenement.class, eventId).setStatut(StatutEvenement.DRAFT));
        assertThatThrownBy(() -> service.create(request(clientId, 1, null))).isInstanceOf(BusinessException.class);
        tx.executeWithoutResult(s -> {
            var e = em.find(Evenement.class, eventId); e.setStatut(StatutEvenement.PUBLISHED);
            e.setDateOuvertureVente(clock.instant().plusSeconds(10));
        });
        assertThatThrownBy(() -> service.create(request(clientId, 1, null))).isInstanceOf(BusinessException.class);
        tx.executeWithoutResult(s -> {
            var e = em.find(Evenement.class, eventId); e.setDateOuvertureVente(clock.instant());
            e.setDateClotureVente(clock.instant().plusSeconds(60));
        });
        var result = service.create(request(clientId, 1, null));
        assertThat(result.dateExpiration()).isEqualTo(clock.instant().plusSeconds(60));
        clock.now = result.dateExpiration();
        assertThatThrownBy(() -> service.create(request(otherClientId, 1, null))).isInstanceOf(BusinessException.class);
    }

    @Test void concurrentRequestsCannotOversellLastTicket() throws Exception {
        editCategory(1, 1);
        List<Boolean> results = race(request(clientId, 1, null), request(otherClientId, 1, null));
        assertThat(results).containsExactlyInAnyOrder(true, false);
    }

    @Test void concurrentRequestsCannotExceedPromotionLimit() throws Exception {
        List<Boolean> results = race(request(clientId, 1, promoCode), new ReservationCreateRequest(otherClientId,
                List.of(new LigneReservationRequest(secondCategoryId, 1)), promoCode));
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(promotions.findById(promoId).orElseThrow().getNombreUtilisations()).isEqualTo(1);
    }

    List<Boolean> race(ReservationCreateRequest first, ReservationCreateRequest second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (var request : List.of(first, second)) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try { service.create(request); return true; }
                    catch (BusinessException expected) { return false; }
                }));
            }
            start.countDown();
            return List.of(futures.get(0).get(15, TimeUnit.SECONDS), futures.get(1).get(15, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
    }

    @Test void httpEndpointsAndValidation() throws Exception {
        String body = "{\"clientId\":" + clientId + ",\"lignes\":[{\"categorieBilletEvenementId\":" + categoryId + ",\"quantite\":1}]}";
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("montantTotal").value(100)).andExpect(jsonPath("lignes[0].id").isNumber());
        Long id = service.findByClientId(clientId).get(0).id();
        mvc.perform(get("/api/reservations/" + id)).andExpect(status().isOk());
        mvc.perform(get("/api/reservations/client/" + clientId)).andExpect(status().isOk());
        mvc.perform(get("/api/reservations/reference/" + service.findById(id).reference())).andExpect(status().isOk());
        mvc.perform(post("/api/reservations/" + id + "/cancel")).andExpect(status().isNoContent());
        for (String invalid : List.of("{}", "{\"clientId\":" + clientId + ",\"lignes\":[]}",
                body.replace("\"quantite\":1", "\"quantite\":0"),
                "{\"clientId\":" + clientId + ",\"lignes\":[null]}")) {
            mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/reservations/0")).andExpect(status().isNotFound());
        mvc.perform(get("/api/reservations/reference/UNKNOWN")).andExpect(status().isNotFound());
        mvc.perform(get("/api/reservations/client/0")).andExpect(status().isNotFound());
        mvc.perform(post("/api/reservations/0/cancel")).andExpect(status().isNotFound());
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("\"clientId\":" + clientId, "\"clientId\":0"))).andExpect(status().isNotFound());
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("\"categorieBilletEvenementId\":" + categoryId, "\"categorieBilletEvenementId\":0")))
                .andExpect(status().isNotFound());
        editCategory(0, 1);
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }
}
