package example.learning;

import java.util.*;

final class AccountDeletionService {
    interface Gateway {
        List<Session> listSessions(String learnerId) throws Exception;
        void revokeSession(String sessionId, String idempotencyKey) throws Exception;
        void revokeKey(String keyId) throws Exception;
    }

    private final Gateway gateway;
    AccountDeletionService(Gateway gateway) { this.gateway = gateway; }

    DeletionResult deleteLearner(Learner learner) throws Exception {
        if (learner.lifecycle() == Lifecycle.DELETED) return new DeletionResult(learner.id(), Lifecycle.DELETED, 0, true);
        int revoked = 0;
        for (Session session : gateway.listSessions(learner.id())) {
            gateway.revokeSession(session.id(), "delete-" + learner.id() + "-" + session.id());
            revoked++;
        }
        gateway.revokeKey(learner.accountKey());
        return new DeletionResult(learner.id(), Lifecycle.DELETED, revoked, true);
    }
}
