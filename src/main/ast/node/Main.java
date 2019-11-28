package main.ast.node;

import java.util.ArrayList;

import main.ast.node.declaration.ActorInstantiation;
import main.visitor.Visitor;

public class Main extends Node {

    private ArrayList<ActorInstantiation> mainActors = new ArrayList<>();

    public ArrayList<ActorInstantiation> getMainActors(){
        return this.mainActors;
    }

    public void setMainActors(ArrayList<ActorInstantiation> mainActors) {
        this.mainActors = mainActors;
    }

    public void addActorInstantiation(ActorInstantiation actorInstantiation) {
        mainActors.add(actorInstantiation);
    }

    @Override
    public String toString() {
        return "Main";
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}