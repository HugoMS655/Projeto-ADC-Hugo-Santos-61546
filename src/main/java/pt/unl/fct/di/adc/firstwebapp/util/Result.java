package pt.unl.fct.di.adc.firstwebapp.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
@JsonPropertyOrder({ "status", "data" })
public class Result<T> {

    @JsonProperty("status")
    private String status;

    @JsonProperty("data")
    private T data;


    public static class Message {
        @JsonProperty("message")
        public String message;
        public Message(String message) { this.message = message; }
    }

    public static class UserList {
        @JsonProperty("users")
        public List<?> users;
        public UserList(List<?> users) { this.users = users; }
    }

    public static class SessionList {
        @JsonProperty("sessions")
        public List<?> sessions;
        public SessionList(List<?> sessions) { this.sessions = sessions; }
    }

    public static class UserRoleData {
        @JsonProperty("username")
        public String username;
        @JsonProperty("role")
        public String role;
        public UserRoleData(String u, String r) { this.username = u; this.role = r; }
    }

    // --- ENUMS ---

    public enum ErrorCode {
        INVALID_CREDENTIALS("9900", "The username-password pair is not valid"),
        USER_ALREADY_EXISTS("9901", "Error in creating an account because the username already exists"),
        USER_NOT_FOUND("9902", "The username referred in the operation doesn't exist in registered accounts"),
        INVALID_TOKEN("9903", "The operation is called with an invalid token (wrong format for example)"),
        TOKEN_EXPIRED("9904", "The operation is called with a token that is expired"),
        UNAUTHORIZED("9905", "The operation is not allowed for the user role"),
        INVALID_INPUT("9906", "The call is using input data not following the correct specification"),
        FORBIDDEN("9907", "The operation generated a forbidden error by other reason");

        private final String code;
        private final String message;
        ErrorCode(String code, String message) { this.code = code; this.message = message; }
        public String getCode() { return code; }
        public String getMessage() { return message; }
    }

    public enum SuccessMessage {
        USER_CREATED("User created successfully"),
        USER_MODIFIED("Updated successfully"),
        ROLE_UPDATED("Role updated successfully"),
        PWD_CHANGED("Password changed successfully"),
        ACCOUNT_DELETED("Account deleted successfully"),
        LOGOUT_OK("Logout successful");

        private final String message;
        SuccessMessage(String message) { this.message = message; }
        public String getMessage() { return message; }
    }

    public Result() {}
    public Result(String status, T data) { this.status = status; this.data = data; }

    public static <T> Result<T> ok(T value) { return new Result<>("success", value); }

    @SuppressWarnings("unchecked")
    public static <T> Result<T> ok(SuccessMessage msg) {
        return new Result<>("success", (T) new Message(msg.getMessage()));
    }

    @SuppressWarnings("unchecked")
    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), (T) errorCode.getMessage());
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}