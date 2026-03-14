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

    @Column(name = "workspace_name")
    private String workspaceName;

    @Column(name = "workspace_domain")
    private String workspaceDomain;

    @Column(name = "email")
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_user_id", nullable = false)
    private UserJpaEntity user;

    protected SlackIdentityJpaEntity() {
    }

    public SlackIdentityJpaEntity(String slackUserId, String workspaceId, String workspaceName, String workspaceDomain, String email, String displayName, String avatarUrl) {
        this.slackUserId = slackUserId;
        this.workspaceId = workspaceId;
        this.workspaceName = workspaceName;
        this.workspaceDomain = workspaceDomain;
        this.email = email;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
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

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public String getWorkspaceDomain() {
        return workspaceDomain;
    }

    public void setWorkspaceDomain(String workspaceDomain) {
        this.workspaceDomain = workspaceDomain;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public UserJpaEntity getUser() {
        return user;
    }

    public void setUser(UserJpaEntity user) {
        this.user = user;
    }
}
