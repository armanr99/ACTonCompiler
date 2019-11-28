package main.ast.node.expression;

import main.visitor.Visitor;

public class Self extends Expression {
    @Override
    public String toString() {
        return "Self";
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
