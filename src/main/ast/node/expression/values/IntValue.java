package main.ast.node.expression.values;

import main.ast.type.Type;
import main.visitor.Visitor;

public class IntValue extends Value {
    private int constant;

    public IntValue(int constant, Type type) {
        this.constant = constant;
        this.type = type;
    }

    public int getConstant() {
        return constant;
    }

    public void setConstant(int constant) {
        this.constant = constant;
    }

    @Override
    public String toString() {
        return "IntValue " + constant;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
