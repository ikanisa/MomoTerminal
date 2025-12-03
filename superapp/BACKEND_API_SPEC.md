# Generic Super App Backend API Specification

## Overview

This is a domain-agnostic REST API specification that can be adapted for any super app product. All endpoints use generic terminology (entities, collections, actions) that can be renamed per business domain.

## Base URL

```
https://api.example.com/v1
```

## Authentication

All authenticated endpoints require a Bearer token in the Authorization header:

```
Authorization: Bearer <access_token>
```

---

## 1. Authentication Endpoints

### POST /auth/register

Register a new user account.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123",
  "profile": {
    "displayName": "John Doe",
    "metadata": {}
  }
}
```

**Response (201 Created):**
```json
{
  "user": {
    "id": "usr_123abc",
    "email": "user@example.com",
    "profile": {
      "displayName": "John Doe",
      "metadata": {}
    },
    "createdAt": "2025-12-03T18:47:27Z"
  },
  "tokens": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "expiresIn": 3600
  }
}
```

### POST /auth/login

Authenticate and receive access tokens.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Response (200 OK):**
```json
{
  "user": {
    "id": "usr_123abc",
    "email": "user@example.com",
    "profile": {
      "displayName": "John Doe"
    }
  },
  "tokens": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "expiresIn": 3600
  }
}
```

### POST /auth/refresh

Refresh access token using refresh token.

**Request Body:**
```json
{
  "refreshToken": "eyJhbGc..."
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGc...",
  "expiresIn": 3600
}
```

### POST /auth/logout

Invalidate current session.

**Headers:** `Authorization: Bearer <token>`

**Response (204 No Content)**

---

## 2. User Profile Endpoints

### GET /users/me

Get current user profile.

**Headers:** `Authorization: Bearer <token>`

**Response (200 OK):**
```json
{
  "id": "usr_123abc",
  "email": "user@example.com",
  "profile": {
    "displayName": "John Doe",
    "avatarUrl": "https://cdn.example.com/avatars/123.jpg",
    "metadata": {
      "preferences": {},
      "settings": {}
    }
  },
  "createdAt": "2025-12-03T18:47:27Z",
  "updatedAt": "2025-12-03T18:47:27Z"
}
```

### PUT /users/me

Update current user profile.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "profile": {
    "displayName": "Jane Doe",
    "metadata": {
      "preferences": {
        "theme": "dark"
      }
    }
  }
}
```

**Response (200 OK):** Returns updated user object.

---

## 3. Entity Endpoints (Generic Resource)

Entities represent the core domain objects in your app. Rename to match your business domain (e.g., Products, Posts, Items, Tasks).

### GET /entities

Get paginated list of entities with optional filtering.

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**
- `page` (integer, default: 1) - Page number
- `pageSize` (integer, default: 20, max: 100) - Items per page
- `type` (string, optional) - Filter by entity type
- `status` (string, optional) - Filter by status (active, inactive, archived)
- `search` (string, optional) - Search in title and description
- `sortBy` (string, default: createdAt) - Sort field
- `sortOrder` (string, default: desc) - Sort order (asc, desc)

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "ent_456def",
      "type": "typeA",
      "title": "Sample Entity",
      "description": "This is a generic entity description",
      "metadata": {
        "customField1": "value1",
        "customField2": 123
      },
      "status": "active",
      "createdAt": "2025-12-03T18:47:27Z",
      "updatedAt": "2025-12-03T18:47:27Z"
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalPages": 5,
    "totalItems": 100
  }
}
```

### GET /entities/{id}

Get a single entity by ID.

**Headers:** `Authorization: Bearer <token>`

**Response (200 OK):**
```json
{
  "id": "ent_456def",
  "type": "typeA",
  "title": "Sample Entity",
  "description": "Detailed description",
  "metadata": {
    "customField1": "value1"
  },
  "status": "active",
  "createdAt": "2025-12-03T18:47:27Z",
  "updatedAt": "2025-12-03T18:47:27Z"
}
```

### POST /entities

Create a new entity.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "type": "typeA",
  "title": "New Entity",
  "description": "Description here",
  "metadata": {
    "customField": "value"
  },
  "status": "active"
}
```

**Response (201 Created):** Returns created entity object.

### PUT /entities/{id}

