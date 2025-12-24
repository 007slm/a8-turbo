# ojp-cdc-monitor Module

SeaTunnel Event Handler SPI plugin for `ojp-cache` real-time CDC status monitoring.

## Features

Listens for SeaTunnel engine events and pushes CDC status to Redis for `ojp-cache` decision making.

### Listen Events

| Event | Trigger | Behavior |
|:---|:---|:---|
| `LIFECYCLE_READER_CLOSE` | Snapshot Done | Mark `phase=INCREMENTAL`, send `SNAPSHOT_DONE` |
| `CHECKPOINT_COMPLETED` | Checkpoint Done | Update heartbeat, send `CHECKPOINT_OK` |

### Job Context

When receiving an event for a Job for the first time, it queries Job details via **REST API** to get `database/table/connHash` and caches it.

```
GET http://seatunnel-master:8080/job-info/{jobId}
```

## Environment Variables

| Variable | Description | Default |
|:---|:---|:---|
| `OJP_REDIS_HOST` | Redis Host | `redis` |
| `OJP_REDIS_PORT` | Redis Port | `6379` |
| `OJP_REDIS_AUTH` | Redis Password | Empty |
| `OJP_SEATUNNEL_API_URL` | SeaTunnel REST API | `http://seatunnel-master:8080` |

## Deployment

Place the bundled JAR into the SeaTunnel Master `lib` directory.
