package main.ast.node.statement;

import main.visitor.Visitor;

import java.util.ArrayList;

public class Block extends Statement
{
    private ArrayList<Statement> statements = new ArrayList<>();

    public ArrayList<Statement> getStatements() {
        return statements;
    }

    public void addStatement(Statement statement) {
        this.statements.add(statement);
    }

    @Override
    public String toString() {
        return "Block";
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
