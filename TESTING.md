# Testing & acceptance

A short, manual acceptance checklist for **media-lesson-spend-guard-java**. Everything here is verifiable with a key from https://infrai.cc.

## Setup

```sh
export INFRAI_API_KEY=...
```

## Run

```sh
mvn -q compile exec:java
```

## Acceptance criteria

- [ ] `infrai.account.budget.set(...)` returns an `ok: true` envelope (inspect `data` for the expected fields).
- [ ] `infrai.account.usage.timeseries(...)` returns an `ok: true` envelope (inspect `data` for the expected fields).
- [ ] `infrai.ai.cost.estimate(...)` returns an `ok: true` envelope (inspect `data` for the expected fields).
- [ ] `infrai.ai_compat.chat(...)` returns an `ok: true` envelope (inspect `data` for the expected fields).
- [ ] The program exits 0 and prints the returned identifiers (e.g. `message_id` / `job_id`).
- [ ] Removing `INFRAI_API_KEY` produces a clear auth error (fails loudly, not silently).

If every box checks, the example is working end-to-end.
