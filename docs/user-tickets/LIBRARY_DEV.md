# cmp-product-tickets — Library Development Guide

> For consuming-app integration, see [SETUP.md](SETUP.md) or [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md).
> This guide is for contributors developing the library itself.

---

## Overview

`cmp-product-tickets` is a Kotlin Multiplatform library inside the `kmp-toolkit` monorepo. When the framework's tickets capability adds new Supabase columns (via `layers/tickets/templates/migrations/*.sql`), the library's Kotlin models must be updated to match.

The framework tracks this parity state as `CMP_SCHEMA_PARITY` in `TICKETS_MATRIX.yaml`:
- `FAIL` — migration added new columns not yet in library
- `PASS` — library models all framework columns

---

## When to Develop

The `/tickets` matrix signals when library work is needed:

```
[C] CMP sync    ← library sub-state A: "Library: needs dev [C]"
```

This appears when `CMP_SCHEMA_PARITY = FAIL` (a new migration was deployed but Kotlin models haven't been updated yet).

---

## AI-Assisted Development Pipeline

From the framework's `/tickets` command:

```
/tickets
→ [C] CMP sync
→ [G] Generate code   (or [A] Auto-apply)
```

This triggers the **LIBRARY DEVELOPMENT MODE** — a 7-step auto-dev pipeline:

| Step | Action | File(s) |
|------|--------|---------|
| 1 | Parse `SCHEMA_CONTRACT.yaml` diff | framework |
| 2 | Read SOURCE_MAP (exact Kotlin file paths) | sync-product-tickets.md |
| 3 | Edit `UserTicket.kt` — add missing fields | domain/model/ |
| 4 | Edit `UserTicketInsert.kt` — user-submittable only | domain/model/ |
| 5 | Edit `UserTicketDto.kt` — add `@SerialName` fields | data/remote/dto/ |
| 6 | **Test gate** — KMP compile (iOS + Android) | MANDATORY |
| 7 | Prompt release if tests pass | — |

**Step 6 is mandatory.** If compilation fails, the pipeline stops and shows the error. No release prompt until tests pass.

```bash
# What step 6 runs:
./gradlew :cmp-product-tickets:compileKotlinIosArm64 \
          :cmp-product-tickets:compileKotlinAndroid --quiet
```

---

## Manual Development

If you prefer to develop manually:

### 1 — Identify what changed

```bash
# See which columns are in SCHEMA_CONTRACT but not in library
/tickets → [C] CMP sync
```

Or compare directly:

- **Framework schema**: `layers/tickets/templates/SCHEMA_CONTRACT.yaml` → `base_schema.columns[]`
- **Library schema**: `sync-product-tickets.md` → `schema.columns[]`

### 2 — Update Kotlin Models

#### SOURCE_MAP — Exact File Paths

| Model File | Path |
|------------|------|
| `UserTicket.kt` | `cmp-product-tickets/src/commonMain/kotlin/com/mobilebytelabs/producttickets/domain/model/UserTicket.kt` |
| `UserTicketInsert.kt` | `cmp-product-tickets/src/commonMain/kotlin/com/mobilebytelabs/producttickets/domain/model/UserTicketInsert.kt` |
| `UserTicketDto.kt` | `cmp-product-tickets/src/commonMain/kotlin/com/mobilebytelabs/producttickets/data/remote/dto/UserTicketDto.kt` |
| `TicketRepository.kt` | `cmp-product-tickets/src/commonMain/kotlin/com/mobilebytelabs/producttickets/domain/repository/TicketRepository.kt` |

#### Column Classification

| Column | UserTicket | UserTicketInsert | UserTicketDto |
|--------|:----------:|:----------------:|:-------------:|
| `id` | ✅ | ❌ (server-gen) | ✅ |
| `ticket_type` | ✅ | ✅ | ✅ |
| `title` | ✅ | ✅ | ✅ |
| `description` | ✅ | ✅ | ✅ |
| `category` | ✅ | ✅ | ✅ |
| `status` | ✅ | ❌ (admin-only) | ✅ |
| `priority` | ✅ | ✅ | ✅ |
| `platform` | ✅ | ✅ | ✅ |
| `app_version` | ✅ | ✅ | ✅ |
| `milestone` | ✅ | ❌ (admin-only) | ✅ |
| `labels` | ✅ | ✅ | ✅ |
| `attachments` | ✅ | ✅ | ✅ |
| `is_private` | ✅ | ✅ | ✅ |
| `user_id` | ✅ | ✅ | ✅ |
| `user_email` | ✅ | ✅ | ✅ |
| `device_info` | ✅ | ✅ | ✅ |
| `upvotes` | ✅ | ❌ (admin-only) | ✅ |
| `admin_response` | ✅ | ❌ (admin-only) | ✅ |
| `responded_at` | ✅ | ❌ (admin-only) | ✅ |
| `severity` | ✅ | ✅ | ✅ |
| `resolution` | ✅ | ❌ (admin-only) | ✅ |
| `created_at` | ✅ | ❌ (server-gen) | ✅ |
| `updated_at` | ✅ | ❌ (server-gen) | ✅ |

**Rule**: `UserTicketInsert` only includes fields a user can submit. Admin-only and server-generated fields are excluded.

### 3 — Update Sync Contract

After editing Kotlin files, update `sync-product-tickets.md`:
1. Add new columns to `schema.columns[]`
2. Bump `version:` (patch for new fields, minor for new behavior, major for breaking changes)

This is enforced by `RULE-LIB-EVOLVE-TICKETS-001` — every library change must update the sync contract in the same session.

### 4 — Compile and Test

```bash
# KMP compile (iOS + Android)
./gradlew :cmp-product-tickets:compileKotlinIosArm64 \
          :cmp-product-tickets:compileKotlinAndroid

# Run tests
./gradlew :cmp-product-tickets:allTests
```

### 5 — Release

Use the `/tickets` matrix release flow:

```
/tickets → [L] Release lib
```

Or run from the framework:

```
/tickets release cmp-product-tickets {version}
```

The release gate:
1. Confirms all tests pass
2. Bumps `build.gradle.kts` version
3. Auto-updates `layers/tickets/config.yaml: library.latest_version` (E2E-008)
4. Publishes to Maven (via `./gradlew :cmp-product-tickets:publishAllPublicationsToMavenCentral`)
5. Updates `CMP_SCHEMA_PARITY → PASS` in `TICKETS_MATRIX.yaml`

---

## Version Changelog

| Version | Date | Changes |
|---------|------|---------|
| 3.0.0 | 2026-04-22 | **BREAKING**: Renamed from `cmp-user-tickets`. `product_type` removed (per-project isolation). Table: `user_tickets` → `product_tickets`. Config: `FeatureRequestConfig` → `ProductTicketsConfig` (no `productType` param). DI: `featureRequestModule` → `productTicketsModule`. Nav: `featureWishlistDestination` → `productTicketsDestination`. Added: `platform`, `app_version`, `severity` columns. |
| 2.1.0 | 2026-03-15 | Added `toggle_vote` RPC + `ticket_votes` table. Added `add_comment` RPC + `ticket_comments` table. Added `priority`, `milestone`, `labels`, `attachments` columns. |
| 2.0.0 | 2026-02-01 | Initial `cmp-user-tickets` release. Multi-app support via `product_type`. |

---

## Related Rules

| Rule | When | Purpose |
|------|------|---------|
| `RULE-LIB-EVOLVE-TICKETS-001` | After any Write/Edit on library src | Auto-update sync contract |
| `RULE-FW-TICKETS-CMP-001` | After migration Write | Gate 0: update SCHEMA_CONTRACT + matrix signal |
| `RULE-FW-TICKETS-PARITY-001` | After SCHEMA_CONTRACT change | Enforce base schema parity |
| `RULE-SYNC-USER-TICKETS-001` | During `/sync-product-tickets` | Strict 5-gate enforcement |

---

## Quick Reference

```bash
# Check what needs development
/tickets → [C] CMP sync

# Auto-develop + test + release
/tickets → [C] CMP sync → [A] Auto-apply

# Manual compile check
./gradlew :cmp-product-tickets:compileKotlinIosArm64 :cmp-product-tickets:compileKotlinAndroid

# Full test run
./gradlew :cmp-product-tickets:allTests

# Release
/tickets → [L] Release lib
```
