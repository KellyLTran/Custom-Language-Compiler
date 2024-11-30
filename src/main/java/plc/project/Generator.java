package plc.project;

import java.io.PrintWriter;
import java.util.List;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }


    // Generate a source, including a definition for the Main class that contains our code as well as the public static void main(String[] args) method used as the entry point for Java
    @Override
    public Void visit(Ast.Source ast) {
        // Generate the class header, including the opening brace
        print("public class Main {");
        indent++;
        newline(0);

        // Generate the source's fields (properties in Java)
        List<Ast.Field> sourceFieldList = ast.getFields();
        for (int i = 0; i < sourceFieldList.size(); i++) {
            Ast.Field sourceField = sourceFieldList.get(i);
            newline(indent);
            visit(sourceField);
        }
        if (!sourceFieldList.isEmpty()) {
            newline(0);
        }
        // Generate Java's main method [main(String[] args)]
        newline(indent);
        print("public static void main(String[] args) {");
        indent++;

        // new Main() creates an instance of our Main class
        // .main() calls our language's main method (having a different signature since it does not take arguments)
        // System.exit is used to specify the exit code of a Java program, unlike C/C++ which does so automatically
        newline(indent);
        print("System.exit(new Main().main());");
        indent--;
        newline(indent);
        print("}");

        // Properties are grouped together while the generated Java methods are separated by an empty line with newline(0)
        newline(0);

        // Our grammar does not require that the source node include the method main() we are calling, but
        // Our Analyzer verified that one of the methods within the source node was "main", so
        // Analyzer will handle generating the appropriate exception if no main method is found in the source node
        // Generate the source's methods (methods in Java)
        List<Ast.Method> sourceMethodList = ast.getMethods();
        for (int i = 0; i < sourceMethodList.size(); i++) {
            Ast.Method sourceMethod = sourceMethodList.get(i);
            newline(indent);
            visit(sourceMethod);
        }
        newline(0);
        indent--;
        newline(indent);
        // Generate the closing brace for the class
        print("}");
        return null;
    }


    // Generate a field, expressed in Java as a property within our generated class Main
    @Override
    public Void visit(Ast.Field ast) {
        String fieldTypeName = ast.getVariable().getType().getJvmName();

        // Declare a constant field in Java using the final modifier (use keyword final and a single blank space, separating the modifier from the type
        if (ast.getConstant()) {
            print("final " + fieldTypeName + " " + ast.getName());
        }
        // A non-constant field will consist of the type name and the variable name stored in the AST separated by a single space character
        else {
            print(fieldTypeName + " " + ast.getName());
        }
        // If a value is present, then an equal sign character with surrounding single spaces is generated followed by the variable value (expression)
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }
        // A semicolon is generated at the end
        print(";");
        return null;
    }


    // Generate a method, expressed in Java as a method within our generated class Main
    @Override
    public Void visit(Ast.Method ast) {
        // The method should begin with the method's JVM type name followed by the method name, both of which are found in the AST
        String returnType = ast.getFunction().getReturnType().getJvmName();
        if (returnType.equals("Void")) {
            returnType = "void";
        }
        print(returnType + " " + ast.getName());

        // Generate a comma-separated list of the method parameters surrounded by parenthesis
        print("(");
        for (int i = 0; i < ast.getParameters().size(); i++) {

            // Each parameter will consist of a JVM type name and the parameter name with a single space separating them
            String parameterType = ast.getFunction().getParameterTypes().get(i).getJvmName();
            String parameterName = ast.getParameters().get(i);
            print(parameterType + " " + parameterName);

            // A single space will be placed after the list comma and before the next parameter type
            if (i < ast.getParameters().size() - 1) {
                print(", ");
            }
        }
        // No space will be placed after the opening parenthesis and before the closing parenthesis
        print(")");

        // Following a single space, the opening brace should be generated on the same line
        print(" {");
        indent++;

        // If the statements are empty, the closing brace follows immediately on the same line with no spaces in between
        if (ast.getStatements().isEmpty()) {
            print("}");
            indent--;
        }
        // Otherwise, each statement is generated on a new line with increased indentation
        else {
            List<Ast.Statement> methodStatementList = ast.getStatements();
            for (int i = 0; i < methodStatementList.size(); i++) {
                Ast.Statement methodStatement = methodStatementList.get(i);
                newline(indent);
                visit(methodStatement);
            }
            // Followed by a closing brace on a new line with the original indentation
            indent--;
            newline(indent);
            print("}");
        }
        return null;
    }


    // Generate an expression that should consist of the generated expression found in the AST followed by a semicolon
    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        print(";");
        return null;
    }


    // Generate a declaration expression
    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        // The expression should consist of the type name and the variable name stored in the AST separated by a single space
        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());

        // If a value is present, then an equal sign with surrounding single spaces is generated followed by the generated variable value
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }
        // A semicolon should be generated at the end
        print(";");
        return null;
    }


    // Generate a variable assignment expression
    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        // The name should be the receiver of the variable stored in the AST
        visit(ast.getReceiver());

        // An equal sign character with surrounding single spaces should be generated between the name and value
        print(" = ");

        // The value should be the generated value of the variable, and a semicolon generated at the end
        visit(ast.getValue());
        print(";");
        return null;
    }


    // Generate an If expression
    @Override
    public Void visit(Ast.Statement.If ast) {
        // The expression should consist of the if keyword, followed by a single space and the generated condition with the surrounding parenthesis
        print("if (");
        visit(ast.getCondition());

        // Following a single space, the opening brace should be generated on the same line
        print(") {");
        indent++;

        // If the statements are empty, the closing brace follows immediately on the same line with no spaces in between
        if (ast.getThenStatements().isEmpty()) {
            print("}");
            indent--;
        }
        // Otherwise, each statement is generated on a new line with increased indentation
        else {
            List<Ast.Statement> thenStatements = ast.getThenStatements();
            for (int i = 0; i < thenStatements.size(); i++) {
                newline(indent);
                visit(thenStatements.get(i));
            }
            // Followed by a closing brace on a new line with the original indentation
            indent--;
            newline(indent);
        }
        print("}");
        // If there is an else block, then generate the else keyword on the same line with the same block formatting
        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            indent++;
            List<Ast.Statement> elseStatements = ast.getElseStatements();
            for (int i = 0; i < elseStatements.size(); i++) {
                newline(indent);
                visit(elseStatements.get(i));
            }
            indent--;
            newline(indent);
            print("}");
        }
        // If there is not an else block, then the entire else section is left out of the generated code
        return null;
    }


    // Generate a for loop expression
    @Override
    public Void visit(Ast.Statement.For ast) {
        // The expression should consist of the for keyword followed by a single space and an opening parenthesis
        print("for ( ");

        // Generate optionally an initialization statement
        if (ast.getInitialization() != null) {
            if (ast.getInitialization() instanceof Ast.Statement.Declaration) {
                Ast.Statement.Declaration initDeclaration = (Ast.Statement.Declaration) ast.getInitialization();

                // A blank space will precede and follow each statement of the for signature, whether or not a statement is provided
                print(initDeclaration.getVariable().getType().getJvmName(), " ", initDeclaration.getVariable().getJvmName());
                if (initDeclaration.getValue().isPresent()) {
                    print(" = ");
                    visit(initDeclaration.getValue().get());
                }
            }
            else if (ast.getInitialization() instanceof Ast.Statement.Assignment) {
                Ast.Statement.Assignment initAssignment = (Ast.Statement.Assignment) ast.getInitialization();
                visit(initAssignment.getReceiver());
                print(" = ");
                visit(initAssignment.getValue());
            }
        }
        print(";");

        // Generate a conditional expression
        if (ast.getCondition() != null) {
        print(" ");
            visit(ast.getCondition());
        }
        print(";");

         // Generate optionally an increment statement
        if (ast.getIncrement() != null) {
            print(" ");
            if (ast.getIncrement() instanceof Ast.Statement.Assignment) {
                Ast.Statement.Assignment incrAssignment = (Ast.Statement.Assignment) ast.getIncrement();
                visit(incrAssignment.getReceiver());
                print(" = ");
                visit(incrAssignment.getValue());
            }
        }
        // Following a single space after the closing parenthesis of the signature, the opening brace should be generated on the same line
        print(" ) {");
        indent++;

        // If the statements are empty, the closing brace follows immediately on the same line with no spaces in between
        if (ast.getStatements().isEmpty()) {
            print("}");
        indent--;
        }
        // Otherwise, each statement is generated on a new line with increased indentation
        else {
            List<Ast.Statement> forStatements = ast.getStatements();
            for (int i = 0; i < forStatements.size(); i++) {
                newline(indent);
                visit(forStatements.get(i));
            }
            // Followed by a closing brace on a new line with the original indentation
            indent--;
            newline(indent);
            print("}");
        }
        return null;
    }


    // Generate a while loop expression
    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (");
        visit(ast.getCondition());
        print(") {");
        indent++;
        if (ast.getStatements().isEmpty()) {
            newline(indent);
            print("}");
            indent--;
        }
        else {
            List<Ast.Statement> whileStatements = ast.getStatements();
            for (int i = 0; i < whileStatements.size(); i++) {
                newline(indent);
                visit(whileStatements.get(i));
            }
            indent--;
            newline(indent);
            print("}");
        }
        return null;
    }


    // Generate a return expression
    @Override
    public Void visit(Ast.Statement.Return ast) {
        // The expression should consist of the return keyword followed by a single space and the corresponding generated expression value
        print("return ");
        visit(ast.getValue());
        print(";");
        return null;
    }


    // Generate a literal expression (the value of the literal found in the AST)
    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object literalExpression = ast.getLiteral();
        if (literalExpression instanceof Boolean) {
            print(literalExpression.toString());
        }
        // For characters and strings, include the surrounding quotes (No need to convert escape characters back to their escape sequence)
        else if (literalExpression instanceof Character) {
            print("'" + literalExpression + "'");
        }
        else if (literalExpression instanceof String) {
            print("\"" + literalExpression + "\"");
        }
        else if (literalExpression instanceof BigDecimal) {
            print(literalExpression.toString());
        }
        else if (literalExpression instanceof BigInteger) {
            print(literalExpression.toString());
        }
        return null;
    }


    // Generate a group expression that should be a generated expression surrounded by parentheses
    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");
        return null;
    }


    // Generate a binary expression that generates in order of the left expression, binary operator, and right expression
    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());

        // Generate the corresponding JVM binary operator with a single space on each side
        print(" ", ast.getOperator(), " ");
        visit(ast.getRight());
        return null;
    }


    // Generate an access expression
    @Override
    public Void visit(Ast.Expression.Access ast) {
        // Check if the receiver, such as object.field, is present
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            print(".");
        }
        // The name used should be the jvmName of the variable stored in the AST
        print(ast.getVariable().getJvmName());
        return null;
    }


    // Generate a function expression
    @Override
    public Void visit(Ast.Expression.Function ast) {
        // Check if the receiver, such as object.field, is present
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            print(".");
        }
        // The name used should be the jvmName of the function stored in the AST
        print(ast.getFunction().getJvmName());

        // Followed by a comma-separated list of the generated argument expressions surrounded by parenthesis
        print("(");
        List<Ast.Expression> expressionArguments = ast.getArguments();
        for (int i = 0; i < expressionArguments.size(); i++) {
            visit(expressionArguments.get(i));
            if (i < expressionArguments.size() - 1) {
                print(", ");
            }
        }
        print(")");
        return null;
    }
}
