package com.interpreter.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {

    private static void defineAst(
            String outputDir, String baseName, List<String> types)
            throws IOException {

        String path = String.format("%s/%s.java", outputDir, baseName);
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package com.interpreter.lox;");

        writer.println("\nimport java.util.List;");

        writer.println("\n\n");
        writer.println(String.format("abstract class %s {", baseName));

        defineVisitor(writer, baseName, types);

        // Ast classes

        for (String type : types) {

            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);

        }

        writer.println("\n\tabstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {

        writer.println("\n\tinterface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();

            writer.println(
                    String.format("\t\tR visit%s%s (%s %s);",
                            typeName, baseName, typeName, baseName.toLowerCase()));
        }
        writer.println("\t}");
    }

    private static void defineType(
            PrintWriter writer, String baseName, String className, String fieldList) {

        writer.println(
                String.format("\n\n\tstatic class %s extends %s {", className, baseName));
        writer.println();

        // Constructor
        writer.println(String.format("\t\t%s (%s) {", className, fieldList));

        // Store parameters in fields.
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println(String.format("\t\t\tthis.%s = %s;", name, name));
        }

        writer.println("\t\t}\n");

        // Visitor pattern
        writer.println("\t\t@Override");
        writer.println("\t\t<R> R accept(Visitor<R> visitor) {");
        writer.println(
                String.format("\t\t\treturn visitor.visit%s%s(this);", className, baseName));
        writer.println("\t\t}\n");

        // Feilds
        for (String field : fields) {
            writer.println(String.format("\t\tfinal %s;", field));
        }

        writer.println("\t}");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }

        String outputDir = args[0];

        defineAst(outputDir, "Expr", Arrays.asList(
                "Assign     : Token name, Expr value",
                "Binary     : Expr left, Token operator, Expr right",
                "Call       : Expr callee, Token paren, List<Expr> arguments",
                "Get        : Expr object, Token name",
                "Grouping   : Expr expression",
                "Literal    : Object literal",
                "Logical    : Expr left, Token operator, Expr right",
                "Set        : Expr object, Token name, Expr value",
                "Super      : Token keyword, Token method",
                "This       : Token keyword",
                "Unary      : Token operator, Expr right",
                "Variable   : Token name"));

        defineAst(outputDir, "Stmt", Arrays.asList(
                "Block          : List<Stmt> statements",
                "Class          : Token name, Expr.Variable superclass," +
                        " List<Stmt.Function> methods",
                "Expression     : Expr expression",
                "Function       : Token name, List<Token> params, List<Stmt> body",
                "If             : Expr condition, Stmt thenBranch, Stmt elseBranch",
                "Print          : Expr expression",
                "Return         : Token keyword, Expr value",
                "Var            : Token name, Expr initializer",
                "While          : Expr condition, Stmt body"));
    }
}
