package main;

import main.ast.node.Program;
import main.visitor.VisitorImpl;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import parsers.actonLexer; // import from the package your parser & lexer is generated in
import parsers.actonParser;

import java.io.IOException;

// Visit https://stackoverflow.com/questions/26451636/how-do-i-use-antlr-generated-parser-and-lexer
public class Acton {
    public static void main(String[] args) throws IOException {
        CharStream reader = CharStreams.fromFileName(args[1]);
        actonLexer lexer = new actonLexer(reader);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        actonParser parser = new actonParser(tokens);
        Program program = parser.program().p; /* assuming that the name of the Program ast node
                                                 that the program rule returns is p */
        VisitorImpl visitor = new VisitorImpl();
        visitor.check(program);
    }
}