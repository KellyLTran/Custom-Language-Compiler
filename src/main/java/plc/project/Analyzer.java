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

        // Integer: If the value is out of range of a Java int (32-bit signed int), throw a RuntimeException
        if (literalExpression instanceof BigInteger) {
            BigInteger bigIntValue = (BigInteger) literalExpression;
            if (bigIntValue.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 ||
                    bigIntValue.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {
                throw new RuntimeException("Integer out of range.");
            }
            ast.setType(Environment.Type.INTEGER);
        }
        // Decimal: If the value is out of range of a Java double value (64-bit signed float), throw a RuntimeException
        if (literalExpression instanceof BigDecimal) {
            BigDecimal bigDecValue = (BigDecimal) literalExpression;
            double doubleValue = bigDecValue.doubleValue();

            // If the value is infinity, then it does not fit into a double so throw a RuntimeException
            if (Double.isInfinite(doubleValue)) {
                throw new RuntimeException("Decimal out of range.");
            }
            ast.setType(Environment.Type.DECIMAL);
        }
        return null;
    }

    // Validate a group expression
    @Override
    public Void visit(Ast.Expression.Group ast) {
        visit(ast.getExpression());

        // If the expression is not a binary expression (the only type affected by precedence), throw a RunTimeException
        if (!(ast.getExpression() instanceof Ast.Expression.Binary)) {
            throw new RuntimeException("The contained expression is not a binary expression.");
        }
        // Set the type of the group expression to be the type of the contained expression
        ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());
        String operator = ast.getOperator();

        // If it is a logical operator, ensure both operands are Boolean and set the result type to Boolean
        if (operator.equals("&&") || operator.equals("||")) {
            requireAssignable(Environment.Type.BOOLEAN, ast.getLeft().getType());
            requireAssignable(Environment.Type.BOOLEAN, ast.getRight().getType());
            ast.setType(Environment.Type.BOOLEAN);
        }

        // If it is a comparison operator, ensure both sides are Comparable and of the same type
        else if (operator.equals("<") || operator.equals("<=") || operator.equals(">")
                || operator.equals(">=") || operator.equals("==") || operator.equals("!=")) {
            requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
            requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
            ast.setType(Environment.Type.BOOLEAN);
        }

        else if (operator.equals("+")) {
            // If either side is a String, the result is a String, and the other side can be anything
            if (ast.getLeft().getType().equals(Environment.Type.STRING) || ast.getRight().getType().equals(Environment.Type.STRING)) {
                ast.setType(Environment.Type.STRING);
            }
            else {
                // Otherwise, the LHS must be an Integer or Decimal and both the RHS
                if (!ast.getLeft().getType().equals(Environment.Type.INTEGER) && !ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                    throw new RuntimeException("Left operand is not an Integer or Decimal.");
                }
                else {
                    requireAssignable(ast.getLeft().getType(), ast.getRight().getType());
                    // Set the result type to the same as the LHS
                    ast.setType(ast.getLeft().getType());
                }
            }
        }

        else if (operator.equals("-") || operator.equals("*") || operator.equals("/")) {
            if (!ast.getLeft().getType().equals(Environment.Type.INTEGER) && !ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                throw new RuntimeException("Left operand is not an Integer or Decimal.");
            }
            requireAssignable(ast.getLeft().getType(), ast.getRight().getType());
            // Set both the RHS and result type to the same as the LHS
            ast.setType(ast.getLeft().getType());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    // Return void if the assignable type requirements are met; otherwise, generate the exception
    public static void requireAssignable(Environment.Type target, Environment.Type type) {

        // If the target type is Any, anything from our language can be assigned to it (similar to Object class in Java)
        if (target.equals(Environment.Type.ANY)) {
            return;
        }
        // If the target type is Comparable, it can be assigned any of our defined Comparable types:
        // Integer, Decimal, Character, and String (Do not need to support any other Comparable types)
        if (target.equals(Environment.Type.COMPARABLE) &&
                (type.equals(Environment.Type.INTEGER) ||
                        type.equals(Environment.Type.DECIMAL) ||
                        type.equals(Environment.Type.CHARACTER) ||
                        type.equals(Environment.Type.STRING))) {
            return;
        }
        // If the target type does not match the type being used or assigned, throw a RuntimeException
        if (!target.equals(type)) {
            throw new RuntimeException("Type does not match.");
        }
    }
}
