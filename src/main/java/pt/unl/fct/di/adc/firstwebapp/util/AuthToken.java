package pt.unl.fct.di.adc.firstwebapp.util;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.UUID;
@JsonPropertyOrder({ "tokenId", "username", "role", "issuedAt", "expiresAt" })
public class AuthToken {

	@JsonIgnore
	public static final long EXPIRATION_TIME_MS = 1000 * 60 * 15;

	private String username;
	private String tokenId;
	private String role;
	private long issuedAt;
	private long expiresAt;

	// Construtor vazio para Jackson/Datastore
	public AuthToken() { }

	public AuthToken(String username, String role) {
		this.username = username;
		this.role = role;
		this.tokenId = UUID.randomUUID().toString();
		this.issuedAt = System.currentTimeMillis() / 1000;
		this.expiresAt = this.issuedAt + (EXPIRATION_TIME_MS / 1000);
	}

	// --- Getters e Setters ---

	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }

	public String getTokenId() { return tokenId; }
	public void setTokenId(String tokenId) { this.tokenId = tokenId; }

	public String getRole() { return role; }
	public void setRole(String role) { this.role = role; }

	public long getIssuedAt() { return issuedAt; }
	public void setIssuedAt(long issuedAt) { this.issuedAt = issuedAt; }

	public long getExpiresAt() { return expiresAt; }
	public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
}