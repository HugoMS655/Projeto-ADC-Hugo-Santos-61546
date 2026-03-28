package pt.unl.fct.di.adc.firstwebapp.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) // Alterado para true para maior robustez
public class PasswordData {

    private String username;
    private String oldPassword;
    private String newPassword;
    private String confirmation;

    public PasswordData() { }

    public PasswordData(String username, String oldPassword, String newPassword, String confirmation) {
        this.username = username;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
        this.confirmation = confirmation;
    }

    public String getUsername() { return username; }
    public String getOldPassword() { return oldPassword; }
    public String getNewPassword() { return newPassword; }
    public String getConfirmation() { return confirmation; }


    public void setUsername(String username) { this.username = username; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public void setConfirmation(String confirmation) { this.confirmation = confirmation; }
}