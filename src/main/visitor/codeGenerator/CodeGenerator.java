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
import java.util.ArrayList;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Stack;
import java.util.HashMap;
import java.util.HashSet;

public class CodeGenerator extends VisitorImpl {

    private final String outputPath = "./output/";
    private ArrayList<String> currentByteCodes = new ArrayList<>();
    private ArrayList<String> defaultActorByteCodes = new ArrayList<>();

    private HashSet<String> addedDefaultHandlers = new HashSet<>();

    private ActorDeclaration currentActor = null;
    private final int maxStackSize = 50;

    private int currentVariableIndex = 1;

    private boolean inHandler = false;

    private int labelIndex = 0;

    HashMap<String, Integer> mainIndexes = new HashMap<>();
    HashMap<String, Type> mainActorTypes = new HashMap<>();

    static Stack<Integer> loopIndexes = new Stack<>();
    int loopDepth = 0;

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
        SymbolTable.push(getActorDeclarationSymbolTable(actorDeclaration));
    }

    private SymbolTable getActorDeclarationSymbolTable(ActorDeclaration actorDeclaration) {
        try
        {
            String name = actorDeclaration.getName().getName();
            SymbolTableActorItem actorItem = (SymbolTableActorItem) SymbolTable.root.getInCurrentScope(SymbolTableActorItem.STARTKEY + name);
            SymbolTable next = actorItem.getActorSymbolTable();
            return next;
        }
        catch(ItemNotFoundException itemNotFound)
        {
            System.out.println("There is an error in pushing ActorDeclaration symbol table");
            return null;
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

    private ArrayList<String> getActorConstructorByteCodes(ActorDeclaration actorDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<String>();

        byteCodes.add(".method public <init>(I)V");
        byteCodes.add(".limit stack 2");
        byteCodes.add(".limit locals 2");
        byteCodes.add("aload_0");
        byteCodes.add("iload_1");
        byteCodes.add("invokespecial Actor/<init>(I)V");

        for(VarDeclaration actorVar : actorDeclaration.getActorVars())
            byteCodes.addAll(getInitializeActorVarByteCodes(actorVar));

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
        byteCodes.add("getfield " + className + "/sender LActor;");


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

        if(!handlerDeclaration.getName().getName().equals("initial"))
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

        byteCode += ("/<init>(L" + currentActor.getName().getName() + ";");
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
        currentByteCodes.add("ifeq " + oneLabel);
        currentByteCodes.add("iconst_0");
        currentByteCodes.add("goto " + zeroLabel);
        currentByteCodes.add(oneLabel + ":");
        currentByteCodes.add("iconst_1");
        currentByteCodes.add(zeroLabel + ":");
    }

    private void addUnaryMinusByteCodes(UnaryExpression unaryExpression) {
        currentByteCodes.add("iconst_0");
        visitExpr(unaryExpression.getOperand());
        currentByteCodes.add("isub");
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
                currentByteCodes.add("iinc " + operandIndex + (inc ? ", 1" : ", -1"));
            } else {
                currentByteCodes.add("aload_0");
                currentByteCodes.add("dup");
                currentByteCodes.add("getfield " + currentActor.getName().getName() + "/" + operandIdentifier.getName() + " " + getTypeDescriptor(symbolTableVariableItem.getType()));
                currentByteCodes.add("iconst_1");
                currentByteCodes.add((inc ? "iadd" : "isub"));
                currentByteCodes.add("putfield " + currentActor.getName().getName() + "/" + operandIdentifier.getName() + " " + getTypeDescriptor(symbolTableVariableItem.getType()));
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
                byteCode = "if_icmplt";
                break;
            case gt:
                byteCode = "if_icmpgt";
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
        currentByteCodes.add(getBinaryArithmeticOperatorByteCode(binaryExpression.getBinaryOperator()));
    }

    private void addBinaryRelationalOperatorsByteCodes(BinaryExpression binaryExpression) {
        visitExpr(binaryExpression.getLeft());
        visitExpr(binaryExpression.getRight());

        BinaryOperator binaryOperator = binaryExpression.getBinaryOperator();

        if(binaryExpression.getLeft().getType() instanceof IntType ||
           binaryExpression.getLeft().getType() instanceof BooleanType) {
            String nTrue = getLabel();
            String nAfter = getLabel();
            currentByteCodes.add(getBinaryRelationalOperatorByteCode(binaryOperator) + " " + nTrue);
            currentByteCodes.add("iconst_0");
            currentByteCodes.add("goto " + nAfter);
            currentByteCodes.add(nTrue + ":");
            currentByteCodes.add("iconst_1");
            currentByteCodes.add(nAfter + ":");
        } else if(binaryExpression.getLeft().getType() instanceof StringType) {
            currentByteCodes.add("invokevirtual java/lang/String.equals(Ljava/lang/Object;)Z");
        }
        else if(binaryExpression.getLeft().getType() instanceof ArrayType) {
            currentByteCodes.add("invokestatic java/util/Arrays.equals([I[I)Z");
        }
        else {
            currentByteCodes.add("invokevirtual java/lang/Object.equals(Ljava/lang/Object;)Z");
        }
    }

    private void addBinaryAndOperatorByteCodes(BinaryExpression binaryExpression) {
        String nElse = getLabel();
        String nAfter = getLabel();
        visitExpr(binaryExpression.getLeft());
        currentByteCodes.add("ifeq " + nElse);
        visitExpr(binaryExpression.getRight());
        currentByteCodes.add("goto " + nAfter);
        currentByteCodes.add(nElse + ":");
        currentByteCodes.add("iconst_0");
        currentByteCodes.add(nAfter + ":");
    }

    private void addBinaryOrOperatorByteCodes(BinaryExpression binaryExpression) {
        String nElse = getLabel();
        String nAfter = getLabel();
        visitExpr(binaryExpression.getLeft());
        currentByteCodes.add("ifeq " + nElse);
        currentByteCodes.add("iconst_1");
        currentByteCodes.add("goto " + nAfter);
        currentByteCodes.add(nElse + ":");
        binaryExpression.getRight().accept(this);
        currentByteCodes.add(nAfter + ":");
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
        String byteCode = "invokevirtual ";

        String instanceString = msgHandlerCall.getInstance().getType().toString();
        if(instanceString.equals("Sender"))
            byteCode += ("Actor");
        else
            byteCode += instanceString;

        byteCode += "/send_";
        byteCode += (msgHandlerCall.getMsgHandlerName().getName());
        byteCode += "(LActor;";
        for(Expression arg : msgHandlerCall.getArgs())
            byteCode += getTypeDescriptor(arg.getType()); //TODO: Think! isn't there a better approach?
        byteCode += ")V";
        currentByteCodes.add(byteCode);
    }

    private void addMainBeginningByteCodes(Main mainProgram) {
        currentByteCodes.add(".class public Main");
        currentByteCodes.add(".super java/lang/Object");
        currentByteCodes.add("");
        currentByteCodes.add(".method public <init>()V");
        currentByteCodes.add(".limit stack 1");
        currentByteCodes.add(".limit locals 1");
        currentByteCodes.add("0: aload_0");
        currentByteCodes.add("1: invokespecial java/lang/Object/<init>()V");
        currentByteCodes.add("4: return");
        currentByteCodes.add(".end method");
        currentByteCodes.add("");
        currentByteCodes.add(".method public static main([Ljava/lang/String;)V");
        currentByteCodes.add(".limit stack " + maxStackSize);
        currentByteCodes.add(".limit locals " + (mainProgram.getMainActors().size() + 1));
    }

    private ActorDeclaration getActorDeclaration(String actorName) {
        try {
            SymbolTableActorItem actorItem = (SymbolTableActorItem) SymbolTable.root.getInCurrentScope(SymbolTableActorItem.STARTKEY + actorName);
            return actorItem.getActorDeclaration();
        }
        catch(ItemNotFoundException itemNotFound)
        {
            System.out.println("Logical Error in getActorDeclaration");
            return null;
        }
    }

    private void addMainActorsInstantiationsByteCodes(Main mainProgram) {
        int varIndex = 1;
        for(ActorInstantiation actorInstantiation : mainProgram.getMainActors()) {
            mainIndexes.put(actorInstantiation.getIdentifier().getName(), varIndex);
            mainActorTypes.put(actorInstantiation.getIdentifier().getName(), (actorInstantiation.getType()));
            String actorName = ((ActorType)actorInstantiation.getType()).getName().getName();
            currentByteCodes.add("new " + actorName);
            currentByteCodes.add("dup");
            ActorDeclaration actorDeclaration = getActorDeclaration(actorName);
            currentByteCodes.add("ldc " + actorDeclaration.getQueueSize());
            currentByteCodes.add("invokespecial " + actorName + "/<init>(I)V");
            currentByteCodes.add("astore " + varIndex++);
        }
    }

    private void addMainSetKnownActorsByteCodes(Main mainProgram) {
        for(ActorInstantiation actorInstantiation : mainProgram.getMainActors()) {
            currentByteCodes.add("aload " + mainIndexes.get(actorInstantiation.getIdentifier().getName()));
            for(Identifier knownActor : actorInstantiation.getKnownActors()) {
                currentByteCodes.add("aload " + mainIndexes.get(knownActor.getName()));
            }
            String invokeCode = "invokevirtual ";
            invokeCode += (((ActorType)actorInstantiation.getType()).getName().getName());
            invokeCode += "/setKnownActors(";
            for(Identifier knownActor : actorInstantiation.getKnownActors()) {
                invokeCode += getTypeDescriptor(mainActorTypes.get(knownActor.getName()));
            }
            invokeCode += ")V";

            currentByteCodes.add(invokeCode);
        }
    }

    private void addMainInitialsByteCodes(Main mainProgram) {
        int varIndex = 1;
        for(ActorInstantiation actorInstantiation : mainProgram.getMainActors()) {
            String actorName = ((ActorType)actorInstantiation.getType()).getName().getName();
            ActorDeclaration actorDeclaration = getActorDeclaration(actorName);
            SymbolTable actorSymbolTable = getActorDeclarationSymbolTable(actorDeclaration);

            try {
                SymbolTableHandlerItem symbolTableHandlerItem = (SymbolTableHandlerItem) actorSymbolTable.getInCurrentScope(SymbolTableHandlerItem.STARTKEY + "initial");
                HandlerDeclaration handlerDeclaration = symbolTableHandlerItem.getHandlerDeclaration();

                currentByteCodes.add("aload " + varIndex);
                for(Expression initArg : actorInstantiation.getInitArgs()) {
                    visitExpr(initArg);
                }
                String invokeCode = "invokevirtual ";
                invokeCode += actorName;
                invokeCode += "/initial(";
                for(VarDeclaration varDeclaration : handlerDeclaration.getArgs()) {
                    invokeCode += getTypeDescriptor(varDeclaration.getType());
                }
                invokeCode += ")V";
                currentByteCodes.add(invokeCode);
            } catch(ItemNotFoundException itemNotFoundException) {}

            varIndex++;
        }
    }

    private void addMainStartsByteCodes(Main mainProgram) {
        int varIndex = 1;
        for(ActorInstantiation actorInstantiation : mainProgram.getMainActors()) {
            currentByteCodes.add("aload " + varIndex++);
            currentByteCodes.add("invokevirtual " + ((ActorType)actorInstantiation.getType()).getName().getName() + "/start()V");
        }
    }

    private void addMainByteCodesFile(Main mainProgram) {
        addMainBeginningByteCodes(mainProgram);
        addMainActorsInstantiationsByteCodes(mainProgram);
        addMainSetKnownActorsByteCodes(mainProgram);
        addMainInitialsByteCodes(mainProgram);
        addMainStartsByteCodes(mainProgram);
        currentByteCodes.add("return");
        currentByteCodes.add(".end method");

        writeByteCodesFile(currentByteCodes, "Main");
    }

    private void addDefaultActorBeginningByteCodes() {
        defaultActorByteCodes.add(".class public DefaultActor");
        defaultActorByteCodes.add(".super java/lang/Thread");
        defaultActorByteCodes.add("");
        defaultActorByteCodes.add(".method public <init>()V");
        defaultActorByteCodes.add(".limit stack 1");
        defaultActorByteCodes.add(".limit locals 1");
        defaultActorByteCodes.add("aload_0");
        defaultActorByteCodes.add("invokespecial java/lang/Thread/<init>()V");
        defaultActorByteCodes.add("return");
        defaultActorByteCodes.add(".end method");
        defaultActorByteCodes.add("");
    }

    private void addDefaultActorHandlerByteCodes(HandlerDeclaration handlerDeclaration) {
        String handlerInfo = getHandlerInfoByteCodes(handlerDeclaration, true).get(0);
        if(addedDefaultHandlers.contains(handlerInfo)) {
            return;
        } else {
            addedDefaultHandlers.add(handlerInfo);
            defaultActorByteCodes.addAll(getHandlerInfoByteCodes(handlerDeclaration, true));
            defaultActorByteCodes.add(".limit stack " + maxStackSize);
            defaultActorByteCodes.add(".limit locals " + (handlerDeclaration.getArgs().size() + 2));
            defaultActorByteCodes.add("getstatic java/lang/System/out Ljava/io/PrintStream;");
            defaultActorByteCodes.add("ldc \"there is no msghandler named " + handlerDeclaration.getName().getName() + " in sender\"");
            defaultActorByteCodes.add("invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V");
            defaultActorByteCodes.add("return");
            defaultActorByteCodes.add(".end method");
            defaultActorByteCodes.add("");
        }
    }

    private ArrayList<String> getInitializeLocalVarByteCodes(VarDeclaration varDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<>();

        if (varDeclaration.getType() instanceof IntType) {
            byteCodes.add("ldc 0");
            byteCodes.add("istore " + currentVariableIndex);
        }
        else if (varDeclaration.getType() instanceof BooleanType) {
            byteCodes.add("iconst_0");
            byteCodes.add("istore " + currentVariableIndex);
        }
        else if (varDeclaration.getType() instanceof StringType) {
            byteCodes.add("ldc \"\"");
            byteCodes.add("astore " + currentVariableIndex);
        }
        else if(varDeclaration.getType() instanceof ArrayType) {
            byteCodes.add("ldc " + ((ArrayType)varDeclaration.getType()).getSize());
            byteCodes.add("newarray int");
            byteCodes.add("astore " + currentVariableIndex);
        }

        return byteCodes;
    }

    private ArrayList<String> getInitializeActorVarByteCodes(VarDeclaration varDeclaration) {
        ArrayList<String> byteCodes = new ArrayList<>();

        byteCodes.add("aload_0");

        if (varDeclaration.getType() instanceof IntType) {
            byteCodes.add("ldc 0");
        }
        else if (varDeclaration.getType() instanceof BooleanType) {
            byteCodes.add("iconst_0");
        }
        else if (varDeclaration.getType() instanceof StringType) {
            byteCodes.add("ldc \"\"");
        }
        else if(varDeclaration.getType() instanceof ArrayType) {
            byteCodes.add("ldc " + ((ArrayType)varDeclaration.getType()).getSize());
            byteCodes.add("newarray int");
        }
        byteCodes.add("putfield " + currentActor.getName().getName() + "/" + varDeclaration.getIdentifier().getName() + " " + getTypeDescriptor(varDeclaration.getType()));

        return byteCodes;
    }

    @Override
    public void visit(Program program){
        addDefaultActorBeginningByteCodes();

        for(ActorDeclaration actorDeclaration : program.getActors())
            actorDeclaration.accept(this);
        program.getMain().accept(this);

        writeByteCodesFile(defaultActorByteCodes, "DefaultActor");
    }

    @Override
    public void visit(ActorDeclaration actorDeclaration) {
        pushActorDeclarationSymbolTable(actorDeclaration);

        currentActor = actorDeclaration;

        currentByteCodes.addAll(getActorInfoByteCodes(actorDeclaration));
        currentByteCodes.add("");

        currentByteCodes.addAll(getFieldsByteCodes(actorDeclaration));
        currentByteCodes.add("");

        currentByteCodes.addAll(getActorConstructorByteCodes(actorDeclaration));
        currentByteCodes.add("");

        currentByteCodes.addAll(getSetKnownActorsByteCodes(actorDeclaration));
        currentByteCodes.add("");

        for(VarDeclaration varDeclaration: actorDeclaration.getKnownActors())
            varDeclaration.accept(this);

        for(VarDeclaration varDeclaration: actorDeclaration.getActorVars())
            varDeclaration.accept(this);

        if(actorDeclaration.getInitHandler() != null)
            actorDeclaration.getInitHandler().accept(this);

        for(MsgHandlerDeclaration msgHandlerDeclaration: actorDeclaration.getMsgHandlers())
            msgHandlerDeclaration.accept(this);

        writeByteCodesFile(currentByteCodes, actorDeclaration.getName().getName());
        currentByteCodes.clear();

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
            addDefaultActorHandlerByteCodes(handlerDeclaration);
            currentByteCodes.addAll(getHandlerSendByteCodes(handlerDeclaration));
            currentByteCodes.add("");
        }

        currentByteCodes.addAll(getHandlerBeginningByteCodes(handlerDeclaration));

        for(VarDeclaration argDeclaration: handlerDeclaration.getArgs())
            argDeclaration.accept(this);

        for(VarDeclaration localVariable: handlerDeclaration.getLocalVars()) {
            currentByteCodes.addAll(getInitializeLocalVarByteCodes(localVariable));
            localVariable.accept(this);
        }

        for(Statement statement : handlerDeclaration.getBody())
            visitStatement(statement);

        SymbolTable.pop();

        currentByteCodes.add("return");
        currentByteCodes.add(".end method");
        currentByteCodes.add("");
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

        addMainByteCodesFile(programMain);

        SymbolTable.pop();
    }

    @Override
    public void visit(ActorInstantiation actorInstantiation) {
        if(actorInstantiation == null)
            return;
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
        currentByteCodes.add("iaload");
    }

    @Override
    public void visit(ActorVarAccess actorVarAccess) {
        if(actorVarAccess == null)
            return;

        currentByteCodes.add("aload_0");
        currentByteCodes.add("getfield " + currentActor.getName().getName() + "/" + actorVarAccess.getVariable().getName() + " " + getTypeDescriptor(actorVarAccess.getVariable()));
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
                currentByteCodes.add(loadInstruction);
            } else {
                currentByteCodes.add("aload_0");
                currentByteCodes.add("getfield " + currentActor.getName().getName() + "/" + identifier.getName() + " " + getTypeDescriptor(symbolTableVariableItem.getType()));
            }
        }
        catch (ItemNotFoundException itemNotFoundException) {
            System.out.println(identifier.getName());
            System.out.println("Logical Error in Identifier visit");
        }
    }

    @Override
    public void visit(Self self) {
        currentByteCodes.add("aload_0");
    }

    @Override
    public void visit(Sender sender) {
        currentByteCodes.add("aload_1");
    }

    @Override
    public void visit(BooleanValue value) {
        if(value.getConstant() == true)
            currentByteCodes.add("iconst_1");
        else
            currentByteCodes.add("iconst_0");
    }

    @Override
    public void visit(IntValue value) {
        currentByteCodes.add("ldc " + value.getConstant()); //TODO: Bipush? Sipush? Or just ldc?
    }

    @Override
    public void visit(StringValue value) {
        currentByteCodes.add("ldc " + value.getConstant());
    }

    @Override
    public void visit(MsgHandlerCall msgHandlerCall) {
        if(msgHandlerCall == null) {
            return;
        }
        try {
            visitExpr(msgHandlerCall.getInstance());
            currentByteCodes.add("aload_0");
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
            currentByteCodes.add("ifeq " + nAfter);
            visitStatement(conditional.getThenBody());
            currentByteCodes.add(nAfter + ":");
        } else {
            String nFalse = getLabel();
            currentByteCodes.add("ifeq " + nFalse);
            visitStatement(conditional.getThenBody());
            String nAfter = getLabel();
            currentByteCodes.add("goto " + nAfter);
            currentByteCodes.add(nFalse + ":");
            visitStatement(conditional.getElseBody());
            currentByteCodes.add(nAfter + ":");
        }
    }

    @Override
    public void visit(For loop) {
        visitStatement(loop.getInitialize());

        loopDepth++;
        int lastDepth = loopDepth;
        loopIndexes.push(lastDepth);

        String nFor = getLabel();
        currentByteCodes.add(nFor + ":");

        if(loop.getCondition() != null) {
            visitExpr(loop.getCondition());
            currentByteCodes.add("ifeq " + "Break" + lastDepth);
        }

        visitStatement(loop.getBody());

        currentByteCodes.add("Continue" + lastDepth + ":");
        visitStatement(loop.getUpdate());

        currentByteCodes.add("goto " + nFor);
        currentByteCodes.add("Break" + lastDepth + ":");

        loopIndexes.pop();
    }

    @Override
    public void visit(Break b) {
        currentByteCodes.add("goto " + "Break" + loopIndexes.peek());
    }

    @Override
    public void visit(Continue c) {
        currentByteCodes.add("goto " + "Continue" + loopIndexes.peek());
    }

    @Override
    public void visit(Print print) {
        if(print == null)
            return;

        //TODO: Expressions?

        currentByteCodes.add("getstatic java/lang/System/out Ljava/io/PrintStream;");

        Expression printArg = print.getArg();
        visitExpr(printArg);

        if(printArg.getType() instanceof ArrayType) {
            currentByteCodes.add("invokestatic java/util/Arrays.toString([I)Ljava/lang/String;");
            currentByteCodes.add("invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V");
        }
        else
            currentByteCodes.add("invokevirtual java/io/PrintStream/println(" + getTypeDescriptor(printArg.getType()) + ")V");
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
                currentByteCodes.add(loadInstruction);
            } else {
                currentByteCodes.add("aload_0");
                visitExpr(assign.getrValue());
                currentByteCodes.add("putfield " + currentActor.getName().getName() + "/" + ((Identifier)lvalue).getName() + " " + getTypeDescriptor(lvalue.getType()));
            }
        } else if (lvalue instanceof ArrayCall) {
            visitExpr(((ArrayCall) lvalue).getArrayInstance());
            visitExpr(((ArrayCall) lvalue).getIndex());
            visitExpr(assign.getrValue());
            currentByteCodes.add("iastore");
        }
    }
}