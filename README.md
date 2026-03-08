# Contribution OS

**An open coordination protocol for volunteer-driven task completion, peer validation, and verifiable impact tracking.**

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Core Modules](#core-modules)
- [Task Lifecycle](#task-lifecycle)
- [Roles & Permissions](#roles--permissions)
- [Database Schema](#database-schema)
- [API Reference](#api-reference)
- [Security Model](#security-model)
- [Edge Functions](#edge-functions)

---

## Overview

Contribution OS is a platform for coordinating volunteers to complete real-world tasks created by organizations. It supports:

- **Task coordination** — Two-step claim process (reserve → confirm) with time-locked work windows
- **Peer validation** — Three-outcome review (approve / reject / request clarification) with appeals
- **Contribution ledger** — Immutable record of approved work with difficulty-based scoring
- **Verifiable credentials** — Digital certificates tied to completed tasks and milestones
- **Initiative management** — Group tasks under campaigns with priority scoring and progress tracking
- **Public REST API** — API key auth, granular scopes, idempotency, HMAC-SHA256 signed webhooks

### Zero-AI Baseline

The core coordination engine runs entirely on deterministic logic — no AI dependencies. An optional intelligence layer provides advisory outputs (campaign planning, task template generation, analytics summaries) that always require human confirmation before taking effect.

---

## Architecture

```
┌────────────────────────────────────────────────────┐
│              Frontend (React + Vite)                │
│  Pages: MissionFeed, TaskDetails, ActiveWorkspace,  │
│  Contributions, Validation, Initiatives, Developer  │
│  Portal, Leadership, Observability, Transparency    │
├────────────────────────────────────────────────────┤
│                  Edge Functions                      │
│  task-engine, initiative-engine, protocol,           │
│  impact-worker, campaign-ops, leadership,           │
│  recommendation-engine, observability,              │
│  campaign-discovery, platform-worker                │
├────────────────────────────────────────────────────┤
│                 Lovable Cloud                        │
│  PostgreSQL + Auth + RLS + Realtime + Storage       │
└────────────────────────────────────────────────────┘
```

**Modular monolith** designed for future microservice extraction, divided into ten domain modules.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, TypeScript, Vite, Tailwind CSS |
| Components | shadcn/ui, Radix UI, Recharts |
| State | TanStack React Query |
| Routing | React Router v6 |
| Backend | Lovable Cloud (Edge Functions, PostgreSQL) |
| Auth | Email-based authentication with session management |
| Fonts | Space Grotesk (headings), Inter (body) |

---

## Getting Started

```bash
# Clone the repository
git clone <YOUR_GIT_URL>

# Navigate to the project
cd <YOUR_PROJECT_NAME>

# Install dependencies
npm install

# Start dev server
npm run dev
```

### Project Structure

```
src/
├── components/        # UI components (shared, mission, team, layout, etc.)
├── contexts/          # AuthContext
├── hooks/             # React Query hooks for each domain
├── lib/               # API client wrappers for edge functions
├── pages/             # Route pages
└── integrations/      # Auto-generated Supabase client & types

supabase/
├── functions/         # Edge functions (one per domain)
├── migrations/        # Database migrations
└── config.toml        # Supabase configuration
```

---

## Core Modules

| Module | Description |
|--------|-------------|
| **Identity** | Profiles, roles (member/validator/moderator/admin), contributor stats |
| **Task Engine** | Full lifecycle: create → reserve → claim → progress → submit → validate |
| **Validation** | Peer review with approve/reject/clarification + conflict-of-interest checks |
| **Contribution Ledger** | Immutable records, score = difficulty × 10, impact metrics |
| **Initiative Engine** | Campaigns with projects, milestones, micro-initiatives (≤30d, ≤10 tasks, ≤25 volunteers) |
| **Campaign Ops** | Volunteer allocation, content scheduling, region management |
| **Notifications** | Real-time per-user notifications for all state changes |
| **Analytics** | Contributor stats, org metrics, initiative progress, system health |
| **Governance** | Appeals for rejected submissions, audit logs, transparency |
| **Protocol API** | Public REST API with API keys, webhooks, scoped permissions |

---

## Task Lifecycle

```
open → reserved → claimed → in_progress → submitted → validation_pending → validated
                                                                          → rejected → (appeal)
       ↳ expired (lock timeout)
```

| Status | Description |
|--------|-------------|
| `open` | Available for volunteers |
| `reserved` | Soft-locked for 60 seconds during confirmation |
| `claimed` | Locked to volunteer with configurable work window |
| `in_progress` | At least one progress update posted |
| `submitted` | Work submitted for review |
| `validation_pending` | Under peer validation |
| `validated` | Approved — contribution recorded |
| `rejected` | Rejected by validator (appealable) |
| `expired` | Lock expired before submission |

---

## Roles & Permissions

Roles are stored in a dedicated `user_roles` table (never on profiles) to prevent privilege escalation.

| Role | Capabilities |
|------|-------------|
| **member** | Browse tasks, claim, submit, join initiatives, view own contributions |
| **validator** | + Review submissions, approve/reject/request clarification |
| **moderator** | + Resolve appeals, manage initiative tasks |
| **admin** | + Grant/revoke roles, issue credentials, update priority, access audit logs |

Security-definer function `has_role(user_id, role)` used in RLS policies to prevent recursive checks.

---

## Database Schema

19 core tables with Row-Level Security. All use UUID primary keys (UUIDv7 for time-ordering).

### Core Tables

| Table | Purpose |
|-------|---------|
| `profiles` | User profiles (display_name, interests, location, status) |
| `user_roles` | Role assignments (member, validator, moderator, admin) |
| `tasks` | Task records with lifecycle status, difficulty 1-5, tags, visibility |
| `task_claims` | Claim history with expiry timestamps |
| `task_submissions` | Submitted work (content + attachments) |
| `task_updates` | Progress updates (notes + attachments) |
| `validations` | Review decisions (approved, rejected, clarification_requested) |
| `contributions` | Immutable ledger of approved contributions with scores |
| `appeals` | Appeal records for rejected submissions |
| `initiatives` | Campaign definitions with priority scoring, micro/full flags |
| `initiative_participants` | Volunteer join records |
| `projects` | Sub-groupings within initiatives |
| `credentials` | Verifiable digital certificates with metadata |
| `organizations` | Org profiles with slug-based routing |
| `events` | Immutable event stream (16 event types, versioned) |
| `api_keys` | Developer API keys (SHA-256 hashed, scoped) |
| `webhook_subscriptions` | Registered webhook endpoints |
| `audit_logs` | Immutable audit trail |
| `notifications` | Per-user notification records |

### Immutable Tables

`contributions`, `audit_logs`, and `events` have triggers that prevent UPDATE/DELETE operations.

---

## API Reference

**Base URL:** `/functions/v1/protocol`  
**Version:** 2.1.0

### Authentication

| Method | Header | Rate Limit |
|--------|--------|-----------|
| API Key | `x-api-key: cos_<64 hex chars>` | 60 req/min (configurable) |
| Bearer Token | `Authorization: Bearer <jwt>` | 120 req/min |

### Idempotency

Pass `x-idempotency-key` header on POST requests for at-most-once delivery. Keys expire after 24h.

### Endpoints

#### Users
| Method | Path | Description |
|--------|------|-------------|
| GET | `/users` | List volunteer profiles |
| GET | `/users/:id` | Profile with stats, credentials, roles |

#### Tasks
| Method | Path | Description |
|--------|------|-------------|
| GET | `/tasks` | List public tasks (?status, ?initiative_id) |
| GET | `/tasks/:id` | Task details |
| POST | `/tasks` | Create a task |

#### Initiatives
| Method | Path | Description |
|--------|------|-------------|
| GET | `/initiatives` | List by priority (?status) |
| GET | `/initiatives/:id` | Details with participants & tasks |
| POST | `/initiatives` | Create initiative |
| POST | `/initiatives/:id/join` | Join initiative |
| GET | `/initiatives/:id/priority` | Priority score breakdown |
| PUT | `/initiatives/:id/priority` | Update priority/featured |

#### Contributions & Credentials
| Method | Path | Description |
|--------|------|-------------|
| GET | `/contributions` | List contributions (?user_id, ?task_id) |
| GET | `/credentials` | List credentials (?user_id, ?type) |
| GET | `/credentials/:id` | Credential details with metadata |
| POST | `/credentials` | Issue credential |
| DELETE | `/credentials/:id` | Revoke credential |

#### Roles
| Method | Path | Description |
|--------|------|-------------|
| GET | `/roles` | List roles and permission mappings |
| POST | `/roles/grant` | Grant role to user |
| POST | `/roles/revoke` | Revoke role from user |

#### Events & Webhooks
| Method | Path | Description |
|--------|------|-------------|
| GET | `/events` | Query event stream (?type, ?since, ?version) |
| GET/POST | `/keys` | Manage API keys |
| DELETE | `/keys/:id` | Revoke API key |
| GET/POST | `/webhooks` | Manage webhook subscriptions |

### Permission Scopes

**Legacy:** `users`, `tasks`, `initiatives`, `contributions`, `events`, `webhooks`, `*`

**Granular (v2):** `tasks:read`, `tasks:create`, `tasks:claim`, `tasks:validate`, `tasks:manage`, `initiatives:read`, `initiatives:create`, `initiatives:manage`, `initiatives:join`, `initiatives:prioritize`, `contributions:read`, `profiles:read`, `submissions:review`, `credentials:read`, `credentials:issue`, `credentials:revoke`, `roles:read`, `roles:grant`, `roles:revoke`

### Event Types

`TaskCreated`, `TaskReserved`, `TaskClaimed`, `TaskSubmitted`, `ValidationApproved`, `ValidationRejected`, `ContributionRecorded`, `InitiativeCreated`, `CredentialIssued`, `CredentialRevoked`, `RoleGranted`, `RoleRevoked`, `InitiativeJoined`, `InitiativePriorityUpdated`

### Webhook Format

Delivered as signed POST requests:
- **Headers:** `X-Webhook-Signature` (HMAC-SHA256), `X-Event-Type`, `X-Protocol-Version`
- **Body:** `{ event: string, data: object, timestamp: ISO 8601, version: number }`

---

## Security Model

- **Row-Level Security** on all tables — users only access authorized data
- **Roles in separate table** (`user_roles`) — prevents privilege escalation
- **Security-definer functions** (`has_role`, `is_org_member`, `is_org_admin`) — prevents recursive RLS
- **Immutable audit trail** — contributions, events, audit_logs cannot be modified
- **API key hashing** — SHA-256 hashed, only prefix stored in plaintext
- **Webhook signatures** — HMAC-SHA256 payload verification
- **Rate limiting** — Per-key and per-user limits with sliding window
- **Idempotency** — Prevents duplicate operations on retries

---

## Edge Functions

| Function | Domain | Description |
|----------|--------|-------------|
| `task-engine` | Tasks | CRUD, reserve/claim, submit, validate, teams, appeals |
| `initiative-engine` | Initiatives | CRUD, projects, task generation, join, promote |
| `protocol` | API | Public protocol API with auth, credentials, roles, webhooks |
| `impact-worker` | Analytics | Post-validation impact processing |
| `campaign-ops` | Campaigns | Operational tools for initiative managers |
| `campaign-discovery` | Discovery | Initiative browsing and search |
| `leadership` | Analytics | Leadership dashboards and metrics |
| `observability` | Monitoring | System health and performance metrics |
| `recommendation-engine` | Discovery | Personalized task/initiative recommendations |
| `platform-worker` | System | Background processing and maintenance |
