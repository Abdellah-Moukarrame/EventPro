# Réservations — partie 2

## Règles appliquées

- Un panier appartient à un Client existant et concerne un seul événement.
- L'événement doit être PUBLISHED ; ouverture des ventes incluse, clôture et début de l'événement exclus.
- Chaque ligne utilise l'identifiant de CategorieBilletEvenement (pas celui de CategorieBillet).
- Quantité minimum : 1. Panier non vide ; lignes nulles interdites. Catégories répétées rejetées.
- Prix et sous-totaux sont calculés côté serveur et conservés dans les lignes ; modifier le tarif de l'événement ne modifie pas un panier existant.
- Les lignes sont enregistrées avec la réservation via cascade, dans une transaction : tout réussit ou rien n'est enregistré.
- La référence est RES- suivie d'un UUID, avec une contrainte unique en base.

## Stock et quota

quantiteTotale reste la capacité configurée de la catégorie. Le disponible est :

    quantiteTotale - quantités CONFIRMED - quantités PENDING_PAYMENT non expirées

Le quotaMaxParClient s'applique au cumul du client pour cette catégorie, sur les mêmes états actifs. Il ne se limite pas à la requête courante. Exemple : quota 3, panier actif de 2 billets ; une nouvelle demande de 2 est refusée.

Les catégories sont verrouillées en écriture dans un ordre stable pendant la création. Les requêtes concurrentes attendent puis relisent les quantités occupées. Une annulation ou expiration libère logiquement les places : aucune augmentation de quantiteTotale. Répéter une annulation ne crée donc pas de places supplémentaires.

## Montant et promotion

    sousTotal = prixUnitaire × quantite
    montantBrut = somme des sousTotaux
    PERCENTAGE : montantTotal = montantBrut - montantBrut × valeur / 100
    FIXED_AMOUNT : montantTotal = max(0, montantBrut - valeur)

Exemple : 2 × 100, promotion 10 % => 180. Montants BigDecimal, arrondi HALF_UP à deux décimales sur le total final.

Le code doit correspondre exactement (casse comprise, espaces extérieurs retirés) à une promotion de l'événement. Date de début et de fin incluses, fuseau Europe/Paris. Un code absent/vide signifie sans promotion. Promotion inconnue, hors dates, valeur invalide ou limite atteinte : 409.

Une utilisation est réservée par panier actif, pas par billet. La limite compte les paniers confirmés et les paniers en attente non expirés. Le verrou sur la promotion protège sa dernière utilisation même entre catégories différentes. Le compteur nombreUtilisations est synchronisé lors de la création, de l'annulation et de l'expiration. La décision d'éligibilité utilise les réservations actives, pas un compteur potentiellement en retard.

## Durée et états

Création : PENDING_PAYMENT. Délai par défaut : 15 minutes, raccourci si la vente ferme avant.

    dateExpiration = min(dateCreation + 15 minutes, dateClotureVente, dateDebut)

Configuration : reservation.payment-timeout=PT15M.
À l'instant exact de dateExpiration, le panier ne bloque plus de stock ni de quota/promotion. Une tâche toutes les 60 secondes persiste EXPIRED ; la consultation met aussi l'état à jour. Configuration : reservation.expiration.check-ms=60000. La tâche peut être désactivée avec reservation.expiration.enabled=false (tests).

Annuler un panier en attente donne CANCELLED, ou EXPIRED si son délai est déjà écoulé. Annuler un panier déjà CANCELLED/EXPIRED est sans effet et renvoie 204. Un panier CONFIRMED/REFUNDED ne peut pas être annulé par cette opération : 409. Le paiement et le remboursement seront traités dans leur propre partie.

## Endpoints

| Méthode | URL | Réponse |
| --- | --- | --- |
| POST | /api/reservations | 201 + Location |
| GET | /api/reservations/{id} | 200 |
| GET | /api/reservations/reference/{reference} | 200 |
| GET | /api/reservations/client/{clientId} | 200, liste éventuellement vide |
| POST | /api/reservations/{id}/cancel | 204 |

Création :

```json
{
  "clientId": 1,
  "lignes": [
    {"categorieBilletEvenementId": 1, "quantite": 2}
  ],
  "codePromotion": "PROMO10"
}
```

Remplacer les identifiants et le code par ceux de la base. Ne pas envoyer montantTotal, prixUnitaire, statut ou dates : le serveur les calcule.

400 : DTO invalide. 404 : client, catégorie ou réservation inexistante. 409 : stock, quota, promotion, ventes fermées, panier multi-événement, catégorie répétée, ou état incompatible avec l'annulation.

## Essai Postman

1. Démarrer PostgreSQL : docker compose up -d postgres.
2. Démarrer l'application : .\\gradlew.bat bootRun.
3. Les endpoints actuels d'Evenement ne permettent pas de préparer à eux seuls un événement publié avec ses catégories. Pour un essai local, exécuter postman/reservation-demo.sql dans la base eventpro après le démarrage. Ce script ajoute un jeu de démonstration et retourne clientId, categorieId, codePromotion ; il ne s'exécute pas automatiquement.
4. Importer EventPro-Reservations.postman_collection.json. Copier les trois valeurs retournées dans les variables de la collection.
5. Exécuter les 12 requêtes dans l'ordre. Elles vérifient création, lectures, quota, stock, validation, annulation répétée et nouvelle réservation après libération ; le dernier panier est annulé.
6. Swagger : http://localhost:8080/swagger-ui/index.html, groupe Réservations.

Pour vérifier l'expiration manuellement sans attendre 15 minutes, démarrer avec :
```powershell
.\\gradlew.bat bootRun --args="--reservation.payment-timeout=PT30S"
```
Créer un panier, attendre 30 secondes puis le consulter : EXPIRED. Revenir au délai standard pour les autres essais.

## Tests et limites de cette étape

Tests automatisés H2 en mode PostgreSQL : prix, arrondis, promotions, quotas cumulés, stock, annulation répétée, expiration exacte, tâche d'expiration, ventes fermées, rollback et requêtes concurrentes. Le test HTTP utilise MockMvc avec le vrai service et la vraie base de test.

Les tests H2 ne remplacent pas une validation sur PostgreSQL, notamment des verrous concurrents. Authentification et contrôle du propriétaire restent prévus dans la partie Security : clientId est actuellement fourni par la requête. Aucun paiement ni billet n'est généré dans cette partie.

Pour la future partie Paiement : verrouiller la réservation, vérifier PENDING_PAYMENT et dateExpiration > maintenant avant de confirmer. Une réservation expirée ne doit jamais être confirmée : ses places peuvent déjà avoir été reprises. CONFIRMED continue à consommer le stock. Les remboursements devront définir leur propre règle de libération.
