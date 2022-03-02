package com.interpreter.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import java.io.PrintStream;

/**
 * Entry point of the Lox Laguage interpreter.
 */
class Lox {

    static PrintStream out = System.out;
    static PrintStream err = System.err;

    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    /**
     * Load the source file and run.
     *
     * @param path A path to the source file.
     * @throws IOException If the source file is not found.
     */
    private static void runFile(String path) throws IOException {

        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if (hadError)
            System.exit(65);
        if (hadRuntimeError)
            System.exit(70);
    }

    /**
     * Runs the Language's main loop, reading and interpreting one line at a time.
     *
     * @throws IOException
     */
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        while (true) {
            out.print(":> ");
            String line = reader.readLine();
            if (line == null)
                break;
            run(line);
            hadError = false;
        }
    }

    /**
     * Both the prompt and ther file runner are thin wrappers arround this core
     * function.
     *
     * @param source The source file or a line of code.
     */
    private static void run(String source) {

        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error
        if (hadError)
            return;

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        // Stop if there was a resolver error
        if (hadError)
            return;

        interpreter.interpret(statements);
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {

        System.err.println(
                "[line " + line + "] Error " + where + ": " + message);
        hadError = true;

    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end ", message);
        } else {
            report(token.line, String.format(" at '%s'", token.lexeme), message);
        }
    }

    static void runtimeError(RuntimeError error) {
        err.println(String.format("[line %d] RuntimeError: %s", error.token.line, error.getMessage()));
        hadRuntimeError = true;
    }

    public static void main(String[] args) throws IOException {

        if (args.length > 1) {

            out.println("Usage: jlox [script]");
            System.exit(64);

        } else if (args.length == 1) {

            runFile(args[0]);

        } else {

            runPrompt();

        }
    }
}
