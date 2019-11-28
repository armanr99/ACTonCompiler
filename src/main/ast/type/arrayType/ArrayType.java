package main.ast.type.arrayType;

import main.ast.type.Type;

public class ArrayType extends Type {
    private int size;

    public ArrayType(int size){
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
    
    @Override
    public String toString() {
        return "int[]";
    }

    
}
