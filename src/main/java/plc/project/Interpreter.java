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

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.For ast) {
        throw new UnsupportedOperationException(); //TODO
    }

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

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException(); //TODO
    }

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
