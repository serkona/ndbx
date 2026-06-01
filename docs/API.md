# EventHub API

Краткая документация backend-сервиса EventHub с акцентом на HTTP API. Проект сделан на Spring Boot и демонстрирует работу нескольких NoSQL-хранилищ в одной системе мероприятий.

## 1. Обзор проекта

| Часть системы | Назначение | Хранилище |
| --- | --- | --- |
| Users, Events | пользователи, мероприятия, поиск и фильтры | MongoDB |
| Sessions, Cache | сессии, агрегаты реакций/отзывов, кэш рекомендаций | Redis |
| Reactions, Reviews | лайки, дизлайки, отзывы, рейтинги | Cassandra |
| Recommendations | граф пользовательских интересов | Neo4j |

Запуск:

```bash
make run
```

Основные команды `Makefile`:

| Команда | Что делает |
| --- | --- |
| `make run` | запускает все сервисы в фоне |
| `make rund` | запускает сервисы с логами в текущем терминале |
| `make services` | показывает статус контейнеров |
| `make stop` | останавливает контейнеры |
| `make clean` | останавливает контейнеры и удаляет volumes |

Базовый адрес зависит от `APP_HOST` и `APP_PORT` из `.env.local`.

```text
http://APP_HOST:APP_PORT
```

Postman collection:

| Файл | Назначение |
| --- | --- |
| `docs/api/EventHub.postman_collection.json` | готовая коллекция запросов для импорта в Postman |

Основные переменные коллекции: `base_url`, `session_id`, `user_id`, `event_id`, `review_id`. Запросы регистрации, логина, создания события и создания отзыва автоматически сохраняют нужные значения в переменные коллекции.

## 2. Общие правила API

| Правило | Значение |
| --- | --- |
| Формат | JSON |
| Авторизация | cookie `X-Session-Id` |
| Cookie | `HttpOnly`, path `/`, TTL из `APP_USER_SESSION_TTL` |
| Body-даты | ISO Offset Date Time: `2026-06-01T19:00:00Z` |
| Query-даты | `yyyyMMdd`: `20260601` |
| Пагинация | `limit`, `offset`; по умолчанию `10` и `0` |
| Ошибка валидации | `400 {"message":"invalid \"field\" field"}` |

Авторизованными считаются запросы, где cookie `X-Session-Id` существует в Redis и привязана к пользователю.

## 3. Карта API

| Метод | URL | Доступ | Назначение |
| --- | --- | --- | --- |
| `GET` | `/health` | публичный | health-check |
| `POST` | `/session` | публичный | создать/обновить гостевую сессию |
| `POST` | `/users` | публичный | регистрация |
| `GET` | `/users` | публичный | список или поиск пользователей |
| `GET` | `/users/{id}` | публичный | пользователь по id |
| `POST` | `/auth/login` | публичный | вход |
| `POST` | `/auth/logout` | авторизованный | выход |
| `POST` | `/events` | авторизованный | создать мероприятие |
| `GET` | `/events` | публичный | список или поиск мероприятий |
| `GET` | `/events/{id}` | публичный | мероприятие по id |
| `PATCH` | `/events/{id}` | автор события | обновить мероприятие |
| `GET` | `/users/{id}/events` | публичный | мероприятия пользователя |
| `POST` | `/events/{event_id}/like` | авторизованный | поставить лайк |
| `POST` | `/events/{event_id}/dislike` | авторизованный | поставить дизлайк |
| `POST` | `/events/{event_id}/reviews` | авторизованный | создать отзыв |
| `GET` | `/events/{event_id}/reviews` | публичный | список отзывов |
| `PATCH` | `/events/{event_id}/reviews/{review_id}` | автор отзыва | обновить отзыв |
| `GET` | `/recommendations` | авторизованный | персональные рекомендации |

## 4. Авторизация и сессии

### `POST /session`

Создает гостевую сессию или продлевает существующую.

