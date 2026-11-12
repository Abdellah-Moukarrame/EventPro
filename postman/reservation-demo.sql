-- Données de démonstration uniquement : exécuter après le démarrage de l'application.
-- Chaque exécution crée un nouveau jeu de données, sans modifier les comptes existants.
-- Les comptes de fixture n'ont pas de mot de passe de connexion utilisable.
BEGIN;
SET LOCAL TIME ZONE 'Europe/Paris';
WITH organizer_user AS (
    INSERT INTO utilisateurs (nom, email, mot_de_passe)
    VALUES ('Organisateur demo reservation', 'organizer-' || md5(random()::text) || '@example.com', 'DEMO-NO-LOGIN')
    RETURNING id
), organizer AS (
    INSERT INTO organisateurs (id) SELECT id FROM organizer_user RETURNING id
), client_user AS (
    INSERT INTO utilisateurs (nom, email, mot_de_passe)
    VALUES ('Client demo reservation', 'client-' || md5(random()::text) || '@example.com', 'DEMO-NO-LOGIN')
    RETURNING id
), demo_client AS (
    INSERT INTO clients (id) SELECT id FROM client_user RETURNING id
), demo_event AS (
    INSERT INTO evenements (titre, lieu, date_debut, date_fin, date_ouverture_vente, date_cloture_vente, capacite, statut, organisateur_id)
    SELECT 'Concert demo reservation', 'Paris', now() + interval '2 days', now() + interval '2 days 3 hours',
           now() - interval '1 day', now() + interval '1 day', 5, 'PUBLISHED', id FROM organizer
    RETURNING id
), demo_category AS (
    INSERT INTO categories_billets (nom) VALUES ('Standard demo reservation') RETURNING id
), configured_category AS (
    INSERT INTO categories_billets_evenements (evenement_id, categorie_billet_id, prix_unitaire, quantite_totale, quota_max_par_client)
    SELECT e.id, c.id, 100.00, 5, 3 FROM demo_event e CROSS JOIN demo_category c RETURNING id
), promo AS (
    INSERT INTO promotions (code, type_reduction, valeur_reduction, date_debut, date_fin, limite_utilisation, nombre_utilisations, evenement_id)
    SELECT 'DEMO-' || id, 'PERCENTAGE', 10, CURRENT_DATE, CURRENT_DATE + 1, 10, 0, id FROM demo_event
    RETURNING code
)
SELECT c.id AS "clientId", cat.id AS "categorieId", p.code AS "codePromotion"
FROM demo_client c CROSS JOIN configured_category cat CROSS JOIN promo p;
COMMIT;
