# PrivyChat Operations & Incident Response Manual

## 1. Monitoring & Health Checks

- **Application Health**: `GET /actuator/health`
  - Returns `UP` when PostgreSQL connection and disk space are healthy.
- **Metrics**: `GET /actuator/metrics`
  - Exposes JVM memory, GC pauses, HikariCP pool usage, and active WebSocket connection counts.
- **Structured Redacted Logging**:
  - Log levels set to `INFO` in production.
  - Plaintext messages, OTPs, JWT tokens, and private keys are never logged.

---

## 2. Backup & Disaster Recovery

### Automated PostgreSQL Backup
```powershell
# Take a full logical backup
pg_dump -U privychat_user -h localhost -d privychat -F c -f "privychat_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').dump"
```

### Restoration Procedure
```powershell
# Restore from backup
pg_restore -U privychat_user -h localhost -d privychat -c "privychat_backup_20261029.dump"
```

---

## 3. Incident Playbooks

### Incident 1: Database Migration Failure
1. Inspect migration history:
   ```sql
   SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;
   ```
2. If a migration failed, resolve the underlying SQL error, then run:
   ```powershell
   mvn flyway:repair
   ```
3. Re-run `mvn flyway:migrate`.

### Incident 2: Coturn TURN Relay Degradation
1. Verify Coturn service status: `systemctl status coturn`
2. Inspect TURN allocation rate and CPU usage.
3. Test connectivity with `turnutils_uclient`:
   ```bash
   turnutils_uclient -u privychat_turn_user -w turn_secret turn.privychat.example.com
   ```

### Incident 3: Suspected Compromised User Device
1. User or admin triggers session revocation: `POST /api/v1/auth/revoke-sessions`.
2. All active refresh tokens and user device sessions are invalidated.
3. Android client discards cached Double Ratchet state and generates new identity keys on next login.

