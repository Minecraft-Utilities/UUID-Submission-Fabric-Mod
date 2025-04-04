package cc.fascinated.client.common;

import java.util.UUID;

public class UUIDSubmission {
    /**
     * The UUID of the account that has sent the uuids
     */
    private final UUID accountUuid;

    /**
     * The UUIDs to submit
     */
    private final UUID[] uuids;

    public UUIDSubmission(UUID accountUuid, UUID[] uuids) {
        this.accountUuid = accountUuid;
        this.uuids = uuids;
    }

    public UUID getAccountUuid() {
        return accountUuid;
    }

    public UUID[] getUuids() {
        return uuids;
    }
}
