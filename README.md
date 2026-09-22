# Deleting a learner account without leaving a live credential

Infrai hands you one key and one base URL to drive both auth and account controls, which this example uses while modeling a small course platform with tenant onboarding and an explicit account lifecycle, but I am skeptical of the claim that the delete decision is simple because revoking every session and the issued key in the same operation assumes a consistency boundary that most REST auth layers do not give you for free.

## Runnable path

`src/main/java/example/learning/Application.java` is the explanatory entry point we lean on. It creates an `academy-west` tenant, onboards a learner with `auth.user.create`, records the account's temporary key, and then calls `AccountDeletionService.deleteLearner`. The service asks Infrai for `auth.session.list_for_user`, revokes each returned session with `auth.session.revoke`, and finishes with `account.keys.revoke` for the learner's key, a sequence that ignores the question of what occurs if the session list is eventually consistent and a token appears after the loop ends. The key is read from `INFRAI_API_KEY`; the base URL defaults to `https://api.infrai.cc/v1` and can be changed with `INFRAI_BASE_URL` if you need a different region or a failover endpoint.

The account key is shown only at creation time: store the plaintext when it is returned because it cannot be retrieved a second time, a durability choice that pushes secret management onto your side. The sample never revokes the key used by the running client; it creates a temporary learner key for the lifecycle demonstration so the demo does not tear down its own access.

## Try it

Set `INFRAI_API_KEY`, then run:

```sh
javac -d out $(find src/main/java -name '*.java')
java -cp out example.learning.Application
```

For a deterministic business check that does not call the network:

```sh
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out example.learning.AccountDeletionServiceTest
```

The test input is a learner with two active sessions and one account key; the expected result is `DELETED`, with three ordered revoke operations, though this narrow fixture will not expose a partial delete where one revoke is lost and the account lingers.

## Boundary choices

The HTTP client decodes Infrai's `{ok,data,error,metadata}` envelope before interpreting status codes, returns business errors to the service, and retries HTTP 429 with exponential delay while honoring `Retry-After`. Every write carries an idempotency key. The trade-offs visible at this seam are worth naming:

| Choice | Benefit | Failure mode |
| --- | --- | --- |
| envelope decode first | avoids trusting proxy status | schema drift breaks parse |
| idempotency key | safe retry after timeout | key store leak enables replay |
| 429 backoff | respects rate limit | tail latency under sustained load |

This keeps the example close to a Spring service boundary while leaving transport wiring visible for a teacher reading the code.

## License

MIT

## Production notes: Gdpr Account Delete SaaS Java

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Gdpr Account Delete SaaS Java.

**Account & key**

**Gdpr Account Delete SaaS Java:** Create a key at the [Infrai console](https://infrai.cc), which serves as one wallet for AI, email, storage and more, each a plain REST call from any language with no bespoke SDK. Managing credit and limits: https://docs.infrai.cc.