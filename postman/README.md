# Utilisateurs — Partie 1

## Démarrage

1. Démarrer Docker Desktop, puis `docker compose up -d postgres` depuis le projet.
2. Lancer `./gradlew.bat bootRun` (Java 17).
3. Importer `EventPro-Utilisateurs.postman_collection.json` dans Postman.
4. Exécuter les requêtes dans l'ordre, ou utiliser Collection Runner. `baseUrl` vaut `http://localhost:8080`. La collection génère un email unique et mémorise `userId` automatiquement.

Swagger : http://localhost:8080/swagger-ui/index.html

| Méthode | URL | Résultat |
| --- | --- | --- |
| POST | /api/utilisateurs/register | 201 |
| GET | /api/utilisateurs | 200 |
| GET | /api/utilisateurs/{id} | 200 ou 404 |
| PUT | /api/utilisateurs/{id} | 200, 400, 404 ou 409 |
| DELETE | /api/utilisateurs/{id} | 204, 404 ou 409 si données liées |

Inscription :
```json
{"nom":"Client Test","email":"client@example.com","motDePasse":"Secret123!"}
```

Modification (nom et email obligatoires) :
```json
{"nom":"Nouveau nom","email":"nouveau@example.com"}
```

Le mot de passe est haché avec BCrypt et absent des réponses. Minimum : 8 caractères ; maximum : 72 octets UTF-8. Les emails sont enregistrés en minuscules ; les doublons, même avec une casse différente, renvoient 409. Un email mal formé ou un champ obligatoire absent renvoie 400. La vérification concerne le format et l'unicité, pas une confirmation par email.

Les tests automatisés utilisent H2 en mémoire et ne modifient pas PostgreSQL. Commande : `./gradlew.bat build`. Les contraintes spécifiques à PostgreSQL et les essais manuels Swagger/Postman restent à vérifier sur l'application démarrée.

Cette étape ne met pas encore en place l'authentification et les autorisations (partie 5). L'inscription crée uniquement un Client.

Si PostgreSQL contient déjà des utilisateurs, l'ajout du champ obligatoire `mot_de_passe` nécessite une migration des anciens comptes (réinitialisation du mot de passe). Ne pas supprimer les données existantes pour contourner cette migration.
