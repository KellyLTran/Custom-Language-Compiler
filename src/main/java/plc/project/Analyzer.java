package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {
    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Field ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Method ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException();  // TODO
    }


    // Validate and set type of the literal
    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object literalExpression = ast.getLiteral();
        // NIL, Boolean, Character, String: No additional behavior
        if (literalExpression == null) {
            ast.setType(Environment.Type.NIL);
        }
        // Use instanceof to identify the literal value to distinguish between the type in our language and the type of the Java object
        if (literalExpression instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        }
        if (literalExpression instanceof String) {
            ast.setType(Environment.Type.STRING);
        }
        if (literalExpression instanceof Character)
            ast.setType(Environment.Type.CHARACTER);

        // Integer: Throw a RuntimeException if the value is out of range of a Java int (32-bit signed int)
        if (literalExpression instanceof BigInteger) {
            BigInteger bigIntValue = (BigInteger) literalExpression;

            // Check if within the range of a Java 32-bit signed int
            if (bigIntValue.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 ||
                    bigIntValue.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {
                throw new RuntimeException("Integer out of range.");
            }
            ast.setType(Environment.Type.INTEGER);
        }
        // Decimal: Throws a RuntimeException if the value is out of range of a Java double value (64-bit signed float)
        if (literalExpression instanceof BigDecimal) {
            BigDecimal bigDecValue = (BigDecimal) literalExpression;
            double doubleValue = bigDecValue.doubleValue();

            // If the value does not fit into a double, it will be converted to infinity
            if (Double.isInfinite(doubleValue)) {
                throw new RuntimeException("Decimal out of range.");
            }
            ast.setType(Environment.Type.DECIMAL);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        throw new UnsupportedOperationException();  // TODO
    }

}
