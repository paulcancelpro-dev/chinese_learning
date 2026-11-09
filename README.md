# Backend d’apprentissage du mandarin

Backend mono-utilisateur Spring Boot **4.1.2**, Java **25**, Maven, PostgreSQL, JPA et Flyway. Les versions du projet initial ont été conservées. Le mode local fonctionne sans clé OpenAI.

## Démarrage

Prérequis : JDK 25, Docker avec Compose (ou une instance PostgreSQL), accès Internet au premier lancement Maven.

```sh
cp .env.example .env
# Adapter les valeurs si nécessaire ; ne jamais versionner .env.
docker compose up -d --wait

# Spring Boot ne charge pas automatiquement le fichier .env.
# Charger uniquement votre fichier local de confiance dans le shell :
set -a
. ./.env
set +a
./mvnw spring-boot:run
```

- Swagger UI : <http://localhost:8080/swagger-ui/index.html>
- Contrat OpenAPI : <http://localhost:8080/v3/api-docs>
- Serveur lié à `127.0.0.1` par défaut. CORS autorise `http://localhost:5173`, configurable avec `FRONTEND_ORIGIN`.
- PostgreSQL est exposé uniquement sur l’interface locale. Son volume `chinese_data` conserve les données après `docker compose down`. **`docker compose down -v` supprime ce volume.**
- Avec PostgreSQL existant, créer une base puis définir `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` ; Compose n’est pas nécessaire.
- Flyway crée le schéma ; Hibernate le valide avec `ddl-auto: validate`. Aucune mise à jour automatique du schéma.

L’API n’a pas d’authentification. Ne pas la publier sur Internet sans authentification, HTTPS et contrôle d’accès, notamment pour les imports et les appels payants. CORS n’est pas un mécanisme d’authentification.

## Premier parcours

```sh
curl -sS -X POST http://localhost:8080/api/vocabulary/import \
  -H 'Content-Type: application/json' --data-binary @examples/vocabulary.json

curl -sS -X POST http://localhost:8080/api/exercises/generate \
  -H 'Content-Type: application/json' \
  -d '{"count":4,"types":["HANZI_TO_FRENCH","HANZI_TO_PINYIN"],"difficulty":1,"strategy":"NEW"}'
```

Exemple de réponse publique (identifiants variables) :

```json
{
  "source": "local",
  "exercises": [{
    "id": "34721e30-9c11-412a-b8eb-e72301e053a8",
    "sessionId": "60d773aa-d636-4317-a217-435e95e055b8",
    "type": "HANZI_TO_FRENCH",
    "question": "朋友",
    "choices": [],
    "difficulty": 1,
    "newVocabulary": []
  }]
}
```

Remplacer l’identifiant par celui renvoyé :

```sh
curl -sS -X POST http://localhost:8080/api/exercises/34721e30-9c11-412a-b8eb-e72301e053a8/answers \
  -H 'Content-Type: application/json' -d '{"answer":"ami"}'
curl -sS http://localhost:8080/api/progress
curl -sS http://localhost:8080/api/progress/due
curl -sS http://localhost:8080/api/statistics/overview
```

La correction contient `exerciseId`, `correct`, `acceptedAnswers`, `explanation`, `answeredAt`. Les réponses attendues et l’explication ne sont jamais incluses dans les DTO publics d’exercices. La réponse à un QCM est **le texte du choix**, pas sa position.

## Import et export du vocabulaire

`POST /api/vocabulary/import` accepte un corps JSON directement (pas un upload multipart) :

```json
[
  {"hanzi":"朋友","pinyin":"péngyou","traductions_fr":["ami","amie"]}
]
```

Ou l’enveloppe `{"vocabulary":[...]}`. Voir `examples/vocabulary.json`.

- Maximum 5 000 entrées par import ; hanzi 100 caractères, pinyin 200, 1 à 30 traductions de 300 caractères maximum.
- Tous les champs sont obligatoires ; les champs inconnus sont refusés.
- Identité : **hanzi + pinyin**. Deux prononciations du même hanzi restent deux entrées. Les traductions d’un même couple sont réunies sans doublons ; importer deux fois le même fichier est sans effet supplémentaire.
- Les espaces en début/fin sont retirés, le pinyin passe en minuscules. Les tons ne sont pas supprimés. `péngyou` et `pengyou` restent distincts.
- L’import valide toutes les entrées avant écriture, puis s’exécute dans une transaction. Une erreur annule tout le lot. Le `ProblemDetail.detail` indique les indices invalides, à partir de 0.
- `GET /api/vocabulary/export` retourne l’enveloppe réimportable, sans identifiants techniques.
- `PUT` remplace les traductions ; l’import les fusionne. `DELETE` supprime aussi la progression, les exercices et les événements liés au mot. Les séances restent conservées.

