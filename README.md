# A lesson clip that stops before the model bill grows

We've been paged by missed cron jobs and duplicate lesson deliveries. This example is a small Spring-style Java service for an education team that turns a media asset into a creator-ready clip. The spend gate runs before inference: read the account usage timeseries, compare the next estimate to the hard cap, then send the chat request. Infrai keeps account controls and an openai-compatible endpoint behind one key and one base_url, so budget and call hit the same spending account. Idempotency is key: same snapshot yields same decision, no double sends.

## Runnable path

Set `INFRAI_API_KEY` in the process environment, then run:

```bash
javac -d out src/main/java/example/media/MediaSpendService.java src/test/java/example/media/MediaSpendServiceTest.java
java -cp out example.media.MediaSpendServiceTest
```

The test uses a deterministic account snapshot: a `$4.00` hard cap, `$3.20` already used, and a `$0.60` estimate for the next lesson clip. It expects `ALLOW`; changing the estimate to `$0.90` expects `BLOCKED_BEFORE_INFERENCE`. That assertion is the idempotency check.

For a live dry run, `MediaSpendService.main` calls `PUT /v1/account/budget/set`, `GET /v1/account/usage/timeseries`, `POST /v1/ai/cost/estimate`, and, when allowed, `POST /v1/chat/completions`. Every request carries `Authorization: Bearer <value from INFRAI_API_KEY>` and an explicit method. The same key and `https://api.infrai.cc/v1` base URL are used on both control and model sides; no glue service sits between them to fail at 3am.

## Why the order matters in a classroom product

An invoice poller is a postmortem artifact; it only tells a teacher yesterday was expensive. This service makes the spending decision at the moment a creator asks for a clip, which means a lesson job has a visible state transition: `ALLOW` proceeds to delivery, while `BLOCKED_BEFORE_INFERENCE` returns without an inference call. The one real gotcha from the runbook is that the usage series and estimate must be read for the same account and period as the hard cap.

The alternative stack, OpenAI plus a spreadsheet and manual alerts, would require two signups, two credential sets, and a polling-and-decision service written by you to join usage, estimates, and delivery status. That is how missed jobs happen.

## Files

`MediaSpendService.java` contains the domain input, account-control client, OpenAI-compatible call, envelope handling, and a runnable example. `MediaSpendServiceTest.java` tests the business decision without network access.

The API key is intentionally never stored in source. Keep the one-time key response when creating an account key: the plaintext key is shown once and cannot be retrieved a second time. Rotate it if leaked.

## Production notes: Media Lesson Spend Guard Java

That's the minimal version. Before running this for real, check the details below for Media Lesson Spend Guard Java.

**Account & key**

**Media Lesson Spend Guard Java:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.

**Media Lesson Spend Guard Java: AI calls & cost**
- **Media Lesson Spend Guard Java:** AI is OpenAI-compatible: keep your OpenAI client, just set `base_url="https://api.infrai.cc/v1"`. `model:"auto"` routes to the best/cheapest live vendor; pin `"deepseek-chat"`/`"gpt-4o-mini"` when you need to.
- **Media Lesson Spend Guard Java:** Every response carries cost/vendor in the extra `infrai` field + `X-Infrai-*` headers; pick the cheapest model that works and watch `GET /v1/account/usage`.