package com.coagent4u.persistence.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "slack_identities")
public class SlackIdentityJpaEntity {

    @Id
    @Column(name = "slack_user_id", length = 64)
    private String slackUserId;

    @Column(name = "workspace_id", nullable = false, length = 64)
    private String workspaceId;

    @Column(name = "display_name")
    private String displayName;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_user_id", nullable = false)
    private UserJpaEntity user;

    protected SlackIdentityJpaEntity() {
    }

    public SlackIdentityJpaEntity(String slackUserId, String workspaceId, String displayName) {
        this.slackUserId = slackUserId;
        this.workspaceId = workspaceId;
        this.displayName = displayName;
    }

    public String getSlackUserId() {
        return slackUserId;
    }

    public void setSlackUserId(String slackUserId) {
        this.slackUserId = slackUserId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public UserJpaEntity getUser() {
        return user;
    }

    public void setUser(UserJpaEntity user) {
        this.user = user;
    }
}
