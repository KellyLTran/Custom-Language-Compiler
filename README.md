# Custom Language Compiler

This is a Java-based compiler for a custom language that parses, analyzes, and generates Java code. The project supports 26 abstract syntax types and has been tested with over 350 JUnit test cases to ensure reliability and functionality.

## Features

- **Lexer**: Converts source code into a stream of tokens to handle keywords, identifiers, operators, and literals.

- **Parser**: Builds an Abstract Syntax Tree (AST) from the token stream to represent code structure.

- **Analyzer**: Performs semantic checks to validate type consistency, scope resolution, and language constraints.

- **Interpreter**: Executes AST nodes to evaluate and run code directly without needing to generate Java source code.

- **Generator**: Translates the AST into Java source code to make the program executable on the JVM (Java Virtual Machine).
