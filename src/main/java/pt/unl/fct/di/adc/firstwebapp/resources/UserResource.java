package pt.unl.fct.di.adc.firstwebapp.resources;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import pt.unl.fct.di.adc.firstwebapp.data.User;
import pt.unl.fct.di.adc.firstwebapp.db.UserDataService;
import pt.unl.fct.di.adc.firstwebapp.util.*;
import pt.unl.fct.di.adc.firstwebapp.util.Request;
import pt.unl.fct.di.adc.firstwebapp.util.Result.*;

import java.util.List;
import java.util.stream.Collectors;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
    private final UserDataService dao = new UserDataService();

    private long currentTimeSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    private AuthToken validateAndGetToken(AuthToken token) {
        if (token == null || token.getTokenId() == null) return null;
        return dao.getValidToken(token.getTokenId());
    }

    private <T> T parseAndValidate(String jsonRaw, Class<T> targetClass) {
        try {
            int posInput = jsonRaw.indexOf("\"input\"");
            int posToken = jsonRaw.indexOf("\"token\"");

            if (posToken != -1 && posInput != -1 && posToken < posInput) {
                return null;
            }
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            return mapper.readValue(jsonRaw, targetClass);
        } catch (Exception e) {
            return null;
        }
    }

    // --- Op1: Create Account ---
    @POST
    @Path("/createaccount")
    public Response doCreateAccount(String jsonRaw) {
        RegisterRequest req = parseAndValidate(jsonRaw, RegisterRequest.class);
        if (req == null || req.getInput() == null) return Response.ok(Result.error(ErrorCode.INVALID_INPUT)).build();

        RegisterData data = req.getInput();
        if (data.getUsername() == null || data.getPassword() == null || data.getConfirmation() == null) {
            return Response.ok(Result.error(ErrorCode.INVALID_INPUT)).build();
        }
        if (!data.getUsername().matches(EMAIL_PATTERN) || !data.getPassword().equals(data.getConfirmation())) {
            return Response.ok(Result.error(ErrorCode.INVALID_INPUT)).build();
        }
        if (data.getRole() == null || (!data.getRole().equals(UserRole.USER.name()) &&
                !data.getRole().equals(UserRole.BOFFICER.name()) && !data.getRole().equals(UserRole.ADMIN.name()))) {
            return Response.ok(Result.error(ErrorCode.INVALID_INPUT)).build();
        }
        if (dao.getUser(data.getUsername()) != null) {
            return Response.ok(Result.error(ErrorCode.USER_ALREADY_EXISTS)).build();
        }

        User newUser = new User(data.getUsername(), data.getPassword(), data.getPhone(), data.getAddress(), data.getRole());
        dao.storeUser(newUser);
        return Response.ok(Result.ok(new UserRoleData(newUser.getUsername(), newUser.getRole()))).build();
    }

    // --- Op2: Login ---
    @POST
    @Path("/login")
    public Response doLogin(String jsonRaw) {
        LoginRequest req = parseAndValidate(jsonRaw, LoginRequest.class);
        if (req == null || req.getInput() == null) return Response.ok(Result.error(ErrorCode.INVALID_INPUT)).build();

        LoginData data = req.getInput();
        if (data.getUsername() == null || data.getPassword() == null) {
            return Response.ok(Result.error(ErrorCode.INVALID_INPUT)).build();
        }

        User user = dao.getUser(data.getUsername());
        if (user == null) return Response.ok(Result.error(ErrorCode.USER_NOT_FOUND)).build();
        if (!user.hasPassword(data.getPassword())) return Response.ok(Result.error(ErrorCode.INVALID_CREDENTIALS)).build();

        AuthToken token = new AuthToken(user.getUsername(), user.getRole());
        dao.storeToken(token);
        return Response.ok(Result.ok(token)).build();
    }

    // --- Op3: Show Users ---
    @POST
    @Path("/showusers")
    public Response doShowUsers(String jsonRaw) {
        RequestEmpty req = parseAndValidate(jsonRaw, RequestEmpty.class);
        if (req == null || req.getToken() == null) return Response.ok(Result.error(ErrorCode.INVALID_INPUT)).build();

        AuthToken session = validateAndGetToken(req.getToken());
        if (session == null) return Response.ok(Result.error(ErrorCode.INVALID_TOKEN)).build();

        if (session.getExpiresAt() < currentTimeSeconds()) {
            dao.removeToken(session.getTokenId());
            return Response.ok(Result.error(ErrorCode.TOKEN_EXPIRED)).build();
        }
        if (session.getRole().equals(UserRole.USER.name())) return Response.ok(Result.error(ErrorCode.UNAUTHORIZED)).build();

        List<User> allUsers = dao.listAllUsers();
        List<User> filteredUsers;
        if (session.getRole().equals(UserRole.ADMIN.name())) {
            filteredUsers = allUsers;
        } else {
            filteredUsers = allUsers.stream()
                    .filter(u -> u.getRole().equals(UserRole.BOFFICER.name()) || u.getRole().equals(UserRole.USER.name()))
                    .collect(Collectors.toList());
        }
        return Response.ok(Result.ok(new UserList(filteredUsers))).build();
    }

    // --- Op4: Delete Account ---
    @POST
    @Path("/deleteaccount")
    public Response doDeleteAccount(String jsonRaw) {
        RequestDelete req = parseAndValidate(jsonRaw, RequestDelete.class);
        if (req == null || req.getToken() == null || req.getInput() == null || req.getInput().getUsername() == null) {
            return Response.ok(Result.error(ErrorCode.INVALID_INPUT)).build();
        }
        AuthToken session = validateAndGetToken(req.getToken());
        if (session == null) return Response.ok(Result.error(ErrorCode.INVALID_TOKEN)).build();

        if (session.getExpiresAt() < currentTimeSeconds()) {
            dao.removeToken(session.getTokenId());
            return Response.ok(Result.error(ErrorCode.TOKEN_EXPIRED)).build();
        }
        if (!session.getRole().equals(UserRole.ADMIN.name())) return Response.ok(Result.error(ErrorCode.UNAUTHORIZED)).build();

        String targetUsername = req.getInput().getUsername();
        if (dao.getUser(targetUsername) == null) return Response.ok(Result.error(ErrorCode.USER_NOT_FOUND)).build();

        dao.removeAllUserTokens(targetUsername);
        dao.deleteUser(targetUsername);
        return Response.ok(Result.ok(SuccessMessage.ACCOUNT_DELETED)).build();
    }

    // --- Op5: Modify Account ---
    // --- Op5: Modify Account (Atualizado com validação de campos) ---
    @POST
    @Path("/modaccount")
    public Response doModifyAccount(String jsonRaw) {
        RequestModify req = parseAndValidate(jsonRaw, RequestModify.class);
        if (req == null || req.getToken() == null || req.getInput() == null) {
            return Response.ok(Result.error(ErrorCode.INVALID_INPUT)).build();
        }

        String targetUsername = req.getInput().getUsername();
        UserAttributes attrs = req.getInput().getAttributes();

        // Validação: Garante que o input existe e que pelo menos UM dos campos tem conteúdo
        if (targetUsername == null || attrs == null) {
            return Response.ok(Result.error(ErrorCode.INVALID_INPUT)).build();
        }

        String newPhone = attrs.getPhone();
        String newAddress = attrs.getAddress();

        boolean hasPhone = (newPhone != null && !newPhone.trim().isEmpty());
        boolean hasAddress = (newAddress != null && !newAddress.trim().isEmpty());

        // Se ambos estiverem vazios, não há nada a fazer
        if (!hasPhone && !hasAddress) {
            return Response.ok(Result.error(ErrorCode.INVALID_INPUT)).build();
        }

        AuthToken session = validateAndGetToken(req.getToken());
        if (session == null) return Response.ok(Result.error(ErrorCode.INVALID_TOKEN)).build();

        if (session.getExpiresAt() < currentTimeSeconds()) {
            dao.removeToken(session.getTokenId());
            return Response.ok(Result.error(ErrorCode.TOKEN_EXPIRED)).build();
        }

        User target = dao.getUser(targetUsername);
        if (target == null) return Response.ok(Result.error(ErrorCode.USER_NOT_FOUND)).build();

        // Regras de permissão
        boolean isSelf = session.getUsername().equals(targetUsername);
        boolean isAdmin = session.getRole().equals(UserRole.ADMIN.name());
        boolean isBOfficerOnUser = session.getRole().equals(UserRole.BOFFICER.name()) &&
                target.getRole().equals(UserRole.USER.name());

        if (!isSelf && !isAdmin && !isBOfficerOnUser) {
            return Response.ok(Result.error(ErrorCode.UNAUTHORIZED)).build();
        }

        dao.updateUserAttributes(targetUsername, attrs);
        return Response.ok(Result.ok(SuccessMessage.USER_MODIFIED)).build();
    }

    // --- Op6: Show Auth Sessions ---
    @POST
    @Path("/showauthsessions")
    public Response doShowSessions(String jsonRaw) {
        RequestEmpty req = parseAndValidate(jsonRaw, RequestEmpty.class);
        if (req == null || req.getToken() == null) return Response.ok(Result.error(ErrorCode.INVALID_INPUT)).build();

        AuthToken session = validateAndGetToken(req.getToken());
        if (session == null) return Response.ok(Result.error(ErrorCode.INVALID_TOKEN)).build();

        if (session.getExpiresAt() < currentTimeSeconds()) {
            dao.removeToken(session.getTokenId());
            return Response.ok(Result.error(ErrorCode.TOKEN_EXPIRED)).build();
        }
        if (!session.getRole().equals(UserRole.ADMIN.name())) return Response.ok(Result.error(ErrorCode.UNAUTHORIZED)).build();

        return Response.ok(Result.ok(new SessionList(dao.listAllSessions()))).build();
    }

    // --- Op7: Show User Role ---
    @POST
    @Path("/showuserrole")
    public Response doShowRole(String jsonRaw) {
        RequestShowRole req = parseAndValidate(jsonRaw, RequestShowRole.class);
        if (req == null || req.getToken() == null || req.getInput() == null || req.getInput().getUsername() == null) {
            return Response.ok(Result.error(ErrorCode.INVALID_INPUT)).build();
        }
        AuthToken session = validateAndGetToken(req.getToken());
        if (session == null) return Response.ok(Result.error(ErrorCode.INVALID_TOKEN)).build();

        if (session.getExpiresAt() < currentTimeSeconds()) {
            dao.removeToken(session.getTokenId());
            return Response.ok(Result.error(ErrorCode.TOKEN_EXPIRED)).build();
        }
        if (session.getRole().equals(UserRole.USER.name())) return Response.ok(Result.error(ErrorCode.UNAUTHORIZED)).build();

        User target = dao.getUser(req.getInput().getUsername());
        if (target == null) return Response.ok(Result.error(ErrorCode.USER_NOT_FOUND)).build();

        return Response.ok(Result.ok(new UserRoleData(target.getUsername(), target.getRole()))).build();
    }

    // --- Op8: Change User Role ---
    @POST
    @Path("/changeuserrole")
    public Response doChangeRole(String jsonRaw) {
        RequestChangeRole req = parseAndValidate(jsonRaw, RequestChangeRole.class);
        if (req == null || req.getToken() == null || req.getInput() == null ||
                req.getInput().getUsername() == null || req.getInput().getNewRole() == null) {
            return Response.ok(Result.error(ErrorCode.INVALID_INPUT)).build();
        }
        AuthToken session = validateAndGetToken(req.getToken());
        if (session == null) return Response.ok(Result.error(ErrorCode.INVALID_TOKEN)).build();

        if (session.getExpiresAt() < currentTimeSeconds()) {
            dao.removeToken(session.getTokenId());
            return Response.ok(Result.error(ErrorCode.TOKEN_EXPIRED)).build();
        }
        if (!session.getRole().equals(UserRole.ADMIN.name())) return Response.ok(Result.error(ErrorCode.UNAUTHORIZED)).build();

        String targetUser = req.getInput().getUsername();
        String newRole = req.getInput().getNewRole();

        if (!newRole.equals(UserRole.USER.name()) && !newRole.equals(UserRole.BOFFICER.name()) && !newRole.equals(UserRole.ADMIN.name())) {
            return Response.ok(Result.error(ErrorCode.INVALID_INPUT)).build();
        }

        dao.updateRole(targetUser, newRole);
        // ATUALIZAÇÃO: Sincroniza a Role em todos os tokens ativos sem expulsar o utilizador
        dao.updateAllUserTokensRole(targetUser, newRole);

        return Response.ok(Result.ok(SuccessMessage.ROLE_UPDATED)).build();
    }

    // --- Op9: Change User Password ---
    @POST
    @Path("/changeuserpwd")
    public Response doChangePassword(String jsonRaw) {
        RequestPasswordData req = parseAndValidate(jsonRaw, RequestPasswordData.class);

        if (req == null || req.getToken() == null || req.getInput() == null ||
                req.getInput().getUsername() == null ||
                req.getInput().getOldPassword() == null ||
                req.getInput().getNewPassword() == null) {
            return Response.ok(Result.error(ErrorCode.INVALID_INPUT)).build();
        }

        AuthToken session = validateAndGetToken(req.getToken());
        if (session == null) return Response.ok(Result.error(ErrorCode.INVALID_TOKEN)).build();

        if (session.getExpiresAt() < currentTimeSeconds()) {
            dao.removeToken(session.getTokenId());
            return Response.ok(Result.error(ErrorCode.TOKEN_EXPIRED)).build();
        }

        // Apenas o próprio pode mudar a sua password
        if (!session.getUsername().equals(req.getInput().getUsername())) {
            return Response.ok(Result.error(ErrorCode.UNAUTHORIZED)).build();
        }

        User target = dao.getUser(req.getInput().getUsername());
        if (target == null) return Response.ok(Result.error(ErrorCode.USER_NOT_FOUND)).build();

        if (!target.hasPassword(req.getInput().getOldPassword())) {
            return Response.ok(Result.error(ErrorCode.INVALID_CREDENTIALS)).build();
        }

        String newPwd = req.getInput().getNewPassword();
        if (newPwd.trim().isEmpty() || req.getInput().getOldPassword().equals(newPwd)) {
            return Response.ok(Result.error(ErrorCode.INVALID_INPUT)).build();
        }

        dao.updatePassword(target.getUsername(), newPwd);

        // MUDANÇA: Não fazemos logout aqui. O utilizador continua com a sua sessão ativa.
        return Response.ok(Result.ok(SuccessMessage.PWD_CHANGED)).build();
    }

    // --- Op10: Logout ---
    @POST
    @Path("/logout")
    public Response doLogout(String jsonRaw) {
        RequestLogout req = parseAndValidate(jsonRaw, RequestLogout.class);
        if (req == null || req.getToken() == null || req.getInput() == null || req.getInput().getUsername() == null) {
            return Response.ok(Result.error(ErrorCode.INVALID_INPUT)).build();
        }
        AuthToken session = validateAndGetToken(req.getToken());
        if (session == null) return Response.ok(Result.error(ErrorCode.INVALID_TOKEN)).build();

        if (session.getExpiresAt() < currentTimeSeconds()) {
            dao.removeToken(session.getTokenId());
            return Response.ok(Result.error(ErrorCode.TOKEN_EXPIRED)).build();
        }

        String targetUsername = req.getInput().getUsername();
        boolean isAdmin = session.getRole().equals(UserRole.ADMIN.name());
        boolean isSelf = session.getUsername().equals(targetUsername);

        if (!isAdmin && !isSelf) {
            return Response.ok(Result.error(ErrorCode.UNAUTHORIZED)).build();
        }

        // Verificação se o utilizador alvo existe
        if (dao.getUser(targetUsername) == null) {
            return Response.ok(Result.error(ErrorCode.USER_NOT_FOUND)).build();
        }

        if (isAdmin && !isSelf) {
            // Admin a expulsar outro: Limpa tudo
            dao.removeAllUserTokens(targetUsername);
        } else {
            // Utilizador a sair: Limpa apenas o token que ele enviou no pedido
            dao.removeToken(session.getTokenId());
        }

        return Response.ok(Result.ok(SuccessMessage.LOGOUT_OK)).build();
    }

    // --- CLASSES AUXILIARES (DTOs e Wrappers) ---

    public static class RegisterRequest {
        private RegisterData input;
        public RegisterRequest() {}
        public RegisterData getInput() { return input; }
        public void setInput(RegisterData input) { this.input = input; }
    }

    public static class LoginRequest {
        private LoginData input;
        public LoginRequest() {}
        public LoginData getInput() { return input; }
        public void setInput(LoginData input) { this.input = input; }
    }

    public static class UsernameInput {
        private String username;
        public UsernameInput() {}
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }

    public static class ModifyInput {
        private String username;
        private UserAttributes attributes;
        public ModifyInput() {}
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public UserAttributes getAttributes() { return attributes; }
        public void setAttributes(UserAttributes attributes) { this.attributes = attributes; }
    }

    public static class ChangeRoleInput {
        private String username;
        private String newRole;
        public ChangeRoleInput() {}
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getNewRole() { return newRole; }
        public void setNewRole(String newRole) { this.newRole = newRole; }
    }

    public static class UserAttributes {
        private String phone;
        private String address;
        public UserAttributes() {}
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
    }

    public static class RequestLogout extends Request<UsernameInput> { public RequestLogout(){} }
    public static class RequestShowRole extends Request<UsernameInput> { public RequestShowRole(){} }
    public static class RequestChangeRole extends Request<ChangeRoleInput> { public RequestChangeRole(){} }
    public static class RequestModify extends Request<ModifyInput> { public RequestModify(){} }
    public static class RequestEmpty extends Request<Void> { public RequestEmpty(){} }
    public static class RequestDelete extends Request<UsernameInput> { public RequestDelete(){} }
    public static class RequestPasswordData extends Request<PasswordData> { public RequestPasswordData(){} }
}