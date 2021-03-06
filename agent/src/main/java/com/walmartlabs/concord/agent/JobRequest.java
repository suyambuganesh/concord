package com.walmartlabs.concord.agent;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2019 Walmart Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import com.walmartlabs.concord.agent.logging.ProcessLog;
import com.walmartlabs.concord.agent.logging.ProcessLogFactory;
import com.walmartlabs.concord.agent.logging.RemoteProcessLog;
import com.walmartlabs.concord.imports.Imports;
import com.walmartlabs.concord.server.queueclient.message.ProcessResponse;

import java.nio.file.Path;
import java.util.UUID;

public class JobRequest {

    public static JobRequest from(ProcessResponse resp, Path workDir, ProcessLogFactory logFactory) {
        RemoteProcessLog log = logFactory.createRemoteLog(resp.getProcessId());
        return new JobRequest(Type.RUNNER, resp.getProcessId(),
                workDir,
                resp.getOrgName(),
                resp.getRepoUrl(),
                resp.getRepoPath(),
                resp.getCommitId(),
                resp.getSecretName(),
                log,
                resp.getImports());
    }

    private final Type type;
    private final UUID instanceId;
    private final Path payloadDir;
    private final String orgName; // TODO rename to secretOrgName
    private final String repoUrl;
    private final String repoPath;
    private final String commitId;
    private final String secretName;
    private final ProcessLog log;
    private final Imports imports;

    protected JobRequest(Type type,
                         UUID instanceId,
                         Path payloadDir,
                         String orgName,
                         String repoUrl,
                         String repoPath,
                         String commitId,
                         String secretName,
                         ProcessLog log,
                         Imports imports) {

        this.type = type;
        this.instanceId = instanceId;
        this.payloadDir = payloadDir;
        this.orgName = orgName;
        this.repoUrl = repoUrl;
        this.repoPath = repoPath;
        this.commitId = commitId;
        this.secretName = secretName;
        this.log = log;
        this.imports = imports != null ? imports : Imports.builder().build();
    }

    public Type getType() {
        return type;
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public Path getPayloadDir() {
        return payloadDir;
    }

    public String getOrgName() {
        return orgName;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public String getRepoPath() {
        return repoPath;
    }

    public String getCommitId() {
        return commitId;
    }

    public String getSecretName() {
        return secretName;
    }

    public ProcessLog getLog() {
        return log;
    }

    public Imports getImports() {
        return imports;
    }

    @Override
    public String toString() {
        return "JobRequest{" +
                "type=" + type +
                ", instanceId=" + instanceId +
                ", payloadDir=" + payloadDir +
                ", orgName='" + orgName + '\'' +
                ", repoUrl='" + repoUrl + '\'' +
                ", repoPath='" + repoPath + '\'' +
                ", commitId='" + commitId + '\'' +
                ", secretName='" + secretName + '\'' +
                ", log=" + log +
                ", imports=" + imports +
                '}';
    }

    public enum Type {

        /**
         * A concord-runner based job.
         */
        RUNNER
    }
}
