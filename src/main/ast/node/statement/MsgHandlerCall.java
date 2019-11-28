package main.ast.node.statement;

import main.ast.node.expression.Expression;
import main.ast.node.expression.Identifier;
import main.visitor.Visitor;

import java.util.ArrayList;

public class MsgHandlerCall extends Statement {
    private Expression instance;
    private Identifier msgHandlerName;
    private ArrayList<Expression> args = new ArrayList<>();

    public MsgHandlerCall(Expression instance, Identifier msgHandlerName) {
        this.instance = instance;
        this.msgHandlerName = msgHandlerName;
    }

    public Expression getInstance() {
        return instance;
    }

    public void setInstance(Expression instance) {
        this.instance = instance;
    }

    public Identifier getMsgHandlerName() {
        return msgHandlerName;
    }

    public void setMsgHandlerName(Identifier msgHandlerName) {
        this.msgHandlerName = msgHandlerName;
    }

    public void setArgs(ArrayList<Expression> args) {
        this.args = args;
    }

    public ArrayList<Expression> getArgs() {
        return args;
    }

    public void addArg(Expression arg) {
        this.args.add(arg);
    }


    @Override
    public String toString() {
         return "MsgHandlerCall";
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
