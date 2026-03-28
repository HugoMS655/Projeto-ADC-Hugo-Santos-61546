package pt.unl.fct.di.adc.firstwebapp.util;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "input", "token" })
public class Request<T> {

    public AuthToken token;
    public T input;

    public Request() { }

    public Request(AuthToken token, T input) {
        this.token = token;
        this.input = input;
    }
    public AuthToken getToken() {
        return token;
    }
    public T getInput() {
        return input;
    }
}