| Ответ | Когда |
| --- | --- |
| `201 Created` | создана новая cookie `X-Session-Id` |
| `200 OK` | существующая сессия продлена |

### `POST /users`

Регистрирует пользователя и привязывает текущую/новую сессию к нему.

Request:

```json
{
  "full_name": "Ivan Petrov",
  "username": "ivan",
  "password": "secret"
}
```

| Ответ | Когда |
| --- | --- |
| `201 Created` | пользователь создан |
| `409 Conflict` | `{"message":"user already exists"}` |
| `400 Bad Request` | невалидные `full_name`, `username`, `password` |

### `POST /auth/login`

Request:

```json
{
  "username": "ivan",
  "password": "secret"
}
```

| Ответ | Когда |
| --- | --- |
| `204 No Content` | вход выполнен |
| `401 Unauthorized` | `{"message":"invalid credentials"}` |

### `POST /auth/logout`

| Ответ | Когда |
| --- | --- |
| `204 No Content` | сессия удалена |
| `401 Unauthorized` | нет активной авторизованной сессии |

## 5. Пользователи

### `GET /users`

Query-параметры:

| Параметр | Описание |
| --- | --- |
| `id` | точный поиск по id |
| `name` | поиск по `full_name` |
| `limit` | размер выборки |
| `offset` | смещение |

Response:

```json
{
  "users": [
    {
      "id": "665a...",
      "full_name": "Ivan Petrov",
      "username": "ivan"
    }
  ],
  "count": 1
}
```

### `GET /users/{id}`

Возвращает одного пользователя:

```json
{
  "id": "665a...",
  "full_name": "Ivan Petrov",
  "username": "ivan"
}
```

Если пользователь не найден: `404 {"message":"Not found"}`.

## 6. Мероприятия

### Модель события

| Поле | Тип | Комментарий |
| --- | --- | --- |
| `id` | string | MongoDB id |
| `title` | string | название, уникально для создания |
| `description` | string | описание, может быть пустым |
| `category` | string | категория события |
| `price` | number | целая цена `>= 0` |
| `location.city` | string | город |
| `location.address` | string | адрес |
| `created_at` | string | дата создания |
| `created_by` | string | id организатора |
| `started_at` | string | дата начала |
| `finished_at` | string | дата окончания |

### `POST /events`

Создает мероприятие. Требуется авторизованная сессия.

Request:

```json
{
  "title": "NoSQL Meetup",
  "description": "Discussion about NoSQL databases",
  "address": "Kronverksky pr., 49",
  "started_at": "2026-06-01T19:00:00Z",
  "finished_at": "2026-06-01T21:00:00Z"
}
```

Response: `201 {"id":"665a..."}`.

Ошибки: `401` без авторизации, `409` если `title` уже существует, `400` при неверных датах или пустых обязательных полях.

### `PATCH /events/{id}`

Обновляет мероприятие. Доступно только автору события.

Request может содержать:

| Поле | Правило |
| --- | --- |
| `category` | одно из `APP_EVENT_CATEGORIES`, по умолчанию `meetup,concert,exhibition,party,other` |
| `price` | целое число `>= 0` |
| `city` | строка или `null`; `null` удаляет город |

Успех: `204 No Content`.

### `GET /events/{id}`

Возвращает событие. Поддерживает `include`.

```http
GET /events/{id}?include=reactions,reviews
```

| `include` | Дополнительный блок |
| --- | --- |
| `reactions` | `{"likes":12,"dislikes":1}` |
| `reviews` | `{"count":5,"rating":4.6}` |

### `GET /events`

Поиск и фильтрация мероприятий.

| Параметр | Описание |
| --- | --- |
| `id` | точный поиск по id |
| `title` | поиск по названию без учета регистра |
| `category` | фильтр по категории |
| `city` | фильтр по городу |
| `user` | username организатора |
| `price_from`, `price_to` | диапазон цены |
| `date_from`, `date_to` | диапазон дат начала, формат `yyyyMMdd` |
| `limit`, `offset` | пагинация |
| `include` | `reactions`, `reviews` через запятую |

