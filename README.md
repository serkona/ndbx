# EventHub

[![EventHub](https://github.com/serkona/ndbx/actions/workflows/eventhub.yml/badge.svg)](https://github.com/serkona/ndbx/actions/workflows/eventhub.yml)
![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-brightgreen)
![Docker](https://img.shields.io/badge/Runtime-Docker-2496ED)

EventHub - backend-сервис платформы мероприятий для практического изучения NoSQL баз данных.

Вся документация проекта находится в папке [docs](docs/):

| Документ | Назначение |
| --- | --- |
| [docs/README.md](docs/README.md) | основная документация проекта |
| [docs/API.md](docs/API.md) | описание HTTP API |
| [docs/api/EventHub.postman_collection.json](docs/api/EventHub.postman_collection.json) | Postman collection |
| [docs/api/README.md](docs/api/README.md) | как импортировать Postman collection |

Быстрый запуск:

```bash
make run
curl http://localhost:8080/health
```
