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
        newline(0);

        // Generate the source's fields (properties in Java)
        List<Ast.Field> sourceFieldList = ast.getFields();
        for (int i = 0; i < sourceFieldList.size(); i++) {
            Ast.Field sourceField = sourceFieldList.get(i);
            newline(1);
            visit(sourceField);
        }
        // Generate Java's main method [main(String[] args)]
        newline(1);
        print("public static void main(String[] args) {");
        newline(2);

        // new Main() creates an instance of our Main class
        // .main() calls our language's main method (having a different signature since it does not take arguments)
        // System.exit is used to specify the exit code of a Java program, unlike C/C++ which does so automatically
        print("System.exit(new Main().main());");
        newline(1);
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
            newline(1);
            visit(sourceMethod);
            newline(0);
        }
        // Generate the closing brace for the class
        print("}");
        return null;
    }

    // Generate a field, expressed in Java as a property within our generated class Main
    @Override
    public Void visit(Ast.Field ast) {

        // Declare a constant field in Java using the final modifier (use keyword final and a single blank space, separating the modifier from the type
        if (ast.getConstant()) {
            print("final " + ast.getTypeName() + " " + ast.getName());
        }
        // A non-constant field will consist of the type name and the variable name stored in the AST separated by a single space character
        else {
            print(ast.getTypeName() + " " + ast.getName());
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

    @Override
    public Void visit(Ast.Method ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException(); //TODO
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

    @Override
    public Void visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }
}
