package com.interpreter.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.interpreter.lox.TokenType.*;

public class Scanner {

    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();

        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
    }

    private int start = 0;
    private int current = 0;
    private int line = 1;

    Scanner(String source) {
        this.source = source;
    }

    /**
     * Scans the tokens and populates the list of tokens.
     *
     * @return A list of tokens.
     */
    List<Token> scanTokens() {

        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    /**
     * Scans one character at a time.
     */
    private void scanToken() {

        char c = advance();

        switch (c) {
            // Find Single Character tokens.
            case '(':
                addToken(LEFT_PAREN);
                break;
            case ')':
                addToken(RIGHT_PAREN);
                break;
            case '{':
                addToken(LEFT_BRACE);
                break;
            case '}':
                addToken(RIGHT_BRACE);
                break;
            case ',':
                addToken(COMMA);
                break;
            case '.':
                addToken(DOT);
                break;
            case '-':
                addToken(MINUS);
                break;
            case '+':
                addToken(PLUS);
                break;
            case ';':
                addToken(SEMICOLON);
                break;
            case '*':
                addToken(STAR);
                break;

            // Find Single or two character tokens.
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;

            // Find longer lexemes
            case '/':
                if (match('/')) {

                    // A comment goes until the end of the line.
                    while (peek() != '\n' && !isAtEnd())
                        advance();

                } else if (match('*')) { // CHALLENGE

                    // Muti-Line comment.
                    comment();

                } else {

                    addToken(SLASH);

                }

                // Find and skip whitespace and new lines
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                break;

            // Find String Literals
            case '"':
                string();
                break;

            default:
                if (isDigit(c)) {

                    number();

                } else if (isAlpha(c)) {

                    identifier();

                } else {

                    Lox.error(line, "Unexpected character " + c);

                }
                break;

        }
    }

    /**
     * CHALLENGE:
     * This consumes the tokens making up a Multi-line comment.
     */
    private void comment() {
        /**
         * Consume characters until, '/*' for nested comments, or '*\/' to end the
         * comment.
         */

        while (true) {

            if (match('*')) {
                if (match('/')) {
                    return;
                }
            }

            if (peek() == '\n')
                line++;

            if (match('/')) {
                if (match('*')) {
                    comment();
                }
            }

            if (isAtEnd()) {

                Lox.error(line, "Unterminated comment.");
                return;

            } else {
                advance();
            }
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek()))
            advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);

        if (type == null)
            type = IDENTIFIER;

        addToken(type);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z' ||
                c >= 'A' && c <= 'Z' ||
                c == '_');
    }

    private boolean isAlphaNumeric(char c) {
        return (isAlpha(c) || isDigit(c));
    }

    /**
     * We consume as many digits as posible for the integer part, we look for
     * dot(.), which denotes a decimal.
     * And many digits past the decimal point are consumed.
     */
    private void number() {

        while (isDigit(peek()))
            advance();

        // Look for the decimal part.

        if (peek() == '.' && isDigit(peekNext())) {
            advance();

            while (isDigit(peek()))
                advance();
        }

        addToken(NUMBER,
                Double.parseDouble(source.substring(start, current)));
    }

    /**
     * Every character that is not the closing delimiter, is consumed as part of the
     * string literal.
     */
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n')
                line++;
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        // The closing '"'
        advance();

        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    /**
     * Check if the next character is the same as the expected character.
     *
     * @param expected The expected character.
     * @return true if the current character is equal to the expected character,
     *         false otherwise.
     */
    private boolean match(char expected) {
        if (isAtEnd())
            return false;
        if (source.charAt(current) != expected)
            return false;

        current++;
        return true;
    }

    /**
     * Have one-character lookahead.
     *
     * @return The next character after the current one, if not at the end of the
     *         source input.
     */
    private char peek() {
        if (isAtEnd())
            return '\0';
        return source.charAt(current);
    }

    /**
     * Does two character lookahead.
     *
     * @return The second next character, if not at the end of the source input.
     */
    private char peekNext() {
        if (current + 1 >= source.length())
            return '\0';
        return source.charAt(current + 1);
    }

    /**
     * Checks if the current character is a digit.
     *
     * @param c
     * @return
     */
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Checks if the end of the source has been reached.
     *
     * @return true if the end of the source has been reached, false otherwise.
     */
    private boolean isAtEnd() {
        return current >= source.length();
    }

    /**
     * Advances to the next character.
     *
     * @return The current character.
     */
    private char advance() {
        return source.charAt(current++);
    }

    /**
     * Adds token types with no literals.
     *
     * @param type The token type to add to the list of tokens.
     */
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    /**
     * Adds the specified token type and its literal value to the list of tokens.
     *
     * @param type    The token type.
     * @param literal The literal value.
     */
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}
