package main;

import main.ast.node.Program;
import main.compileError.CompileErrorException;
import main.visitor.nameAnalyser.NameAnalyser;
import main.visitor.semanticAnalyser.SemanticAnalyser;
import main.visitor.codeGenerator.CodeGenerator;
import org.antlr.v4.runtime.*;
import antlr.actonLexer;
import antlr.actonParser;

import java.io.IOException;

// Visit https://stackoverflow.com/questions/26451636/how-do-i-use-antlr-generated-parser-and-lexer
public class Acton {
    public static void main(String[] args) throws IOException {
        CharStream reader = CharStreams.fromFileName(args[0]);
        actonLexer lexer = new actonLexer(reader);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        actonParser parser = new actonParser(tokens);
        try {
            Program program = parser.program().p; // program is starting production rule
            NameAnalyser nameAnalyser = new NameAnalyser();
            nameAnalyser.visit(program);
            if( nameAnalyser.numOfErrors() > 0 ) {
                throw new CompileErrorException();
            } else {
                SemanticAnalyser semanticAnalyser = new SemanticAnalyser();
                semanticAnalyser.visit(program);
                if(semanticAnalyser.numOfErrors() > 0) {
                    throw new CompileErrorException();
                } else {
                    CodeGenerator codeGenerator = new CodeGenerator();
                    codeGenerator.visit(program);
                }
            }
        }
        catch(CompileErrorException compileError){
        }
    }
}