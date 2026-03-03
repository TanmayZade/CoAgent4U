package com.coagent4u.user.port.in;

import com.coagent4u.shared.UserId;

/**
 * Inbound port — revokes a user's external service connection.
 */
public interface DisconnectServiceUseCase {
    /**
     * @param userId      the user to disconnect
     * @param serviceType e.g. "GOOGLE_CALENDAR"
     */
    void disconnect(UserId userId, String serviceType);
}
