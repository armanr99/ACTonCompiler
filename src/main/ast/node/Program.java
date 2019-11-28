package main.ast.node;

import main.visitor.Visitor;
import main.ast.node.declaration.ActorDeclaration;

import java.util.ArrayList;
import java.util.List;

public class Program {
    private ArrayList<ActorDeclaration> actors = new ArrayList<>();
    private Main programMain;


    public void addActor(ActorDeclaration actorDeclaration) {
        actors.add(actorDeclaration);
    }

    public ArrayList<ActorDeclaration> getActors() {
        return actors;
    }


    public Main getMain() {
        return this.programMain;
    }

    public void setMain(Main mainActors) {
        this.programMain = mainActors;
    }

    @Override
    public String toString() {
        return "Program";
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

}
