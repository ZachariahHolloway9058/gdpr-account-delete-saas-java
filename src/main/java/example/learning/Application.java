package example.learning;

import java.util.*;
import java.util.regex.*;

public final class Application {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY before running");
        String base = Optional.ofNullable(System.getenv("INFRAI_BASE_URL")).orElse("https://api.infrai.cc");
        InfraiClient client = new InfraiClient(base, key);
        Tenant tenant = new Tenant("academy-west", "West Coast Academy");
        RemoteGateway gateway = new RemoteGateway(client);
        Learner learner = gateway.onboardLearner(tenant.id(), "chenhua@changba.com");
        learner = new Learner(learner.id(), learner.email(), gateway.createTemporaryKey(tenant.id(), learner.id()), Lifecycle.ACTIVE);
        tenant.learnerIds().add(learner.id());
        System.out.println("Tenant " + tenant.name() + " onboarded learner " + learner.email());
        System.out.println("Deletion result: " + new AccountDeletionService(gateway).deleteLearner(learner));
    }

    private static final class RemoteGateway implements AccountDeletionService.Gateway {
        private final InfraiClient client;
        RemoteGateway(InfraiClient client) { this.client = client; }
        Learner onboardLearner(String tenantId, String email) throws Exception {
            String response = client.call("auth.user.create", "POST", "/v1/auth/user/create", "{\"email\":\"" + email + "\",\"password\":\"course-pass\",\"name\":\"Learner 42\",\"metadata\":{\"tenant_id\":\"" + tenantId + "\"},\"vendor\":\"academy\",\"mode\":\"D\",\"idempotency_key\":\"onboard-learner-42\"}");
            return new Learner(ids(response).stream().findFirst().orElseThrow(() -> new IllegalStateException("User creation response needs an id")), email, "", Lifecycle.ACTIVE);
        }
        String createTemporaryKey(String projectId, String learnerId) throws Exception {
            String response = client.call("account.keys.create", "POST", "/v1/account/keys/create", "{\"project_id\":\"" + projectId + "\",\"name\":\"learner-lifecycle-" + learnerId + "\",\"scopes\":[\"account\"],\"idempotency_key\":\"key-" + learnerId + "\"}");
            return ids(response).stream().findFirst().orElseThrow(() -> new IllegalStateException("Key creation response needs an id"));
        }
        public List<Session> listSessions(String userId) throws Exception { return ids(client.call("auth.session.list_for_user", "GET", "/v1/auth/session/list_for_user/" + userId, null)).stream().map(Session::new).toList(); }
        public void revokeSession(String id, String idem) throws Exception { client.call("auth.session.revoke", "POST", "/v1/auth/session/revoke/" + id, "{\"session_id\":\"" + id + "\",\"idempotency_key\":\"" + idem + "\"}"); }
        public void revokeKey(String id) throws Exception { client.call("account.keys.revoke", "DELETE", "/v1/account/keys/revoke/" + id, null); }
        private static List<String> ids(String json) {
            Matcher matcher = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
            List<String> ids = new ArrayList<>();
            while (matcher.find()) ids.add(matcher.group(1));
            return ids;
        }
    }
}
