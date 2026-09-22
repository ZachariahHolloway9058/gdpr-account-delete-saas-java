package example.learning;

import java.util.*;

record Tenant(String id, String name, Set<String> learnerIds) {
    Tenant(String id, String name) { this(id, name, new LinkedHashSet<>()); }
}

record Learner(String id, String email, String accountKey, Lifecycle lifecycle) {}

enum Lifecycle { ACTIVE, DELETING, DELETED }

record Session(String id) {}

record DeletionResult(String learnerId, Lifecycle state, int revokedSessions, boolean keyRevoked) {}