Response:

```json
{
  "events": [
    {
      "id": "665a...",
      "title": "NoSQL Meetup",
      "description": "Discussion",
      "location": {
        "city": "Saint Petersburg",
        "address": "Kronverksky pr., 49"
      },
      "created_at": "2026-05-30T10:00:00Z",
      "created_by": "665b...",
      "started_at": "2026-06-01T19:00:00Z",
      "finished_at": "2026-06-01T21:00:00Z"
    }
  ],
  "count": 1
}
```

### `GET /users/{id}/events`

Возвращает мероприятия конкретного пользователя. Поддерживает те же фильтры, что `GET /events`, кроме `user`: пользователь задается в path.

## 7. Реакции

```http
POST /events/{event_id}/like
POST /events/{event_id}/dislike
```

| Особенность | Описание |
| --- | --- |
| Авторизация | обязательна |
| Хранение | Cassandra, ключ `event_id + user_id` |
| Повторная реакция | заменяет предыдущую |
| Лайки в рекомендациях | лайк также записывается в граф Neo4j |

Ответы:

| Код | Значение |
| --- | --- |
| `204` | реакция сохранена |
| `401` | пользователь не авторизован |
| `404` | мероприятие не найдено |

## 8. Отзывы

### `POST /events/{event_id}/reviews`

Создает отзыв. Один пользователь может оставить один отзыв на одно событие.

Request:

```json
{
  "comment": "Great event",
  "rating": 5
}
```

| Поле | Правило |
| --- | --- |
| `comment` | непустая строка до 300 символов |
| `rating` | целое число от `1` до `5` |

Response: `201 {"id":"b5c16332-7c87-4ff3-80d1-6712dd18a0b4"}`.

### `GET /events/{event_id}/reviews`

Параметры: `limit`, `offset`.

Response:

```json
{
  "reviews": [
    {
      "id": "b5c16332-7c87-4ff3-80d1-6712dd18a0b4",
      "event_id": "665a...",
      "comment": "Great event",
      "created_at": "2026-05-30T10:10:00Z",
      "created_by": "665b...",
      "rating": 5,
      "updated_at": "2026-05-30T10:10:00Z"
    }
  ],
  "count": 1
}
```

### `PATCH /events/{event_id}/reviews/{review_id}`

Обновляет отзыв автора.

Request может содержать `comment`, `rating` или оба поля:

```json
{
  "comment": "Updated comment",
  "rating": 4
}
```

Успех: `204 No Content`. Если событие/отзыв не найден или отзыв принадлежит другому пользователю: `404`.

## 9. Рекомендации

```http
GET /recommendations
```

Требуется авторизованная сессия.

| Шаг | Что происходит |
| --- | --- |
| 1 | пользователь ставит лайки событиям |
| 2 | связь сохраняется в Neo4j |
| 3 | `RecommendationService` получает id рекомендуемых событий |
| 4 | данные событий берутся из MongoDB |
| 5 | результат кэшируется в Redis на `APP_RECOMMENDATIONS_TTL` |

Response:

```json
{
  "events": [
    {
      "id": "665a...",
      "title": "NoSQL Meetup",
      "description": "Discussion",
      "location": {
        "city": "Saint Petersburg",
        "address": "Kronverksky pr., 49"
      },
      "created_at": "2026-05-30T10:00:00Z",
      "created_by": "665b...",
      "started_at": "2026-06-01T19:00:00Z",
      "finished_at": "2026-06-01T21:00:00Z"
    }
  ]
}
```

## 10. Коды ошибок

| Код | Когда возникает | Пример |
| --- | --- | --- |
| `400` | неверное поле или параметр | `{"message":"invalid \"rating\" field"}` |
| `401` | нет сессии или пользователь не вошел | `{"message":"invalid credentials"}` |
| `404` | объект не найден | `{"message":"Not found"}` |
| `409` | конфликт уникальности | `{"message":"Already exists"}` |
