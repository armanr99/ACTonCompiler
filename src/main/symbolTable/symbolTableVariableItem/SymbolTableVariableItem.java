package main.symbolTable.symbolTableVariableItem;

import main.ast.type.Type;
import main.ast.node.declaration.VarDeclaration;
import main.symbolTable.SymbolTableItem;

public abstract class SymbolTableVariableItem extends SymbolTableItem {

	protected int index;
	protected Type type;
	protected VarDeclaration varDeclaration;
    public static final String STARTKEY = "Variable_";
	
	public SymbolTableVariableItem(String name, Type type, int index) {
		this.name = name;
	    this.type = type;
	    this.index = index;
	}
	
    public SymbolTableVariableItem(VarDeclaration varDeclaration)
    {
        this.name = varDeclaration.getIdentifier().getName();
        this.type = varDeclaration.getType();
        this.index = 0;
        this.varDeclaration = varDeclaration;
    }

	public String getName() {
		return name;
	}

	public void setName(String name)
    {
        this.name = name;
        varDeclaration.getIdentifier().setName(name);
    }

	public Type getType() {
		return type;
	}

    public void setType(Type type) {
        this.type = type;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index)
    {
        this.index = index;
    }

    public VarDeclaration getVarDeclaration()
    {
        return varDeclaration;
    }
    
    public void setVarDeclaration(VarDeclaration varDeclaration)
    {
        this.varDeclaration = varDeclaration;
    }

    @Override
    public String getKey() {
        return SymbolTableVariableItem.STARTKEY + this.name;
    }
}