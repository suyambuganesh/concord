package com.walmartlabs.concord.server.process.state;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
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

import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.server.cfg.SecretStoreConfiguration;
import com.walmartlabs.concord.server.org.secret.SecretUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class EncryptStreamProcessor implements StreamProcessor {

    private final SecretStoreConfiguration secretCfg;

    public EncryptStreamProcessor(SecretStoreConfiguration secretCfg) {
        this.secretCfg = secretCfg;
    }

    @Override
    public InputStream process(InputStream in) throws IOException {
        byte[] ab = SecretUtils.encrypt(IOUtils.toByteArray(in), secretCfg.getServerPwd(), secretCfg.getSecretStoreSalt());
        return new ByteArrayInputStream(ab);
    }
}