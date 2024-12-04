package plc.project;
import java.util.List;
import java.util.ArrayList;
/**
 * The lexer works through three main functions:
 *
 * - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 * - {@link #lexToken()}, which lexes the next token
 * - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the invalid character.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation easier.
 */
public final class Lexer {
    private final CharStream chars;
    public Lexer(String input) {
        chars = new CharStream(input);
    }
    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        // Create a list to store the lexed tokens
        List<Token> lexTokens = new ArrayList<Token>();
        while (chars.has(0)) {
            // If the current character is a whitespace, advance to the next character and reset the token size
            if (peek("\\s")) {
                chars.advance();
                chars.skip();
            }
            // Otherwise, lex the character and add it to the list
            else {
                lexTokens.add(lexToken());
            }
        }
        return lexTokens;
    }
    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     * <p>
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if (chars.has(0)) {
            if (peek("[A-Za-z_]")) {
                return lexIdentifier();
            }
            else if (peek("[0-9]") || (peek("[+-]") && chars.has(1) &&
                    Character.isDigit(chars.get(1)))) {
                return lexNumber();
            }
            else if (peek("'")) {
                return lexCharacter();
            }
            else if (peek("\"")) {
                return lexString();
            }
            else {
                return lexOperator();
            }
        }
        return new Token(Token.Type.OPERATOR, "", chars.index);
    }
    public Token lexIdentifier() {
        if (!match("[A-Za-z_]")) {
            return new Token(Token.Type.IDENTIFIER, "", chars.index);
        }
        while (chars.has(0) && peek("[A-Za-z0-9_-]")) {
            match("[A-Za-z0-9_-]");
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        boolean isDecimal = false;

        // INTEGER: An optional sign + or - can immediately prefix a non-zero integer
        // DECIMAL: An optional sign + or - can immediately prefix the decimal as positive or negative
        if (peek("[+-]")) {
            match("[+-]");
        }

        // Ensure the number starts with a valid digit
        if (!peek("[0-9]")) {
            throw new ParseException("Invalid number format.", chars.index);
        }

        while (peek("[0-9]")) {
            match("[0-9]");
        }
        // If a decimal point is found, then make isDecimal true and match the digits that follow
        if (peek("\\.")) {
            match("\\.");
            isDecimal = true;
            if (!peek("[0-9]")) {
                throw new ParseException("Invalid decimal format.", chars.index);
            }
            while (peek("[0-9]")) {
                match("[0-9]");
            }
        }
        if (isDecimal) {
            return chars.emit(Token.Type.DECIMAL);
        }
        else {
            return chars.emit(Token.Type.INTEGER);
        }
    }

    /*

        if (peek("0")) {
            match("0");
            if (peek("[0-9]")) {
                match("[0-9]");
                return new Token(Token.Type.INTEGER, "", chars.index);
            }
        }
        if (peek("[+-]")) {
            match("[+-]");
            if (peek("0")) {
                match("0");
                return new Token(Token.Type.INTEGER, "", chars.index);
            }
        }
        while (chars.has(0)) {
            if (peek("[0-9]")) {
                match("[0-9]");
            }
            else if (peek("\\.") && chars.has(1) &&
                    Character.isDigit(chars.get(1))) {
                isDecimal = true;
                match("\\.");
                while (peek("[0-9]")) {
                    match("[0-9]");
                }
            }
            else {
                break;
            }
        }
        if (isDecimal) {
            return chars.emit(Token.Type.DECIMAL);
        }
        else {
            return chars.emit(Token.Type.INTEGER);
        }
    }


     */
    public Token lexCharacter() {
        if (!match("'")) {
            throw new ParseException("Missing Beginning Single Quote.",
                    chars.index);
        }
        if (peek("\\\\")) {
            match("\\\\");
            lexEscape();
        }
        else if (!match("[^'\\\\]")) {
            throw new ParseException("Invalid Escape Use.", chars.index);
        }
        if (!match("'")) {
            throw new ParseException("Missing Ending Single Quote.", chars.index);
        }
        return chars.emit(Token.Type.CHARACTER);
    }
    public Token lexString() {
        if (!match("\"")) {
            throw new ParseException("Missing Beginning Double Quote.",
                    chars.index);
        }
        while (chars.has(0) && !peek("\"")) {
            if (peek("\\\\")) {
                match("\\\\");
                lexEscape();
            }
            // If there is a literal newline, throw a parse exception
            else if (peek("\n")) {
                throw new ParseException("Missing Ending Double Quote.",
                        chars.index);
            }
            else {
                match(".");
            }
        }
        if (!match("\"")) {
            throw new ParseException("Missing Ending Double Quote.", chars.index);
        }
        return chars.emit(Token.Type.STRING);
    }
    public void lexEscape() {
        if (!match("[bnrt'\"\\\\]")) {
            throw new ParseException("Invalid escape sequence.", chars.index);
        }
    }
    public Token lexOperator() {
        if (match(";")) {
            return chars.emit(Token.Type.OPERATOR);
        }
        else if (match("[<>!=]", "=")) {
            return chars.emit(Token.Type.OPERATOR);
        }
        // Fixed to ensure compound expression operators are combined into a single token
        else if (match("&", "&") || match("\\|", "\\|")) {
            return chars.emit(Token.Type.OPERATOR);
        }
        else if (match("[^\\s]")) {
            return chars.emit(Token.Type.OPERATOR);
        }
        return new Token(Token.Type.OPERATOR, "", chars.index);
    }
    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if(!chars.has(i)||!String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }
    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
    }
    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {
        private final String input;
        private int index = 0;
        private int length = 0;
        public CharStream(String input) {
            this.input = input;
        }
        public boolean has(int offset) {
            return index + offset < input.length();
        }
        public char get(int offset) {
            return input.charAt(index + offset);
        }
        public void advance() {
            index++;
            length++;
        }
        public void skip() {
            length = 0;
        }
        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }
    }
}