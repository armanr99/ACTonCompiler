package main.ast.node.statement;

import main.visitor.Visitor;
import main.ast.node.expression.Expression;

public class Conditional extends Statement {
    private Expression expression;
    private Statement thenBody;
    private Statement elseBody;

    public Conditional(Expression expression, Statement thenBody) {
        this.expression = expression;
        this.thenBody = thenBody;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public Statement getThenBody() {
        return thenBody;
    }

    public void setThenBody(Statement thenBody) {
        this.thenBody = thenBody;
    }

    public Statement getElseBody() {
        return elseBody;
    }

    public void setElseBody(Statement elseBody) {
        this.elseBody = elseBody;
    }

    @Override
    public String toString() {
        return "Conditional";
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
