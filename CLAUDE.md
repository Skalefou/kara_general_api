# 🚀 Projet Kara API — Directives de développement Backend

## 🎯 Objectif principal

Code **qualité pro et maintenable**, standards industrie, API Spring Boot entreprise. Respect strict **Architecture Hexagonale (Ports & Adapters)**.

## 🛠️ Stack technique

| Composant        | Choix                                                           |
|------------------|-----------------------------------------------------------------|
| Langage          | **Kotlin** (JVM Target 25)                                      |
| Runtime          | **Eclipse Temurin 25**                                          |
| Framework        | **Spring Boot 4.1.0**                                           |
| Build            | **Gradle 8+ (Kotlin DSL)**                                      |
| Base de données  | **PostgreSQL 18.4**                                             |
| Requêtes SQL     | **`NamedParameterJdbcTemplate`** — SQL écrit en dur, sans ORM  |
| DDL (dev)        | **Hibernate** (`ddl-auto=create`) — génération depuis `@Entity` |
| DDL (prod)       | **`init.sql`** — script SQL versionné et maîtrisé              |
| Sécurité         | Spring Security 7 + **JWT (RS256)**                             |
| Temps réel       | **Spring WebSocket** (STOMP)                                    |
| Paiement         | **Stripe SDK**                                                  |
| Emails           | Spring Mail + **Resend**                                        |
| Notifications    | **Firebase Cloud Messaging (FCM)**                              |
| Cache            | **Redis**                                                       |
| Scheduler        | Spring Scheduler (notifications rappels)                        |
| Tests            | JUnit 5 + MockK + Testcontainers                                |
| Documentation    | **SpringDoc OpenAPI 3**                                         |

---

## 🏗️ Architecture obligatoire : Architecture Hexagonale

### Principes fondamentaux (IMPÉRATIF)

- **Hexagone = Domaine pur** : cœur métier connaît **aucune** dépendance Spring, JPA, HTTP, librairie infra
- **Ports** : interfaces dans domaine, jamais dans infra
  - **Ports primaires (input)** : ce que l'app *offre* (use cases)
  - **Ports secondaires (output)** : ce que l'app *demande* (repos, services externes)
- **Adaptateurs** : implémentations concrètes branchées sur ports
  - **Adaptateurs primaires (driving)** : REST, WebSocket — *appellent* ports input
  - **Adaptateurs secondaires (driven)** : JPA, FCM, Stripe — *implémentent* ports output
- **Dependency Rule** : dépendances pointent **toujours vers l'intérieur** (infrastructure, application, domain). Jamais l'inverse.
- **Testabilité** : 100 % domaine et application testable sans Spring context (MockK suffit)

### Structure des packages (IMPÉRATIF)

