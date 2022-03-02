package com.interpreter.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.interpreter.lox.TokenType.*;

/**
 * A Parser for the Lox Language. It takes a list of tokens and parses them
 * using the
 * Recursive Descent Parsing technique.
 */
public class Parser {

    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * The parsing of tokens to produce a list of statements.
     *
     * program -> declaration* ;
     *
     * @return The list of statements.
     */
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();

        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    /**
     * The expression in the Lox Language.
     *
     * expression -> equality;
     *
     * @return The equality expression.
     */
    private Expr expression() {
        return assignment();
    }

    /**
     * A declaration in the Lox Language.
     *
     * declaration -> varDecl | statement;
     *
     * @return
     */
    private Stmt declaration() {
        try {

            if (match(CLASS))
                return classDeclaration();

            if (match(FUN))
                return function("function");

            if (match(VAR))
                return varDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    /**
     * A class declaration statement rule in the Lox Language.
     *
     * classDecl -> "class" IDENTIFIER "{" function* "}"
     *
     * @return The class declaration statement.
     */
    private Stmt classDeclaration() {

        Token name = consume(IDENTIFIER, "Expected a class name.");

        Expr.Variable superclass = null;
        if (match(LESS)) {
            consume(IDENTIFIER, "Expected a superclass name");
            superclass = new Expr.Variable(previous());
        }
        consume(LEFT_BRACE, "Expected '{' before class body.");

        List<Stmt.Function> methods = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"));
        }

        consume(RIGHT_BRACE, "Expected '}' after class body.");

        return new Stmt.Class(name, superclass, methods);
    }

    /**
     * A statement in the Lox Language.
     *
     * statement -> printStmt | ifStmt | block;
     *
     * @return
     */
    private Stmt statement() {

        if (match(PRINT))
            return printStatement();

        if (match(RETURN))
            return returnStatement();

        if (match(WHILE))
            return whileStatement();

        if (match(FOR))
            return forStatement();

        if (match(IF))
            return ifStatement();

        if (match(LEFT_BRACE))
            return new Stmt.Block(block());

        return expressionStatment();
    }

    /**
     * The for statment rule in the Lox Language.
     *
     * forStmt -> "for" "(" ( varDecl | assignmentStmt| ) ";" expression ";"
     * (expression | ) ";" ")" statement;
     *
     * @return The for statement.
     */
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expected '(' after 'for'.");

        // The initializer of the for loop.
        Stmt initializer;

        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatment();
        }

        // The condition of the for loop.
        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expected ';' after loop condition.");

        // The increment of the loop.
        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expected ')' afterfor clause");

