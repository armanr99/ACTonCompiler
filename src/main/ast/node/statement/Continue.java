package main.ast.node.statement;

import main.visitor.Visitor;

public class Continue extends Statement {

	@Override
    public String toString() {
        return "Continue";
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

}
