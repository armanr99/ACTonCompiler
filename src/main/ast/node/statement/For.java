package main.ast.node.statement;

import main.ast.node.expression.Expression;
import main.visitor.Visitor;

public class For extends Statement {
    private Assign initialize;
    private Expression condition;
    private Assign update;
    private Statement body;

    public Assign getInitialize() {
        return initialize;
    }

    public void setInitialize(Assign initialize) {
        this.initialize = initialize;
    }

    public Expression getCondition() {
        return condition;
    }

    public void setCondition(Expression condition) {
        this.condition = condition;
    }

    public Assign getUpdate() {
        return update;
    }

    public void setUpdate(Assign update) {
        this.update = update;
    }

    public Statement getBody() {
        return body;
    }

    public void setBody(Statement body) {
        this.body = body;
    }

    @Override
    public String toString() {
         return "For";
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

}
