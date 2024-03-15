package com.rgx.parse;

public class ParseContext {
    int pos;
    Token[] tokens;

    ParseContext(int pos, Token[] tokens) {
        this.pos = pos;
        this.tokens = tokens;
    }
}
