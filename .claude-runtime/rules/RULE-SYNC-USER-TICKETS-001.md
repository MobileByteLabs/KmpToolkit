# RULE-SYNC-USER-TICKETS-001 — Strict Gate Enforcement for /sync-user-tickets

> **Always active during /sync-user-tickets execution.**
> Every gate MUST be explicitly evaluated. No gate may be skipped, soft-passed, or assumed.
> A FAIL on any gate stops execution immediately. User must resolve before continuing.

---

## Trigger

Load when:
- `/sync-user-tickets` is invoked (any flag)
- `/lib-sync cmp-user-tickets` is invoked
- CHECK-TICKETS-003 in `/project-verify` runs

---

## Gate Execution Protocol

**MANDATORY**: Run gates in order. Gate N does not start until Gate N-1 is PASS.

```
GATE 1 → GATE 2 → GATE 3a → GATE 3b → GATE 3c → COMPLETE
  ↓ FAIL     ↓ FAIL    ↓ FAIL     ↓ FAIL     ↓ FAIL
  STOP       STOP      STOP       STOP       STOP
```

Each gate has exactly three outcomes:

| Outcome | Meaning | Action |
|---------|---------|--------|
| ✅ PASS | Check fully satisfied | Continue to next gate |
| ⚡ FIXED | Was wrong — auto-fixed | Continue (show what changed) |
| ❌ FAIL | Cannot auto-fix | STOP — show error block — wait for user |
| ⚠️ WARN | Non-blocking issue | Continue — show warning in summary |

**FAIL = hard stop. No exceptions.**

---

## Gate 1: Gradle Dependency — STRICT

### What MUST be true (all 3 required)

**1.1 — Library entry exists in `libs.versions.toml`**
```
Read: gradle/libs.versions.toml
Find: line containing "kmptoolkit-user-tickets" in [libraries]
```
- FOUND → continue to 1.2
- MISSING → ❌ FAIL (see GATE-1-MISSING block)

**1.2 — Version resolves to exactly 2.1.0**
```
Read the version.ref value from the library entry
Look up that ref key in [versions] section → get resolved version
```
- resolved == "2.1.0" → ✅ PASS
- resolved != "2.1.0" → ⚡ FIXED:
  - If version.ref is a shared key (e.g., "kmptoolkit"): ADD dedicated key `kmptoolkit-user-tickets = "2.1.0"`, update library entry to `version.ref = "kmptoolkit-user-tickets"`. DO NOT remove the shared key.
  - If version.ref is already dedicated: Update the value to "2.1.0"

**1.3 — Dependency declared in build.gradle.kts**
```
Glob: **/build.gradle.kts (exclude build/ directories)
Grep: "kmptoolkit.user.tickets"
```
- FOUND in at least one module → ✅ PASS
- MISSING → ⚡ FIXED: Insert `implementation(libs.kmptoolkit.user.tickets)` in commonMain.dependencies of the feature module that contains the tickets UI (or ask user which module)

**GATE-1-MISSING block:**
```
━━━ GATE 1 FAIL: cmp-user-tickets not in Gradle ━━━━━━━━━━━━━━━━━━━━
  libs.versions.toml: no "kmptoolkit-user-tickets" entry found

  Add to gradle/libs.versions.toml:
    [versions]
    kmptoolkit-user-tickets = "2.1.0"

    [libraries]
    kmptoolkit-user-tickets = { module = "io.github.mobilebytelabs:kmptoolkit-user-tickets", version.ref = "kmptoolkit-user-tickets" }

  Add to your KMP module build.gradle.kts:
    commonMain.dependencies {
        implementation(libs.kmptoolkit.user.tickets)
    }

  Sync Gradle, then re-run /sync-user-tickets.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```
**STOP. Wait for user.**

---

## Gate 2: Supabase Schema — STRICT

### Credential resolution (try in order, fail if all missing)

```
1. Env vars: $SUPABASE_URL + $SUPABASE_ANON_KEY
2. Source grep: FeatureRequestConfig.init(...) → extract supabaseUrl + supabaseAnonKey values
3. AskUserQuestion: "Enter Supabase URL and anon key for schema check"
```

If no credentials after step 3: ❌ FAIL with GATE-2-NO-CREDS block.

### What MUST be true