Update an existing entity.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "title": "Updated Title",
  "description": "Updated description",
  "metadata": {
    "customField": "newValue"
  },
  "status": "inactive"
}
```

**Response (200 OK):** Returns updated entity object.

### DELETE /entities/{id}

Delete an entity.

**Headers:** `Authorization: Bearer <token>`

**Response (204 No Content)**

---

## 4. Collection Endpoints (Grouping Resource)

Collections represent groups or categories of entities. Rename to match your domain (e.g., Categories, Folders, Playlists).

### GET /collections

Get all collections for the current user.

**Headers:** `Authorization: Bearer <token>`

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "col_789ghi",
      "name": "My Collection",
      "description": "Collection description",
      "entityCount": 15,
      "metadata": {},
      "createdAt": "2025-12-03T18:47:27Z",
      "updatedAt": "2025-12-03T18:47:27Z"
    }
  ]
}
```

### GET /collections/{id}

Get a single collection with its entities.

**Headers:** `Authorization: Bearer <token>`

**Response (200 OK):**
```json
{
  "id": "col_789ghi",
  "name": "My Collection",
  "description": "Collection description",
  "entities": [
    {
      "id": "ent_456def",
      "title": "Entity in collection"
    }
  ],
  "metadata": {},
  "createdAt": "2025-12-03T18:47:27Z"
}
```

### POST /collections

Create a new collection.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "name": "New Collection",
  "description": "Description",
  "entityIds": ["ent_456def", "ent_789ghi"],
  "metadata": {}
}
```

**Response (201 Created):** Returns created collection object.

### DELETE /collections/{id}

Delete a collection.

**Headers:** `Authorization: Bearer <token>`

**Response (204 No Content)**

---

## 5. Action Endpoints (Generic Operations)

Actions represent domain-specific operations that don't fit CRUD patterns. Examples: process, submit, approve, calculate.

### POST /actions/{type}

Execute a generic action.

**Headers:** `Authorization: Bearer <token>`

**Path Parameters:**
- `type` (string) - Action type (e.g., "process", "submit", "calculate")

**Request Body:**
```json
{
  "targetId": "ent_456def",
  "parameters": {
    "param1": "value1",
    "param2": 123
  }
}
```

**Response (200 OK):**
```json
{
  "actionId": "act_012jkl",
  "status": "completed",
  "result": {
    "output": "Action result data"
  },
  "executedAt": "2025-12-03T18:47:27Z"
}
```

---

## Error Responses

All error responses follow this format:

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": {
      "field": "Additional context"
    }
  }
}
```

### Common Error Codes

| Status | Code | Description |
|--------|------|-------------|
| 400 | INVALID_REQUEST | Request validation failed |
| 401 | UNAUTHORIZED | Missing or invalid authentication |
| 403 | FORBIDDEN | Insufficient permissions |
| 404 | NOT_FOUND | Resource not found |
| 409 | CONFLICT | Resource conflict (e.g., duplicate) |
| 429 | RATE_LIMIT_EXCEEDED | Too many requests |
| 500 | INTERNAL_ERROR | Server error |

---

## Rate Limiting

- 1000 requests per hour per user
- 100 requests per minute per IP
- Headers returned:
  - `X-RateLimit-Limit`: Request limit
  - `X-RateLimit-Remaining`: Remaining requests
  - `X-RateLimit-Reset`: Reset timestamp

---

## Pagination

All list endpoints support pagination with these query parameters:
- `page`: Page number (1-indexed)
- `pageSize`: Items per page (max 100)

Response includes pagination metadata:
```json
{
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalPages": 5,
    "totalItems": 100,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

---

## Filtering and Sorting

List endpoints support:
- `search`: Full-text search
- `sortBy`: Field to sort by
- `sortOrder`: `asc` or `desc`
- Custom filters based on entity type

---

## Webhooks (Optional)

Configure webhooks to receive real-time notifications:

**Events:**
- `entity.created`
- `entity.updated`
- `entity.deleted`
- `collection.created`
- `action.completed`

**Webhook Payload:**
```json
{
  "event": "entity.created",
  "timestamp": "2025-12-03T18:47:27Z",
  "data": {
    "id": "ent_456def",
    "type": "typeA"
  }
}
```
