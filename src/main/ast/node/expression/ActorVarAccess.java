package main.ast.node.expression;

import main.visitor.Visitor;

public class ActorVarAccess extends Expression {
    private Identifier variableName;
    private Self self;

    public ActorVarAccess(Identifier variableName) {
        this.variableName = variableName;
        this.self = new Self();
    }

    public Identifier getVariable() {
        return variableName;
    }

    public void setVariable(Identifier variableName) {
        this.variableName = variableName;
    }

    public Self getSelf() {
        return self;
    }

    @Override
    public String toString() {
         return "ActorVarAccess";
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
