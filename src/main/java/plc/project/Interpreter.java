package plc.project;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.math.RoundingMode;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }


    @Override
    public Environment.PlcObject visit(Ast.Source ast)  {

        // Evaluate globals (fields) followed by functions (methods) by calling the visit method on each
        for (int i = 0; i < ast.getFields().size(); i++) {
            Ast.Field field = ast.getFields().get(i);
            visit(field);
        }
        for (int i = 0; i < ast.getMethods().size(); i++) {
            Ast.Method method = ast.getMethods().get(i);
            visit(method);
        }
        // Return the result of calling the main/0 function and invoke an empty array list since main takes no arguments
        try {
            return scope.lookupFunction("main", 0).invoke(new ArrayList<>());
        }
        // If the evaluation fails, throw a Runtime Exception since the main function does not exist within the source
        catch (RuntimeException failedEvaluation) {
            throw new RuntimeException("The main function does not exist within the source.", failedEvaluation);
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        Environment.PlcObject fieldValue;

        // If the field has an initial value, evaluate it
        if (ast.getValue().isPresent()) {
            fieldValue = visit(ast.getValue().get());
        }
        // Otherwise, use NIL as the default value for any variables not required to be initialized at declaration
        else {
            fieldValue = Environment.NIL;
        }
        // Define a variable in the current scope with the evaluated or default value then return NIL
        scope.defineVariable(ast.getName(), ast.getConstant(), fieldValue);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        // Define the function in the current scope as a lambda function
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            // Capture scope in a variable and set it to be a new child of the scope where the function was defined
            Scope previousScope = scope;
            scope = new Scope(previousScope);
            try {
                // Define variables for the incoming arguments, using the parameter names (Assume correct args provided)
                for (int i = 0; i < ast.getParameters().size(); i++) {
                    scope.defineVariable(ast.getParameters().get(i), false, args.get(i));
                }
                // Evaluate each statement
                for (int i = 0; i < ast.getStatements().size(); i++) {
                    visit(ast.getStatements().get(i));
                }
                // If no Return exception is thrown, return NIL
                return Environment.NIL;
            }
            // Catch any Return exceptions and complete the behavior by returning value contained in a Return exception
            catch (Return returnValue) {
                return returnValue.value;
            }
            finally {
                // Ensure that the scope is properly restored whether it fails or not
                scope = previousScope;
            }
        });
        // Return NIL
        return Environment.NIL;
    }

    // Evaluate the expression and return NIL
    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        Environment.PlcObject declarationValue;
        if (ast.getValue().isPresent()) {
            declarationValue = visit(ast.getValue().get());
        }
        else {
            declarationValue = Environment.NIL;
        }
        // Define a local variable, which is never constant, in the current scope
        scope.defineVariable(ast.getName(), false, declarationValue);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        // Ensure that the receiver is an Ast.Expression.Access to ensure it is assignable
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Invalid receiver.");
        }
        Ast.Expression.Access assignmentReceiver = (Ast.Expression.Access) ast.getReceiver();
        Environment.PlcObject assignmentValue = visit(ast.getValue());

        // If that access expression has a receiver, evaluate it and set the associated field
        if (assignmentReceiver.getReceiver().isPresent()) {
            Environment.PlcObject fieldValue = visit(assignmentReceiver.getReceiver().get());
            fieldValue.getField(assignmentReceiver.getName()).setValue(assignmentValue);
        }
        // Otherwise, lookup and set a variable in the current scope
        else {
            Environment.Variable variable = scope.lookupVariable(assignmentReceiver.getName());

            // Assign to a non-NIL since constant field will cause the evaluation to fail.
            if (variable.getConstant()) {
                throw new RuntimeException("Invalid assignment.");
            }
            variable.setValue(assignmentValue);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {

        // Create a new scope
        Scope previousScope = scope;
        scope = new Scope(previousScope);

        try {
            List<Ast.Statement> thenStatements = ast.getThenStatements();
            List<Ast.Statement> elseStatements = ast.getElseStatements();

            // Use requireType to ensure the condition evaluates to a Boolean and evaluate thenStatements if TRUE
            if (requireType(Boolean.class, visit(ast.getCondition()))) {
                for (int i = 0; i < thenStatements.size(); i++) {
                    visit(thenStatements.get(i));
                }
            }
            // Otherwise evaluate elseStatements
            else {
                for (int i = 0; i < elseStatements.size(); i++) {
                    visit(elseStatements.get(i));
                }
            }
        }
        finally {
            scope = previousScope;
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.For ast) {
        // Initialize the loop variable a single time
        visit(ast.getInitialization());

        // Re-evaluate the condition for each iteration
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            Scope previousScope = scope;
            scope = new Scope(previousScope);

            // If the condition is TRUE, evaluate the statements iteratively
            try {
                List<Ast.Statement> forStatements = ast.getStatements();
                for (int i = 0; i < forStatements.size(); i++) {
                    visit(forStatements.get(i));
                }
            }
            finally {
                scope = previousScope;
            }
            // Perform the increment statement before next re-evaluation
            visit(ast.getIncrement());
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            Scope previousScope = scope;
            scope = new Scope(previousScope);
            try {
                List<Ast.Statement> whileStatements = ast.getStatements();
                for (int i = 0; i < whileStatements.size(); i++) {
                    visit(whileStatements.get(i));
                }
            }
            finally {
                scope = previousScope;
            }
        }
        return Environment.NIL;
    }

    // Evaluate the value and throw it inside a Return exception
    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        Environment.PlcObject returnValue = visit(ast.getValue());
        throw new Return(returnValue);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        // If the literal is null, return Environment.NIL
        if (ast.getLiteral() == null) {
            return Environment.NIL;
        }
        else {
            // Otherwise, create and return a PlcObject containing the literal
            return Environment.create(ast.getLiteral());
        }
    }

    // Evaluate the contained expression and return its value
    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    // Evaluates arguments based on the specific binary operator, returning the appropriate result for the operation
    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        // Evaluate and store the left-hand side (LHS) expression for later use
        Environment.PlcObject leftExpression = visit(ast.getLeft());

        if (ast.getOperator().equals("&&")) {
            // If left expression is not a boolean, return false
            // Not necessary to evaluate right since "&&" requires both true (short circuiting rules)
            if (!requireType(Boolean.class, leftExpression)) {
                return Environment.create(false);
            }
            // Otherwise, evaluate the right-hand side (RHS) expression and return based on if it is a boolean or not
            Environment.PlcObject rightExpression = visit(ast.getRight());
            return Environment.create(requireType(Boolean.class, rightExpression));
        }
        // If left is a boolean, return true without evaluating right since "||" requires only one to be true
        else if (ast.getOperator().equals("||")) {
            if (requireType(Boolean.class, leftExpression)) {
                return Environment.create(true);
            }
            Environment.PlcObject rightExpression = visit(ast.getRight());
            return Environment.create(requireType(Boolean.class, rightExpression));
        }
        else if (ast.getOperator().equals("<") || ast.getOperator().equals("<=") ||
                ast.getOperator().equals(">") || ast.getOperator().equals(">=")) {

            // Evaluate both sides and ensure each are Comparable
            Environment.PlcObject rightExpression = visit(ast.getRight());
            Comparable leftComparable = requireType(Comparable.class, leftExpression);
            Comparable rightComparable = requireType(Comparable.class, rightExpression);

            // Ensure both operands are the same type (class)
            if (!leftComparable.getClass().equals(rightComparable.getClass())) {
                throw new RuntimeException("Operands must be of the same type.");
            }

            // Get and return the comparison result (-1 for less, 0 for equal, 1 for greater)
            int comparison = leftComparable.compareTo(rightComparable);
            boolean result;
            if (ast.getOperator().equals("<")) {
                result = (comparison < 0);
            }
            else if (ast.getOperator().equals("<=")) {
                result = (comparison <= 0);
            }
            else if (ast.getOperator().equals(">")) {
                result = (comparison > 0);
            }
            else {
                result = (comparison >= 0);
            }
            return Environment.create(result);
        }

        // Use Objects.equals to test for equality after evaluating both operands
        else if (ast.getOperator().equals("==")) {
            Environment.PlcObject rightExpression = visit(ast.getRight());
            return Environment.create(Objects.equals(leftExpression.getValue(), rightExpression.getValue()));
        }
        else if (ast.getOperator().equals("!=")) {
            Environment.PlcObject rightExpression = visit(ast.getRight());
            return Environment.create(!Objects.equals(leftExpression.getValue(), rightExpression.getValue()));
        }

        else if (ast.getOperator().equals("+")) {
            Environment.PlcObject rightExpression = visit(ast.getRight());

            try {
                // If either expression is a String, the result is their concatenation
                String leftString = requireType(String.class, leftExpression);
                String rightString = requireType(String.class, rightExpression);
                return Environment.create(leftString + rightString);
            }

            // If the string concatenation failed, try treating LHS as a BigInteger
            catch (RuntimeException failedEvaluation) {
                try {
                    // If the LHS is a BigInteger, then the RHS must also be the same type, and the result is their addition
                    BigInteger leftBigInt = requireType(BigInteger.class, leftExpression);
                    BigInteger rightBigInt = requireType(BigInteger.class, rightExpression);
                    return Environment.create(leftBigInt.add(rightBigInt));
                }

                // Otherwise, it must be a BigDecimal
                catch (RuntimeException failedEvaluation2) {
                    BigDecimal leftBigDec = requireType(BigDecimal.class, leftExpression);
                    BigDecimal rightBigDec = requireType(BigDecimal.class, rightExpression);
                    return Environment.create(leftBigDec.add(rightBigDec));
                }
            }
        }

        // Handle subtraction for BigInteger/BigDecimal and ensure they are the same type
        else if (ast.getOperator().equals("-")) {
            Environment.PlcObject rightExpression = visit(ast.getRight());
            if (requireType(BigInteger.class, leftExpression) != null) {
                BigInteger leftBigInt = requireType(BigInteger.class, leftExpression);
                BigInteger rightBigInt = requireType(BigInteger.class, rightExpression);
                return Environment.create(leftBigInt.subtract(rightBigInt));
            }
            if (requireType(BigDecimal.class, leftExpression) != null) {
                BigDecimal leftBigDec = requireType(BigDecimal.class, leftExpression);
                BigDecimal rightBigDec = requireType(BigDecimal.class, rightExpression);
                return Environment.create(leftBigDec.subtract(rightBigDec));
            }
            throw new RuntimeException("Operands must be of the same type.");
        }

        // Handle multiplication for BigInteger/BigDecimal and ensure they are the same type
        else if (ast.getOperator().equals("*")) {
            Environment.PlcObject rightExpression = visit(ast.getRight());
            if (requireType(BigInteger.class, leftExpression) != null) {
                BigInteger leftBigInt = requireType(BigInteger.class, leftExpression);
                BigInteger rightBigInt = requireType(BigInteger.class, rightExpression);
                return Environment.create(leftBigInt.multiply(rightBigInt));
            }
            if (requireType(BigDecimal.class, leftExpression) != null) {
                BigDecimal leftBigDec = requireType(BigDecimal.class, leftExpression);
                BigDecimal rightBigDec = requireType(BigDecimal.class, rightExpression);
                return Environment.create(leftBigDec.multiply(rightBigDec));
            }
            throw new RuntimeException("Operands must be of the same type.");
        }

        // Handle division for BigInteger/BigDecimal and ensure they are the same type
        else if (ast.getOperator().equals("/")) {
            Environment.PlcObject rightExpression = visit(ast.getRight());
            if (requireType(BigInteger.class, leftExpression) != null) {
                BigInteger leftBigInt = requireType(BigInteger.class, leftExpression);
                BigInteger rightBigInt = requireType(BigInteger.class, rightExpression);

                // If the denominator is zero, the evaluation fails
                if (rightBigInt.equals(BigInteger.ZERO)) {
                    throw new RuntimeException("Division by zero.");
                }
                return Environment.create(leftBigInt.divide(rightBigInt));
            }
            if (requireType(BigDecimal.class, leftExpression) != null) {
                BigDecimal leftBigDec = requireType(BigDecimal.class, leftExpression);
                BigDecimal rightBigDec = requireType(BigDecimal.class, rightExpression);
                if (rightBigDec.compareTo(BigDecimal.ZERO) == 0) {
                    throw new RuntimeException("Division by zero.");
                }
                // Use RoundingMode.HALF_EVEN to round midpoints to the nearest even value (1.5, 2.5 --> 2.0)
                return Environment.create(leftBigDec.divide(rightBigDec, RoundingMode.HALF_EVEN));
            }
            throw new RuntimeException("Operands must be of the same type.");
        }
        else {
            throw new RuntimeException("Invalid Operator.");
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        // If the expression has a receiver, evaluate it and return the value of the appropriate field
        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject accessReceiver = visit(ast.getReceiver().get());
            return requireType(Environment.PlcObject.class, accessReceiver).getField(ast.getName()).getValue();
        }
        // Otherwise, return the value of the appropriate variable in the current scope
        else {
            return scope.lookupVariable(ast.getName()).getValue();
        }
    }
    
    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        List<Environment.PlcObject> functionArguments = new ArrayList<>();
        for (int i = 0; i < ast.getArguments().size(); i++) {
            Ast.Expression argument = ast.getArguments().get(i);
            functionArguments.add(visit(argument));
        }
        // If the expression has a receiver, evaluate it and return the result of calling the appropriate method
        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject functionReceiver = visit(ast.getReceiver().get());
            return requireType(Environment.PlcObject.class, functionReceiver).callMethod(ast.getName(), functionArguments);
        }
        else {
            // Otherwise, return value of invoking the appropriate function in the current scope with evaluated arguments.
            return scope.lookupFunction(ast.getName(), functionArguments.size()).invoke(functionArguments);
        }
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