```
src/
└── main/
    └── kotlin/
        └── com/kara/api/
            │
            ├── domain/                          # ① HEXAGONE — aucune dépendance externe
            │   ├── model/                       # Entités, Agrégats, Value Objects
            │   │   ├── user/
            │   │   │   ├── User.kt
            │   │   │   ├── UserRole.kt          # Enum : GUEST, CLIENT, SERVER, ADMIN
            │   │   │   └── vo/
            │   │   │       ├── Email.kt         # Value Object validé
            │   │   │       ├── PhoneNumber.kt
            │   │   │       └── HashedPassword.kt
            │   │   ├── room/
            │   │   │   ├── Room.kt              # Agrégat Salle
            │   │   │   ├── RoomService.kt       # Service (liste des services dispo)
            │   │   │   ├── Decoration.kt        # Enum : SIMPLE, BIRTHDAY, HALLOWEEN, CLASSE
            │   │   │   └── RoomStatus.kt        # Enum : OPEN, CLOSED
            │   │   ├── booking/
            │   │   │   ├── Booking.kt           # Agrégat Réservation
            │   │   │   ├── BookingStatus.kt     # PENDING, CONFIRMED, CANCELLED, ACTIVE, COMPLETED
            │   │   │   ├── TimeSlot.kt          # Value Object : startAt + endAt
            │   │   │   └── Participant.kt       # Invité dans la réservation
            │   │   ├── payment/
            │   │   │   ├── Payment.kt           # Agrégat Paiement
            │   │   │   ├── PaymentMethod.kt     # CB sauvegardée
            │   │   │   ├── Pool.kt              # Cagnotte partagée
            │   │   │   ├── PoolShare.kt         # Part d'un participant
            │   │   │   ├── PaymentStatus.kt     # PENDING, PAID, REFUNDED, FAILED
            │   │   │   └── Invoice.kt           # Facture
            │   │   ├── order/
            │   │   │   ├── Order.kt             # Commande en cours de réservation
            │   │   │   ├── OrderItem.kt
            │   │   │   └── OrderStatus.kt       # PENDING, PREPARING, DELIVERED
            │   │   ├── product/
            │   │   │   ├── Product.kt           # Catalogue produit local à une salle
            │   │   │   └── Stock.kt
            │   │   └── chat/
            │   │       ├── ChatMessage.kt
            │   │       └── ChatRoom.kt
            │   │
            │   ├── port/
            │   │   ├── input/                   # Ports primaires = Use Case interfaces
            │   │   │   ├── auth/
            │   │   │   │   ├── RegisterUseCase.kt
            │   │   │   │   ├── LoginUseCase.kt
            │   │   │   │   └── VerifyEmailUseCase.kt
            │   │   │   ├── user/
            │   │   │   │   ├── GetProfileUseCase.kt
            │   │   │   │   ├── UpdateProfileUseCase.kt
            │   │   │   │   ├── DeleteAccountUseCase.kt
            │   │   │   │   ├── ManagePaymentMethodUseCase.kt
            │   │   │   │   └── GetInvoiceHistoryUseCase.kt
            │   │   │   ├── room/
            │   │   │   │   ├── ListRoomsUseCase.kt
            │   │   │   │   ├── GetRoomUseCase.kt
            │   │   │   │   ├── GetRoomAvailabilityUseCase.kt
            │   │   │   │   ├── CreateRoomUseCase.kt
            │   │   │   │   ├── UpdateRoomUseCase.kt
            │   │   │   │   └── CloseRoomUseCase.kt
            │   │   │   ├── booking/
            │   │   │   │   ├── CreateBookingUseCase.kt
            │   │   │   │   ├── GetBookingUseCase.kt
            │   │   │   │   ├── ValidateBookingAccessUseCase.kt
            │   │   │   │   ├── ExtendBookingUseCase.kt
            │   │   │   │   └── TriggerEmergencyUseCase.kt
            │   │   │   ├── payment/
            │   │   │   │   ├── InitiatePaymentUseCase.kt
            │   │   │   │   ├── PayPoolShareUseCase.kt
            │   │   │   │   ├── UpdatePoolShareUseCase.kt
            │   │   │   │   └── GetBookingSummaryUseCase.kt
            │   │   │   ├── order/
            │   │   │   │   ├── PlaceOrderUseCase.kt
            │   │   │   │   ├── UpdateCartUseCase.kt
            │   │   │   │   └── GetOrderUseCase.kt
            │   │   │   ├── stock/
            │   │   │   │   └── ManageStockUseCase.kt
            │   │   │   ├── chat/
            │   │   │   │   ├── SendMessageUseCase.kt
            │   │   │   │   └── GetChatHistoryUseCase.kt
            │   │   │   └── admin/
            │   │   │       ├── CreateServerAccountUseCase.kt
            │   │   │       ├── ListAllAccountsUseCase.kt
            │   │   │       ├── AssignServerToRoomUseCase.kt
            │   │   │       └── DownloadAllInvoicesUseCase.kt
            │   │   │
            │   │   └── output/                  # Ports secondaires = contrats vers l'infra
            │   │       ├── UserRepository.kt
            │   │       ├── RoomRepository.kt
            │   │       ├── BookingRepository.kt
            │   │       ├── PaymentRepository.kt
            │   │       ├── OrderRepository.kt
            │   │       ├── ProductRepository.kt
            │   │       ├── ChatRepository.kt
            │   │       ├── PaymentGateway.kt    # Port Stripe
            │   │       ├── NotificationService.kt  # Port FCM
            │   │       ├── EmailService.kt      # Port email
            │   │       └── TokenService.kt      # Port JWT
            │   │
            │   └── event/                       # Domain Events
            │       ├── BookingConfirmedEvent.kt
            │       ├── PaymentCompletedEvent.kt
            │       ├── PoolFullyPaidEvent.kt
            │       ├── EmergencyTriggeredEvent.kt
            │       ├── OrderPlacedEvent.kt
            │       └── BookingEndingSoonEvent.kt
            │
            ├── application/                     # ② ORCHESTRATION — Spring autorisé, domaine pur non
            │   └── service/                     # Implémentations des ports input
            │       ├── auth/
            │       │   ├── RegisterService.kt
            │       │   ├── LoginService.kt
            │       │   └── VerifyEmailService.kt
            │       ├── user/
            │       │   └── UserProfileService.kt
            │       ├── room/
            │       │   └── RoomService.kt
            │       ├── booking/
            │       │   └── BookingService.kt
            │       ├── payment/
            │       │   └── PaymentService.kt
            │       ├── order/
            │       │   └── OrderService.kt
            │       ├── chat/
            │       │   └── ChatService.kt
            │       ├── notification/
            │       │   └── NotificationSchedulerService.kt
            │       └── admin/
            │           └── AdminService.kt
            │
            └── infrastructure/                  # ③ ADAPTATEURS — toutes les dépendances externes
                ├── adapter/
                │   ├── input/                   # Adaptateurs primaires (driving)
                │   │   ├── rest/                # Contrôleurs HTTP
                │   │   │   ├── auth/
                │   │   │   │   └── AuthController.kt
                │   │   │   ├── user/
                │   │   │   │   └── UserController.kt
                │   │   │   ├── room/
                │   │   │   │   └── RoomController.kt
                │   │   │   ├── booking/
                │   │   │   │   └── BookingController.kt
                │   │   │   ├── payment/
                │   │   │   │   └── PaymentController.kt
                │   │   │   ├── order/
                │   │   │   │   └── OrderController.kt
                │   │   │   ├── chat/
                │   │   │   │   └── ChatController.kt
                │   │   │   └── admin/
                │   │   │       └── AdminController.kt
                │   │   └── websocket/           # Adaptateurs WebSocket (STOMP)
                │   │       ├── ChatWebSocketHandler.kt
                │   │       └── EmergencyWebSocketHandler.kt
                │   │
                │   └── output/                  # Adaptateurs secondaires (driven)
                │       ├── persistence/         # JDBC — SQL écrit en dur, zéro ORM
                │       │   ├── user/
                │       │   │   ├── UserEntity.kt           # @Entity Hibernate (DDL dev uniquement)
                │       │   │   ├── UserRowMapper.kt        # RowMapper<User> → ResultSet → domain
                │       │   │   └── UserRepositoryAdapter.kt # Implémente UserRepository (JDBC)
                │       │   ├── room/
                │       │   │   ├── RoomEntity.kt
                │       │   │   ├── RoomRowMapper.kt
                │       │   │   └── RoomRepositoryAdapter.kt
                │       │   ├── booking/
                │       │   │   ├── BookingEntity.kt
                │       │   │   ├── BookingRowMapper.kt
                │       │   │   └── BookingRepositoryAdapter.kt
                │       │   ├── payment/
                │       │   │   ├── PaymentEntity.kt
                │       │   │   ├── PaymentRowMapper.kt
                │       │   │   └── PaymentRepositoryAdapter.kt
                │       │   ├── order/
                │       │   │   ├── OrderEntity.kt
                │       │   │   ├── OrderRowMapper.kt
                │       │   │   └── OrderRepositoryAdapter.kt
                │       │   └── chat/
                │       │       ├── ChatMessageEntity.kt
                │       │       ├── ChatMessageRowMapper.kt
                │       │       └── ChatRepositoryAdapter.kt
                │       ├── payment/
                │       │   ├── StripePaymentAdapter.kt  # Implémente PaymentGateway
                │       │   └── dto/
                │       │       ├── StripePaymentIntentDto.kt
                │       │       └── StripeWebhookEvent.kt
                │       ├── messaging/
                │       │   ├── FcmNotificationAdapter.kt
                │       │   └── ResendEmailAdapter.kt
                │       └── security/
                │           └── JwtTokenAdapter.kt
                │
                └── config/                      # Configuration Spring
                    ├── SecurityConfig.kt
                    ├── WebSocketConfig.kt
                    ├── JdbcConfig.kt            # DataSource + NamedParameterJdbcTemplate
                    ├── HibernateDevConfig.kt    # ddl-auto=create (profil "dev" uniquement)
                    ├── RedisConfig.kt
                    ├── OpenApiConfig.kt
                    └── SchedulerConfig.kt
```

