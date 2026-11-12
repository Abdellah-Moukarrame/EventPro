# Billet — partie 4

## Périmètre et conception conservée

Aucune entity, relation, table, enum ou structure de packages existante n'a été modifiée pour cette étape. Ajouts dans les couches existantes : BilletService/Impl, BilletController, BilletMapper, DTOs et implémentation de BilletRepository.

Billet conserve son constructeur protected. La création passe par une insertion SQL dans BilletRepository, utilisant uniquement les colonnes existantes, sans réflexion ni changement de constructeur.

## Génération

Après un paiement SUCCESS, la réservation passe à CONFIRMED puis les billets sont générés dans la même transaction. Une erreur de génération annule aussi le paiement local et la confirmation. Il s'agit toujours du paiement simulé.

Chaque LigneReservation produit autant de billets que sa quantité. Chaque billet reçoit :
- un code TKT- suivi d'un UUID, protégé par la contrainte unique déjà existante ;
- dateEmission, au fuseau de l'horloge du projet (Europe/Paris), stockée dans le LocalDateTime existant ;
- statut VALID et dateControle null ;
- la LigneReservation et la CategorieBillet générale associée à la catégorie configurée de l'événement.

La réservation est verrouillée pendant la génération. Rejouer le paiement ne génère aucun doublon. Réappeler la méthode interne de génération retourne les billets existants, sans réinitialiser les billets USED/CANCELLED. Une ligne contenant un nombre partiel incohérent de billets provoque un conflit plutôt qu'une duplication silencieuse.

Aucun endpoint public ne permet de créer un billet arbitrairement. Les anciens paiements réussis avant cette étape ne sont pas automatiquement régénérés : créer une nouvelle réservation pour la démonstration.

## Consultation, vérification et compostage

| Méthode | URL | Effet |
| --- | --- | --- |
| GET | /api/billets/{id} | Consultation |
| GET | /api/billets/code/{code} | Consultation par code |
| GET | /api/billets/reservation/{reservationId} | Liste, vide tant qu'aucun billet émis |
| POST | /api/billets/{code}/verifier | Contrôle des conditions, sans consommation |
| POST | /api/billets/{code}/controler | Compostage unique VALID -> USED |

Body pour verifier/controler :
```json
{"evenementId": 1}
```

Le contrôle refuse un billet déjà USED ou CANCELLED, une réservation qui n'est plus CONFIRMED, un mauvais événement, un événement non PUBLISHED ou dont dateFin est atteinte. Aucun créneau d'entrée supplémentaire avant dateDebut n'est imposé : le cahier des charges ne définit pas d'heure d'ouverture des portes.

verifier n'enregistre pas de passage et ne garantit pas qu'un autre scan ne consommera pas le billet juste après. controler prend un verrou pessimiste sur le billet, vérifie puis met à jour statut et dateControle dans une transaction. Deux scans concurrents : un seul réussit ; l'autre reçoit 409. La date du premier passage est conservée.

La consultation GET retourne l'état enregistré, y compris USED/CANCELLED ; elle n'autorise pas l'entrée. Aucun stock n'est libéré lors d'un scan.

## Réponse

```json
{
  "id": 1,
  "code": "TKT-<UUID>",
  "dateEmission": "2031-06-10T12:00:00",
  "dateControle": null,
  "statut": "VALID",
  "reservationId": 1,
  "ligneReservationId": 1,
  "categorieBilletId": 1,
  "evenementId": 1
}
```

200 : lecture/vérification/compostage réussi.
400 : evenementId absent, nul ou non positif.
404 : billet/code/réservation inexistant.
409 : billet non utilisable ou mauvais événement.

## Démonstration Postman

1. Démarrer PostgreSQL et l'application :
   .\\gradlew.bat bootRun
2. Exécuter postman/reservation-demo.sql sur la base locale pour disposer d'un nouveau client et d'une catégorie.
3. Importer EventPro-Billets.postman_collection.json.
4. Remplir clientId et categorieId avec les valeurs du SQL ; baseUrl vaut http://localhost:8080.
5. Exécuter les 14 requêtes dans l'ordre. Les IDs des billets, leurs codes et evenementId sont mémorisés automatiquement.

La collection réserve 2 places, vérifie l'absence de billets avant paiement, paye, récupère 2 codes, rejoue le paiement sans duplication, vérifie et composte un billet, puis vérifie le refus du deuxième passage. Le deuxième billet reste VALID.

Les deux places restent vendues après la démonstration ; utiliser un nouveau jeu SQL pour répéter sans dépasser le quota. Swagger : http://localhost:8080/swagger-ui/index.html, groupe Billets.

## Vérification et limites

Tests H2 : génération multi-lignes, liens et quantité exacte, refus sans paiement, idempotence, rollback si génération impossible, scan unique, scans concurrents, génération concurrente, mauvaise réservation/événement, et HTTP 200/400/404/409.

La validation des verrous sur PostgreSQL reste distincte des tests H2. La sécurité CLIENT/AGENT et les remboursements restent dans leurs parties prévues. Aucun PDF ni QR code n'est ajouté : le code unique est utilisable directement pour tester l'entrée.
