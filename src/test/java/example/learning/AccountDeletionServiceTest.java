package example.learning;

import java.util.*;

public final class AccountDeletionServiceTest {
    public static void main(String[] args) throws Exception {
        List<String> calls = new ArrayList<>();
        AccountDeletionService.Gateway gateway = new AccountDeletionService.Gateway() {
            public List<Session> listSessions(String id) { calls.add("list:" + id); return List.of(new Session("s1"), new Session("s2")); }
            public void revokeSession(String id, String key) { calls.add("session:" + id); }
            public void revokeKey(String id) { calls.add("key:" + id); }
        };
        DeletionResult result = new AccountDeletionService(gateway).deleteLearner(new Learner("l1", "a@b.edu", "k1", Lifecycle.ACTIVE));
        if (result.state() != Lifecycle.DELETED || result.revokedSessions() != 2 || !result.keyRevoked() || !calls.equals(List.of("list:l1", "session:s1", "session:s2", "key:k1"))) throw new AssertionError(calls);
        System.out.println("PASS deletion revokes sessions before key");
    }
}
