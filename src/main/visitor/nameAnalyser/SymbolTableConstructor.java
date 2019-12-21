package main.visitor.nameAnalyser;

import main.ast.node.Main;
import main.ast.node.declaration.*;
import main.ast.node.declaration.handler.*;
import main.symbolTable.*;
import main.symbolTable.symbolTableVariableItem.*;
import main.symbolTable.itemException.ItemAlreadyExistsException;

import java.util.ArrayList;

public class SymbolTableConstructor {

    public void constructProgramSymbolTable()
    {
        SymbolTable.push(new SymbolTable());
        SymbolTable.root = SymbolTable.top;
    }

    public void construct(ActorDeclaration actorDeclaration)
    {
        SymbolTable actorSymbolTable = new SymbolTable(SymbolTable.top, actorDeclaration.getName().getName());
        SymbolTableActorItem actorItem = new SymbolTableActorItem(actorDeclaration);
        try {
            actorItem.setActorSymbolTable(actorSymbolTable);
            SymbolTable.root.put(actorItem);
        }
        catch(ItemAlreadyExistsException itemDuplication)
        {
            String actorName = SymbolTable.root.getItemCount() + "$" + actorDeclaration.getName().getName();
            actorItem.setName(actorName);
            try {
                SymbolTable.root.put(actorItem);
            }
            catch(ItemAlreadyExistsException itemReduplication)
            {
                System.out.println("an error occurred");
            }
        }
        SymbolTable.push(actorSymbolTable);
        addVarsToSymbolTable(actorDeclaration.getKnownActors(), SymbolTableKnownActorItem.class);
        addVarsToSymbolTable(actorDeclaration.getActorVars(), SymbolTableActorVariableItem.class);
    }

    public void construct(HandlerDeclaration handlerDeclaration)
    {
        SymbolTableHandlerItem handlerItem = new SymbolTableHandlerItem(handlerDeclaration);
        SymbolTable handlerSymbolTable = new SymbolTable(SymbolTable.top, handlerDeclaration.getName().getName());
        try
        {
            handlerItem.setHandlerSymbolTable(handlerSymbolTable);
            SymbolTable.top.put(handlerItem);
        }
        catch(ItemAlreadyExistsException itemDuplication) {
            String newHandlerName = SymbolTable.top.getItemCount() + "$" + handlerDeclaration.getName().getName();
            handlerItem.setName(newHandlerName);
            try {
                SymbolTable.top.put(handlerItem);
            } catch (ItemAlreadyExistsException itemReduplication) {
                System.out.println("an error occurred in adding method to symbol table");
            }
        }
        SymbolTable.push(handlerSymbolTable);
        addVarsToSymbolTable(handlerDeclaration.getArgs(), SymbolTableHandlerArgumentItem.class);
        addVarsToSymbolTable(handlerDeclaration.getLocalVars(), SymbolTableLocalVariableItem.class);
    }

    public  void construct(Main main)
    {
        SymbolTable mainSymbolTable = new SymbolTable(SymbolTable.top, "main");
        SymbolTableMainItem mainItem = new SymbolTableMainItem(main);
        mainItem.setMainSymbolTable(mainSymbolTable);
        try {
            SymbolTable.root.put(mainItem);
        }
        catch(ItemAlreadyExistsException itemDuplication)
        {
            System.out.println("an error occurred in main construction");
        }
        SymbolTable.push(mainSymbolTable);
        addVarsToSymbolTable(main.getMainActors(), SymbolTableLocalVariableItem.class);
    }

    public void addVarsToSymbolTable(ArrayList<? extends VarDeclaration> varDeclarations,
                                     Class<? extends SymbolTableVariableItem> VariableItem){
        for(VarDeclaration varDeclaration: varDeclarations){
            try{
                SymbolTableVariableItem varItem = VariableItem.getConstructor(VarDeclaration.class).newInstance(varDeclaration);
                try{
                    SymbolTable.top.put(varItem);
                }
                catch(ItemAlreadyExistsException itemDuplication){
                    String newName =  SymbolTable.top.getItemCount() + "$" + varDeclaration.getIdentifier().getName();
                    varItem.setName(newName);
                    try
                    {
                        SymbolTable.top.put(varItem);
                    }
                    catch(ItemAlreadyExistsException itemReduplication)
                    {
                        System.out.println("error occurred in adding variable to symbol table");
                    }
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}