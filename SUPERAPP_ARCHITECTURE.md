# Generic Super App Architecture

## Module Structure

### Core Modules
- **:core:common** - Shared utilities, extensions, base classes
- **:core:designsystem** - Material 3 theme, typography, colors, custom components
- **:core:ui** - Reusable UI components, base screens
- **:core:network** - Network layer (Retrofit/Ktor), API clients
- **:core:database** - Room database, DAOs, entities
- **:core:data** - Repository implementations, data sources
- **:core:domain** - Use cases, domain models, repository interfaces

### Feature Modules
- **:feature:auth** - Authentication, user profile
- **:feature:featureA** - Generic feature A (rename per product)
- **:feature:featureB** - Generic feature B (rename per product)
- **:feature:featureC** - Generic feature C (rename per product)

### App Module
- **:app** - Main application, navigation graph, DI setup

## Package Structure

### :app
```
com.superapp/
├── SuperAppApplication.kt
├── di/
│   ├── AppModule.kt
│   └── NavigationModule.kt
├── navigation/
│   ├── AppNavHost.kt
│   ├── NavigationDestination.kt
│   └── NavigationExtensions.kt
└── ui/
    └── SuperAppScreen.kt
```

### :core:common
```
com.superapp.core.common/
├── result/
│   └── Result.kt
├── util/
│   ├── DateTimeUtil.kt
│   └── StringUtil.kt
└── extensions/
    ├── FlowExtensions.kt
    └── ContextExtensions.kt
```

### :core:domain
```
com.superapp.core.domain/
├── model/
│   ├── Entity.kt
│   ├── Collection.kt
│   └── User.kt
├── repository/
│   ├── EntityRepository.kt
│   ├── CollectionRepository.kt
│   └── UserRepository.kt
└── usecase/
    ├── base/
    │   ├── UseCase.kt
    │   └── FlowUseCase.kt
    └── entity/
        ├── GetEntitiesUseCase.kt
        └── GetEntityByIdUseCase.kt
```

### :core:data
```
com.superapp.core.data/
├── repository/
│   ├── EntityRepositoryImpl.kt
│   └── CollectionRepositoryImpl.kt
├── source/
│   ├── local/
│   │   ├── EntityLocalDataSource.kt
│   │   └── CollectionLocalDataSource.kt
│   └── remote/
│       ├── EntityRemoteDataSource.kt
│       └── CollectionRemoteDataSource.kt
└── mapper/
    ├── EntityMapper.kt
    └── CollectionMapper.kt
```

### :feature:featureA
```
com.superapp.feature.featurea/
├── FeatureAScreen.kt
├── FeatureAViewModel.kt
├── FeatureAUiState.kt
├── FeatureAUiEvent.kt
├── components/
│   ├── FeatureAListItem.kt
│   └── FeatureADetailCard.kt
└── navigation/
    └── FeatureANavigation.kt
```

## Backend API Structure

### Generic REST API Endpoints

```
/api/v1/
├── /auth
│   ├── POST /login
│   ├── POST /register
│   ├── POST /refresh
│   └── POST /logout
├── /users
│   ├── GET /me
│   ├── PUT /me
│   └── DELETE /me
├── /entities
│   ├── GET /entities (paginated, filterable)
│   ├── GET /entities/{id}
│   ├── POST /entities
│   ├── PUT /entities/{id}
│   └── DELETE /entities/{id}
├── /collections
│   ├── GET /collections
│   ├── GET /collections/{id}
│   ├── POST /collections
│   └── DELETE /collections/{id}
└── /actions
    └── POST /actions/{type}
```

### Generic JSON Schemas

#### Entity Response
```json
{
  "id": "string",
  "type": "string",
  "title": "string",
  "description": "string",
  "metadata": {
    "key": "value"
  },
  "status": "active|inactive|archived",
  "createdAt": "2025-12-03T18:47:27Z",
  "updatedAt": "2025-12-03T18:47:27Z"
}
```

#### Paginated Response
```json
{
  "data": [],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalPages": 5,
    "totalItems": 100
  }
}
```

#### Error Response
```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable message",
    "details": {}
  }
}
```

## Technology Stack

### Android
- Kotlin 1.9+
- Jetpack Compose + Material 3
- Navigation Compose
- Hilt (Dependency Injection)
- Coroutines + Flow
- Retrofit + OkHttp
- Room Database
- DataStore (Preferences)
- Coil (Image Loading)

### Backend (Technology Agnostic)
- REST or GraphQL
- JWT Authentication
- PostgreSQL/MongoDB
- Redis (Caching)
- S3-compatible storage
