package main.visitor.codeGenerator;

import main.ast.node.*;
import main.ast.node.Program;
import main.ast.node.declaration.*;
import main.ast.node.declaration.handler.HandlerDeclaration;
import main.ast.node.declaration.handler.MsgHandlerDeclaration;
import main.ast.node.expression.operators.BinaryOperator;
import main.ast.node.expression.operators.UnaryOperator;
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
import main.symbolTable.symbolTableVariableItem.SymbolTableVariableItem;
import main.visitor.VisitorImpl;
import org.stringtemplate.v4.compiler.Bytecode;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.io.FileWriter;
import java.io.IOException;

public class CodeGenerator extends VisitorImpl {

    private final String outputPath = "./output/";
    private ArrayList<String> actorByteCodes = new ArrayList<>();

    private ActorDeclaration currentActor = null;
    private final int maxStackSize = 50;

    private int currentVariableIndex = 1;

    private boolean inHandler = false;

    private int labelIndex = 0;

    private String getLabel() {
        return ("Label" + (labelIndex++));
    }

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

    private ArrayList<String> getFieldsByteCodes(ActorDeclaration actorDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<>();

        for(VarDeclaration knownActor : actorDeclaration.getKnownActors())
            byteCodes.addAll(getFieldByteCodes(knownActor, ""));

        for(VarDeclaration actorVar : actorDeclaration.getActorVars())
            byteCodes.addAll(getFieldByteCodes(actorVar, ""));

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

    private ArrayList<String> getSetKnownActorsInfoByteCodes(ActorDeclaration actorDeclaration) {
        String methodInfo = ".method public setKnownActors(";
        for(VarDeclaration knownActor : actorDeclaration.getKnownActors()) {
            methodInfo += (getTypeDescriptor(knownActor.getType()));
        }
        methodInfo += ")V";

        ArrayList<String> byteCodes = new ArrayList<>();
        byteCodes.add(methodInfo);
        return byteCodes;
    }

    private ArrayList<String> getSetKnownActorsAssignsByteCodes(ActorDeclaration actorDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<>();

        int varIndex = 1;
        for(VarDeclaration knownActor : actorDeclaration.getKnownActors()) {
            byteCodes.add("aload_0");
            byteCodes.add("aload " + varIndex++);
            byteCodes.add("putfield " + currentActor.getName().getName() + "/" + knownActor.getIdentifier().getName() + " " + getTypeDescriptor(knownActor.getType()));
        }

        return byteCodes;
    }

    private ArrayList<String> getSetKnownActorsByteCodes(ActorDeclaration actorDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<>();

        byteCodes.addAll(getSetKnownActorsInfoByteCodes(actorDeclaration));
        byteCodes.add(".limit stack " + maxStackSize);
        byteCodes.add(".limit locals " + (actorDeclaration.getKnownActors().size() + 1));
        byteCodes.addAll(getSetKnownActorsAssignsByteCodes(actorDeclaration));
        byteCodes.add("return");
        byteCodes.add(".end method");

        return byteCodes;
    }

    private ArrayList<String> getHandlerInfoArgsByteCodes(HandlerDeclaration handlerDeclaration) {
        String argsInfo = "";

        argsInfo += "LActor;";

        for(VarDeclaration arg : handlerDeclaration.getArgs())
            argsInfo += getTypeDescriptor(arg.getType());

        ArrayList<String> byteCodes = new ArrayList<>();
        byteCodes.add(argsInfo);

        return byteCodes;
    }


    private ArrayList<String> getHandlerInfoByteCodes(HandlerDeclaration handlerDeclaration, boolean isSend) {
        String handlerInfo = ".method public ";
        handlerInfo += (isSend ? "send_" : "");
        handlerInfo += handlerDeclaration.getName().getName();

        handlerInfo += "(";
        handlerInfo += getHandlerInfoArgsByteCodes(handlerDeclaration).get(0);
        handlerInfo += ")V";

        ArrayList<String> byteCodes = new ArrayList<>();
        byteCodes.add(handlerInfo);
        return byteCodes;
    }

    private ArrayList<String> getHandlerSendInvokeByteCodes(HandlerDeclaration handlerDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<>();

        String byteCode = "invokespecial ";
        byteCode += (currentActor.getName().getName() + "_" + handlerDeclaration.getName().getName());

        byteCode += ("(L" + currentActor.getName().getName() + ";");
        byteCode += "LActor;";
        for(VarDeclaration arg : handlerDeclaration.getArgs())
            byteCode += (getTypeDescriptor(arg.getType()));
        byteCode += ")V";

        byteCodes.add(byteCode);
        return byteCodes;
    }


    private ArrayList<String> getHandlerSendByteCodes(HandlerDeclaration handlerDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<>();

        byteCodes.addAll(getHandlerInfoByteCodes(handlerDeclaration, true));
        byteCodes.add(".limit stack " + maxStackSize);
        byteCodes.add(".limit locals " + (handlerDeclaration.getArgs().size() + 2));

        byteCodes.add("aload_0");
        byteCodes.add("new " + currentActor.getName().getName() + "_" + handlerDeclaration.getName().getName());
        byteCodes.add("dup");
        byteCodes.add("aload_0");
        byteCodes.add("aload_1");

        int currentIndex = 2;
        for(VarDeclaration arg : handlerDeclaration.getArgs()) {
            String loadInstruction = "";
            if(arg.getType() instanceof IntType || arg.getType() instanceof BooleanType)
                loadInstruction = "iload ";
            else
                loadInstruction = "aload ";
            loadInstruction += (currentIndex++);

            byteCodes.add(loadInstruction);
        }

        byteCodes.addAll(getHandlerSendInvokeByteCodes(handlerDeclaration));
        byteCodes.add("invokevirtual " + currentActor.getName().getName() + "/" + "send(LMessage;)V");
        byteCodes.add("return");
        byteCodes.add(".end method");

        return byteCodes;
    }

    private ArrayList<String> getHandlerBeginningByteCodes(HandlerDeclaration handlerDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<>();

        byteCodes.addAll(getHandlerInfoByteCodes(handlerDeclaration, false));
        byteCodes.add(".limit stack " + maxStackSize);
        int localsSize = handlerDeclaration.getArgs().size() + handlerDeclaration.getLocalVars().size() + 1;
        if(handlerDeclaration.getName().getName() != "initial")
            localsSize += 1;
        byteCodes.add(".limit locals " + localsSize);

        return byteCodes;
    }

    private void addUnaryNotByteCodes(UnaryExpression unaryExpression) {
        visitExpr(unaryExpression.getOperand());
        String oneLabel = getLabel();
        String zeroLabel = getLabel();
        actorByteCodes.add("ifeq " + oneLabel);
        actorByteCodes.add("iconst_0");
        actorByteCodes.add("goto " + zeroLabel);
        actorByteCodes.add(oneLabel + ":");
        actorByteCodes.add("iconst_1");
        actorByteCodes.add(zeroLabel + ":");
    }

    private void addUnaryMinusByteCodes(UnaryExpression unaryExpression) {
        actorByteCodes.add("iconst_0");
        visitExpr(unaryExpression.getOperand());
        actorByteCodes.add("isub");
    }

    private void addUnaryIncDecByteCodes(UnaryExpression unaryExpression, boolean pre, boolean inc) {
        if(!pre)
            visitExpr(unaryExpression.getOperand());

        try {
            Identifier operandIdentifier = (Identifier)unaryExpression.getOperand();
            String symbolTableVariableItemName = SymbolTableVariableItem.STARTKEY + operandIdentifier.getName();
            SymbolTableVariableItem symbolTableVariableItem = (SymbolTableVariableItem) SymbolTable.top.get(symbolTableVariableItemName);
            int operandIndex = symbolTableVariableItem.getIndex();

            if(operandIndex != -1) {
                actorByteCodes.add("iinc " + operandIndex + (inc ? ", 1" : ", -1"));
            } else {
                actorByteCodes.add("aload_0");
                actorByteCodes.add("dup");
                actorByteCodes.add("getfield " + currentActor.getName().getName() + "/" + operandIdentifier.getName() + " " + getTypeDescriptor(symbolTableVariableItem.getType()));
                actorByteCodes.add("iconst_1");
                actorByteCodes.add((inc ? "iadd" : "isub"));
                actorByteCodes.add("putfield " + currentActor.getName().getName() + "/" + operandIdentifier.getName() + " " + getTypeDescriptor(symbolTableVariableItem.getType()));
            }
        } catch(ItemNotFoundException itemNotFoundException) {
            System.out.println("Logical Error in addUnaryPreIncByteCodes");
        }

        if(pre)
            visitExpr(unaryExpression.getOperand());
    }

    private String getBinaryArithmeticOperatorByteCode(BinaryOperator binaryOperator) {
        String byteCode = "";
        switch(binaryOperator) {
            case add:
                byteCode = "iadd";
                break;
            case sub:
                byteCode = "isub";
                break;
            case mult:
                byteCode = "imult";
                break;
            case div:
                byteCode = "idiv";
                break;
            case mod:
                byteCode = "irem";
                break;
            default:
                break;
        }
        return byteCode;
    }

    private String getBinaryRelationalOperatorByteCode(BinaryOperator binaryOperator) {
        String byteCode = "";
        switch(binaryOperator) {
            case lt:
                byteCode = "if_icmple";
                break;
            case gt:
                byteCode = "if_icmpge";
                break;
            case eq:
                byteCode = "if_icmpeq";
                break;
            case neq:
                byteCode = "if_icmpne";
                break;
            default:
                break;
        }
        return byteCode;
    }

    private void addBinaryArithmeticOperatorsByteCodes(BinaryExpression binaryExpression) {
        visitExpr(binaryExpression.getLeft());
        visitExpr(binaryExpression.getRight());
        actorByteCodes.add(getBinaryArithmeticOperatorByteCode(binaryExpression.getBinaryOperator()));
    }

    private void addBinaryRelationalOperatorsByteCodes(BinaryExpression binaryExpression) {
        visitExpr(binaryExpression.getLeft());
        visitExpr(binaryExpression.getRight());

        BinaryOperator binaryOperator = binaryExpression.getBinaryOperator();

        if(binaryExpression.getLeft().getType() instanceof IntType ||
           binaryExpression.getLeft().getType() instanceof BooleanType) {
            String nTrue = getLabel();
            String nAfter = getLabel();
            actorByteCodes.add(getBinaryRelationalOperatorByteCode(binaryOperator) + " " + nTrue);
            actorByteCodes.add("iconst_0");
            actorByteCodes.add("goto " + nAfter);
            actorByteCodes.add(nTrue + ":");
            actorByteCodes.add("iconst_1");
            actorByteCodes.add(nAfter + ":");
        } else if(binaryExpression.getLeft().getType() instanceof StringType) {
            actorByteCodes.add("invokevirtual java/lang/String.equals(Ljava/lang/Object;)Z");
        }
        else {
            actorByteCodes.add("invokevirtual java/lang/Object.equals(Ljava/lang/Object;)Z");
        }
    }

    private void addBinaryAndOperatorByteCodes(BinaryExpression binaryExpression) {
        String nElse = getLabel();
        String nAfter = getLabel();
        visitExpr(binaryExpression.getLeft());
        actorByteCodes.add("ifeq " + nElse);
        visitExpr(binaryExpression.getRight());
        actorByteCodes.add("goto " + nAfter);
        actorByteCodes.add(nElse + ":");
        actorByteCodes.add("iconst_0");
        actorByteCodes.add(nAfter + ":");
    }

    private void addBinaryOrOperatorByteCodes(BinaryExpression binaryExpression) {
        String nElse = getLabel();
        String nAfter = getLabel();
        visitExpr(binaryExpression.getLeft());
        actorByteCodes.add("ifeq " + nElse);
        actorByteCodes.add("iconst_1");
        actorByteCodes.add("goto " + nAfter);
        actorByteCodes.add(nElse + ":");
        binaryExpression.getRight().accept(this);
        actorByteCodes.add(nAfter + ":");
    }

    private void addBinaryAssignByteCodes(BinaryExpression binaryExpression) {
        visitStatement(new Assign(binaryExpression.getLeft(),binaryExpression.getRight()));
        visitExpr(binaryExpression.getLeft());
    }

    private String getTypeDescriptor(Identifier identifier) {
        try {
            String symbolTableVariableItemName = SymbolTableVariableItem.STARTKEY + identifier.getName();
            SymbolTableVariableItem symbolTableVariableItem = (SymbolTableVariableItem) SymbolTable.top.get(symbolTableVariableItemName);
            return getTypeDescriptor(symbolTableVariableItem.getType());
        } catch(ItemNotFoundException itemNotFoundException) {
            System.out.println("Logical Error in getTypeDescriptor(ID)");
            return "";
        }
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

    private int getIndex(Identifier identifier) {
        try {
            String symbolTableVariableItemName = SymbolTableVariableItem.STARTKEY + identifier.getName();
            SymbolTableVariableItem symbolTableVariableItem = (SymbolTableVariableItem) SymbolTable.top.get(symbolTableVariableItemName);
            return symbolTableVariableItem.getIndex();
        } catch (ItemNotFoundException itemNotFoundException) {
            System.out.println("Logical Error in Identifier visit");
            return -1;
        }
    }

    private void addMsgHandlerCallInvokeByteCodes(MsgHandlerCall msgHandlerCall) {
        String byteCode = "invokevirtual Actor/send_";
        byteCode += (msgHandlerCall.getMsgHandlerName().getName());
        byteCode += "(LActor;";
        for(Expression arg : msgHandlerCall.getArgs())
            byteCode += getTypeDescriptor(arg.getType()); //TODO: Think! isn't there a better approach?
        byteCode += ")V";
        actorByteCodes.add(byteCode);
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

        actorByteCodes.addAll(getFieldsByteCodes(actorDeclaration));
        actorByteCodes.add("");

        actorByteCodes.addAll(getActorConstructorByteCodes(actorDeclaration));
        actorByteCodes.add("");

        actorByteCodes.addAll(getSetKnownActorsByteCodes(actorDeclaration));
        actorByteCodes.add("");

        for(VarDeclaration varDeclaration: actorDeclaration.getKnownActors())
            varDeclaration.accept(this);

        for(VarDeclaration varDeclaration: actorDeclaration.getActorVars())
            varDeclaration.accept(this);

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

        inHandler = true;
        pushHandlerDeclarationSymbolTable(handlerDeclaration);

        if(handlerDeclaration.getName().getName() == "initial")
            currentVariableIndex = 1;
        else
            currentVariableIndex = 2;

        if(handlerDeclaration.getName().getName() != "initial") {
            addHandlerByteCodesFile(handlerDeclaration);
            actorByteCodes.addAll(getHandlerSendByteCodes(handlerDeclaration));
            actorByteCodes.add("");
        }

        actorByteCodes.addAll(getHandlerBeginningByteCodes(handlerDeclaration));

        for(VarDeclaration argDeclaration: handlerDeclaration.getArgs())
            argDeclaration.accept(this);

        for(VarDeclaration localVariable: handlerDeclaration.getLocalVars())
            localVariable.accept(this);

        for(Statement statement : handlerDeclaration.getBody())
            visitStatement(statement);

        SymbolTable.pop();

        actorByteCodes.add("return");
        actorByteCodes.add(".end method");
        actorByteCodes.add("");
        inHandler = false;
    }

    @Override
    public void visit(VarDeclaration varDeclaration) {
        if(varDeclaration == null)
            return;

        try {
            String symbolTableVariableItemName = SymbolTableVariableItem.STARTKEY + varDeclaration.getIdentifier().getName();
            SymbolTableVariableItem symbolTableVariableItem = (SymbolTableVariableItem) SymbolTable.top.get(symbolTableVariableItemName);
            symbolTableVariableItem.setIndex((inHandler ? currentVariableIndex++ : -1));
        } catch(ItemNotFoundException itemNotFoundException) {
            System.out.println("Logical Error in VarDeclaration visit");
        }
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

        if(unaryExpression.getUnaryOperator() == UnaryOperator.not)
            addUnaryNotByteCodes(unaryExpression);
        else if(unaryExpression.getUnaryOperator() == UnaryOperator.minus)
            addUnaryMinusByteCodes(unaryExpression);
        else if(unaryExpression.getUnaryOperator() == UnaryOperator.preinc)
            addUnaryIncDecByteCodes(unaryExpression, true,true);
        else if(unaryExpression.getUnaryOperator() == UnaryOperator.predec)
            addUnaryIncDecByteCodes(unaryExpression, true, false);
        else if(unaryExpression.getUnaryOperator() == UnaryOperator.postinc)
            addUnaryIncDecByteCodes(unaryExpression, false, true);
        else if(unaryExpression.getUnaryOperator() == UnaryOperator.postdec)
            addUnaryIncDecByteCodes(unaryExpression, false, false);
    }

    @Override
    public void visit(BinaryExpression binaryExpression) {
        if(binaryExpression == null)
            return;

        BinaryOperator binaryOperator = binaryExpression.getBinaryOperator();

        if(binaryOperator == BinaryOperator.add ||
           binaryOperator == BinaryOperator.sub ||
           binaryOperator == BinaryOperator.mult ||
           binaryOperator == BinaryOperator.div ||
           binaryOperator == BinaryOperator.mod) {
            addBinaryArithmeticOperatorsByteCodes(binaryExpression);
        } else if(binaryOperator == BinaryOperator.eq ||
                  binaryOperator == BinaryOperator.neq ||
                  binaryOperator == BinaryOperator.gt ||
                  binaryOperator == BinaryOperator.lt) {
            addBinaryRelationalOperatorsByteCodes(binaryExpression);
        } else if(binaryOperator == binaryOperator.and) {
            addBinaryAndOperatorByteCodes(binaryExpression);
        } else if(binaryOperator == binaryOperator.or) {
            addBinaryOrOperatorByteCodes(binaryExpression);
        } else if(binaryOperator == binaryOperator.assign) {
            addBinaryAssignByteCodes(binaryExpression);
        }
    }

    @Override
    public void visit(ArrayCall arrayCall) {
        visitExpr(arrayCall.getArrayInstance());
        visitExpr(arrayCall.getIndex());
        actorByteCodes.add("iaload");
    }

    @Override
    public void visit(ActorVarAccess actorVarAccess) {
        if(actorVarAccess == null)
            return;

        actorByteCodes.add("aload_0");
        actorByteCodes.add("getfield " + currentActor.getName().getName() + "/" + actorVarAccess.getVariable().getName() + " " + getTypeDescriptor(actorVarAccess.getVariable()));
//        visitExpr(actorVarAccess.getVariable());
    }

    @Override
    public void visit(Identifier identifier) {
        if(identifier == null)
            return;

        try {
            String symbolTableVariableItemName = SymbolTableVariableItem.STARTKEY + identifier.getName();
            SymbolTableVariableItem symbolTableVariableItem = (SymbolTableVariableItem) SymbolTable.top.get(symbolTableVariableItemName);
            int variableIndex = symbolTableVariableItem.getIndex();
            if(variableIndex != -1) {
                String loadInstruction = "";

                if(symbolTableVariableItem.getType() instanceof IntType || symbolTableVariableItem.getType() instanceof BooleanType)
                    loadInstruction = "iload ";
                else
                    loadInstruction = "aload ";

                loadInstruction += (variableIndex);
                actorByteCodes.add(loadInstruction);
            } else {
                actorByteCodes.add("aload_0");
                actorByteCodes.add("getfield " + currentActor.getName().getName() + "/" + identifier.getName() + " " + getTypeDescriptor(symbolTableVariableItem.getType()));
            }
        }
        catch (ItemNotFoundException itemNotFoundException) {
            System.out.println(identifier.getName());
            System.out.println("Logical Error in Identifier visit");
        }
    }

    @Override
    public void visit(Self self) {
        actorByteCodes.add("aload_0");
    }

    @Override
    public void visit(Sender sender) {
        actorByteCodes.add("aload_1");
    }

    @Override
    public void visit(BooleanValue value) {
        if(value.getConstant() == true)
            actorByteCodes.add("iconst_1");
        else
            actorByteCodes.add("iconst_0");
    }

    @Override
    public void visit(IntValue value) {
        actorByteCodes.add("ldc " + value.getConstant()); //TODO: Bipush? Sipush? Or just ldc?
    }

    @Override
    public void visit(StringValue value) {
        actorByteCodes.add("ldc " + value.getConstant());
    }

    @Override
    public void visit(MsgHandlerCall msgHandlerCall) {
        if(msgHandlerCall == null) {
            return;
        }
        try {
            visitExpr(msgHandlerCall.getInstance());
            actorByteCodes.add("aload_0");
            for (Expression argument : msgHandlerCall.getArgs())
                visitExpr(argument);
            addMsgHandlerCallInvokeByteCodes(msgHandlerCall);
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

        if(conditional.getElseBody() == null) {
            String nAfter = getLabel();
            actorByteCodes.add("ifeq " + nAfter);
            visitStatement(conditional.getThenBody());
            actorByteCodes.add(nAfter + ":");
        } else {
            String nFalse = getLabel();
            actorByteCodes.add("ifeq " + nFalse);
            visitStatement(conditional.getElseBody());
            String nAfter = getLabel();
            actorByteCodes.add("goto " + nAfter);
            actorByteCodes.add(nFalse + ":");
            visitStatement(conditional.getElseBody());
            actorByteCodes.add(nAfter + ":");
        }
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

        //TODO: Expressions?

        actorByteCodes.add("getstatic java/lang/System/out Ljava/io/PrintStream;");

        Expression printArg = print.getArg();
        visitExpr(printArg);

        if(printArg instanceof Identifier && getTypeDescriptor((Identifier)printArg).equals("[I")) {
            actorByteCodes.add("invokestatic java/util/Arrays.toString([I)Ljava/lang/String;");
            actorByteCodes.add("invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V");
        }
        else
            actorByteCodes.add("invokevirtual java/io/PrintStream/println(" + getTypeDescriptor((Identifier)printArg) + ")V");
    }

    @Override
    public void visit(Assign assign) {
        Expression lvalue = assign.getlValue();

        if (lvalue instanceof Identifier) {
            int lvalueIndex = getIndex((Identifier)lvalue);

            if (lvalueIndex != -1) {
                visitExpr(assign.getrValue());
                String loadInstruction = "";
                if (lvalue.getType() instanceof IntType || lvalue.getType() instanceof BooleanType) {
                    loadInstruction = "istore ";
                } else {
                    loadInstruction = "astore ";
                }
                loadInstruction += lvalueIndex;
                actorByteCodes.add(loadInstruction);
            } else {
                actorByteCodes.add("aload_0");
                visitExpr(assign.getrValue());
                actorByteCodes.add("putfield " + currentActor.getName().getName() + "/" + ((Identifier)lvalue).getName() + " " + getTypeDescriptor(lvalue.getType()));
            }
        } else if (lvalue instanceof ArrayCall) {
            visitExpr(((ArrayCall) lvalue).getArrayInstance());
            visitExpr(((ArrayCall) lvalue).getIndex());
            visitExpr(assign.getrValue());
            actorByteCodes.add("iastore");
        }
    }
}