package main.ast.node.declaration.handler;

import main.ast.node.expression.Identifier;
import main.visitor.Visitor;

public class InitHandlerDeclaration extends HandlerDeclaration {

    public InitHandlerDeclaration(Identifier name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "InitHandlerDeclaration";
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
