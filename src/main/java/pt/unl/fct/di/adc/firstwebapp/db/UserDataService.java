package pt.unl.fct.di.adc.firstwebapp.db;

import com.google.cloud.datastore.*;
import org.apache.commons.codec.digest.DigestUtils;
import pt.unl.fct.di.adc.firstwebapp.data.User;
import pt.unl.fct.di.adc.firstwebapp.resources.UserResource;
import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;

import java.util.ArrayList;
import java.util.List;

public class UserDataService {

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
    private final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("Token");

    public UserDataService() { }

    public void storeUser(User user) {
        Key userKey = userKeyFactory.newKey(user.getUsername());
        String pwdToStore = user.getPassword();
        if (pwdToStore != null && pwdToStore.length() != 128) {
            pwdToStore = DigestUtils.sha512Hex(pwdToStore);
        }
        Entity userEntity = Entity.newBuilder(userKey)
                .set("password", pwdToStore)
                .set("role", user.getRole())
                .set("phone", user.getPhone() != null ? user.getPhone() : "")
                .set("address", user.getAddress() != null ? user.getAddress() : "")
                .set("creationTime", user.getCreationTime())
                .build();
        datastore.put(userEntity);
    }

    public User getUser(String username) {
        if(username == null) return null;
        Key userKey = userKeyFactory.newKey(username);
        Entity entity = datastore.get(userKey);
        if(entity == null) return null;
        User user = new User();
        user.setUsername(username);
        user.setPassword(entity.contains("password") ? entity.getString("password") : "");
        user.setRole(entity.contains("role") ? entity.getString("role") : "USER");
        user.setPhone(entity.contains("phone") ? entity.getString("phone") : "");
        user.setAddress(entity.contains("address") ? entity.getString("address") : "");
        user.setCreationTime(entity.contains("creationTime") ? entity.getLong("creationTime") : 0L);
        return user;
    }

    public List<User> listAllUsers() {
        Query<Entity> query = Query.newEntityQueryBuilder().setKind("User").build();
        QueryResults<Entity> results = datastore.run(query);
        List<User> users = new ArrayList<>();
        while (results.hasNext()) {
            Entity entity = results.next();
            User u = new User();
            u.setUsername(entity.getKey().getName());
            u.setRole(entity.contains("role") ? entity.getString("role") : "USER");
            users.add(u);
        }
        return users;
    }

    public void deleteUser(String username) {
        if (username != null) {
            datastore.delete(userKeyFactory.newKey(username));
        }
    }

    public void removeAllUserTokens(String username) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("Token")
                .setFilter(StructuredQuery.PropertyFilter.eq("username", username))
                .build();
        QueryResults<Entity> results = datastore.run(query);
        List<Key> keysToDelete = new ArrayList<>();
        while (results.hasNext()) {
            keysToDelete.add(results.next().getKey());
        }
        if (!keysToDelete.isEmpty()) {
            datastore.delete(keysToDelete.toArray(new Key[0]));
        }
    }

    // ---  ATUALIZAR ROLE NOS TOKENS ---
    public void updateAllUserTokensRole(String username, String newRole) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("Token")
                .setFilter(StructuredQuery.PropertyFilter.eq("username", username))
                .build();
        QueryResults<Entity> results = datastore.run(query);
        List<Entity> updatedTokens = new ArrayList<>();
        while (results.hasNext()) {
            updatedTokens.add(Entity.newBuilder(results.next()).set("role", newRole).build());
        }
        if (!updatedTokens.isEmpty()) {
            datastore.put(updatedTokens.toArray(new Entity[0]));
        }
    }

    public void updateUserAttributes(String username, UserResource.UserAttributes attrs) {
        User user = getUser(username);
        if (user == null || attrs == null) return;
        boolean changed = false;
        // Só marca como 'changed' se o novo telefone for diferente do atual
        if (attrs.getPhone() != null && !attrs.getPhone().trim().isEmpty()) {
            if (!attrs.getPhone().equals(user.getPhone())) {
                user.setPhone(attrs.getPhone());
                changed = true;
            }
        }
        // Só marca como 'changed' se a nova morada for diferente da atual
        if (attrs.getAddress() != null && !attrs.getAddress().trim().isEmpty()) {
            if (!attrs.getAddress().equals(user.getAddress())) {
                user.setAddress(attrs.getAddress());
                changed = true;
            }
        }
        // Só toca na base de dados se algo mudou de facto
        if (changed) {
            storeUser(user);
        }
    }

    public void updateRole(String username, String newRole) {
        User user = getUser(username);
        if(user != null && newRole != null) {
            user.setRole(newRole);
            storeUser(user);
        }
    }

    public void updatePassword(String username, String newPassword) {
        User user = getUser(username);
        if(user != null && newPassword != null) {
            user.setPassword(newPassword);
            storeUser(user);
        }
    }

    public void storeToken(AuthToken token) {
        Key tokenKey = tokenKeyFactory.newKey(token.getTokenId());
        Entity tokenEntity = Entity.newBuilder(tokenKey)
                .set("username", token.getUsername())
                .set("role", token.getRole())
                .set("issuedAt", token.getIssuedAt())
                .set("expiresAt", token.getExpiresAt())
                .build();
        datastore.put(tokenEntity);
    }

    public AuthToken getValidToken(String tokenId) {
        if (tokenId == null) return null;
        Key tokenKey = tokenKeyFactory.newKey(tokenId);
        Entity entity = datastore.get(tokenKey);
        if (entity == null) return null;
        long expiresAt = entity.getLong("expiresAt");
        if ((System.currentTimeMillis() / 1000) > expiresAt) {
            datastore.delete(tokenKey);
            return null;
        }
        AuthToken token = new AuthToken();
        token.setTokenId(tokenId);
        token.setUsername(entity.getString("username"));
        token.setRole(entity.getString("role"));
        token.setExpiresAt(expiresAt);
        if (entity.contains("issuedAt")) token.setIssuedAt(entity.getLong("issuedAt"));
        return token;
    }

    public List<AuthToken> listAllSessions() {
        Query<Entity> query = Query.newEntityQueryBuilder().setKind("Token").build();
        QueryResults<Entity> results = datastore.run(query);
        List<AuthToken> tokens = new ArrayList<>();
        while (results.hasNext()) {
            Entity entity = results.next();
            AuthToken t = new AuthToken();
            t.setTokenId(entity.getKey().getName());
            t.setUsername(entity.contains("username") ? entity.getString("username") : "n/a");
            t.setRole(entity.contains("role") ? entity.getString("role") : "USER");
            t.setExpiresAt(entity.contains("expiresAt") ? entity.getLong("expiresAt") : 0L);
            if (entity.contains("issuedAt")) t.setIssuedAt(entity.getLong("issuedAt"));
            tokens.add(t);
        }
        return tokens;
    }

    public void removeToken(String tokenId) {
        if (tokenId != null) datastore.delete(tokenKeyFactory.newKey(tokenId));
    }
}