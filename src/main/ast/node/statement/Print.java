package main.ast.node.statement;

import main.visitor.Visitor;
import main.ast.node.expression.Expression;

public class Print extends Statement {
    private Expression arg;

    public Print(Expression arg) {
        this.arg = arg;
    }

    public Expression getArg() {
        return arg;
    }

    public void setArg(Expression arg) {
        this.arg = arg;
    }

    @Override
    public String toString() {
        return "Print";
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
