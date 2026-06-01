# EventHub API Specification

В этой папке хранится API-спецификация проекта для импорта в Postman.

| Файл | Назначение |
| --- | --- |
| `EventHub.postman_collection.json` | Postman collection с запросами ко всем основным endpoint'ам |

## Импорт

1. Откройте Postman.
2. Нажмите `Import`.
3. Выберите файл `docs/api/EventHub.postman_collection.json`.
4. Проверьте переменную `base_url`.

## Переменные Коллекции

| Переменная | Значение по умолчанию   | Назначение |
| --- |-------------------------| --- |
| `base_url` | `http://localhost:8080` | базовый URL приложения |
| `session_id` | -                       | cookie `X-Session-Id` |
| `user_id` | -                       | id пользователя для запросов `/users/{id}` |
| `event_id` | -                       | id мероприятия |
| `review_id` | -                       | id отзыва |

Часть запросов содержит test scripts, которые автоматически сохраняют `session_id`, `user_id`, `event_id` и `review_id` после успешных ответов.
