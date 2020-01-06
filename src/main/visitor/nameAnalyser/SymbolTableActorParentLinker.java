package main.visitor.nameAnalyser;

import main.ast.node.Program;
import main.ast.node.declaration.ActorDeclaration;
import main.symbolTable.*;
import main.symbolTable.itemException.ItemNotFoundException;

import java.util.HashSet;
import java.util.Set;

public class SymbolTableActorParentLinker {

    void findActorsParents(Program program)
    {
        Set<String> visitedActors = new HashSet<>();
        for(ActorDeclaration actorDeclaration: program.getActors())
        {
            linkActorWithItsParents(actorDeclaration, visitedActors);
        }
    }

    private void linkActorWithItsParents(ActorDeclaration actorDeclaration, Set<String> visitedActors)
    {
        SymbolTable currentSymTable, prevSymTable = null;
        SymbolTableActorItem actorItem;
        String name = actorDeclaration.getName().getName();
        try {
            do {
                actorItem = ((SymbolTableActorItem) SymbolTable.root.getInCurrentScope(SymbolTableActorItem.STARTKEY + name));
                currentSymTable = actorItem.getActorSymbolTable();
                if (prevSymTable != null)
                    prevSymTable.setPreSymbolTable(currentSymTable);
                if (name != null && visitedActors.contains(name))
                    break;
                visitedActors.add(name);
                name = actorItem.getParentName();
                prevSymTable = currentSymTable;
            } while (name != null);
        }
        catch( ItemNotFoundException itemNotFound )
        {
//            System.out.println( "error occurred in linking parents" );
        }
    }

    private Set<SymbolTable> actorsInCycle = new HashSet<>();

    boolean hasCyclicInheritance(ActorDeclaration actorDeclaration)
    {
        SymbolTableActorItem actorItem;
        SymbolTable actorSymTable, currentSymTable;
        String name = actorDeclaration.getName().getName();
        Set<SymbolTable> visitedActors = new HashSet<>();
        try {
            actorItem = ((SymbolTableActorItem) SymbolTable.root.getInCurrentScope(SymbolTableActorItem.STARTKEY + name));
            actorSymTable = actorItem.getActorSymbolTable();
            if(actorsInCycle.contains(actorSymTable)){
                return false;
            }
            currentSymTable = actorSymTable;
            do {
                if(visitedActors.contains(currentSymTable)){
                    if(currentSymTable == actorSymTable){
                        actorsInCycle.addAll(visitedActors);
                        return true;
                    }
                    else{
                        return false;
                    }
                }
                visitedActors.add(currentSymTable);
                currentSymTable = currentSymTable.getPreSymbolTable();
            } while (currentSymTable != null);
            return false;
        }
        catch(ItemNotFoundException itemNotFound)
        {
            return false;
        }
    }
}