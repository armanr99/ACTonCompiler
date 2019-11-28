package main.symbolTable.symbolTableVariableItem;

import main.ast.type.Type;
import main.ast.node.declaration.VarDeclaration;

public class SymbolTableActorVariableItem extends SymbolTableVariableItem{
    
    public SymbolTableActorVariableItem(String name, Type type, int index)
    {
        super(name, type, index);
    }
    
    public SymbolTableActorVariableItem(VarDeclaration actorVar)
    {
        super(actorVar);
    }
}