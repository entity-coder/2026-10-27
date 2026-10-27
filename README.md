# RepairDesk Android

A job-card tracker for mobile and laptop repair shops — the Android port of
[RepairDesk (2026-10-17)](https://github.com/entity-coder/2026-10-17), the
Python desktop app from the same sellable-apps lineup. It runs fully offline
on the shop's phone: no account, no server, no internet needed.

## What it does

- **Job cards** — customer name, phone, device, problem, accessories received,
  estimate, promised date, and notes. Every job gets a sequential code
  (`RD-0001`, `RD-0002`, …) that is never reused, even if a job is deleted.
- **Status pipeline** — Received → Diagnosing → In Repair → Ready →
  Delivered, advanced one tap at a time. Moving a job to Delivered stamps the
  delivery time, which is what powers "delivered today" on the dashboard.
- **Search & filter** — find a job by job code, customer name, phone number,
  or device, and filter by pipeline stage.
- **Due-payment tracking** — record part-payments (Cash / UPI / Card / Bank)
  against a job. Payments are append-only and can never exceed the amount
  still due, so the money trail for a job is always auditable.
- **Dashboard** — jobs in progress, jobs delivered today, total pending
  payments, jobs due in the next 2 days, and jobs ready for collection.
- **Job receipts** — print a receipt through the system print dialog, or
  share it as a PDF (WhatsApp, Gmail, …). The receipt lists the estimate,
  what has been paid, and what is still due.

## How to run

This project was **not compiled or tested on a device** in the environment
where it was built (no Android SDK there). To run it:

1. Open Android Studio and choose **Open** → select this folder.
2. Let Gradle sync (JDK 17, Gradle 8.2, Android Gradle Plugin 8.2.2).
3. Run on an emulator or a physical phone (min SDK 24, target SDK 34).

## Direct APK download

Once the APK workflow is added to this repo (see below), every push to
`main` builds a debug APK and attaches it to a rolling `apk-latest`
release:

```
https://github.com/entity-coder/2026-10-27/releases/download/apk-latest/RepairDesk-v1.0.apk
```

> ⚠️ That link only works after the workflow file exists in the repo and
> its first build has finished. Until then it will 404 — that is expected,
> not a broken link.

### One manual step: the APK workflow

The GitHub integration used to publish this repo cannot write files under
`.github/workflows/` (GitHub returns 403 for Apps without the workflows
permission), so the workflow file must be added once by hand:

1. Open the repo on GitHub → **Add file → Create new file**.
2. Name it exactly `.github/workflows/build-apk.yml` — the leading dot is
   mandatory. Without it, GitHub Actions silently ignores the file and no
   build will ever run.
3. Paste the workflow content (adapted for `RepairDesk-v1.0.apk`) and commit.
4. Check the file's breadcrumb/path shows `.github/workflows/` — not just
   the commit message text.
5. In repo **Settings**, untick **Enable release immutability** — otherwise
   the rolling `apk-latest` release cannot be updated on later pushes.

## Tech

- Kotlin, single-activity + fragments with bottom navigation
- Room (SQLite) with `jobs` and `payments` tables; `payments.jobId` is a
  foreign key with `CASCADE` delete, so deleting a job removes its payments
- Kotlin Flows + `repeatOnLifecycle` for live UI updates
- View Binding, Material 3, `PdfDocument` for receipts (no third-party PDF
  library), `FileProvider` for sharing
- kapt version declared explicitly and exactly once (`1.9.22`, matching
  the Kotlin plugin) in `app/build.gradle.kts` — CI builds fail if kapt is
  applied without a version

## Project structure

```
app/src/main/java/com/repairdesk/app/
├── data/
│   ├── Job.kt / JobStatus.kt / Payment.kt   # Room entities + pipeline enum
│   ├── JobDao.kt                            # Queries, incl. dashboard stats
│   ├── RepairDatabase.kt                    # Room database singleton
│   └── Format.kt                            # Money/date formatting helpers
└── ui/
    ├── MainActivity.kt                      # Bottom-nav shell
    ├── DashboardFragment.kt                 # Shop-at-a-glance
    ├── JobsFragment.kt                      # Search + status filter + list
    ├── NewJobFragment.kt                    # Intake form
    ├── JobDetailActivity.kt                 # Stage advance, payments, receipt
    ├── JobAdapter.kt / PaymentAdapter.kt    # RecyclerView adapters
    └── ReceiptPdf.kt                        # PDF receipt: print + share
```

## ## Interview notes

### What is this project?

RepairDesk Android is an offline job-card tracker for repair shops. A
customer drops off a phone or laptop, the shop creates a job card with a
sequential code, moves it through a five-stage repair pipeline, records
part-payments against the estimate, and hands the customer a printed or
shared PDF receipt. It is the Android port of a desktop app I built for
the same purpose, aimed at small shops that work from a phone at the
counter rather than a PC.

### Why these technical decisions?

- **Room + SQLite, offline-first.** A repair shop cannot depend on shop
  Wi-Fi or a backend. All data lives on the device, and every screen
  observes Room Flows, so the UI updates the moment data changes without
  manual refreshes.
- **Single activity, three bottom-nav tabs.** The phone is used one-handed
  at the counter, so bottom navigation beats a drawer or deep navigation.
  Dashboard, Jobs, and New Job are the only three things a shopkeeper
  reaches for dozens of times a day.
- **Sequential job codes derived from the stored maximum (RD-0001…).**
  Shops quote job codes over the phone, so codes must be short, stable,
  and never reused. Deriving the next code from the highest stored code
  (rather than the row count) guarantees that even after a deletion, no
  code is ever recycled.
- **Append-only payments capped at the due amount.** Money is the one
  thing a shop app must never get wrong silently. No edit or delete on
  payments means the trail is auditable, and the cap means the app can
  never record that a customer overpaid.
- **Status as a strict one-step pipeline (enum with `next()`).** Stages
  cannot be skipped, which matches how a real bench works — a device is
  never "Ready" before it was "In Repair" — and it makes the dashboard
  counts (in progress, delivered today) trivially correct.
- **PdfDocument instead of a PDF library.** Receipts are simple text, so
  the platform API is enough and avoids a dependency for one feature.
  The same rendered file serves both printing and sharing, so the two
  actions can never disagree about what the receipt says.
- **In-memory filtering on the Jobs tab.** A shop has hundreds of jobs,
  not millions, so one Flow plus local filtering is simpler and faster
  than re-querying Room on every keystroke, and it keeps search and the
  status chips in one code path.

### Trade-offs and limitations

- **Money is stored as `Double` rupees, not integer paise.** Estimates are
  entered and printed in rupees, so this matches the shop's mental model,
  but binary floating point can show 1-paise rounding edges on odd
  percentages. For a v1 shop app the totals shown are exact for the
  add/subtract math used here; a money-critical rewrite would move to
  integer paise or `BigDecimal`.
- **Not compiled or device-tested here.** The build environment has no
  Android SDK, so the project is written against the same dependency
  versions as earlier shipped Android projects in this series (AGP 8.2.2,
  Kotlin 1.9.22, Room 2.6.1) but has not been run. The README says so
  plainly instead of claiming a test pass.
- **No PNG launcher icons.** The publishing integration cannot push binary
  files reliably, so the launcher icon is vector-only adaptive XML, which
  covers API 26+. On older devices the app falls back to the default icon.
- **No sync, no backup export yet.** Data lives only on the phone. If the
  phone is lost, the job book is lost with it — CSV backup export (as in
  the GymTrack app from this series) is the obvious next feature.
- **No user accounts or multi-staff roles.** One phone, one shop book.
  That is a deliberate scope choice for a first version, not an oversight.

### How would I extend it?

- CSV backup export and restore, matching the other shop apps in this
  series.
- Customer lookup by phone number, so repeat customers auto-fill.
- SMS/WhatsApp "your device is ready" message from the Ready stage.
- A parts-used list per job, so the estimate can be built from parts +
  labour instead of a single figure.
- Optional cloud sync for shops with more than one counter phone.
