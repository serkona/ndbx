.DEFAULT_GOAL = run

COMPOSE = docker compose --env-file .env.local
GRADLE_IMAGE ?= gradle:8.5-jdk17
GRADLE_CMD ?= docker run --rm -v "$(CURDIR):/home/gradle/src" -w /home/gradle/src $(GRADLE_IMAGE) gradle --no-daemon

ifneq (,$(wildcard .env.local))
include .env.local
export
endif

APP_HOST ?= localhost
APP_PORT ?= 8080
BASE_HOST ?= $(if $(filter 0.0.0.0,$(APP_HOST)),localhost,$(APP_HOST))
BASE_URL ?= http://$(BASE_HOST):$(APP_PORT)

.PHONY: help
help:
	@printf "EventHub Make targets:\n"
	@printf "  make run              Start all services in detached mode\n"
	@printf "  make rund             Start all services with logs attached\n"
	@printf "  make rebuild          Recreate containers and rebuild images\n"
	@printf "  make restart          Restart the application container\n"
	@printf "  make stop             Stop all services\n"
	@printf "  make clean            Stop all services and remove volumes\n"
	@printf "  make services         Show service statuses\n"
	@printf "  make logs             Follow logs for all services\n"
	@printf "  make logs-app         Follow application logs\n"
	@printf "  make health           Call the /health endpoint\n"
	@printf "  make build            Run Gradle clean build in Docker\n"
	@printf "  make test             Run Gradle tests in Docker\n"
	@printf "  make bootjar          Build Spring Boot jar in Docker\n"
	@printf "  make shell-app        Open shell in the app container\n"
	@printf "  make shell-redis      Open shell in the Redis container\n"
	@printf "  make shell-mongo      Open shell in the mongos container\n"
	@printf "  make shell-cassandra  Open shell in the Cassandra container\n"
	@printf "  make shell-neo4j      Open shell in the Neo4j container\n"

# Runs all services in detached mode.
.PHONY: run up
run up:
	$(COMPOSE) up -d --build

# Runs all services without detached mode (for debugging).
.PHONY: rund
rund:
	$(COMPOSE) up --build

# Recreates containers and rebuilds images.
.PHONY: rebuild
rebuild:
	$(COMPOSE) down
	$(COMPOSE) up -d --build --force-recreate

# Restarts only the application container.
.PHONY: restart
restart:
	$(COMPOSE) restart app

# Shows all service statuses.
.PHONY: services ps
services ps:
	$(COMPOSE) ps

# Shows logs for all services.
.PHONY: logs
logs:
	$(COMPOSE) logs -f --tail=200

# Shows logs for the application service.
.PHONY: logs-app
logs-app:
	$(COMPOSE) logs -f --tail=200 app

# Calls the application health-check endpoint.
.PHONY: health
health:
	curl -fsS $(BASE_URL)/health
	@printf "\n"

# Runs a full local Gradle build.
.PHONY: build
build:
	$(GRADLE_CMD) clean build

# Runs local tests.
.PHONY: test
test:
	$(GRADLE_CMD) test

# Builds the Spring Boot jar.
.PHONY: bootjar
bootjar:
	$(GRADLE_CMD) bootJar

# Opens shells in containers.
.PHONY: shell-app shell-redis shell-mongo shell-cassandra shell-neo4j
shell-app:
	$(COMPOSE) exec app sh

shell-redis:
	$(COMPOSE) exec redis sh

shell-mongo:
	$(COMPOSE) exec mongos sh

shell-cassandra:
	$(COMPOSE) exec cassandra bash

shell-neo4j:
	$(COMPOSE) exec neo4j bash

# Stops all running services.
.PHONY: stop
stop:
	$(COMPOSE) down

# Cleans up all resources including volumes.
.PHONY: clean
clean:
	$(COMPOSE) down -v
