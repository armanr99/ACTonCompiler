package main.symbolTable.symbolTableVariableItem;

import main.ast.type.Type;
import main.ast.node.declaration.VarDeclaration;

public class SymbolTableKnownActorItem extends SymbolTableVariableItem {
    
    public SymbolTableKnownActorItem(String name, Type type, int index)
    {
        super(name ,type ,index);
    }
    
    public SymbolTableKnownActorItem(VarDeclaration localVar)
    {
        super(localVar);
    }
}
