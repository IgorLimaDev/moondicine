# Moondicine Supabase Dashboard Briefing

## Instructions for the dashboard-building AI

Create an authenticated web dashboard for managing the Moondicine medical question bank. The primary workflow is creating, editing, reviewing, and deleting questions together with their answer options. The mobile Android app reads this question bank from Supabase and copies it into a local Room database for offline use.

Use the existing SQL migration as the source of truth:

`supabase/migrations/001_initial_schema.sql`

Do not rename tables or columns. Preserve the foreign keys, constraints, and data types described below.

## Supabase connection

- Project URL: `https://cyczozmaszmymagrozic.supabase.co`
- REST base URL: `https://cyczozmaszmymagrozic.supabase.co/rest/v1/`
- Publishable key: configured as the client-side Supabase key
- SQL schema: apply the migration in the Supabase SQL Editor or through an authenticated CLI

The publishable key is not a secret and is suitable for browser reads. Never expose the PostgreSQL password or a Supabase service-role key in browser code. Any privileged write operation must be performed through authenticated server-side code, an Edge Function, or carefully scoped authenticated RLS policies.

## Primary tables

### `public.questions`

One row represents one medical exam question.

| Column | Type | Rules | Meaning |
|---|---|---|---|
| `id` | `bigint` | identity primary key | Stable question ID |
| `exam_source` | `text` | required | Exam name/source, for example `ENAM 2024` |
| `question_number` | `integer` | required | Number within the source exam |
| `question_text` | `text` | required | Full question statement |
| `specialty` | `text` | required | Primary medical specialty |
| `sub_topic` | `text` | required | More specific subject classification |
| `difficulty` | `integer` | required, default `3`, range `1..5` | Difficulty level |
| `image_url` | `text` | nullable | Optional URL for an image used by the question |
| `created_at` | `timestamptz` | required, default `now()` | Creation timestamp |
| `updated_at` | `timestamptz` | required, default `now()` | Last-edit timestamp |

There is a unique index on `(exam_source, question_number)`, so the dashboard must reject duplicate question numbers within the same exam source. There is also an index on `specialty` for filtering.

### `public.answer_options`

One row represents one answer choice belonging to a question.

| Column | Type | Rules | Meaning |
|---|---|---|---|
| `id` | `bigint` | identity primary key | Stable option ID |
| `question_id` | `bigint` | required foreign key to `questions.id` | Parent question |
| `option_letter` | `text` | required, only `A`, `B`, `C`, `D`, or `E` | Choice label |
| `option_text` | `text` | required | Choice text |
| `is_correct` | `boolean` | required, default `false` | Whether this is the correct answer |

The foreign key uses `on delete cascade`: deleting a question automatically deletes its options. There is a unique constraint on `(question_id, option_letter)`, so a question can have at most one option for each letter. The normal product rule should be exactly one correct option per question.

## Supporting tables

These tables mirror the Android Room database and are present for a future account/progress synchronization feature. The current Android app does not upload these records; its progress remains local.

### `public.user_profile`

One profile row, currently modeled as a singleton by the Android app.

Columns: `id integer primary key`, `name text`, `target_specialty text`, `experience_level text`, `onboarding_completed boolean`, `join_date timestamptz`, `total_questions_answered integer`, `current_streak_days integer`, `last_study_date timestamptz nullable`, and `total_study_time_minutes integer`.

### `public.user_answers`

Stores attempts: `id`, `question_id`, `selected_option_id`, `is_correct`, `time_spent_seconds`, `answered_at`, and `is_flagged`. `question_id` references `questions.id`; `selected_option_id` references `answer_options.id`. Both relationships cascade on question/option deletion.

### `public.question_notes`

Stores personal notes with `id`, `question_id`, `note_text`, `created_at`, and `updated_at`. `question_id` references `questions.id` with cascade delete.

### `public.ai_explanations`

Stores cached explanations with `id`, `question_id`, `explanation_text`, `correct_reasoning`, `wrong_reasoning jsonb`, and `cached_at`. `question_id` references `questions.id` with cascade delete.

### `public.user_stats`

Stores aggregate statistics by specialty: `id`, `specialty`, `total_answered`, `total_correct`, `average_time_seconds`, `last_studied_at`, and `weakest_sub_topics jsonb`. `specialty` is unique.

## Relationships

```text
questions 1 ──── many answer_options
questions 1 ──── many user_answers
answer_options 1 ──── many user_answers through selected_option_id
questions 1 ──── many question_notes
questions 1 ──── many ai_explanations
```

The dashboard’s central editor should load one question and all of its `answer_options`. Save option rows using the parent question’s `id`. Deleting a question also deletes dependent options and other dependent records through cascading foreign keys, so deletion must require confirmation.

## Security and write behavior

The migration enables Row Level Security on every table.

The current migration explicitly grants only these anonymous/authenticated operations:

- `SELECT` on `public.questions`
- `SELECT` on `public.answer_options`

There are no write policies in the migration. Therefore, a dashboard using only the publishable key cannot insert, update, or delete question-bank records until a secure write strategy is added.

For a real dashboard, prefer:

1. User authentication for dashboard operators.
2. Server-side mutations or Supabase Edge Functions using a service-role key stored only in server secrets.
3. Strict validation before writes.
4. No anonymous insert, update, or delete policies.

If authenticated client-side writes are intentionally added, create narrowly scoped RLS policies for an admin role. Do not weaken RLS globally.

## Dashboard features

Generate these views:

- Question list with search and filters for `exam_source`, `specialty`, `sub_topic`, and `difficulty`.
- Question editor for all required question fields and optional `image_url`.
- Answer-option editor for choices A through E, including exactly one correct choice.
- Create-question flow that creates the question first, then its options using the returned question ID.
- Edit-question flow that updates the question and upserts its options.
- Delete-question flow with a warning about cascading dependent records.
- Validation/error states for duplicate `(exam_source, question_number)`, missing required fields, invalid difficulty, invalid option letters, and missing correct answer.
- Updated-at display and sorting.

For atomic question-plus-options writes, use a server-side transaction, RPC, or Edge Function. Separate browser requests are not an actual database transaction and can leave a question without options if the second request fails.

## Mobile sync contract

The Android app requests:

```text
GET /rest/v1/questions?select=*&order=updated_at.desc
GET /rest/v1/answer_options?select=*&order=question_id.asc,option_letter.asc
```

The app maps Supabase `questions.id` to local `questions.remoteId` and Supabase `answer_options.id` to local `answer_options.remoteId`. It then reads all quiz data from Room, allowing the app to work offline after a successful sync.

When editing existing records, always update `updated_at`. The migration does not define an automatic timestamp trigger. New questions should use the database defaults. Future incremental sync can filter by `updated_at` after storing the last successful sync timestamp.

Do not change or reuse primary keys. The mobile app relies on stable remote IDs to update local records without breaking locally stored answer references.

## Content conventions

- Store medical text as plain text in `question_text` and `option_text`.
- Store `difficulty` as an integer from 1 to 5.
- Use uppercase option letters `A` through `E`.
- Store JSON values in `wrong_reasoning` and `weakest_sub_topics` as valid JSON.
- Keep `exam_source` consistent so the unique exam/number rule works as intended.
- Prefer HTTPS image URLs and allow `image_url` to be null.
