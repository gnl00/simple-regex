package com.rgx.parse;

import java.util.HashSet;
import java.util.Set;

public class Parser {
    static ParseContext parse(String regex) {
        ParseContext ctx = new ParseContext(0, new Token[0]);
        while (ctx.pos < regex.length()) {
            process(regex, ctx);
            ctx.pos++;
        }
        return ctx;
    }

    static void process(String regex, ParseContext ctx) {
        char ch = regex.charAt(ctx.pos);
        if (ch == '(') { // <1>
            ParseContext groupCtx = new ParseContext(ctx.pos, new Token[0]);
            parseGroup(regex, groupCtx);
            ctx.tokens = appendToken(ctx.tokens, new Token(TokenType.GROUP, groupCtx.tokens));
        } else if (ch == '[') { // <2>
            parseBracket(regex, ctx);
        } else if (ch == '|') { // <3>
            parseOr(regex, ctx);
        } else if (ch == '*' || ch == '?' || ch == '+') { // <4>
            parseRepeat(regex, ctx);
        } else if (ch == '{') { // <5>
            parseRepeatSpecified(regex, ctx);
        } else { // <6>
            // literal
            Token t = new Token(TokenType.LITERAL, ch);
            ctx.tokens = appendToken(ctx.tokens, t);
        }
    }

    static Token[] appendToken(Token[] tokens, Token token) {
        Token[] newTokens = new Token[tokens.length + 1];
        System.arraycopy(tokens, 0, newTokens, 0, tokens.length);
        newTokens[tokens.length] = token;
        return newTokens;
    }

    static void parseGroup(String regex, ParseContext ctx) {
        ctx.pos += 1; // get past the LPAREN (
        while (regex.charAt(ctx.pos) != ')') {
            process(regex, ctx);
            ctx.pos += 1;
        }
    }

    static void parseBracket(String regex, ParseContext ctx) {
        ctx.pos++; // get past the LBRACKET
        StringBuilder literalsBuilder = new StringBuilder();
        while (regex.charAt(ctx.pos) != ']') { // <1>
            char ch = regex.charAt(ctx.pos);

            if (ch == '-') { // <2>
                char next = regex.charAt(ctx.pos + 1); // <3-1>
                char prev = literalsBuilder.charAt(literalsBuilder.length() - 1); // <3-2>
                literalsBuilder.deleteCharAt(literalsBuilder.length() - 1); // Remove the previous character
                for (char i = prev; i <= next; i++) {
                    literalsBuilder.append(i); // <3-3>
                }
                ctx.pos++; // to consume the 'next' char
            } else { // <4>
                literalsBuilder.append(ch);
            }

            ctx.pos++; // <5>
        }

        Set<Character> literalsSet = new HashSet<>();
        for (int i = 0; i < literalsBuilder.length(); i++) {
            literalsSet.add(literalsBuilder.charAt(i));
        }

        ctx.tokens = appendToken(ctx.tokens, new Token(TokenType.BRACKET, literalsSet)); // <8>
    }

    static void parseOr(String regex, ParseContext ctx) {
        // <1:start>
        ParseContext rhsContext = new ParseContext(ctx.pos, new Token[0]);
        rhsContext.pos += 1; // get past |
        while (rhsContext.pos < regex.length() && regex.charAt(rhsContext.pos) != ')') {
            process(regex, rhsContext);
            rhsContext.pos += 1;
        }
        // <1:end>

        // both sides of the OR expression
        Token left = new Token(TokenType.GROUP_UNCAPTURED, ctx.tokens); // <2>

        Token right = new Token(TokenType.GROUP_UNCAPTURED, rhsContext.tokens); // <3>
        ctx.pos = rhsContext.pos; // <4>

        ctx.tokens = new Token[]{ // <5>
                new Token(TokenType.OR, new Token[]{left, right})
        };
    }


    static final int REPEAT_INFINITY = -1;

    static void parseRepeat(String regex, ParseContext ctx) {
        char ch = regex.charAt(ctx.pos);
        int min, max;
        if (ch == '*') {
            min = 0;
            max = REPEAT_INFINITY;
        } else if (ch == '?') {
            min = 0;
            max = 1;
        } else {
            // ch == '+'
            min = 1;
            max = REPEAT_INFINITY;
        }
        // we need to wrap the last token with the quantifier data
        // so that we know what the min and max apply to
        Token lastToken = ctx.tokens[ctx.tokens.length - 1];
        ctx.tokens[ctx.tokens.length - 1] = new Token(TokenType.REPEAT, new RepeatPayload(min, max, lastToken));
    }

    static class RepeatPayload {
        int min;
        int max;
        Token token;

        RepeatPayload(int min, int max, Token token) {
            this.min = min;
            this.max = max;
            this.token = token;
        }
    }

    static void parseRepeatSpecified(String regex, ParseContext ctx) {
        // +1 because we skip LCURLY { at the beginning
        int start = ctx.pos + 1;
        // proceed until we reach to the end of the curly braces
        while (regex.charAt(ctx.pos) != '}') {
            ctx.pos++;
        }

        String boundariesStr = regex.substring(start, ctx.pos); // <1>
        String[] pieces = boundariesStr.split(","); // <2>
        int min, max;
        if (pieces.length == 1) { // <3>
            min = Integer.parseInt(pieces[0]);
            max = min;
        } else if (pieces.length == 2) { // <4>
            min = Integer.parseInt(pieces[0]);
            if (pieces[1].equals("")) {
                max = REPEAT_INFINITY;
            } else {
                max = Integer.parseInt(pieces[1]);
            }
        } else {
            throw new IllegalArgumentException(String.format("There must be either 1 or 2 values specified for the quantifier: provided '%s'", boundariesStr));
        }

        // we need to wrap the last token with the quantifier data
        // so that we know what the min and max apply to
        // <5>
        Token lastToken = ctx.tokens[ctx.tokens.length - 1];
        ctx.tokens[ctx.tokens.length - 1] = new Token(TokenType.REPEAT, new RepeatPayload(min, max, lastToken));
    }

}