---

## 📚 Ressources architecture obligatoires

**Architecture Hexagonale (Alistair Cockburn) :**
https://alistair.cockburn.us/hexagonal-architecture/

**Ports & Adapters in Spring Boot :**
https://www.baeldung.com/hexagonal-architecture-ddd-spring

**Domain-Driven Design (Eric Evans) :**
https://martinfowler.com/bliki/DomainDrivenDesign.html

**Spring Boot 4.x Official Docs :**
https://docs.spring.io/spring-boot/docs/4.1.0/reference/html/

---

## 🎨 Domaine métier — Kara API

### Contexte

Kara = plateforme location salles de fête à l'heure.
API expose fonctionnalités pour **deux apps front** :

- **App A** (clients) : invités, clients — réservation, paiement, événement live
- **App B** (staff)  : serveurs, admins — gestion opérationnelle

**Rôles (UserRole) :**

| Rôle          | Description                             |
|---------------|-----------------------------------------|
| `GUEST`       | Non authentifié — accès lecture seule   |
| `CLIENT`      | Utilisateur authentifié côté client     |
| `SERVER`      | Serveur rattaché à une salle            |
| `ADMIN`       | Administrateur de la plateforme         |

---

## 🗂️ Use Cases par domaine

### 1. Authentification & Comptes (`auth/`)

| Use Case                     | Acteur           | Description                                                                 |
|------------------------------|------------------|-----------------------------------------------------------------------------|
| `RegisterUseCase`            | GUEST            | Inscription obligatoire avant de valider une réservation                    |
| `LoginUseCase`               | CLIENT/SERVER/ADMIN | Authentification JWT (email + password)                                  |
| `VerifyEmailUseCase`         | CLIENT           | Validation de l'email via un lien envoyé par mail                           |
| `CreateServerAccountUseCase` | ADMIN            | Crée un compte SERVER avec un mot de passe temporaire de 32 caractères, valable 24h, envoyé par email |
| `ForceChangePasswordUseCase` | SERVER (first login) | Oblige le changement de mot de passe à la première connexion            |

**Règles sécurité mot de passe :**
- Min 32 caractères
- Min 1 majuscule, 1 minuscule, 1 chiffre, 1 caractère spécial (`!`, `*`, `&`, `$`, `€`, …)
- Mot de passe temporaire invalide après 24h
- Nouvelle invitation invalide l'ancienne

---

### 2. Catalogue Salles (`room/`)

| Use Case                     | Acteur      | Description                                                                     |
|------------------------------|-------------|---------------------------------------------------------------------------------|
| `ListRoomsUseCase`           | GUEST/CLIENT | Liste toutes les salles disponibles (vue carte)                                |
| `GetRoomUseCase`             | GUEST/CLIENT | Détail d'une salle : images, prix/heure/personne, capacité max, adresse, description, services |
| `GetRoomAvailabilityUseCase` | GUEST/CLIENT | Calendrier de disponibilité (créneaux libres/bloqués par date)                 |
| `CreateRoomUseCase`          | ADMIN        | Crée une nouvelle salle avec ses services et décorations                        |
| `UpdateRoomUseCase`          | ADMIN        | Modifie les informations d'une salle                                            |
| `CloseRoomUseCase`           | ADMIN        | Ferme une salle (plus de nouvelles réservations)                               |
| `ListAllRoomsUseCase`        | ADMIN        | Voir le détail complet de toutes les salles                                     |

**Modèle Room :**
```kotlin
data class Room(
    val id: RoomId,
    val name: String,
    val description: String,
    val address: Address,           // Value Object
    val pricePerPersonPerHour: Money, // Value Object
    val maxCapacity: Int,
    val images: List<ImageUrl>,
    val availableServices: Set<RoomService>, // FOOD, DRINK, SECURITY, DECORATION, WIFI
    val availableDecorations: Set<Decoration>, // SIMPLE, BIRTHDAY, HALLOWEEN, CLASSE
    val status: RoomStatus          // OPEN, CLOSED
)
```

