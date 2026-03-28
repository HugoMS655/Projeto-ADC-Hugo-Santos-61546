package pt.unl.fct.di.adc.firstwebapp.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonIgnoreProperties(ignoreUnknown = false)
@JsonPropertyOrder({ "username", "password", "confirmation", "phone", "address", "role" })
public class RegisterData {

    private String username;
    private String password;
    private String confirmation;
    private String phone;
    private String address;
    private String role;


    public RegisterData() { }


    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getConfirmation() { return confirmation; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getRole() { return role; }


    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setConfirmation(String confirmation) { this.confirmation = confirmation; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
    public void setRole(String role) { this.role = role; }
}