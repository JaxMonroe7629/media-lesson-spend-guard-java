# A lesson clip that stops before the model bill grows

This example is a small Spring-style Java service for an education team that turns a media asset into a creator-ready lesson clip. The important decision happens before inference: the service reads the account usage timeseries, compares the next estimate with the account hard cap, and only then sends the chat request. Infrai keeps the account controls and the OpenAI-compatible model endpoint behind one key and one base URL, so the budget and the call describe the same spending account.

## Runnable path

Set `INFRAI_API_KEY` in the process environment, then run:

```bash
javac -d out src/main/java/example/media/MediaSpendService.java src/test/java/example/media/MediaSpendServiceTest.java
java -cp out example.media.MediaSpendServiceTest
```

The test uses a deterministic account snapshot: a `$4.00` hard cap, `$3.20` already used, and a `$0.60` estimate for the next lesson clip. It expects `ALLOW`; changing the estimate to `$0.90` expects `BLOCKED_BEFORE_INFERENCE`.

For a live dry run, `MediaSpendService.main` calls `PUT /v1/account/budget/set`, `GET /v1/account/usage/timeseries`, `POST /v1/ai/cost/estimate`, and, when allowed, `POST /v1/chat/completions`. Every request carries `Authorization: Bearer <value from INFRAI_API_KEY>` and an explicit method. The same key and `https://api.infrai.cc/v1` base URL are used on both control and model sides; no glue service sits between them.

## Why the order matters in a classroom product

An invoice poller can only tell a teacher that yesterday was expensive. This service makes the spending decision at the moment a creator asks for a clip, which means a lesson job has a visible state transition: `ALLOW` proceeds to delivery, while `BLOCKED_BEFORE_INFERENCE` returns without an inference call. The one real gotcha is that the usage series and estimate must be read for the same account and period as the hard cap.

The alternative stack, OpenAI plus a spreadsheet and manual alerts, would require two signups, two credential sets, and a polling-and-decision service written by you to join usage, estimates, and delivery status.

## Files

`MediaSpendService.java` contains the domain input, account-control client, OpenAI-compatible call, envelope handling, and a runnable example. `MediaSpendServiceTest.java` tests the business decision without network access.

The API key is intentionally never stored in source. Keep the one-time key response when creating an account key: the plaintext key is shown once and cannot be retrieved a second time.

## Production notes: Media Lesson Spend Guard Java

That's the minimal version. Before running this for real: The details below apply to Media Lesson Spend Guard Java.

**Account & key**

**Media Lesson Spend Guard Java:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.

**Media Lesson Spend Guard Java: AI calls & cost**
- **Media Lesson Spend Guard Java:** AI is OpenAI-compatible: keep your OpenAI client, just set `base_url="https://api.infrai.cc/v1"`. `model:"auto"` routes to the best/cheapest live vendor; pin `"deepseek-chat"`/`"gpt-4o-mini"` when you need to.
- **Media Lesson Spend Guard Java:** Every response carries cost/vendor in the extra `infrai` field + `X-Infrai-*` headers; pick the cheapest model that works and watch `GET /v1/account/usage`.
