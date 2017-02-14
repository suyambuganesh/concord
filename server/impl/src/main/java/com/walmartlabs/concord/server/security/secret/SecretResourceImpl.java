package com.walmartlabs.concord.server.security.secret;

import com.walmartlabs.concord.server.MultipartUtils;
import com.walmartlabs.concord.server.api.security.Permissions;
import com.walmartlabs.concord.server.api.security.secret.*;
import com.walmartlabs.concord.server.cfg.SecretStoreConfiguration;
import com.walmartlabs.concord.server.security.PasswordManager;
import com.walmartlabs.concord.server.security.apikey.ApiKey;
import com.walmartlabs.concord.server.security.secret.SecretDao.SecretDataEntry;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;
import org.jooq.Field;
import org.sonatype.siesta.Resource;
import org.sonatype.siesta.Validate;
import org.sonatype.siesta.ValidationErrorsException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.walmartlabs.concord.server.jooq.public_.tables.Secrets.SECRETS;

@Named
public class SecretResourceImpl implements SecretResource, Resource {

    private final SecretDao secretDao;
    private final SecretStoreConfiguration secretCfg;
    private final PasswordManager passwordManager;

    private final Map<String, Field<?>> key2Field;

    @Inject
    public SecretResourceImpl(SecretDao secretDao, SecretStoreConfiguration secretCfg,
                              PasswordManager passwordManager) {

        this.secretDao = secretDao;
        this.secretCfg = secretCfg;
        this.passwordManager = passwordManager;

        this.key2Field = new HashMap<>();
        key2Field.put("name", SECRETS.SECRET_NAME);
        key2Field.put("type", SECRETS.SECRET_TYPE);
    }

    @Override
    @RequiresPermissions(Permissions.SECRET_CREATE_NEW)
    @Validate
    public PublicKeyResponse createKeyPair(String name) {
        assertUnique(name);

        byte[] password = passwordManager.getPassword(name, ApiKey.getCurrentKey());

        KeyPair k = KeyPair.create();
        byte[] ab = SecretUtils.encrypt(KeyPair::serialize, k, password, secretCfg.getSalt());

        secretDao.insert(name, SecretType.KEY_PAIR, ab);
        return toPublicKey(name, k.getPublicKey());
    }

    @Override
    @RequiresPermissions(Permissions.SECRET_CREATE_NEW)
    @Validate
    public UploadSecretResponse uploadKeyPair(String name, MultipartInput input) {
        assertUnique(name);

        KeyPair k;
        try {
            InputStream publicIn = assertStream(input, "public");
            InputStream privateIn = assertStream(input, "private");
            k = KeyPair.create(publicIn, privateIn);
        } catch (IOException e) {
            throw new WebApplicationException("Key pair processing error", e);
        }

        byte[] password = passwordManager.getPassword(name, ApiKey.getCurrentKey());
        byte[] ab = SecretUtils.encrypt(KeyPair::serialize, k, password, secretCfg.getSalt());

        secretDao.insert(name, SecretType.KEY_PAIR, ab);
        return new UploadSecretResponse();
    }

    @Override
    @RequiresPermissions(Permissions.SECRET_CREATE_NEW)
    @Validate
    public UploadSecretResponse addUsernamePassword(String name, UsernamePasswordRequest request) {
        assertUnique(name);

        UsernamePassword k = new UsernamePassword(request.getUsername(), request.getPassword());

        byte[] password = passwordManager.getPassword(request.getUsername(), ApiKey.getCurrentKey());
        byte[] ab = SecretUtils.encrypt(UsernamePassword::serialize, k, password, secretCfg.getSalt());

        secretDao.insert(name, SecretType.USERNAME_PASSWORD, ab);
        return new UploadSecretResponse();
    }

    @Override
    public PublicKeyResponse getPublicKey(String secretName) {
        assertPermissions(secretName, Permissions.SECRET_READ_INSTANCE,
                "The current user does not have permissions to access the specified secret");

        assertSecret(secretName);

        SecretDataEntry s = secretDao.get(secretName);
        if (s.getType() != SecretType.KEY_PAIR) {
            throw new ValidationErrorsException("The specified secret is not a key pair");
        }

        byte[] password = passwordManager.getPassword(s.getName(), ApiKey.getCurrentKey());
        KeyPair k = SecretUtils.decrypt(KeyPair::deserialize, s.getData(), password, secretCfg.getSalt());
        return toPublicKey(s.getName(), k.getPublicKey());
    }

    @Override
    public List<SecretEntry> list(String sortBy, boolean asc) {
        Field<?> sortField = key2Field.get(sortBy);
        if (sortField == null) {
            throw new ValidationErrorsException("Unknown sort field: " + sortBy);
        }
        return secretDao.list(sortField, asc);
    }

    @Override
    @Validate
    public DeleteSecretResponse delete(String name) {
        assertPermissions(name, Permissions.SECRET_DELETE_INSTANCE,
                "The current user does not have permissions to delete the specified secret");

        assertSecret(name);

        secretDao.delete(name);
        return new DeleteSecretResponse();
    }

    private void assertUnique(String name) {
        if (secretDao.exists(name)) {
            throw new ValidationErrorsException("Secret already exists: " + name);
        }
    }

    private void assertSecret(String name) {
        if (!secretDao.exists(name)) {
            throw new ValidationErrorsException("Secret not found: " + name);
        }
    }

    private void assertPermissions(String name, String wildcard, String message) {
        Subject subject = SecurityUtils.getSubject();
        if (!subject.isPermitted(String.format(wildcard, name))) {
            throw new UnauthorizedException(message);
        }
    }

    private static PublicKeyResponse toPublicKey(String name, byte[] ab) {
        String s = new String(ab).trim();
        return new PublicKeyResponse(name, s);
    }

    private static InputStream assertStream(MultipartInput input, String key) throws IOException {
        for (InputPart p : input.getParts()) {
            String name = MultipartUtils.extractName(p);
            if (key.equals(name)) {
                return p.getBody(InputStream.class, null);
            }
        }
        throw new ValidationErrorsException("Value not found: " + key);
    }
}