## Import et export de progression

La progression est **un historique d’événements**, pas un niveau fourni par le client. Format : tableau ou enveloppe `{"progress":[...]}`. Voir `examples/progress.json`.

```json
{
  "progress": [{
    "id":"7e818ffc-4018-4d71-891e-ec3d8fd55fbc",
    "hanzi":"朋友",
    "pinyin":"péngyou",
    "skill":"HANZI_TO_FRENCH",
    "correct":true,
    "reviewedAt":"2026-01-01T10:00:00Z",
    "answer":"ami"
  }]
}
```

Importer le vocabulaire avant la progression. Chaque événement possède un UUID stable : la réimportation du même événement est ignorée ; réutiliser l’UUID avec un contenu différent est une erreur. Les événements sont triés par date puis UUID et rejoués par l’algorithme Java. Les dates futures, les mots inconnus et les événements antérieurs à l’historique déjà enregistré de cette compétence sont refusés. Une erreur annule tout le lot.

Le format ne permet pas d’importer directement `level`, des compteurs ou une date de prochaine révision. Un fichier existant contenant uniquement des niveaux devra être converti en événements vérifiables ; aucun résultat historique n’est inventé. `GET /api/progress/export` produit le format ci-dessus, portable entre bases grâce à hanzi–pinyin. Il exporte aussi les réponses issues des exercices.

## Progression et répétition espacée

Compétences :

| Compétence | Exercices qui l’évaluent |
| --- | --- |
| `HANZI_RECOGNITION` | `PINYIN_TO_HANZI`, `MULTIPLE_CHOICE`, `FILL_IN_THE_BLANK` |
| `HANZI_TO_PINYIN` | `HANZI_TO_PINYIN` |
| `HANZI_TO_FRENCH` | `HANZI_TO_FRENCH`, `SENTENCE_TRANSLATION` |
| `WRITING` | Réservée aux événements importés / évolutions futures |

Un mot importé a un niveau `null` (« non évalué ») pour chaque compétence active, distinct du niveau `0`. Les lignes de progression sont créées lors de la première réponse. Les listes affichent aussi les compétences non évaluées. `WRITING` n’entre pas dans les listes de révision du MVP.

Politique déterministe `RevisionPolicy` :

1. Une réussite incrémente les réussites et la série consécutive. La première évaluation réussie donne le niveau 1. Ensuite, le niveau monte d’un cran à partir de la deuxième réussite consécutive, jusqu’à 5.
2. Une erreur incrémente les erreurs, remet la série à zéro, diminue le niveau d’un cran (minimum 0) et programme une révision dans **10 minutes**.
3. Après réussite, les intervalles de base aux niveaux 0 à 5 sont **0, 1, 3, 7, 14, 30 jours**. L’intervalle est multiplié par `(réussites + 1) / (réussites + erreurs + 1)`, arrondi en heures et borné à un minimum de 12 heures.
4. Les compteurs cumulés influencent donc les intervalles. Même au niveau 5, une nouvelle révision est prévue.

Les mots non évalués passent en premier dans la sélection, puis les échéances les plus anciennes. Stratégies : `NEW` (au moins une compétence demandée non évaluée), `DIFFICULT` (niveau ≤ 2 ou davantage d’erreurs que de réussites), `DUE` (non évalué ou échéance atteinte), `RANDOM`.

Chaque exercice n’évalue que son mot cible et sa compétence, pas les mots incidents d’une phrase. Les niveaux ≥ 4 sont comptés comme maîtrisés dans les statistiques, sans être définitifs. La mesure porte sur les **mots**, pas sur une compétence indépendante de chaque caractère composant un mot.

