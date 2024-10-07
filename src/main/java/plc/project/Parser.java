package plc.project;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling those functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, for, while, or return
     * statement, then it is an expression/assignment statement.
     */

    // statement ::= ... | expression ('=' expression)? ';'
    public Ast.Statement parseStatement() throws ParseException {
        // Initialize the left expression with Ast.Expression
        Ast.Expression leftExpression = parseExpression();

        // If the "=" operator is present, match as an operator token then initialize the right expression
        if (peek("=")) {
            match(Token.Type.OPERATOR);
            Ast.Expression rightExpression = parseExpression();

            // If the statement ends with the required semicolon, return the statement assignment
            if (peek(";")) {
                match(";");
                return new Ast.Statement.Assignment(leftExpression, rightExpression);
            }
            // Otherwise, throw a parse exception and compute the index for where the semicolon should have been
            else {
                throw new ParseException("Expected ';'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        }
        // If the "=" operator is not present, return only the left statement expression
        else if (peek(";")) {
            match(";");
            return new Ast.Statement.Expression(leftExpression);
        }
        else {
            throw new ParseException("Expected ';'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Statement.For parseForStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    // expression ::= logical_expression
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    // logical_expression ::= comparison_expression (('&&' | '||') comparison_expression)*
    public Ast.Expression parseLogicalExpression() throws ParseException {
        // Parse the left expression, which should be an equality/comparison expression, of the logical expression
        Ast.Expression leftExpression = parseEqualityExpression();

        // Continue to parse while "&&" or "||" are present, then parse the right expression
        while (peek("&&") || peek("||")) {
            String operatorToken = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);
            Ast.Expression rightExpression = parseEqualityExpression();

            // Update the leftExpression to a binary expression that includes the operator token and right expression
            leftExpression = new Ast.Expression.Binary(operatorToken, leftExpression, rightExpression);
        }
        return leftExpression;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    // comparison_expression ::= additive_expression (('<' | '<=' | '>' | '>=' | '==' | '!=') additive_expression)*
    public Ast.Expression parseEqualityExpression() throws ParseException {
        Ast.Expression leftExpression = parseAdditiveExpression();
        while (peek("<") || peek("<=") || peek(">") ||
                peek(">=") || peek("==") || peek("!=")) {
            String operatorToken = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);
            Ast.Expression rightExpression = parseAdditiveExpression();
            leftExpression = new Ast.Expression.Binary(operatorToken, leftExpression, rightExpression);
        }
        return leftExpression;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    // additive_expression ::= multiplicative_expression (('+' | '-') multiplicative_expression)*
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression leftExpression = parseMultiplicativeExpression();
        while (peek("+") || peek("-")) {
            String operatorToken = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);
            Ast.Expression rightExpression = parseMultiplicativeExpression();
            leftExpression = new Ast.Expression.Binary(operatorToken, leftExpression, rightExpression);
        }
        return leftExpression;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    // multiplicative_expression ::= secondary_expression (('*' | '/') secondary_expression)*
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression leftExpression = parseSecondaryExpression();
        while (peek("*") || peek("/")) {
            String operatorToken = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);
            Ast.Expression rightExpression = parseSecondaryExpression();
            leftExpression = new Ast.Expression.Binary(operatorToken, leftExpression, rightExpression);
        }
        return leftExpression;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    // secondary_expression ::= primary_expression ('.' identifier ('(' (expression (',' expression)*)? ')')?)*
    public Ast.Expression parseSecondaryExpression() throws ParseException {
        Ast.Expression leftExpression = parsePrimaryExpression();
        String identifierToken = "";

        // If "." is present, an identifier must follow it
        while (peek(".")) {
            match(".");
            if (peek(Token.Type.IDENTIFIER)) {
                identifierToken = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
            }
            else {
                throw new ParseException("Expected identifier.", tokens.get(0).getIndex());
            }
            // If an opening parentheses is present, match then create an array list
            if (peek("(")) {
                match("(");
                List<Ast.Expression> expressionsList = new ArrayList<>();

                // While the closing parentheses is not found yet, parse and add each expression to the array list
                while (!peek(")")) {
                    expressionsList.add(parseExpression());
                    if (peek(",")) {
                        match(",");
                    }
                }
                // After all expressions are added, match the closing parentheses and return the expression function with the list
                match(")");
                leftExpression = new Ast.Expression.Function(Optional.of(leftExpression), identifierToken, expressionsList);
            }
            // If an opening parentheses is not present, return only the left expression and identifier
            else {
                leftExpression = new Ast.Expression.Access(Optional.of(leftExpression), identifierToken);
            }
        }
        return leftExpression;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    // primary_expression ::=
    //    'NIL' | 'TRUE' | 'FALSE' |
    //    integer | decimal | character | string |
    //    '(' expression ')' |
    //    identifier ('(' (expression (',' expression)*)? ')')?
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (peek("NIL")) {
            match("NIL");
            return new Ast.Expression.Literal(null);
        }
        else if (peek("TRUE")) {
            match("TRUE");
            return new Ast.Expression.Literal(true);
        }
        else if (peek("FALSE")) {
            match("FALSE");
            return new Ast.Expression.Literal(false);
        }
        else if (peek(Token.Type.INTEGER)) {

            // Convert the token's literal value to a BigInteger as represented in the AST
            BigInteger integerToken = new BigInteger(tokens.get(0).getLiteral());
            match(Token.Type.INTEGER);
            return new Ast.Expression.Literal(integerToken);
        }
        else if (peek(Token.Type.DECIMAL)) {
            BigDecimal decimalToken = new BigDecimal(tokens.get(0).getLiteral());
            match(Token.Type.DECIMAL);
            return new Ast.Expression.Literal(decimalToken);
        }
        else if (peek(Token.Type.CHARACTER)) {

            // If the character has a length of 3, including the single quotes "''", then match it as a character token
            String charToken = tokens.get(0).getLiteral();
            if (charToken.length() == 3) {
                match(Token.Type.CHARACTER);

                // Return the character specifically inside the single quotes at index 1 to exclude the quotes
                return new Ast.Expression.Literal(charToken.charAt(1));
            }
            // Otherwise, handle escape sequence cases that caused the higher length by replacing each with its literal value
            else {
                charToken = charToken.replace("\\b", "\b");
                charToken = charToken.replace("\\n", "\n");
                charToken = charToken.replace("\\r", "\r");
                charToken = charToken.replace("\\t", "\t");
                charToken = charToken.replace("\\\"", "\"");
                charToken = charToken.replace("\\\\", "\\");
                charToken = charToken.replace("\\\'", "\'");
                match(Token.Type.CHARACTER);
                return new Ast.Expression.Literal(charToken.charAt(1));
            }
        }
        else if (peek(Token.Type.STRING)) {
            String stringToken = tokens.get(0).getLiteral();

            // Exclude the first and last characters of the string, which removes the quotes
            stringToken = stringToken.substring(1, stringToken.length() - 1);

            // Replace each escape sequence with its literal value that it represents
            stringToken = stringToken.replace("\\b", "\b");
            stringToken = stringToken.replace("\\n", "\n");
            stringToken = stringToken.replace("\\r", "\r");
            stringToken = stringToken.replace("\\t", "\t");
            stringToken = stringToken.replace("\\\"", "\"");
            stringToken = stringToken.replace("\\\\", "\\");
            stringToken = stringToken.replace("\\'", "'");
            match(Token.Type.STRING);
            return new Ast.Expression.Literal(stringToken);
        }
        // If an open parentheses is present, initialize the expression that follows with Ast.Expression
        else if (peek("(")) {
            match("(");
            Ast.Expression expression = parseExpression();

            // If the expression is properly closed with closing parentheses, return it as a grouped expression
            if (peek(")")) {
                match(")");
                return new Ast.Expression.Group(expression);
            }
            // Otherwise, throw a parse exception for the missing closed parentheses
            else {
                throw new ParseException("Expected ')'", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        }
        // If an identifier is present and an opening parentheses follows, parse and add each expression to an array list
        else if (peek(Token.Type.IDENTIFIER)) {
            String identifierToken = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if (peek("(")) {
                match("(");
                List<Ast.Expression> expressionsList = new ArrayList<>();
                while (!peek(")")) {
                    expressionsList.add(parseExpression());
                    if (peek(",")) {
                        match(",");
                    }
                }
                match(")");
                return new Ast.Expression.Function(Optional.empty(), identifierToken, expressionsList);
            }
            else {
                return new Ast.Expression.Access(Optional.empty(), identifierToken);
            }
        }
        else {
            throw new ParseException("Invalid primary expression.", tokens.get(0).getIndex());
        }
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            }
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            }
            else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            }
            else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
