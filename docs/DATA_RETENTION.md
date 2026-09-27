# OpenKrishi AI — Data Retention

## Objective
Retain data only as long as needed for product functionality, safety, security, support, and applicable legal requirements.

## Retention Categories
Define explicit retention periods for:
- account records
- advisory history
- crop images
- voice/audio
- weather snapshots
- operational logs
- audit events
- feedback
- backups

Exact periods should be finalized with product, legal, and privacy requirements before production launch.

## Default Principle
If data no longer has a documented purpose, delete it or anonymize it.

## Media
Images and audio should generally have shorter retention than essential account or transaction metadata unless the user explicitly requests history storage.

## Logs
Operational logs should have bounded retention and should avoid sensitive content.

## Backups
Backups may have a different lifecycle but must still have documented expiration and access controls.

## Deletion
User deletion workflows must remove or anonymize data across primary databases, object storage, caches, search indexes, and applicable backups according to the documented policy.

## Verification
Retention jobs and deletion workflows must be monitored and tested.
