package com.rgx.parse;

public class Token {
    TokenType tokenType;
    Object value;

    Token(TokenType tokenType, Object value) {
        this.tokenType = tokenType;
        this.value = value;
    }
}
