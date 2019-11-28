package main.ast.node.declaration;

import main.ast.node.declaration.handler.InitHandlerDeclaration;
import main.ast.node.declaration.handler.MsgHandlerDeclaration;
import main.ast.node.expression.Expression;
import main.ast.node.expression.Identifier;
import main.ast.node.expression.values.IntValue;
import main.ast.type.Type;
import main.visitor.Visitor;
import java.util.ArrayList;

public class ActorDeclaration extends Declaration {
    private ArrayList<VarDeclaration> knownActors = new ArrayList<>();
    private ArrayList<VarDeclaration> actorVars = new ArrayList<>();
    private ArrayList<MsgHandlerDeclaration> msgHandlers = new ArrayList<>();
    private InitHandlerDeclaration initHandler;
    private Identifier name;
    private Identifier parentName;
    private int queueSize;

    public ActorDeclaration(Identifier name){
        this.name = name;
    }

    public ArrayList<VarDeclaration> getKnownActors() {
        return knownActors;
    }

    public void addKnownActor(VarDeclaration knownActor){
        this.knownActors.add(knownActor);
    }

    public void setActorVars(ArrayList<VarDeclaration> actorVars) {
        this.actorVars = actorVars;
    }

    public ArrayList<VarDeclaration> getActorVars() {
        return actorVars;
    }

    public void addActorvar(VarDeclaration actorVar){
        this.actorVars.add(actorVar);
    }

    public ArrayList<MsgHandlerDeclaration> getMsgHandlers() {
        return this.msgHandlers;
    }

    public void addMsgHandler(MsgHandlerDeclaration msgHandler){
        this.msgHandlers.add(msgHandler);
    }

    public InitHandlerDeclaration getInitHandler() {
        return initHandler;
    }

    public void setInitHandler(InitHandlerDeclaration initHandler) {
        this.initHandler = initHandler;
    }

    public Identifier getName() {
        return name;
    }

    public Identifier getParentName() {
        return parentName;
    }

    public void setParentName(Identifier name) {
        this.parentName = name;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int size) {
        this.queueSize = size;
    }

    @Override
    public String toString() {
        return "ActorDeclaration";
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

}
