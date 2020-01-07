package main.visitor.codeGenerator;

import main.ast.node.*;
import main.ast.node.Program;
import main.ast.node.declaration.*;
import main.ast.node.declaration.handler.HandlerDeclaration;
import main.ast.node.declaration.handler.MsgHandlerDeclaration;
import main.ast.node.statement.*;
import main.ast.node.expression.*;
import main.ast.node.expression.values.*;
import main.ast.type.*;
import main.ast.type.actorType.ActorType;
import main.ast.type.arrayType.ArrayType;
import main.ast.type.primitiveType.BooleanType;
import main.ast.type.primitiveType.IntType;
import main.ast.type.primitiveType.StringType;
import main.symbolTable.*;
import main.symbolTable.itemException.*;
import main.visitor.VisitorImpl;
import org.stringtemplate.v4.compiler.Bytecode;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.io.FileWriter;
import java.io.IOException;

public class CodeGenerator extends VisitorImpl {

    private final String outputPath = "./output/";
    private ArrayList<String> actorByteCodes = new ArrayList<>();

    private boolean inKnownActors = false;
    private boolean inActorVars = false;

    private ActorDeclaration currentActor = null;
    private final int maxStackSize = 50;

    private void pushMainSymbolTable(){
        try{
            SymbolTableMainItem mainItem = (SymbolTableMainItem) SymbolTable.root.getInCurrentScope(SymbolTableMainItem.STARTKEY + "main");
            SymbolTable next = mainItem.getMainSymbolTable();
            SymbolTable.push(next);
        }
        catch(ItemNotFoundException itemNotFound)
        {
            System.out.println("There is an error in pushing Main symbol table");
        }
    }

    private void pushActorDeclarationSymbolTable(ActorDeclaration actorDeclaration) {
        try
        {
            String name = actorDeclaration.getName().getName();
            SymbolTableActorItem actorItem = (SymbolTableActorItem) SymbolTable.root.getInCurrentScope(SymbolTableActorItem.STARTKEY + name);
            SymbolTable next = actorItem.getActorSymbolTable();
            SymbolTable.push(next);
        }
        catch(ItemNotFoundException itemNotFound)
        {
            System.out.println("There is an error in pushing ActorDeclaration symbol table");
        }
    }

    private void pushHandlerDeclarationSymbolTable(HandlerDeclaration handlerDeclaration) {
        try
        {
            String name = handlerDeclaration.getName().getName();
            SymbolTableHandlerItem handlerItem = (SymbolTableHandlerItem) SymbolTable.top.getInCurrentScope(SymbolTableHandlerItem.STARTKEY + name);
            SymbolTable next = handlerItem.getHandlerSymbolTable();
            SymbolTable.push(next);
        }
        catch(ItemNotFoundException itemNotFound)
        {
            System.out.println("There is an error in pushing HandlerDeclaration symbol table");
        }
    }

    private void writeByteCodesFile(ArrayList<String> byteCodes, String fileName) {
        try {
            String path = outputPath + fileName + ".j";
            FileWriter writer = new FileWriter(path);

            for(String bytecode: byteCodes) {
                writer.write(bytecode + System.lineSeparator());
            }
            writer.close();
        }
        catch (IOException ex)
        {
            System.out.println(ex.getMessage());
        }
    }

    private ArrayList<String> getActorInfoByteCodes(ActorDeclaration actorDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<>();

        String classByteCode = ".class public " + actorDeclaration.getName().getName();
        byteCodes.add(classByteCode);

        String superByteCode = ".super Actor";
        byteCodes.add(superByteCode);

        return byteCodes;
    }

    private ArrayList<String> getFieldByteCodes(VarDeclaration varDeclaration, String accessLevel) {
        String fieldName = varDeclaration.getIdentifier().getName();
        String typeDescriptor = getTypeDescriptor(varDeclaration.getType());
        String byteCode = ".field " + (accessLevel == "" ? "" : accessLevel + " ") + fieldName + " " + typeDescriptor;

        ArrayList<String> byteCodes = new ArrayList<>();
        byteCodes.add(byteCode);
        return byteCodes;
    }

