# OpenKrishi AI — Database Design

## Goals
Provide a durable, queryable source of truth for application data while keeping AI workloads and large media objects separate.

## Recommended Stores
### PostgreSQL
Primary transactional store for:
- users and profiles
- advisory requests/results metadata
- feedback
- usage and quota records
- audit events
- provider/model metadata
- notification preferences

### Redis/Valkey
Ephemeral/high-speed data:
- rate-limit counters
- short-lived caches
- distributed locks
- job coordination
- temporary session state where required

### Object Storage
Store:
- crop images
- audio uploads
- generated audio
- other large binary artifacts

Database records should contain object identifiers and metadata, not large binary payloads.

### Firebase
Firebase Authentication may remain the initial identity provider. If Firestore is retained, define explicit ownership boundaries so PostgreSQL and Firestore do not become competing sources of truth.

## Data Ownership
Each entity should have one authoritative store. Cross-store references use stable IDs.

## Core Entities
- users
- advisory_requests
- advisory_results
- vision_analyses
- voice_jobs
- weather_snapshots
- feedback
- usage_events
- audit_events

## Security
Use least-privilege database roles, encrypted connections, private networking where available, parameterized queries, migrations, backups, and tested restore procedures.

## Privacy
Store only data required for product functionality, safety, debugging, billing/quota enforcement, or legal obligations. Sensitive content should have explicit retention rules.

## Migrations
All schema changes must be versioned, reviewed, backward-compatible where practical, and tested before production rollout.

## Scaling
Start with one managed PostgreSQL instance and connection pooling. Add read replicas, partitioning, or specialized stores only when measured workload requires them.
