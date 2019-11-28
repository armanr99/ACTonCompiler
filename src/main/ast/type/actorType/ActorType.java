package main.ast.type.actorType;

import main.ast.type.Type;
import main.ast.node.declaration.ActorDeclaration;
import main.ast.node.expression.Identifier;

public class ActorType extends Type {
    private ActorDeclaration actorDeclaration;
    private Identifier name;

    public ActorType(Identifier name) {
        this.name = name;
    }

    public ActorDeclaration getActorDeclaration() {
        return actorDeclaration;
    }

    public void setActorDeclaration(ActorDeclaration actorDeclaration) {
        this.actorDeclaration = actorDeclaration;
    }

    public Identifier getName() {
        return name;
    }

    public void setName(Identifier name) {
        this.name = name;
    }

    @Override
    public String toString() {
        //return classDeclaration.getName().getName();
        return this.name.getName();
    }
}