    private ArrayList<String>  getActorConstructorByteCodes(ActorDeclaration actorDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<String>();

        byteCodes.add("method public <init>(I)V");
        byteCodes.add(".limit stack 2");
        byteCodes.add(".limit locals 2");
        byteCodes.add("aload_0");
        byteCodes.add("iload_1");
        byteCodes.add("invokespecial Actor/<init>(I)V");
        byteCodes.add("return");
        byteCodes.add(".end method");

        return byteCodes;
    }

    private String getTypeDescriptor(Type type) {
        if(type instanceof IntType)
            return "I";
        else if(type instanceof BooleanType)
            return "Z";
        else if(type instanceof StringType)
            return "Ljava/lang/String;";
        else if(type instanceof ArrayType)
            return "[I";
        else if(type instanceof ActorType)
            return ("L" + ((ActorType)type).getName().getName()) + ";";
        else
            return "";
    }

    private String getHandlerFileClassName(HandlerDeclaration handlerDeclaration) {
        return (currentActor.getName().getName() + "_" + handlerDeclaration.getName().getName());
    }

    private ArrayList<String> getHandlerMessageInfoByteCodes(HandlerDeclaration handlerDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<>();

        String className = getHandlerFileClassName(handlerDeclaration);

        String classByteCode = ".class public " + className;
        byteCodes.add(classByteCode);

        String superByteCode = ".super Message";
        byteCodes.add(superByteCode);

        return byteCodes;
    }

    private ArrayList<String> getHandlerMessageFieldsByteCodes(HandlerDeclaration handlerDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<>();

        for(VarDeclaration arg : handlerDeclaration.getArgs())
            byteCodes.addAll(getFieldByteCodes(arg, "private"));

        byteCodes.add(".field private receiver " + "L" + currentActor.getName().getName() + ";");
        byteCodes.add(".field private sender LActor;");

        return byteCodes;
    }

    private ArrayList<String> getHandlerMessageConstructorInfoByteCodes(HandlerDeclaration handlerDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<>();

        String methodInfo = ".method public <init>(";

        methodInfo += ("L" + currentActor.getName().getName() + ";");
        methodInfo += "LActor;";

        for(VarDeclaration arg : handlerDeclaration.getArgs())
            methodInfo += (getTypeDescriptor(arg.getType()));

        methodInfo += ")V";

        byteCodes.add(methodInfo);

        return byteCodes;
    }

    private ArrayList<String> getHandlerMessageConstructorAssignsByteCodes(HandlerDeclaration handlerDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<>();
        String className = getHandlerFileClassName(handlerDeclaration);

        byteCodes.add("aload_0");
        byteCodes.add("aload_1");
        byteCodes.add("putfield " + className + "/receiver " + "L" + currentActor.getName().getName() + ";");
        byteCodes.add("aload_0");
        byteCodes.add("aload_2");
        byteCodes.add("putfield " + className + "/sender LActor;");

        int varIndex = 3;
        for(VarDeclaration arg : handlerDeclaration.getArgs()) {
            byteCodes.add("aload_0");

            String loadInstruction;
            if(arg.getType() instanceof IntType || arg.getType() instanceof BooleanType)
                loadInstruction = "iload ";
            else
                loadInstruction = "aload ";
            loadInstruction += (varIndex++);

            byteCodes.add(loadInstruction);

            byteCodes.add("putfield " + className + "/" + arg.getIdentifier().getName() + " " + getTypeDescriptor(arg.getType()));
        }

        return byteCodes;
    }

    private ArrayList<String> getHandlerMessageConstructorByteCodes(HandlerDeclaration handlerDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<>();

        byteCodes.addAll(getHandlerMessageConstructorInfoByteCodes(handlerDeclaration));

        byteCodes.add(".limit stack " + maxStackSize);
        byteCodes.add(".limit locals " + (handlerDeclaration.getArgs().size() + 3));

        byteCodes.add("aload_0");
        byteCodes.add("invokespecial Message/<init>()V");

        byteCodes.addAll(getHandlerMessageConstructorAssignsByteCodes(handlerDeclaration));

        byteCodes.add("return");
        byteCodes.add(".end method");

        return byteCodes;
    }

