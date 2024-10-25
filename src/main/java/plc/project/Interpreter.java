package plc.project;

import java.util.ArrayList;

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
            throw new RuntimeException("Error: The main function does not exist within the source.", failedEvaluation);
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
                // Restore the scope when finished
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

    // First, ensure that the receiver is an Ast.Expression.Access (any other type is not assignable causing the evaluation to fail).
    // If that access expression has a receiver, evaluate it and set the associated field, otherwise lookup and set a variable in the current scope. Returns NIL.
    // Assignments to a non-NIL, constant field will cause the evaluation to fail.
    // Returns NIL.
    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
    }


    // Ensure the condition evaluates to a Boolean (hint: use requireType), otherwise the evaluation fails.
    // Inside of a new scope, if the condition is TRUE, evaluate thenStatements, otherwise evaluate elseStatements. Returns NIL.
    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    // Ensure the condition evaluates to a Boolean (hint: use requireType). If the condition is TRUE, evaluate the statements and repeat. Remember...
    //the initialization step is performed a single time.
    //to re-evaluate the condition each iteration.
    //to perform the increment statement after all statements within the body of the for, but prior to re-evaluating the condition.
    //Returns NIL.
    @Override
    public Environment.PlcObject visit(Ast.Statement.For ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    // Ensure the condition evaluates to a Boolean (hint: use requireType), otherwise the evaluation fails. If the condition is TRUE, evaluate the statements and repeat.
    //Remember to re-evaluate the condition itself each iteration!
    //Returns NIL.
    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException(); //TODO
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

    // Evaluates arguments based on the specific binary operator, returning the appropriate result for the operation (hint: use requireType and Environment.create as needed).  Whenever something is observed but not permitted, the evaluation fails.
    //&&/||:
    //Evaluate the LHS expression, which must be a Boolean. Following short circuiting rules, evaluate the LHS expression, which also must be a Boolean, if necessary.
    //</<=/>/>=:
    //Evaluate the LHS expression, which must be Comparable, and compare it to the RHS expression, which must be the same type (class) as the LHS.
    //You will need to determine how to use Comparable (hint: review our lectures at the beginning of the semester and check out the Java docs).
    //==/!=:
    //Evaluate both operands and test for equality using Objects.equals (this is not the standard equals method, consider what this does by reading the Java docs and recalling what we have said about ==/!= ).
    //+:
    //Evaluate both the LHS and RHS expressions. If either expression is a String, the result is their concatenation. Else, if the LHS is a BigInteger/BigDecimal, then the RHS must also be the same type (a BigInteger/BigDecimal) and the result is their addition, otherwise the evaluation fails.
    //-/*:
    //Evaluate both the LHS and RHS expressions. If the LHS is a BigInteger/BigDecimal, then the RHS must also be the same type (a BigInteger/BigDecimal) and the result is their subtraction/multiplication, otherwise the evaluation fails.
    //Evaluate both the LHS and RHS expressions. If the LHS is a BigInteger/BigDecimal, then the RHS must also be the same type (a BigInteger/BigDecimal) and the result is their division, otherwise throw an exception.
    //For BigDecimal, use RoundingMode.HALF_EVEN, which rounds midpoints to the nearest even value (1.5, 2.5
    // 2.0). This is actually the default mode in Python, which can catch developers off-guard as they often do not expect this behavior.
    //If the denominator is zero, the evaluation fails.
    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {

        // If the expression has a receiver, evaluate it and return the value of the appropriate field
        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject receiver = visit(ast.getReceiver().get());
            return receiver.getField(ast.getName()).getValue();
        }
        // Otherwise, return the value of the appropriate variable in the current scope
        else {
            return scope.lookupVariable(ast.getName()).getValue();
        }
    }

    // If the expression has a receiver, evaluate it and return the result of calling the appropriate method,
    // otherwise return the value of invoking the appropriate function in the current scope with the evaluated arguments.
    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO
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