---

### 3. Réservation (`booking/`)

| Use Case                      | Acteur        | Description                                                                                   |
|-------------------------------|---------------|-----------------------------------------------------------------------------------------------|
| `CreateBookingUseCase`        | CLIENT        | Crée une réservation : date, heure d'arrivée/départ, services, décoration, nb personnes, mode paiement |
| `GetBookingUseCase`           | CLIENT        | Récapitulatif complet d'une réservation (accessible via lien partageable)                     |
| `ValidateBookingAccessUseCase`| SERVER        | Vérifie qu'un client présente une réservation valide (face à face)                           |
| `ExtendBookingUseCase`        | CLIENT        | Prolonge la durée si aucune réservation suivante dans le créneau                             |
| `TriggerEmergencyUseCase`     | CLIENT        | Déclenche une alerte urgence : notifie le SERVER rattaché à la salle instantanément          |
| `ListServerBookingsUseCase`   | SERVER        | Liste toutes les réservations dont le serveur est responsable (agenda)                       |
| `ListAllBookingsUseCase`      | ADMIN         | Consulte toutes les réservations de la plateforme                                             |

**Règles métier critiques :**
- Min 2 personnes par réservation, max capacité salle
- Créneaux indisponibles bloqués en lecture (`GetRoomAvailabilityUseCase`)
- Réservation passe `PENDING` → `CONFIRMED` uniquement quand totalité paiement reçue
- Réservation `ACTIVE` s'ouvre à l'heure prévue, ferme auto à fin créneau

---

### 4. Paiement & Cagnotte (`payment/`)

| Use Case                    | Acteur  | Description                                                                                  |
|-----------------------------|---------|----------------------------------------------------------------------------------------------|
| `InitiatePaymentUseCase`    | CLIENT  | Initie un PaymentIntent Stripe (mode solo) ; propose de sauvegarder le moyen de paiement    |
| `GetBookingSummaryUseCase`  | CLIENT  | Récapitulatif complet avant paiement : prix horaire × nb personnes × nb heures              |
| `CreatePoolUseCase`         | CLIENT  | Crée une cagnotte, définit la part de chaque participant, génère un lien partageable         |
| `PayPoolShareUseCase`       | CLIENT  | Un participant paie sa part via Stripe                                                        |
| `UpdatePoolShareUseCase`    | CLIENT  | Le créateur modifie la part d'un participant (uniquement si celui-ci n'a pas encore payé)   |
| `GetPoolStatusUseCase`      | CLIENT  | Récapitulatif cagnotte : parts, montants payés, restants                                     |
| `ManagePaymentMethodUseCase`| CLIENT  | Ajoute, liste ou supprime une CB sauvegardée                                                 |
| `GetInvoiceHistoryUseCase`  | CLIENT  | Liste toutes les factures du client                                                           |
| `DownloadAllInvoicesUseCase`| ADMIN   | Télécharge l'ensemble des factures de la plateforme                                          |

**Règles métier critiques :**
- Mode cagnotte : **aucun prélèvement** tant que tous n'ont pas payé leur part
- Cagnotte non complétée dans délai imparti → réservation annulée, **zéro prélèvement**
- Facture émise pour **chaque** paiement individuel
- Email confirmation (date, heure, lieu) envoyé une fois paiement total reçu
- MVP : **seul paiement CB disponible**

---

### 5. Événement Live (`event/`)