    private ArrayList<String> getHandlerMessageExecuteInvokeByteCodes(HandlerDeclaration handlerDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<>();

        String byteCode = "invokevirtual ";
        byteCode += (currentActor.getName().getName() + "/" + handlerDeclaration.getName().getName());

        byteCode += "(LActor;";
        for(VarDeclaration arg : handlerDeclaration.getArgs())
            byteCode += (getTypeDescriptor(arg.getType()));
        byteCode += ")V";

        byteCodes.add(byteCode);

        return byteCodes;
    }

    private ArrayList<String> getHandlerMessageExecuteArgsByteCodes(HandlerDeclaration handlerDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<>();
        String className = getHandlerFileClassName(handlerDeclaration);

        byteCodes.add("aload_0");
        byteCodes.add("getfield " + className + "/receiver " + "L" + currentActor.getName().getName() + ";");
        byteCodes.add("aload_0");
        byteCodes.add("getfield " + className + "/sender LActor");


        for(VarDeclaration arg : handlerDeclaration.getArgs()) {
            byteCodes.add("aload_0");
            byteCodes.add("getfield " + className + "/" + arg.getIdentifier().getName() + " " + getTypeDescriptor(arg.getType()));
        }

        return byteCodes;
    }

    private ArrayList<String> getHandlerMessageExecuteByteCodes(HandlerDeclaration handlerDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<>();

        byteCodes.add(".method public execute()V");
        byteCodes.add(".limit stack " + maxStackSize);
        byteCodes.add(".limit locals 1");
        byteCodes.addAll(getHandlerMessageExecuteArgsByteCodes(handlerDeclaration));
        byteCodes.addAll(getHandlerMessageExecuteInvokeByteCodes(handlerDeclaration));
        byteCodes.add("return");
        byteCodes.add(".end method");

        return byteCodes;
    }

    private void addHandlerByteCodesFile(HandlerDeclaration handlerDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<>();

        byteCodes.addAll(getHandlerMessageInfoByteCodes(handlerDeclaration));
        byteCodes.add("");
        byteCodes.addAll(getHandlerMessageFieldsByteCodes(handlerDeclaration));
        byteCodes.add("");
        byteCodes.addAll(getHandlerMessageConstructorByteCodes(handlerDeclaration));
        byteCodes.add("");
        byteCodes.addAll(getHandlerMessageExecuteByteCodes(handlerDeclaration));

        writeByteCodesFile(byteCodes, getHandlerFileClassName(handlerDeclaration));
    }

    @Override
    public void visit(Program program){
        for(ActorDeclaration actorDeclaration : program.getActors())
            actorDeclaration.accept(this);
        program.getMain().accept(this);
    }

    @Override
    public void visit(ActorDeclaration actorDeclaration) {
        pushActorDeclarationSymbolTable(actorDeclaration);

        currentActor = actorDeclaration;

        actorByteCodes.addAll(getActorInfoByteCodes(actorDeclaration));
        actorByteCodes.add("");

        inKnownActors = true;
        for(VarDeclaration varDeclaration: actorDeclaration.getKnownActors())
            varDeclaration.accept(this);
        inKnownActors = false;

        inActorVars = true;
        for(VarDeclaration varDeclaration: actorDeclaration.getActorVars())
            varDeclaration.accept(this);
        inActorVars = false;

        actorByteCodes.add("");

        actorByteCodes.addAll(getActorConstructorByteCodes(actorDeclaration));

        actorByteCodes.add("");

        if(actorDeclaration.getInitHandler() != null)
            actorDeclaration.getInitHandler().accept(this);
        for(MsgHandlerDeclaration msgHandlerDeclaration: actorDeclaration.getMsgHandlers())
            msgHandlerDeclaration.accept(this);

        writeByteCodesFile(actorByteCodes, actorDeclaration.getName().getName());
        actorByteCodes.clear();

        SymbolTable.pop();
    }

