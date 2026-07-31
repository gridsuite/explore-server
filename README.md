# Explore Server

[![Actions Status](https://github.com/gridsuite/explore-server/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/gridsuite/explore-server/actions)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=org.gridsuite%3Aexplore-server&metric=coverage)](https://sonarcloud.io/component_measures?id=org.gridsuite%3Aexplore-server&metric=coverage)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)

## Description

The **explore-server** is a microservice of the [GridSuite](https://github.com/gridsuite) platform. It acts as the **central orchestration façade** for managing all user-facing grid study artifacts — studies, cases, filters, contingency lists, network modifications, parameters, diagram configs, spreadsheet configurations, workspaces, dynamic mappings, and process configs — as well as the **directory tree** that organizes them.

It provides the following capabilities:

- **CRUD orchestration**: delegates create/read/update/delete operations to the appropriate specialist microservice (study-server, case-server, filter-server, etc.) while simultaneously registering or removing the corresponding metadata entry in the **directory-server**.
- **Transactional safety with rollback**: when creating an element fails at registration time, the service performs an automatic compensating rollback on the specialist server.
- **Authorization enforcement**: every mutating endpoint is protected by Spring Security's `@PreAuthorize`, delegating permission checks to the directory-server's permission API.
- **User quota enforcement**: before creating cases or studies, it checks user quotas (max allowed cases) via user-admin-server and optionally emits threshold-warning notifications.
- **Directory navigation**: full directory tree operations — root directories, subdirectories, element path resolution, name-collision detection, element search via Elasticsearch (proxied to directory-server), and element moves.
- **Event notifications**: publishes RabbitMQ messages whenever elements or directories are updated.
- **Async element deletion**: element deletions are executed asynchronously via a dedicated thread pool.

> The explore-server has **no local database** — it is a pure orchestrator. All state is owned and persisted by downstream specialist microservices.

---

## Technical Stack

- Spring Boot (Web, Security, Actuator, Cloud Stream)
- RabbitMQ via Spring Cloud Stream
- RestTemplate / RestClient (HTTP communication with downstream services)
- API documentation: OpenAPI / Swagger (`springdoc`)
- Micrometer / Prometheus

---

## Development Scripts

Build Docker image

```shell
mvn install -DskipTests -Dpowsybl.docker.install
```

---

## Interactions with Other Microservices

```
┌──────────────────────┐
│   explore-server     │──► directory-server             (element & directory registry, permissions, search)
│                      │──► study-server                 (create, duplicate, delete studies)
│                      │──► case-server                  (upload, persist, duplicate, delete cases)
│                      │──► filter-server                (create, update, duplicate, delete filters)
│                      │──► actions-server               (create, update, duplicate, delete contingency lists)
│                      │──► network-modification-server  (composite modifications CRUD)
│                      │──► loadflow-server              (loadflow parameters CRUD)
│                      │──► security-analysis-server     (security analysis parameters CRUD)
│                      │──► voltage-init-server          (voltage init parameters CRUD)
│                      │──► sensitivity-analysis-server  (sensitivity analysis parameters CRUD)
│                      │──► shortcircuit-server          (short-circuit parameters CRUD)
│                      │──► study-config-server          (workspaces & network-visualizations parameters CRUD)
│                      │──► single-line-diagram-server   (diagram config CRUD)
│                      │──► spreadsheet-config-server    (spreadsheet config & collection CRUD)
│                      │──► dynamic-mapping-server       (dynamic simulation mapping CRUD)
│                      │──► monitor-server               (process config CRUD)
│                      │──► pcc-min-server               (PCC-min parameters CRUD)
│                      │──► network-conversion-server    (case import parameters, case conversion, export/download)
│                      │──► user-admin-server            (user quota checks)
│                      │──► user-identity-server         (resolve user display names)
└──────────────────────┘
         ▼
      RabbitMQ (directory.update / element.update)
```

---

## Notification Events

The service **only publishes** messages — it does not consume any.

| Binding | Destination topic | Description |
|---|---|---|
| `publishDirectoryUpdate-out-0` | `directory.update` | Directory-level update events and user-targeted alert messages (e.g. case quota threshold reached). |
| `publishElementUpdate-out-0` | `element.update` | Element-level update events (e.g. spreadsheet config collection updated). |

---

## Authorization Model

All write operations require the caller's `userId` (forwarded by the gateway via HTTP header) to have the appropriate permission on the target directory or element. Permissions are stored and enforced by the **directory-server**; the explore-server simply proxies the check before delegating to specialist services.

The `SupervisionController` exposes a `DELETE` endpoint (`/v1/supervision/explore/elements`) for maintenance purposes. It has no `@PreAuthorize` check and bypasses ownership/permission checks entirely at the explore-server level. It is **not reachable through the API gateway**: a global gateway filter (`SupervisionAccessControlFilter`) blocks every `/v<n>/supervision/**` path with a 403 Forbidden response, for every downstream service. As a result, this endpoint can only be invoked by calling explore-server directly, bypassing the gateway.

---

## Rollback Pattern

When creating a new element, the service follows a two-phase approach:

1. Create the element in the specialist microservice (e.g. filter-server).
2. Register the element's UUID and metadata in the directory-server.

If step 2 fails, the service automatically calls the specialist service's delete endpoint as a compensating action, ensuring no orphaned elements are left behind.
