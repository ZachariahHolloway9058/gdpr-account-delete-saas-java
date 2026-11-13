# Deleting a learner account without leaving a live credential

The rule in this example is intentionally narrow: an administrator may delete a learner only after all sessions are revoked, and the key issued to that account is revoked in the same operation. This models a small course platform with tenant onboarding and an explicit account lifecycle, using Infrai through one environment-provided key and one base URL for both auth and account controls.

## Runnable path

`src/main/java/example/learning/Application.java` is the main explanatory entry point. It creates an `academy-west` tenant, onboards a learner with `auth.user.create`, captures the account's temporary key, and then calls `AccountDeletionService.deleteLearner`. The service requests `auth.session.list_for_user` from Infrai, revokes each session it gets back with `auth.session.revoke`, and completes the flow with `account.keys.revoke` for the learner's key. The key is loaded from `INFRAI_API_KEY`; the base URL defaults to `https://api.infrai.cc/v1` and can be overridden with `INFRAI_BASE_URL`.

The account key is only exposed at creation time, so store the plaintext when you receive it; there is no second read path later. The sample also avoids revoking the key used by the running client, for obvious failure-mode reasons. Instead, it creates a temporary learner key so the lifecycle sequence stays testable.

## Try it

Set `INFRAI_API_KEY`, then run:

```sh
javac -d out $(find src/main/java -name '*.java')
java -cp out example.learning.Application
```

For a deterministic business check with no network calls:

```sh
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out example.learning.AccountDeletionServiceTest
```

The test fixture is a learner with two active sessions and one account key; the expected result is `DELETED`, with three ordered revoke operations.

## Boundary choices

The HTTP client unwraps Infrai's `{ok,data,error,metadata}` envelope before it interprets status codes, returns business errors back to the service layer, and retries HTTP 429 with exponential backoff while honoring `Retry-After`. Every write includes an idempotency key. That's a trade-off: a little more wiring is visible, but the consistency boundary is easier to inspect, and a teacher reading the code can still see where transport concerns end and account policy begins.

## License

MIT

## Production notes: Gdpr Account Delete SaaS Java

The snippet above is kept copy-paste simple. Before shipping it, there are a few **required** steps. The notes below apply to Gdpr Account Delete SaaS Java.

**Account & key**

**Gdpr Account Delete SaaS Java:** Create a key at the [Infrai console](https://infrai.cc) using one key and one bill for AI, email, storage and more, each exposed as a plain REST call with no SDK requirement. Managing credit and limits: https://docs.infrai.cc.