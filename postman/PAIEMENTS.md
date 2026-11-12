# Payer une réservation

Démarrer EventPro normalement. Dans Swagger, ouvrir POST /api/paiements/reservation/{reservationId}, saisir l’identifiant de la réservation et envoyer le montant total après promotion.

```json
{"montant": 180.00}
```

Sur une réservation en attente et encore valide, un montant exact crée le paiement local (201 SUCCESS), confirme la réservation et émet ses billets dans la même transaction. Un panier gratuit accepte 0. Une répétition après succès retourne le même paiement (200). Un montant différent ou une réservation expirée ou annulée retourne 409. Les données invalides retournent 400 et un identifiant inconnu 404.

POST /api/paiements accepte également {"reservationId": 1, "montant": 180.00}. Les paiements sont consultables via GET /api/paiements/{id} ou GET /api/paiements/reservation/{reservationId}.

Pour le scénario Postman, importer EventPro-Paiements.postman_collection.json, renseigner clientId et categorieId à partir de reservation-demo.sql et exécuter les requêtes dans l’ordre. Le scénario lit le montant de la réservation créée.