**2.1 — `user_tickets` table exists and is reachable**
```bash
GET {supabase_url}/rest/v1/user_tickets?limit=0
Headers: apikey: {anon_key}, Authorization: Bearer {anon_key}
```
- HTTP 200 → continue
- HTTP 404 → ❌ FAIL (see GATE-2-NO-TABLE block)
- HTTP 401 → ❌ FAIL (wrong key — see GATE-2-AUTH block)

**2.2 — All schema.columns present in live table**
```
Parse live column list from response headers / OpenAPI
For each column in schema.columns:
  if NOT in live → MISSING → add to delta list
```
- delta list empty → ✅ PASS
- delta list non-empty → ⚡ FIXED: execute ALTER TABLE for each missing column

**ALTER statement for each missing column:**
```sql
ALTER TABLE user_tickets
  ADD COLUMN IF NOT EXISTS {name} {type}
  {DEFAULT {default} if present}
  {NOT NULL if nullable=false};
```
Execute via psql (SUPABASE_DB_URL) or Supabase Management API. Verify by re-fetching.

**2.3 — Related tables exist: `ticket_votes` + `ticket_comments`**
```
For each table in schema.related_tables:
  GET {supabase_url}/rest/v1/{table}?limit=0
```
- HTTP 200 → ✅ PASS
- HTTP 404 → ❌ FAIL (see GATE-2-MISSING-TABLE block — show CREATE TABLE SQL)

**2.4 — RPCs exist: `toggle_vote` + `add_comment`**
```
For each rpc in [toggle_vote, add_comment]:
  POST {supabase_url}/rest/v1/rpc/{rpc} with body {}
  → 200 or 400 = exists ✅
  → 404 = missing ❌
```
- All present → ✅ PASS
- Any missing → ❌ FAIL (see GATE-2-MISSING-RPC block — show CREATE FUNCTION SQL)

**GATE-2-NO-CREDS block:**
```
━━━ GATE 2 FAIL: No Supabase credentials ━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Cannot reach Supabase for schema verification.

  Add to your .env:
    SUPABASE_URL=https://your-ref.supabase.co
    SUPABASE_ANON_KEY=eyJ...

  Or ensure FeatureRequestConfig.init() has real values (not placeholders).
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**GATE-2-NO-TABLE block:**
```
━━━ GATE 2 FAIL: user_tickets table missing ━━━━━━━━━━━━━━━━━━━━━━━━
  Table "user_tickets" not found in your Supabase project.

  Run the base migration in your Supabase SQL editor:
    docs/user-tickets/SETUP.md → Step 2

  Then re-run /sync-user-tickets.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Gate 3a: FeatureRequestConfig.init() — STRICT

**What MUST be true:**

```
Grep: "FeatureRequestConfig.init" in **/*.kt (exclude build/)
```

**3a.1 — init() is called:**
- FOUND → continue to 3a.2
- MISSING → ❌ FAIL (see GATE-3A-MISSING block)

**3a.2 — Not using placeholder values:**
```
Read the init() call block
Check for "YOUR_PROJECT", "YOUR_ANON_KEY", "YOUR_APP_NAME"
```
- No placeholders → ✅ PASS
- Placeholders found → ❌ FAIL: "FeatureRequestConfig.init() has unset placeholder values. Fill in real supabaseUrl, supabaseAnonKey, and productType."

**3a.3 — userId parameter present:**
```
Read the init() call block
Check for "userId" parameter name
```
- Present → ✅ PASS
- Missing → ⚠️ WARN (non-blocking):
  ```
  ⚠️  userId not passed to FeatureRequestConfig.init()
      My Tickets tab and Contact Support private tickets will not work.
      Add: userId = null  (set via auth after login)
  ```