    @Override
    public void visit(HandlerDeclaration handlerDeclaration) {
        if(handlerDeclaration == null)
            return;

        pushHandlerDeclarationSymbolTable(handlerDeclaration);

        if(handlerDeclaration.getName().getName() != "initial")
            addHandlerByteCodesFile(handlerDeclaration);

        visitExpr(handlerDeclaration.getName());
        for(VarDeclaration argDeclaration: handlerDeclaration.getArgs())
            argDeclaration.accept(this);
        for(VarDeclaration localVariable: handlerDeclaration.getLocalVars())
            localVariable.accept(this);
        for(Statement statement : handlerDeclaration.getBody())
            visitStatement(statement);

        SymbolTable.pop();
    }

    @Override
    public void visit(VarDeclaration varDeclaration) {
        if(varDeclaration == null)
            return;

        if(inKnownActors || inActorVars)
            actorByteCodes.addAll(getFieldByteCodes(varDeclaration, ""));

        visitExpr(varDeclaration.getIdentifier());
    }

    @Override
    public void visit(Main programMain) {
        if(programMain == null)
            return;

        pushMainSymbolTable();

        for(ActorInstantiation mainActor : programMain.getMainActors())
            mainActor.accept(this);

        SymbolTable.pop();
    }

    @Override
    public void visit(ActorInstantiation actorInstantiation) {
        if(actorInstantiation == null)
            return;

        visitExpr(actorInstantiation.getIdentifier());
        for(Identifier knownActor : actorInstantiation.getKnownActors())
            visitExpr(knownActor);
        for(Expression initArg : actorInstantiation.getInitArgs())
            visitExpr(initArg);
    }

    @Override
    public void visit(UnaryExpression unaryExpression) {
        if(unaryExpression == null)
            return;

        visitExpr(unaryExpression.getOperand());
    }

    @Override
    public void visit(BinaryExpression binaryExpression) {
        if(binaryExpression == null)
            return;

        visitExpr(binaryExpression.getLeft());
        visitExpr(binaryExpression.getRight());
    }

    @Override
    public void visit(ArrayCall arrayCall) {

        visitExpr(arrayCall.getArrayInstance());
        visitExpr(arrayCall.getIndex());
    }

    @Override
    public void visit(ActorVarAccess actorVarAccess) {
        if(actorVarAccess == null)
            return;

        visitExpr(actorVarAccess.getVariable());
    }

    @Override
    public void visit(Identifier identifier) {
        if(identifier == null)
            return;
    }

    @Override
    public void visit(Self self) {
    }

    @Override
    public void visit(Sender sender) {
    }

    @Override
    public void visit(BooleanValue value) {
    }

    @Override
    public void visit(IntValue value) {
    }

    @Override
    public void visit(StringValue value) {
    }

    @Override
    public void visit(MsgHandlerCall msgHandlerCall) {
        if(msgHandlerCall == null) {
            return;
        }
        try {
            visitExpr(msgHandlerCall.getInstance());
            visitExpr(msgHandlerCall.getMsgHandlerName());
            for (Expression argument : msgHandlerCall.getArgs())
                visitExpr(argument);
        }
        catch(NullPointerException npe) {
            System.out.println("null pointer exception occurred");
        }
    }

    @Override
    public void visit(Block block) {
        if(block == null)
            return;
        for(Statement statement : block.getStatements())
            visitStatement(statement);
    }

    @Override
    public void visit(Conditional conditional) {
        visitExpr(conditional.getExpression());
        visitStatement(conditional.getThenBody());
        visitStatement(conditional.getElseBody());
    }

    @Override
    public void visit(For loop) {
        visitStatement(loop.getInitialize());
        visitExpr(loop.getCondition());
        visitStatement(loop.getUpdate());
        visitStatement(loop.getBody());
    }

    @Override
    public void visit(Break b) {
    }

    @Override
    public void visit(Continue c) {
    }

    @Override
    public void visit(Print print) {
        if(print == null)
            return;
        visitExpr(print.getArg());
    }

    @Override
    public void visit(Assign assign) {
        visitExpr(assign.getlValue());
        visitExpr(assign.getrValue());
    }
}