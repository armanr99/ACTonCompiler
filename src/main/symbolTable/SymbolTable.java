package main.symbolTable;

import main.symbolTable.itemException.ItemAlreadyExistsException;
import main.symbolTable.itemException.ItemNotFoundException;

import java.util.*;

public class SymbolTable {

	// Static members region

	public static SymbolTable top;
	public static SymbolTable root;
	private static Stack<SymbolTable> stack = new Stack<SymbolTable>();

	// Use it in scope starts
	public static void push(SymbolTable symbolTable) {
		if(top != null)
			stack.push(top);
		top = symbolTable;
	}

	// Use it in scope ends
	public static void pop() {
		top = stack.pop();
	}

	// End of static members region

	SymbolTable pre;
	HashMap<String, SymbolTableItem> items;
	protected int lastUsedIndex;
	String name;

	public SymbolTable() {
		this(null, "");
	}

	public SymbolTable(SymbolTable pre, String name) {
		this.pre = pre;
		this.items = new HashMap<String, SymbolTableItem>();
		this.lastUsedIndex = 1;
		this.name = name;
	}

	public void put(SymbolTableItem item) throws ItemAlreadyExistsException {
		if(items.containsKey(item.getKey()))
			throw new ItemAlreadyExistsException();
		items.put(item.getKey(), item);
	}

	public SymbolTableItem get(String key) throws ItemNotFoundException {
		SymbolTableItem item = items.get(key);
		if(item == null && pre != null)
			return pre.get(key);
		else if(item == null)
			throw new ItemNotFoundException();
		else
			return item;
	}

	public SymbolTableItem getInCurrentScope(String key) throws ItemNotFoundException {
		SymbolTableItem item = items.get(key);
		if( item == null )
			throw new ItemNotFoundException();
		return item;
	}

	public void updateInCurrentScope(String prevKey, SymbolTableItem newItem )
			throws ItemNotFoundException
	{
		SymbolTableItem item = items.get( prevKey );
		if(item == null)
			throw new ItemNotFoundException();
		else {
			items.remove( prevKey );
			items.put(newItem.getKey(), newItem);
		}
	}

	public HashMap< String , SymbolTableItem> getSymbolTableItems()
	{
		return items;
	}

	public SymbolTable getPreSymbolTable() {
		return pre;
	}
	public void setPreSymbolTable(SymbolTable pre)
	{
		this.pre = pre;
	}

	public int getLastUsedIndex()
	{
		return lastUsedIndex;
	}

	public int getItemCount()
	{
		return items.size();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}