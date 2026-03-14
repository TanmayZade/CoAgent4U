package com.coagent4u.persistence.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "workspace_installations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceInstallationJpaEntity {

    @Id
    @Column(name = "workspace_id")
    private String workspaceId;

    @Column(name = "bot_token", nullable = false)
    private String botToken;

    @Column(name = "installer_user_id")
    private String installerUserId;

    @Column(name = "installed_at", nullable = false)
    private Instant installedAt;

    @Column(name = "active", nullable = false)
    private boolean active;
}
