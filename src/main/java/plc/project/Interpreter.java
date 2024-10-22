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
        Environment.PlcObject value;

        // If the field has an initial value, evaluate it
        if (ast.getValue().isPresent()) {
            value = visit(ast.getValue().get());
        }
        // Otherwise, use NIL as the default value for any variables not required to be initialized at declaration
        else {
            value = Environment.NIL;
        }
        // Define a variable in the current scope with the evaluated or default value then return NIL
        scope.defineVariable(ast.getName(), ast.getConstant(), value);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        // Evaluate the expression and return NIL
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException(); //TODO
    }

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

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    // Returns the literal value as a PlcObject
    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        // If the literal is null, return Environment.NIL
        if (ast.getLiteral() == null) {
            return Environment.NIL;
        }
        else {
            // Otherwise, create a PlcObject containing the literal
            return Environment.create(ast.getLiteral());
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException(); //TODO
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
