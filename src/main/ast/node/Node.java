package main.ast.node;

import main.visitor.Visitor;

public abstract class Node {

    private int line;

    public void setLine(int line_num) {
        this.line = line_num;
    }

    public int getLine() {
        return this.line;
    }

    public void accept(Visitor visitor) {}
}