Une soumission corrige un exercice une seule fois. Même réponse répétée : correction initiale renvoyée, sans nouvelle progression. Réponse différente après correction : HTTP 409. Des verrous PostgreSQL sur l’exercice puis le mot sérialisent les soumissions concurrentes. La réponse, l’événement et la progression sont enregistrés dans une même transaction.

## OpenAI

Activer explicitement :

```sh
export OPENAI_ENABLED=true
export OPENAI_API_KEY='votre-cle-locale'
export OPENAI_MODEL=gpt-4o-mini
./mvnw spring-boot:run
```

Le modèle est configurable ; choisir un modèle disponible sur votre compte qui prend en charge Responses et Structured Outputs. La clé reste côté serveur. Elle n’est jamais renvoyée au client ni journalisée par le code. Les erreurs distantes ne relaient pas le corps brut d’OpenAI.

L’intégration utilise `POST /v1/responses`, `store: false` et `text.format` avec `type: json_schema`, `strict: true`, toutes les propriétés requises et `additionalProperties: false`, conformément à la [documentation officielle Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses).

Le backend sélectionne les mots avant l’appel réseau et ferme la transaction de lecture avant cet appel. L’API distante reçoit les mots, traductions et paramètres de génération. Le JSON est vérifié côté serveur : structure/types, nombre d’exercices, cibles autorisées, types demandés, difficulté, tailles, corrections des exercices simples, unicité des choix et une seule bonne réponse de QCM, vocabulaire nouveau déclaré. Un JSON valide ne garantit pas à lui seul une phrase linguistiquement correcte.

Limites : 1 à 20 exercices, difficulté 1 à 5, jusqu’à 20 mots de contexte (au moins 4 candidats pour les QCM si disponibles), entrée OpenAI ≤ 24 000 caractères, sortie ≤ 6 000 tokens par défaut, délai 30 secondes, une génération simultanée, 5 appels OpenAI/minute/processus par défaut. `OPENAI_MAX_OUTPUT_TOKENS` et `OPENAI_REQUESTS_PER_MINUTE` sont configurables. Aucune relance automatique payante. Le compteur minute se réinitialise au redémarrage : configurer également les limites de budget du projet OpenAI ; ce n’est pas un plafond monétaire global.

Le générateur local couvre `HANZI_TO_FRENCH`, `HANZI_TO_PINYIN`, `PINYIN_TO_HANZI`, `MULTIPLE_CHOICE`. Le QCM exige au moins deux mots de significations distinctes. Les homophones sont désambiguïsés par une traduction française. Sans OpenAI, demander un exercice de phrase ou à trou renvoie HTTP 400.

Avec OpenAI activé, un échec ou une sortie invalide déclenche le secours local uniquement si **tous** les types demandés sont simples. `source` indique `local`, `openai` ou `local_fallback`. Les demandes avancées échouent en HTTP 502 si OpenAI ne peut pas produire un résultat valide. La limite de débit renvoie HTTP 429.

La correction est entièrement Java : normalisation Unicode NFKC, casse, espaces, apostrophe typographique et ponctuation finale. Les tons du pinyin restent significatifs. Pour les traductions de phrases, les variantes acceptées sont préparées lors de la génération. **Pas de correcteur sémantique IA supplémentaire** : une paraphrase correcte absente des variantes peut être refusée. Cela évite un second appel payant et garde la correction reproductible.

## API

| Méthode | Route | Usage |
| --- | --- | --- |
| POST | `/api/vocabulary/import` | Import atomique, compteurs `created`, `merged` |
| GET | `/api/vocabulary` | `q`, `page` (0), `size` (20, maximum 100) |
| GET/PUT/DELETE | `/api/vocabulary/{id}` | Consultation, remplacement, suppression |
| POST | `/api/vocabulary` | Création, HTTP 201 |
| GET | `/api/vocabulary/export` | JSON réimportable |
| POST | `/api/progress/import` | Import d’événements, `created`, `ignored` |
| GET | `/api/progress` | Progression par mot et compétence |
| GET | `/api/progress/due` | Compétences à réviser, non évaluées en tête |
| GET | `/api/progress/export` | Historique réimportable |
| POST | `/api/exercises/generate` | Séance et exercices publics |
| GET | `/api/exercises/{id}` | Exercice sans correction |
| POST | `/api/exercises/{id}/answers` | `{"answer":"..."}`, maximum 2 000 caractères |
| GET | `/api/exercises/history` | Corrections paginées, plus récentes d’abord |
| GET | `/api/statistics/overview` | Mots, compétences évaluées/maîtrisées/dues, résultats |
| GET | `/api/statistics/weak-words` | Compétences évaluées aux niveaux 0–2 |
| GET | `/api/statistics/revisions` | Comptages par jour UTC, de J−7 à J+30 |

