package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
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

        // Visit fields followed by methods (left-depth-first traversal of the AST)
        for (int i = 0; i < ast.getFields().size(); i++) {
            visit(ast.getFields().get(i));
        }
        for (int i = 0; i < ast.getMethods().size(); i++) {
            visit(ast.getMethods().get(i));
        }
        Environment.Function mainFunction;

        // Ensure that the main/0 function (name = main, arity = 0) exists and that it has an Integer return type
        try {
            mainFunction = scope.lookupFunction("main", 0);
        }
        catch (RuntimeException failedEvaluation) {
            throw new RuntimeException("A main/0 function does not exist.");
        }
        if (!mainFunction.getReturnType().equals(Environment.Type.INTEGER)) {
            throw new RuntimeException("The main/0 function does not have an Integer return type.");
        }
        return null;
    }

    // Define a function in the current scope based on certain conditions and set it in the Ast (Ast.Field#setVariable)
    @Override
    public Void visit(Ast.Field ast) {
        // Ensure the type registered in the Environment has the same name as the one in the AST
        Environment.Type fieldType = Environment.getType(ast.getTypeName());

        // If the value is present, ensure that the value is assignable to the field (its type must be a subtype of the field's type)
        if (ast.getValue().isPresent()) {

            // Visit value before the variable is defined to ensure the field is not used before it is initialized
            visit(ast.getValue().get());
            requireAssignable(fieldType, ast.getValue().get().getType());
        }
        // Define the variable in the current scope where the variable's name and jvmName are both the name of the field,
        // its type is registered in the Environment with the same name as the one in the AST, its value is Environment.NIL
        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), fieldType, false, Environment.NIL));
        return null;
    }


    // Define a function in the current scope based on certain conditions and set it in the Ast (Ast.Method#setFunction)
    @Override
    public Void visit(Ast.Method ast) {
        List<Environment.Type> methodParTypes = new ArrayList<>();
        List<String> methodParTypeNames = ast.getParameterTypeNames();

        // Retrieve the function's parameter and return types from the environment using the corresponding names in the method
        for (int i = 0; i < methodParTypeNames.size(); i++) {
            String typeName = methodParTypeNames.get(i);
            methodParTypeNames.set(i, String.valueOf(Environment.getType(typeName)));
        }
        // Coordinate with Ast.Statement.Return and save the expected return type in a variable so it is known
        Environment.Type returnType;
        if (ast.getReturnTypeName().isPresent()) {
            returnType = Environment.getType(ast.getReturnTypeName().get());
        }
        // If the return type is not present in the AST, it is Environment.Type.NIL (optional in the function declaration)
        else {
            returnType = Environment.Type.NIL;
        }
        // The function's name and jvmName are both the name of the method
        // The method's function is args -> Environment.NIL, always returning nil since it is not used by the analyzer
        ast.setFunction(scope.defineFunction(ast.getName(), ast.getName(), methodParTypes, returnType, args -> Environment.NIL));

        // Visit all method's statements inside a new scope containing variables for each parameter
        // Unlike fields, this is done after the method is defined to allow for recursive methods
        Scope previousScope = scope;
        scope = new Scope(previousScope);
        try {
            List<String> methodParNames = ast.getParameters();
            for (int i = 0; i < methodParNames.size(); i++) {
                String methodParName = methodParNames.get(i);
                Environment.Type methodParType = methodParTypes.get(i);
                scope.defineVariable(methodParName, methodParName, methodParType, false, Environment.NIL);
            }
            List<Ast.Statement> methodStatements = ast.getStatements();
            for (int i = 0; i < methodStatements.size(); i++) {
                Ast.Statement methodStatement = methodStatements.get(i);
                visit(methodStatement);
            }
        }
        finally {
            scope = previousScope;
        }
        return null;
    }

    // Validate the expression statement
    @Override
    public Void visit(Ast.Statement.Expression ast) {

        // If the expression is not an Ast.Expression.Function (only type of expression that can cause a side effect), throw a Runtime Exception
        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("The contained expression is not a function expression.");
        }
        visit(ast.getExpression());
        return null;
    }

    // Define and set a variable in the current scope based on certain conditions
    @Override
    public Void visit(Ast.Statement.Declaration ast) {

        // If the value of the declared variable is present, it must be visited before the variable is defined
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());

            // Ensure that the value is assignable to the variable
            requireAssignable(ast.getValue().get().getType(), ast.getValue().get().getType());
        }
        // If the type of the declared variable is present, it must be the type registered in the Environment with the same name as the one in the AST
        if (ast.getTypeName().isPresent()) {

            // The variable's value is Environment.NIL since it is not used by the analyzer
            Environment.Variable declaredVariable = scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName().get()), false, Environment.NIL);
            ast.setVariable(declaredVariable);
        }
        // Otherwise, the variable's type is the type of the value
        else {
            Environment.Variable declaredVariable = scope.defineVariable(ast.getName(), ast.getName(), ast.getValue().get().getType(), false, Environment.NIL);
            ast.setVariable(declaredVariable);

            // If there was a missing type, return null, not an exception
            return null;
        }
        // If neither are present, throw a Runtime Exception
        if (!ast.getTypeName().isPresent() && !ast.getValue().isPresent()) {
            throw new RuntimeException("The declared variable does not have a type or value.");
        }
        return null;
    }

    // Validate an assignment statement
    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        // If the receiver is not an access expression (since any other type is not assignable), throw a Runtime Exception
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("The contained expression is not an access expression.");
        }
        visit(ast.getReceiver());
        visit(ast.getValue());

        // Ensure that the value is assignable to the receiver
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        return null;
    }

    // Validate an if statement
    @Override
    public Void visit(Ast.Statement.If ast) {
        // Ensure that the condition is of type Boolean
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        // If the thenStatements list is empty, throw a RuntimeException
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("Then statements list is empty.");
        }
        // Visit the then and else statements inside of a new scope for each one
        Scope previousScope = scope;
        scope = new Scope(previousScope);
        try {
            List<Ast.Statement> thenStatements = ast.getThenStatements();
            List<Ast.Statement> elseStatements = ast.getElseStatements();
            for (int i = 0; i < thenStatements.size(); i++) {
                visit(thenStatements.get(i));
            }
            for (int i = 0; i < elseStatements.size(); i++) {
                visit(elseStatements.get(i));
            }
        }
        // Ensure that the scope is properly restored whether it fails or not
        finally {
            scope = previousScope;
        }
        return null;
    }

    // Validates a for statement. Throws a RuntimeException if:
    //
    //The identifier, when present, is not a Comparable type.
    //The condition is not of type Boolean.
    //The expression in the increment is not the same type as the identifier given at the start of the for signature.
    //The list of statement is empty.
    //After visiting the condition, visit each case (including the default) statement inside of a new scope for each case.
    @Override
    public Void visit(Ast.Statement.For ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    // Validate a while statement by visiting all of the while loop's statements in a new scope
    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
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
        return null;
    }

    // Validate a return statement
    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        Environment.Variable returnType = scope.lookupVariable("returnType");

        // Ensure that the value is assignable to the return type of the function that the statement is in
        requireAssignable(returnType.getType(), ast.getValue().getType());
        return null;
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

    // Validate an access expression and set the variable of the expression
    @Override
    public Void visit(Ast.Expression.Access ast) {
        // If the receiver is present, then the variable is a field of the receiver
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());

            // Set the type of the expression to be the type of the variable
            Environment.Type receiverType = ast.getReceiver().get().getType();
            Environment.Variable variableField = receiverType.getField(ast.getName());
            ast.setVariable(variableField);
        }
        // Otherwise, the variable is in the current scope
        else {
            Environment.Variable variableScope = scope.lookupVariable(ast.getName());
            ast.setVariable(variableScope);
        }
        return null;
    }

    // Validate a function expression and set the function of the expression
    @Override
    public Void visit(Ast.Expression.Function ast) {
        List<Environment.Type> functionTypes = new ArrayList<>();

        // Visit each argument and get their type
        for (int i = 0; i < ast.getArguments().size(); i++) {
            Ast.Expression functionArgument = ast.getArguments().get(i);
            visit(functionArgument);
            functionTypes.add(functionArgument.getType());
        }
        // If the receiver is present, then the function is a method of the receiver
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());

            // Set the type of the expression to be the return type of the function
            Environment.Type receiverType = ast.getReceiver().get().getType();
            Environment.Function functionMethod = receiverType.getFunction(ast.getName(), ast.getArguments().size());
            ast.setFunction(functionMethod);

            // Ensure that the provided arguments are assignable to the corresponding parameter types of the function
            List<Environment.Type> functionParameters = functionMethod.getParameterTypes();
            requireAssignable(functionParameters.get(0), receiverType);
            for (int i = 0; i < ast.getArguments().size(); i++) {

                // Since the first parameter of a method (retrieved from the receiver) is the object the method is being called on
                // (like self in Python), the first argument is at index 1 in the parameters, not 0, only for methods
                requireAssignable(functionParameters.get(i + 1), functionTypes.get(i));
            }
        }
        // Otherwise, the function is in the current scope
        else {
            Environment.Function functionScope = scope.lookupFunction(ast.getName(), functionTypes.size());
            ast.setFunction(functionScope);
            List<Environment.Type> functionParameters = functionScope.getParameterTypes();
            for (int i = 0; i < ast.getArguments().size(); i++) {
                requireAssignable(functionParameters.get(i), functionTypes.get(i));
            }
        }
        return null;
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
