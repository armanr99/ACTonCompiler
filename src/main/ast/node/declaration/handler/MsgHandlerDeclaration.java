package main.ast.node.declaration.handler;

import main.ast.node.expression.Identifier;
import main.visitor.Visitor;

public class MsgHandlerDeclaration extends HandlerDeclaration {

    public MsgHandlerDeclaration(Identifier name) {
        this.name = name;
    }

	@Override
    public String toString() {
        return "MsgHandlerDeclaration";
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
