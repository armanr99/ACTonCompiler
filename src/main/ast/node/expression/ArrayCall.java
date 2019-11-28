package main.ast.node.expression;

import main.visitor.Visitor;

public class ArrayCall extends Expression {
    private Expression arrayInstance;
    private Expression index;

    public ArrayCall(Expression arrayInstance, Expression index) {
        this.arrayInstance = arrayInstance;
        this.index = index;
    }

    public Expression getArrayInstance() {
        return arrayInstance;
    }

    public void setArrayInstance(Expression arrayInstance) {
        this.arrayInstance = arrayInstance;
    }

    public Expression getIndex() {
        return index;
    }

    public void setIndex(Expression index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "ArrayCall";
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
