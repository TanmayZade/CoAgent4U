package com.coagent4u.coordination.port.in;

import java.util.Optional;

import com.coagent4u.coordination.application.dto.CoordinationDetail;
import com.coagent4u.shared.CoordinationId;

/**
 * Inbound port for coordination detail queries.
 */
public interface GetCoordinationDetailUseCase {
    Optional<CoordinationDetail> getDetail(CoordinationId id, String viewerUsername);
}