| Use Case                        | Acteur  | Description                                                                      |
|---------------------------------|---------|----------------------------------------------------------------------------------|
| `PlaceOrderUseCase`             | CLIENT  | Commande un produit pendant une réservation active ; prélèvement automatique si CB enregistrée |
| `UpdateCartUseCase`             | CLIENT  | Ajoute, retire ou modifie la quantité d'un produit dans le panier               |
| `GetOrderUseCase`               | CLIENT  | Consulte l'état d'une commande                                                   |
| `NotifyOrderToServerUseCase`    | SYSTEM  | Notifie le serveur d'une nouvelle commande (vibration + son)                    |
| `SendMessageUseCase`            | CLIENT/SERVER | Envoie un message dans le chat de la réservation                           |
| `GetChatHistoryUseCase`         | CLIENT/SERVER/ADMIN | Consulte l'historique du chat                                         |
| `RequestUberUseCase`            | CLIENT  | Suggestion Uber (deep link vers l'app Uber avec adresse de la salle)            |

**Règles métier critiques :**
- Chat **lisible** 30 min après fin réservation, mais **plus modifiable**
- Chaque message affiche avertissement : « l'administration Kara a accès aux messages »
- Alerte urgence déclenche vibration + son fort côté SERVER (WebSocket push)
- Alerte commande déclenche vibration + son côté SERVER (WebSocket push)

---

### 6. Notifications (`notification/`)

Notifications push (FCM) **planifiées automatiquement** à confirmation réservation.

| Déclencheur                         | Délai avant réservation | Destinataire |
|-------------------------------------|-------------------------|--------------|
| Rappel J-3                          | 3 jours                 | CLIENT       |
| Rappel J-1                          | 1 jour                  | CLIENT       |
| Rappel H-6                          | 6 heures                | CLIENT       |
| Rappel H-2                          | 2 heures                | CLIENT       |
| Fin imminente (10 min)              | 10 minutes avant fin    | CLIENT       |
| Fin imminente (2 min)               | 2 minutes avant fin     | CLIENT       |
| Urgence client                      | Immédiat                | SERVER       |
| Nouvelle commande                   | Immédiat                | SERVER       |

---

### 7. Profil Utilisateur (`user/`)

| Use Case                    | Acteur        | Description                                                   |
|-----------------------------|---------------|---------------------------------------------------------------|
| `GetProfileUseCase`         | CLIENT/SERVER/ADMIN | Consulte le profil : nom, prénom, photo, tel, email, adresse, date d'anniversaire |
| `UpdateProfileUseCase`      | CLIENT/SERVER/ADMIN | Met à jour les informations du profil                    |
| `DeleteAccountUseCase`      | CLIENT        | Suppression RGPD du compte et des données personnelles        |
| `LogoutUseCase`             | CLIENT/SERVER/ADMIN | Révoque le JWT (invalidation côté Redis)                |
| `ChangePasswordUseCase`     | SERVER/ADMIN  | Modification sécurisée du mot de passe                       |

---

### 8. Stock & Catalogue produit (`stock/`)

| Use Case                   | Acteur        | Description                                                   |
|----------------------------|---------------|---------------------------------------------------------------|
| `ManageStockUseCase`       | SERVER/ADMIN  | Gérer le catalogue produit local à une salle (CRUD)          |

---

### 9. Administration (`admin/`)

| Use Case                      | Acteur | Description                                              |
|-------------------------------|--------|----------------------------------------------------------|
| `CreateServerAccountUseCase`  | ADMIN  | Crée un compte SERVER (voir règles mot de passe)        |
| `ListAllAccountsUseCase`      | ADMIN  | Visualise les informations de tous les comptes           |
| `AssignServerToRoomUseCase`   | ADMIN  | Édite l'agenda et l'affectation des serveurs aux salles  |
| `ListAllChatsUseCase`         | ADMIN  | Consulte tous les chats de toutes les réservations       |

---


### Réinitialisation du mot de passe via code (OTP)
- Accessible depuis écran connexion (non authentifié) et écran profil (authentifié, email pré-rempli non modifiable)
- `POST /api/v1/auth/forgot-password` `{ email }` : envoie code par email, répond toujours 204 même si email inconnu (anti-énumération)
- `POST /api/v1/auth/reset-password` `{ email, code, newPassword }` : valide code et remplace mot de passe (204 succès, 400 code invalide/expiré ou mot de passe non conforme, 404 compte introuvable)
- Politique mot de passe pour **cette route uniquement** : min 8 caractères, une lettre et un chiffre — **différente** de la politique des comptes créés par admin (32 caractères min, majuscule, minuscule, chiffre, caractère spécial, cf. section "Comptes serveur" ci-dessus, inchangée)
- Utilisateurs classiques (client) : une lettre, un chiffre, une majuscule, min 8 caractères


## 🔌 Contrats API (REST)

### Conventions globales

- **Base path** : `/api/v1`
- **Format** : JSON (`Content-Type: application/json`)
- **Auth** : `Authorization: Bearer <JWT>` (sauf routes publiques)
- **Pagination** : `?page=0&size=20` (Spring Pageable)
- **Erreurs** : RFC 9457 Problem Details (`application/problem+json`)
- **Nommage** : `kebab-case` pour les URLs, `camelCase` pour les champs JSON

```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/logout
POST   /api/v1/auth/verify-email
POST   /api/v1/auth/change-password

GET    /api/v1/users/me
PATCH  /api/v1/users/me
DELETE /api/v1/users/me
GET    /api/v1/users/me/invoices
GET    /api/v1/users/me/payment-methods
POST   /api/v1/users/me/payment-methods
DELETE /api/v1/users/me/payment-methods/{id}

GET    /api/v1/rooms                          (GUEST/CLIENT)
GET    /api/v1/rooms/{id}                     (GUEST/CLIENT)
GET    /api/v1/rooms/{id}/availability        (GUEST/CLIENT)
POST   /api/v1/rooms                          (ADMIN)
PATCH  /api/v1/rooms/{id}                     (ADMIN)
DELETE /api/v1/rooms/{id}                     (ADMIN)
GET    /api/v1/rooms/{id}/stock               (SERVER/ADMIN)
PUT    /api/v1/rooms/{id}/stock               (SERVER/ADMIN)

POST   /api/v1/bookings
GET    /api/v1/bookings/{id}
GET    /api/v1/bookings/{id}/summary
POST   /api/v1/bookings/{id}/validate-access  (SERVER)
POST   /api/v1/bookings/{id}/emergency        (CLIENT)
POST   /api/v1/bookings/{id}/extend           (CLIENT)
GET    /api/v1/bookings                       (SERVER/ADMIN)

POST   /api/v1/bookings/{id}/payments
GET    /api/v1/bookings/{id}/pool
POST   /api/v1/bookings/{id}/pool/pay
PATCH  /api/v1/bookings/{id}/pool/shares      (CLIENT créateur)

POST   /api/v1/bookings/{id}/orders
GET    /api/v1/bookings/{id}/orders/{orderId}
PATCH  /api/v1/bookings/{id}/cart

GET    /api/v1/bookings/{id}/chat
POST   /api/v1/bookings/{id}/chat

GET    /api/v1/admin/accounts
POST   /api/v1/admin/accounts/server
GET    /api/v1/admin/invoices/export
GET    /api/v1/admin/chats
```

### WebSocket (STOMP)

```
/ws                              # Point de connexion WebSocket
/topic/booking/{id}/chat         # Messages du chat (subscribe)
/topic/booking/{id}/emergency    # Alerte urgence (subscribe — SERVER)
/topic/booking/{id}/order        # Nouvelle commande (subscribe — SERVER)
/app/booking/{id}/chat           # Envoyer un message (publish)
/app/booking/{id}/emergency      # Déclencher une urgence (publish)
```

---

## 🔒 Sécurité

### JWT (RS256)

- **Access token** : durée vie 15 min
- **Refresh token** : durée vie 7 jours, stocké Redis, révocable
- **Blacklist** : logout invalide token en Redis (clé `blacklist:<jti>`)
- Jamais secret JWT en clair dans code — utiliser variables d'environnement

### Règles Spring Security

```kotlin
// Exemple de configuration par rôle
http.authorizeHttpRequests { auth ->
    auth
        .requestMatchers("/api/v1/auth/**").permitAll()
        .requestMatchers(GET, "/api/v1/rooms/**").permitAll()
        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
        .requestMatchers("/api/v1/bookings/*/validate-access").hasRole("SERVER")
        .anyRequest().authenticated()
}
```

### Mots de passe

- Hachage : **bcrypt** (cost factor 12)
- Jamais mot de passe en clair en base, log ou réponse API

### RGPD

- `DeleteAccountUseCase` : anonymise données personnelles (soft delete + nullification PII)
- Logs : pas de données personnelles dans logs applicatifs

---

## 🧪 Tests

### Stratégie obligatoire

| Couche              | Type de test              | Outil                  | Couverture cible |
|---------------------|---------------------------|------------------------|------------------|
| Domain              | Tests unitaires purs      | JUnit 5 + MockK        | 100%             |
| Application Service | Tests unitaires           | JUnit 5 + MockK        | 90%+             |
| Adapters REST       | Tests d'intégration slice | `@WebMvcTest`          | Tous les endpoints |
| Persistence         | Tests d'intégration       | Testcontainers (PostgreSQL) | Tous les repos |
| E2E                 | Tests d'intégration full  | `@SpringBootTest` + Testcontainers | Parcours critiques |

### Règles de test

```kotlin
// ✅ Correct : le domain n'a aucune annotation Spring
class BookingServiceTest {
    private val bookingRepository = mockk<BookingRepository>()
    private val notificationService = mockk<NotificationService>()
    private val sut = BookingService(bookingRepository, notificationService)

    @Test
    fun `should throw when booking capacity exceeds room max`() { ... }
}

// ✅ Correct : test de slice REST
@WebMvcTest(BookingController::class)
class BookingControllerTest {
    @MockkBean lateinit var createBookingUseCase: CreateBookingUseCase
    ...
}
```

- Tests suivent convention **Given / When / Then**
- Noms tests en **backtick lisible** : `\`should throw when...\``
- Un test = un comportement, jamais plusieurs assertions non liées

---

## ⚙️ Gradle (Kotlin DSL) — dépendances clés

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    kotlin("plugin.jpa") version "2.1.0"   // Hibernate pour la génération DDL uniquement
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }

