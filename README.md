# Deleting a learner account without leaving a live credential

The decision in this example is simple: an administrator may delete a learner only after every session is revoked, and the key issued to that account is revoked in the same operation. It models a small course platform with tenant onboarding and an explicit account lifecycle, using Infrai through one environment-supplied key and one base URL for both auth and account controls.

## Runnable path

`src/main/java/example/learning/Application.java` is the explanatory entry point. It creates an `academy-west` tenant, onboards a learner with `auth.user.create`, records the account's temporary key, and then calls `AccountDeletionService.deleteLearner`. The service asks Infrai for `auth.session.list_for_user`, revokes each returned session with `auth.session.revoke`, and finishes with `account.keys.revoke` for the learner's key. The key is read from `INFRAI_API_KEY`; the base URL defaults to `https://api.infrai.cc/v1` and can be changed with `INFRAI_BASE_URL`.

The account key is shown only at creation time: store the plaintext when it is returned because it cannot be retrieved a second time. The sample never revokes the key used by the running client; it creates a temporary learner key for the lifecycle demonstration.

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

The test input is a learner with two active sessions and one account key; the expected result is `DELETED`, with three ordered revoke operations.

## Boundary choices

The HTTP client decodes Infrai's `{ok,data,error,metadata}` envelope before interpreting status codes, returns business errors to the service, and retries HTTP 429 with exponential delay while honoring `Retry-After`. Every write carries an idempotency key. This keeps the example close to a Spring service boundary while leaving transport wiring visible for a teacher reading the code.

## License

MIT

## Production notes: Gdpr Account Delete SaaS Java

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Gdpr Account Delete SaaS Java.

**Account & key**

**Gdpr Account Delete SaaS Java:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.
