# OpenKrishi AI — Scalability

## Goal
Design for elastic growth without promising literal unlimited capacity.

## Scaling Layers
- CDN/edge for static assets
- horizontally scaled stateless API instances
- Redis/Valkey for shared ephemeral state
- PostgreSQL with pooling and measured scaling
- object storage for media
- asynchronous workers for expensive operations
- provider-aware AI orchestration

## Initial Growth Path
### Stage 1
One production API service, managed PostgreSQL, Redis/Valkey, object storage, and monitoring.

### Stage 2
Horizontal API scaling, worker processes, queue-backed vision/voice tasks, CDN, stronger rate limiting.

### Stage 3
Read replicas, workload-specific workers, partitioning where justified, provider routing, regional strategy.

### Stage 4
Selective service extraction only when ownership, load, or deployment independence justifies it.

## Bottlenecks
Monitor:
- AI provider latency/limits
- database connections
- queue depth
- CPU/memory
- object-storage throughput
- network egress
- rate-limit rejections

## Backpressure
When downstream providers are saturated, queue or reject work predictably rather than allowing unbounded retries.

## Capacity Planning
Use measured requests per second, average/peak latency, job duration, media size, database growth, and AI cost per request.

## Cost Controls
Use caching where safe, model routing, image resizing, bounded context, quotas, and asynchronous processing.

## Multi-Region
Do not introduce multi-region complexity until reliability and geographic requirements justify it.