dependencies {
    // Spring Web & Sécurité
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Persistance — deux niveaux distincts
    // ① JDBC : toutes les requêtes SQL sont écrites manuellement ici
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    // ② JPA/Hibernate : UNIQUEMENT pour la génération DDL en environnement dev
    //    Aucun JpaRepository ne doit être déclaré dans le code
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Stripe
    implementation("com.stripe:stripe-java:27.3.0")

    // Firebase (FCM)
    implementation("com.google.firebase:firebase-admin:9.4.1")

    // OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.0")

    // Tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}
```

---

## 🗄️ Base de données

### Stratégie DDL : dev vs production (IMPÉRATIF)

Création tables suit **deux stratégies distinctes** selon environnement. **Jamais interchangeables**.

#### Environnement de développement (`application-dev.yml`)

Hibernate génère auto les tables depuis classes `@Entity`. Seul usage JPA autorisé dans ce projet — jamais pour requêtes.

```yaml
# src/main/resources/application-dev.yml
spring:
  jpa:
    hibernate:
      ddl-auto: update          # Met à jour le schéma sans effacer les données existantes
    show-sql: false             # Activer ponctuellement pour debug, jamais en continu
  datasource:
    url: jdbc:postgresql://localhost:5432/kara_dev
```

> `update` (et non `create`) pour que données dev survivent aux redémarrages. Si migration schéma incompatible bloque démarrage (changement type colonne, contrainte non satisfaite par données existantes), réinitialiser manuellement la base locale.

#### Environnement de production (`application-prod.yml`)

Hibernate **désactivé pour DDL**. Schéma géré exclusivement par `init.sql`, versionné dans dépôt, appliqué manuellement ou via pipeline CI/CD.

```yaml
# src/main/resources/application-prod.yml
spring:
  jpa:
    hibernate:
      ddl-auto: none            # Hibernate ne touche JAMAIS au schéma en prod
  sql:
    init:
      mode: never               # init.sql appliqué hors Spring, via CI/CD
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/kara_prod
```

```
src/main/resources/
└── db/
    └── init.sql                # Schéma complet production — source de vérité
```

`init.sql` contient intégralité DDL production :
```sql
-- init.sql (extrait)
CREATE TABLE IF NOT EXISTS users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS rooms (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(255) NOT NULL,
    price_per_person_per_hour NUMERIC(10,2) NOT NULL,
    max_capacity            INT NOT NULL,
    status                  VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- ... (une table par agrégat)
```

---

### Requêtes SQL — `NamedParameterJdbcTemplate` (IMPÉRATIF)

**Aucun `JpaRepository`, `CrudRepository` ou méthode dérivée Spring Data autorisé dans ce projet.** Toutes requêtes SQL écrites explicitement dans adaptateurs persistance. Garantit contrôle total sur type et performance de chaque requête envoyée à PostgreSQL.

#### Structure d'un adaptateur JDBC

```kotlin
// ✅ infrastructure/adapter/output/persistence/booking/BookingRepositoryAdapter.kt
@Component
class BookingRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val rowMapper: BookingRowMapper
) : BookingRepository {                            // Port secondaire du domain

    override fun findById(id: BookingId): Booking? {
        val sql = """
            SELECT b.*, r.name AS room_name, r.price_per_person_per_hour
            FROM bookings b
            JOIN rooms r ON r.id = b.room_id
            WHERE b.id = :id
        """.trimIndent()
        return jdbc.query(sql, mapOf("id" to id.value), rowMapper).firstOrNull()
    }

    override fun findActiveByRoomId(roomId: RoomId): List<Booking> {
        val sql = """
            SELECT * FROM bookings
            WHERE room_id = :roomId
              AND status = 'ACTIVE'
            ORDER BY start_at ASC
        """.trimIndent()
        return jdbc.query(sql, mapOf("roomId" to roomId.value), rowMapper)
    }

    override fun save(booking: Booking): Booking {
        val sql = """
            INSERT INTO bookings (id, room_id, client_id, start_at, end_at, status,
                                  participant_count, created_at)
            VALUES (:id, :roomId, :clientId, :startAt, :endAt, :status,
                    :participantCount, NOW())
            ON CONFLICT (id) DO UPDATE SET
                status           = EXCLUDED.status,
                end_at           = EXCLUDED.end_at,
                participant_count = EXCLUDED.participant_count
        """.trimIndent()
        jdbc.update(sql, MapSqlParameterSource()
            .addValue("id",               booking.id.value)
            .addValue("roomId",           booking.roomId.value)
            .addValue("clientId",         booking.clientId.value)
            .addValue("startAt",          booking.slot.startAt)
            .addValue("endAt",            booking.slot.endAt)
            .addValue("status",           booking.status.name)
            .addValue("participantCount", booking.participantCount)
        )
        return booking
    }

    override fun existsOverlappingSlot(roomId: RoomId, slot: TimeSlot): Boolean {
        val sql = """
            SELECT COUNT(*) FROM bookings
            WHERE room_id = :roomId
              AND status NOT IN ('CANCELLED')
              AND start_at < :endAt
              AND end_at   > :startAt
        """.trimIndent()
        val count = jdbc.queryForObject(sql, mapOf(
            "roomId"  to roomId.value,
            "startAt" to slot.startAt,
            "endAt"   to slot.endAt
        ), Int::class.java) ?: 0
        return count > 0
    }
}
```

#### Structure d'un RowMapper

```kotlin
// ✅ infrastructure/adapter/output/persistence/booking/BookingRowMapper.kt
@Component
class BookingRowMapper : RowMapper<Booking> {
    override fun mapRow(rs: ResultSet, rowNum: Int): Booking =
        Booking(
            id             = BookingId(rs.getObject("id", UUID::class.java)),
            roomId         = RoomId(rs.getObject("room_id", UUID::class.java)),
            clientId       = UserId(rs.getObject("client_id", UUID::class.java)),
            slot           = TimeSlot(
                startAt = rs.getTimestamp("start_at").toInstant(),
                endAt   = rs.getTimestamp("end_at").toInstant()
            ),
            status         = BookingStatus.valueOf(rs.getString("status")),
            participantCount = rs.getInt("participant_count")
        )
}
```

#### Structure d'une entité Hibernate (DDL dev uniquement)

```kotlin
// ✅ infrastructure/adapter/output/persistence/booking/BookingEntity.kt
// Rôle UNIQUE : permettre à Hibernate de générer le DDL en dev (ddl-auto=create)
// Cette classe n'est JAMAIS instanciée dans le code applicatif
@Entity
@Table(name = "bookings")
class BookingEntity(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "room_id", nullable = false, columnDefinition = "uuid")
    val roomId: UUID,

    @Column(name = "client_id", nullable = false, columnDefinition = "uuid")
    val clientId: UUID,

    @Column(name = "start_at", nullable = false)
    val startAt: Instant,

    @Column(name = "end_at", nullable = false)
    val endAt: Instant,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: BookingStatusEntity,

    @Column(name = "participant_count", nullable = false)
    val participantCount: Int,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
```

### Règles SQL impératives

- **Jamais** `SELECT *` en production — lister explicitement colonnes nécessaires
- **Toujours** paramètres nommés (`:param`) — jamais concaténation de chaînes
- Requêtes complexes (jointures multiples, agrégations) **commentées** en tête de méthode
- Colonnes sensibles (`password_hash`, `stripe_customer_id`) **jamais** loguées
- Transactions déclarées au niveau service applicatif (`@Transactional`), pas dans adaptateurs

---

## 📏 Conventions de code Kotlin

### Règles impératives

1. **Data classes pour les Value Objects et les DTO**
   ```kotlin
   // Value Object dans le domain
   @JvmInline value class Email(val value: String) {
       init { require(value.contains('@')) { "Email invalide" } }
   }
   ```

2. **`sealed class` / `sealed interface` pour les résultats métier**
   ```kotlin
   sealed interface BookingResult {
       data class Success(val booking: Booking) : BookingResult
       data class RoomUnavailable(val slots: List<TimeSlot>) : BookingResult
       data class CapacityExceeded(val max: Int) : BookingResult
   }
   ```

3. **Pas d'exceptions pour flux métier** — retourner types résultat
   - Exceptions réservées aux **erreurs techniques** inattendues

4. **Extension functions** pour enrichir modèles sans les polluer
   ```kotlin
   fun Booking.totalPrice(): Money =
       room.pricePerPersonPerHour * participants.size * duration.toHours()
   ```

5. **`companion object` pour les factories**
   ```kotlin
   data class Booking private constructor(...) {
       companion object {
           fun create(room: Room, client: User, slot: TimeSlot, participants: Int): BookingResult = ...
       }
   }
   ```

6. **Nullabilité explicite** — jamais `!!` hors tests

7. **`suspend fun` pour appels I/O** si Coroutines activées (optionnel MVP)

### Nommage

| Élément             | Convention           | Exemple                       |
|---------------------|----------------------|-------------------------------|
| Interface port input | `XxxUseCase`        | `CreateBookingUseCase`        |
| Interface port output | `XxxRepository`   | `BookingRepository`           |
| Service applicatif  | `XxxService`         | `BookingService`              |
| Adaptateur REST     | `XxxController`      | `BookingController`           |
| Adaptateur JPA      | `XxxRepositoryAdapter` | `BookingRepositoryAdapter`  |
| Entité JPA          | `XxxEntity`          | `BookingEntity`               |
| DTO REST (request)  | `XxxRequest`         | `CreateBookingRequest`        |
| DTO REST (response) | `XxxResponse`        | `BookingResponse`             |
| Domain Event        | `XxxEvent`           | `BookingConfirmedEvent`       |

---

## 🌍 Internationalisation des erreurs

- Messages d'erreur API en **français** pour affichage utilisateur
- Logs applicatifs en **anglais**
- Codes d'erreur en `SCREAMING_SNAKE_CASE` : `ROOM_NOT_FOUND`, `SLOT_UNAVAILABLE`

```json
{
  "type": "https://kara.api/errors/room-not-found",
  "title": "Salle introuvable",
  "status": 404,
  "detail": "La salle avec l'identifiant abc123 n'existe pas.",
  "code": "ROOM_NOT_FOUND"
}
```

---

## ⚙️ CI/CD

- Tests automatiques sur chaque PR (`./gradlew test`)
- Linting strict (`./gradlew ktlintCheck`)
- Build image Docker (`./gradlew bootBuildImage`)
- Couverture test minimale : **80% globale, 100% domain**

## 📝 Avant chaque commit

```bash
./gradlew ktlintFormat
./gradlew test
./gradlew detekt
./gradlew bootJar
```

---

## 🎯 Philosophie générale

**"Le domain ignore tout de Spring. Spring sait tout du domain."**

Privilégiez :
- ✅ Ports & Adapters > couplage direct
- ✅ Domain pur > annotations partout
- ✅ Types résultat > exceptions métier
- ✅ Value Objects > primitives nues
- ✅ Immutabilité > mutation d'état
- ✅ Tests unitaires > tests d'intégration pour la logique métier
- ✅ SQL explicite > requêtes générées automatiquement
- ✅ `init.sql` versionné > DDL géré par le framework en production

**En cas de doute : si ça compile dans le module `domain/` sans Spring sur le classpath, c'est bon.**

**Sur le SQL : si tu ne peux pas lire la requête envoyée à PostgreSQL, c'est qu'elle ne doit pas exister.**

---

## 📋 Checklist pour chaque nouvelle fonctionnalité

- [ ] Port input (use case interface) défini dans `domain/port/input/`
- [ ] Port output (repository/service) défini dans `domain/port/output/`
- [ ] Entité/Agrégat défini dans `domain/model/`
- [ ] Service applicatif dans `application/service/`
- [ ] Contrôleur REST dans `infrastructure/adapter/input/rest/`
- [ ] Adaptateur JDBC dans `infrastructure/adapter/output/persistence/` (avec `NamedParameterJdbcTemplate`)
- [ ] `@Entity` Hibernate créée (DDL dev) + table ajoutée dans `init.sql` (prod)
- [ ] Toutes les requêtes SQL écrites en dur — aucun `JpaRepository` déclaré
- [ ] `RowMapper` défini pour chaque entité persistée
- [ ] Tests unitaires du domain (MockK, sans Spring)
- [ ] Tests d'intégration REST (`@WebMvcTest`)
- [ ] Tests d'intégration JDBC (Testcontainers PostgreSQL 18.4)
- [ ] Endpoint documenté OpenAPI (`@Operation`, `@ApiResponse`)
- [ ] Sécurité vérifiée (rôles sur le contrôleur)
- [ ] Gestion d'erreurs avec Problem Details
- [ ] Pas de logique métier dans le contrôleur
- [ ] Pas de dépendance Spring dans `domain/`

---

## 🤝 Règles de collaboration

### Commits (Conventional Commits)

```
feat(booking): add pool payment use case
fix(auth): fix JWT expiry validation
refactor(room): extract availability port to domain
test(payment): add stripe adapter integration test
chore(db): add pool_shares table to init.sql and BookingEntity
chore(db): add BookingEntity for hibernate dev DDL generation
```

### Pull Requests

- Titre descriptif (`feat/fix/refactor/test/chore`)
- Description changements + impact architecture
- Tests passent en CI
- Aucune régression sur domain pur
- Code review requise avant merge

---

## 📞 Support et questions

En cas de doute sur architecture ou bonnes pratiques :
1. Consulter ce `CLAUDE.md`
2. Référencer documentation officielle Spring Boot 4.x
3. Appliquer règle de dépendance : **le domain ne connaît rien de l'extérieur**
4. Demander review de code

**Rappelez-vous : l'hexagone protège le métier. Tout ce qui change (framework, BDD, API tierce) reste à l'extérieur.**
