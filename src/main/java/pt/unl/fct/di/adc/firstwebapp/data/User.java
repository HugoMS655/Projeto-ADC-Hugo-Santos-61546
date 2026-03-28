package pt.unl.fct.di.adc.firstwebapp.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.codec.digest.DigestUtils;

@JsonPropertyOrder({ "username", "role", "phone", "address"})
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class User {

    private String username;

    @JsonIgnore
    private String password;

    private String role;

    private long creationTime; // Armazenado em segundos para consistência com o enunciado
    private String phone;
    private String address;

    // Construtor vazio (Obrigatório para Datastore/Jackson)
    public User() { }

    // Construtor auxiliar para novos registos
    public User(String username, String password, String phone, String address, String role) {
        this.username = username;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.role = role;
        this.creationTime = System.currentTimeMillis() / 1000;
    }

    /**
     * Verifica se a password coincide.
     * Nota: O UserDataService já trata do hash SHA-512 antes de comparar.
     */

    public boolean hasPassword(String passwordAttempt) {
        if (passwordAttempt == null || this.password == null) {
            return false;
        }
        // 1. Transforma a tentativa do Postman num Hash SHA-512
        String hashedAttempt = DigestUtils.sha512Hex(passwordAttempt);

        // 2. Compara os dois hashes (ambos terão 128 caracteres)
        return this.password.equals(hashedAttempt);
    }

    // --- Getters e Setters ---

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }


    public long getCreationTime() { return creationTime; }
    public void setCreationTime(long creationTime) { this.creationTime = creationTime; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}