**GATE-3A-MISSING block:**
```
━━━ GATE 3a FAIL: FeatureRequestConfig.init() not found ━━━━━━━━━━━━
  No FeatureRequestConfig.init() call found in source.

  Add to your app initialization (Application.kt / App.kt / KoinModules.init):
    import com.mobilebytelabs.usertickets.config.FeatureRequestConfig

    FeatureRequestConfig.init(
        supabaseUrl     = "https://YOUR_PROJECT.supabase.co",
        supabaseAnonKey = "YOUR_ANON_KEY",
        productType     = "YOUR_APP_NAME",
        userId          = null,
    )

  Then re-run /sync-user-tickets.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Gate 3b: Koin Module — STRICT

```
Grep: "featureRequestModule" in **/*.kt (exclude build/)
```

**3b.1 — Module imported and included:**
- FOUND in startKoin { modules(...) } or in a module that's included there → ✅ PASS
- FOUND but NOT in a modules() call → ❌ FAIL: "featureRequestModule is imported but not installed in Koin. Add it to your startKoin modules list."
- MISSING entirely → ⚡ FIXED: locate startKoin block, insert featureRequestModule + import

---

## Gate 3c: Navigation Destinations — STRICT

**All three destinations MUST be present (no partial pass):**

```
Grep: "featureWishlistDestination" in **/*.kt (exclude build/)
Grep: "createTicketDestination"    in **/*.kt (exclude build/)
Grep: "ticketDetailDestination"    in **/*.kt (exclude build/)
```

For each destination:
- FOUND → ✅
- MISSING → ❌ FAIL (see GATE-3C-MISSING block for that destination)

**If any one is missing:** ❌ FAIL — do not continue.

**GATE-3C-MISSING block:**
```
━━━ GATE 3c FAIL: Navigation destination missing ━━━━━━━━━━━━━━━━━━━
  Missing: {destination_name}

  Add to your NavHost:
    {full destination code with imports}

  All 3 destinations are required:
    featureWishlistDestination(onBackClick, onNavigateToCreateTicket, onNavigateToTicketDetail)
    createTicketDestination(onBackClick)
    ticketDetailDestination(onBackClick)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Final Completion Block

Only shown when ALL gates are PASS or FIXED (no FAILs):

```
╔══════════════════════════════════════════════════════════════════╗
║  /sync-user-tickets — ALL GATES PASSED                          ║
╠══════════════════════════════════════════════════════════════════╣
║  GATE 1   Gradle        {✅ PASS | ⚡ FIXED}                    ║
║  GATE 2   Schema        {✅ PASS | ⚡ FIXED}                    ║
║             user_tickets  {col_count} columns ✅                ║
║             ticket_votes  ✅                                    ║
║             ticket_comments ✅                                  ║
║             toggle_vote RPC ✅  add_comment RPC ✅              ║
║  GATE 3a  Config        {✅ PASS | ⚡ FIXED}   {⚠️ userId warn} ║
║  GATE 3b  Koin          {✅ PASS | ⚡ FIXED}                    ║
║  GATE 3c  Navigation    ✅  All 3 destinations                  ║
╠══════════════════════════════════════════════════════════════════╣
║  cmp-user-tickets v2.1.0 — fully integrated                     ║
╚══════════════════════════════════════════════════════════════════╝
```

If any WARNs: list them below the box.
If any gate was FIXED: show diff of what changed.

---

## --check (Dry Run) Protocol

With `--check`: run all checks, show outcomes — ZERO writes, ZERO SQL.

```
GATE 1  Gradle
  1.1  library entry       ✅ / ❌ MISSING
  1.2  version resolved    ✅ 2.1.0 / ❌ 2.0.5 [WOULD FIX]
  1.3  build.gradle.kts    ✅ / ❌ MISSING [WOULD ADD]

GATE 2  Supabase
  2.1  user_tickets table  ✅ / ❌ MISSING
  2.2  columns (21)        ✅ / ⚡ 2 missing [WOULD ADD: priority, milestone]
  2.3  ticket_votes        ✅ / ❌ MISSING [WOULD SHOW CREATE SQL]
  2.4  ticket_comments     ✅ / ❌ MISSING [WOULD SHOW CREATE SQL]
  2.5  toggle_vote RPC     ✅ / ❌ MISSING
  2.6  add_comment RPC     ✅ / ❌ MISSING

GATE 3a Config
  3a.1  init() called       ✅ / ❌ MISSING
  3a.2  no placeholders     ✅ / ❌ PLACEHOLDER VALUES
  3a.3  userId present      ✅ / ⚠️ MISSING (non-blocking)

GATE 3b Koin
  featureRequestModule     ✅ / ❌ MISSING

GATE 3c Navigation
  featureWishlistDestination  ✅ / ❌ MISSING
  createTicketDestination     ✅ / ❌ MISSING
  ticketDetailDestination     ✅ / ❌ MISSING
```