Les pages ont les champs stables `content`, `number`, `size`, `totalElements`, `totalPages`. Les erreurs métier renvoient `application/problem+json` avec `status`, `title`, `detail`. HTTP 400 pour entrées invalides, 404 pour ressource absente, 409 pour conflit, 429 pour limite de génération, 502 pour échec OpenAI non récupérable.

## Architecture et persistance

- `vocabulary` : DTO, recherche paginée, import et CRUD.
- `progress` : progression séparée, historique, import/export d’événements.
- `revision` : politique pure Java, horloge injectée dans les services.
- `exercise` : sélection, générateur local, validation, stockage des séances et corrections ; DTO interne distinct du DTO public.
- `ai` : adaptateur HTTP Responses et limite d’appels.
- `statistics` : agrégations du MVP.
- `common` : parsing d’import, pagination et `ProblemDetail`.
- `config` : horloge et origine frontend autorisée.

`V1__initial_schema.sql` définit les clés étrangères, unicités et contraintes de niveaux/compteurs. Les traductions, choix et réponses acceptées utilisent des tables dédiées. Les UUID sont créés côté backend. Aucun secret dans les migrations.

Les imports et statistiques chargent des collections en mémoire : approprié à un vocabulaire personnel, pas à un service massif. La pagination du vocabulaire et de l’historique se fait en base. Pas d’OCR, d’évaluation de l’écriture manuscrite, de gestion de comptes, ni de garantie automatique de qualité linguistique des phrases IA. Le mode local accepte la difficulté demandée comme métadonnée mais ne simule pas une complexité linguistique variable. Modifier un mot ne réécrit pas les corrections des exercices déjà générés, qui sont des instantanés.

## Tests et construction

```sh
./mvnw test
./mvnw package
java -jar target/learning-0.0.1-SNAPSHOT.jar
```

Les tests unitaires et le serveur HTTP OpenAI simulé ne nécessitent aucune clé et ne font aucun appel payant. Avec Docker disponible, les tests Testcontainers démarrent PostgreSQL 17 et exécutent les scénarios complets avec Flyway/JPA/MockMvc. Sans Docker, ces scénarios sont explicitement marqués ignorés.

Alternative sans Docker, sur une **base dédiée jetable** (les tests tronquent toutes les données métier avant chaque scénario) :

```sh
TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/chinese_test \
TEST_DATABASE_USER=chinese TEST_DATABASE_PASSWORD=chinese_dev ./mvnw test
```

Les mêmes scénarios sont exécutés dans `LearningApplicationTests` lorsque `TEST_DATABASE_URL` est défini. Sans cette variable, cette variante est ignorée. La présence de tests ignorés ne signifie donc pas qu’une base a été testée.

Couverture : validation et atomicité d’import, doublons et homographes, CRUD/recherche/export, niveaux et intervalles, correction et normalisation, absence de fuite des réponses, concurrence et idempotence, OpenAPI, JSON OpenAI strict, HTTP 429, refus, sortie incomplète/malformée, timeout et secours local. Les rapports se trouvent dans `target/surefire-reports`.

## Validation effectuée le 9 novembre 2026

- `mvn package` : succès, **35 tests exécutés avec succès**, dont 12 scénarios d’intégration sur PostgreSQL local **18.6**.
- Les 12 mêmes scénarios en variante Testcontainers/PostgreSQL 17 ont été ignorés : Docker absent de l’environnement. Le fichier Compose est fourni mais n’a pas été exécuté ici.
- Test réel du JAR sur une base isolée : import de quatre mots, génération, correction, accès Swagger UI, arrêt/redémarrage, conservation des données et répétition idempotente de la réponse après redémarrage.
- Aucun appel réel à OpenAI : tests exclusivement sur serveur HTTP simulé. La qualité linguistique et l’accès du compte au modèle restent à vérifier avec une clé configurée.