        // The body of the for loop.
        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(
                    Arrays.asList(
                            body, new Stmt.Expression(increment)));
        }

        if (condition == null)
            condition = new Expr.Literal(true);

        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    /**
     * A while statement in the Lox Language.
     *
     * whileStmt -> "while" "(" expression ")" statement;
     *
     * @return The while statement.
     */
    private Stmt whileStatement() {

        consume(LEFT_PAREN, "Expected '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ')' after the expression.");

        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    /**
     * The if-else statement in the Lox Language.
     *
     * ifStmt -> "if" "(" expression ")" statement ( "else" statement )?;
     *
     * @return The if-else statement.
     */
    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expected '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;

        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    /**
     * The print statement rule in the Lox Language.
     *
     * printStmt -> 'print' expression;
     *
     * @return The print statement.
     */
    private Stmt printStatement() {

        Expr value = expression();
        consume(SEMICOLON, "Expected ';' after the expression.");

        return new Stmt.Print(value);
    }

    /**
     * The return statement rule in the Lox Language.
     *
     * returStmt -> "return" expression? ;
     *
     * @return
     */
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;

        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Expected ';' after return value.");

        return new Stmt.Return(keyword, value);
    }

    /**
     * Variable declaration rule in the Lox Language.
     *
     * varDecl -> IDENTIFIER '=' expression;
     *
     * @return The variable declaration statement.
     */
    private Stmt varDeclaration() {

        Token name = consume(IDENTIFIER, "Expected a varaible name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expected a ';' after the variable declaration.");

        return new Stmt.Var(name, initializer);
    }

    /**
     * A expression statement in the Lox Language.
     *
     * expressionStmt -> expression;
     *
     * @return The expression statement.
     */
    private Stmt expressionStatment() {

        Expr expr = expression();
        consume(SEMICOLON, "Expected ';' after the expression.");

        return new Stmt.Expression(expr);
    }

    /**
     * The function declaration rule in the Lox Language.
     *
     * funDecl -> "fun" function;
     *
     * @param kind The kind is either "function" or "method".
     * @return The function statement.
     */
    private Stmt.Function function(String kind) {

        Token name = consume(IDENTIFIER, "Expected " + kind + "name,");
        consume(LEFT_PAREN, "Expected '(' after " + kind + " name.");

        List<Token> parameters = new ArrayList<>();

        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }

                parameters.add(
                        consume(IDENTIFIER, "Expected a parameter name."));

            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expected ')' after parameters.");
        consume(LEFT_BRACE, "Expected '{' before " + kind + " body.");

        List<Stmt> body = block();

        return new Stmt.Function(name, parameters, body);
    }

    /**
     * A block of statements in the Lox Language.
     *
     * @return The list of statements in the block.
     */
    private List<Stmt> block() {

        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expected '}' after block statements.");

        return statements;
    }

    /***
     * The assignment expression rule in the Lox Language.
     *
     * assigment -> IDENTIFIER '=' assignment | equality;
     *
     * @return The newly assignment value or the equality expression value.
     */
    private Expr assignment() {

        Expr expr = or();

        if (match(EQUAL)) {

            Token eqals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {

                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);

            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get) expr;
                return new Expr.Set(get.object, get.name, value);
            }

            error(eqals, "Invalid assignment target.");
        }

        return expr;
    }

    /**
     * The logic_or expression rule in the Lox Language.
     *
     * logic_or -> logic_and ( "or" logic_and )* ;
     *
     * @return The result of the logic_or operation or that of logic_and operation.
     */
    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();

            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    /**
     * The logic_and expression rule in the Lox Language.
     *
     * logic_and -> equality ( "and" equality )* ;
     *
     * @return The result of the logic_and operation or that of equality expression.
     */
    private Expr and() {

        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();

            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    /**
     * The equality expression in the Lox Language.
     *
     * equality -> comparison ( ( "!=" | "==" ) comparison )* ;
     *
     * @return The result of the equality operators operation between two comparison
     *         expressions.
     */
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();

            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * The comparison expression rule in the Lox Language.
     *
     * comparison -> term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
     *
     * @return The result of the comparison operators between two term expression
     *         rules.
     */
    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * The term expression rule in the Lox Language.
     *
     * term -> factor ( ( "-" | "+" ) factor )* ;
     *
     * @return The result of the term expression operators between two factor
     *         expression rules.
     */
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * The factor expression rule in the Lox Language.
     *
     * factor -> unary ( ( "*" | "/" ) unary )* ;
     *
     * @return The result of the factor operators operation between two unary
     *         expression rules.
     */
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * The unary expression rule in the Lox Language.
     *
     * unary -> ( "!" | "-" ) unary | primary;
     *
     * @return The result of the unary expression operators operation on unary
     *         itself or primary expression rule.
     */
    private Expr unary() {

        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    /**
     * Finishes the function call.
     *
     * @param callee The function that is called.
     * @return The call expression.
     */
    private Expr finishCall(Expr callee) {

        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }

                arguments.add(expression());

            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expected ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    /**
     * The function call rule in the Lox Language.
     *
     * call -> primary....
     *
     * @return The call expression.
     */
    private Expr call() {
        Expr expr = primary();

        while (true) {

            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {

                Token name = consume(IDENTIFIER, "Expected a property name after '.'.");
                expr = new Expr.Get(expr, name);

            } else {
                break;
            }
        }

        return expr;
    }

    /**
     * The primary expression rule in the Lox Language.
     *
     * primary -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
     *
     * @return
     */
    private Expr primary() {

        if (match(FALSE))
            return new Expr.Literal(false);
        if (match(TRUE))
            return new Expr.Literal(true);
        if (match(NIL))
            return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(SUPER)) {
            Token keyword = previous();
            consume(DOT, "Expected '.' after super.");
            Token method = consume(IDENTIFIER, "Expected superclass method name.");

            return new Expr.Super(keyword, method);
        }

        if (match(THIS))
            return new Expr.This(previous());

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expected ')' after the expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expected an expression");
    }

    /**
     * Checks if any of the given operators is the current token.
     *
     * @param types A list of operator token types.
     * @return true if any of the given types is the current token, false otherwise.
     */
    private boolean match(TokenType... types) {

        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    /**
     * Consumes the current token type and advances to the next.
     *
     * @param type    The token type.
     * @param message The error message, if the given token type
     *                is not the current token type.
     * @return The current token type.
     */
    private Token consume(TokenType type, String message) {
        if (check(type))
            return advance();

        throw error(peek(), message);
    }

    /**
     * Checks if the given token type is the current token type.
     *
     * @param type The operator token type.
     * @return true if the given type is the current token type, false otherwise.
     */
    private boolean check(TokenType type) {
        if (isAtEnd())
            return false;
        return peek().type == type;
    }

    /**
     * Advances to the next token, and returns the previous token.
     *
     * @return THe previous token.
     */
    private Token advance() {
        if (!isAtEnd())
            current++;
        return previous();
    }

    /**
     * Checks if all the tokens have been parsed.
     *
     * @return true if all tokens have been parsed, false otherwise.
     */
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    /**
     * Returns the current token.
     *
     * @return The current token.
     */
    private Token peek() {
        return tokens.get(current);
    }

    /**
     * Returns the previous token.
     *
     * @return The previous token.
     */
    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);

        return new ParseError();
    }

    /**
     * Synchronizes the parser when it goes into panic mode.
     */
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON)
                return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
                default:
                    break;
            }

            advance();
        }
    }
}
