package main.ast.node.expression.values;

import main.ast.type.Type;
import main.visitor.Visitor;

public class BooleanValue extends Value {
    private boolean constant;

    public BooleanValue(boolean constant, Type type) {
        this.constant = constant;
        this.type = type;
    }

    public boolean getConstant() {
        return constant;
    }

    public void setConstant(boolean constant) {
        this.constant = constant;
    }

    @Override
    public String toString() {
         return "BooleanValue " + constant;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
