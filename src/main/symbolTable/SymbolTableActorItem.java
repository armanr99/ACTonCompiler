package main.symbolTable;

import main.ast.node.declaration.ActorDeclaration;
import main.ast.node.expression.Identifier;

public class SymbolTableActorItem extends SymbolTableItem {

    protected SymbolTable actorSymbolTable;
    protected ActorDeclaration actorDeclaration;
    public static final String STARTKEY = "Actor_";

    public SymbolTableActorItem(ActorDeclaration actorDeclaration)
    {
        this.name = actorDeclaration.getName().getName();
        this.actorDeclaration = actorDeclaration;
    }
    
    public void setActorSymbolTable(SymbolTable actorSymbolTable)
    {
        this.actorSymbolTable = actorSymbolTable;
    }

    public SymbolTable getActorSymbolTable()
    {
        return actorSymbolTable;
    }

    public void setName(String name)
    {
        this.name = name;
        this.actorDeclaration.getName().setName(name);
    }

    public String getParentName()
    {
        if(actorDeclaration.getParentName() == null)
            return null;
        return actorDeclaration.getParentName().getName();
    }

    public void setParentName(String parentName)
    {
        actorDeclaration.getParentName().setName(parentName);
    }

    public void setActorDeclaration(ActorDeclaration actorDeclaration)
    {
        this.actorDeclaration = actorDeclaration;
    }

    public ActorDeclaration getActorDeclaration()
    {
        return actorDeclaration;
    }

    @Override
    public String getKey()
    {
        return SymbolTableActorItem.STARTKEY + this.name;
    }
}
