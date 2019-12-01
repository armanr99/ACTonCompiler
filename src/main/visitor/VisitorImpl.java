package main.visitor;

import main.ast.node.*;
import main.ast.node.Program;
import main.ast.node.declaration.*;
import main.ast.node.declaration.handler.*;
import main.ast.node.declaration.VarDeclaration;
import main.ast.node.expression.*;
import main.ast.node.expression.values.BooleanValue;
import main.ast.node.expression.values.IntValue;
import main.ast.node.expression.values.StringValue;
import main.ast.node.statement.*;
import main.symbolTable.*;
import main.symbolTable.itemException.*;
import main.symbolTable.symbolTableVariableItem.*;

import java.util.*;
import java.util.logging.Handler;

import main.ast.type.*;
import main.ast.type.arrayType.*;

public class VisitorImpl implements Visitor {

    ArrayList<String> preOrder = new ArrayList<String>();
    HashSet<String> errors = new HashSet<String>();

    boolean firstPass = true;
    boolean secondPass = false;
    int actorTempCount = 0;

    public void printPreOrder() {
        for(String node : preOrder) {
            System.out.println(node);
        }
    }

    public void check(Program program) {
        SymbolTable globalSymbolTable = new SymbolTable();
        SymbolTable.push(globalSymbolTable);
        SymbolTable.root = globalSymbolTable;

        visit(program);
        firstPass = false;
        secondPass = true;
        visit(program);
        secondPass = false;

        if(hasErrors()) {
            printErrors();
        }
//        else {
//            printPreOrder();
//        }

    }

    private int getErrorLine(String error) {
        String line = "";
        for(int i = 5; (i < error.length() && error.charAt(i) != ':'); i++) {
            line += error.charAt(i);
        }
        return Integer.parseInt(line);
    }

    public void printErrors() {
        List<String> sortedErrors = new ArrayList<String>(errors);
        sortedErrors.sort(Comparator.comparingInt(this::getErrorLine));

        for(String error : sortedErrors) {
            System.out.println(error);
        }
    }

    public boolean hasErrors() {
        return (errors.size() > 0);
    }

    private void addActorRedefinitionError(ActorDeclaration actorDeclaration) {
        String error = "Line:";
        error += actorDeclaration.getLine();
        error += ":";
        error += "Redefinition of actor ";
        error += actorDeclaration.getName().getName();
        errors.add(error);
    }

    private void addQueueSizeError(ActorDeclaration actorDeclaration) {
        String error = "Line:";
        error += actorDeclaration.getLine();
        error += ":Queue size must be positive";
        errors.add(error);
    }

    private void addArraySizeError(VarDeclaration varDeclaration) {
        String error = "Line:";
        error += varDeclaration.getLine();
        error += ":Array size must be positive";
        errors.add(error);
    }

    private void addVarRedefinitionError(VarDeclaration varDeclaration) {
        String error = "Line:";
        error += varDeclaration.getLine();
        error += ":Redefinition of variable ";
        error += varDeclaration.getIdentifier().getName();
        errors.add(error);
    }

    private void addHandlerRedefinitionError(HandlerDeclaration handlerDeclaration) {
        String error = "Line:";
        error += handlerDeclaration.getLine();
        error += ":Redefinition of msghandler ";
        error += handlerDeclaration.getName().getName();
        errors.add(error);
    }

    private String getActorKey(String actorName) {
        return (SymbolTableActorItem.STARTKEY + actorName);
    }

    private void handleVariableItemFirstPass(SymbolTableVariableItem variableItem, VarDeclaration varDeclaration) {
        String variableItemKey = variableItem.getKey();

        try {
            SymbolTable preSymbolTable = SymbolTable.top.getPreSymbolTable();
            try {
                preSymbolTable.get(variableItem.getKey());
                throw new ItemAlreadyExistsException();
            } catch (ItemNotFoundException e1) {}

            SymbolTable.top.put(variableItem);

        } catch(ItemAlreadyExistsException e) {
            addVarRedefinitionError(varDeclaration);
            int count = 1;

            while(true) {
                try {
                    String newName = variableItemKey + count;
                    variableItem.setName(newName);
                    SymbolTable.top.put(variableItem);
                } catch(ItemAlreadyExistsException e2) {
                    count++;
                    continue;
                }
                break;
            }
        }
    }

    private void handleVariableItemSecondPass(SymbolTableVariableItem variableItem, VarDeclaration varDeclaration, String actorName) {
        String actorKey = getActorKey(actorName);
        SymbolTableItem actorItem;

        try {
            actorItem = SymbolTable.root.get(actorKey);
            HashSet<String> visited = new HashSet<String>();

            visited.add(actorName);
            String parent = ((SymbolTableActorItem) actorItem).getParentName();

            while(parent != null && !visited.contains(parent)) {
                String parentKey = getActorKey(parent);
                SymbolTableItem parentItem;

                try {
                    parentItem = SymbolTable.root.get(parentKey);
                    SymbolTable parentSymbolTable = ((SymbolTableActorItem) parentItem).getActorSymbolTable();
                    try {
                        parentSymbolTable.get(variableItem.getKey());
                        addVarRedefinitionError(varDeclaration);
                        break;
                    } catch(ItemNotFoundException e2) {}

                    visited.add(parent);
                    parent = ((SymbolTableActorItem) parentItem).getParentName();
                } catch (ItemNotFoundException e1) {
                    // Parent Not Found
                }
            }

        } catch(ItemNotFoundException e) {
            // Actor Not Found
        }
    }

    private void handleHandlerItemFirstPass(SymbolTableHandlerItem handlerItem, HandlerDeclaration handlerDeclaration) {
        String handlerItemKey = handlerItem.getKey();

        try {
            SymbolTable.top.put(handlerItem);
        } catch(ItemAlreadyExistsException e) {
            addHandlerRedefinitionError(handlerDeclaration);
            int count = 1;

            while(true) {
                try {
                    String newName = handlerItemKey + count;
                    handlerItem.setName(newName);
                    SymbolTable.top.put(handlerItem);
                } catch(ItemAlreadyExistsException e2) {
                    count++;
                    continue;
                }
                break;
            }
        }
    }

    private void handleHandlerItemSecondPass(SymbolTableHandlerItem handlerItem, HandlerDeclaration handlerDeclaration, String actorName) {
        String actorKey = getActorKey(actorName);
        SymbolTableItem actorItem;

        try {
            actorItem = SymbolTable.root.get(actorKey);
            HashSet<String> visited = new HashSet<String>();

            visited.add(actorName);
            String parent = ((SymbolTableActorItem) actorItem).getParentName();

            while(parent != null && !visited.contains(parent)) {
                String parentKey = getActorKey(parent);
                SymbolTableItem parentItem;

                try {
                    parentItem = SymbolTable.root.get(parentKey);
                    SymbolTable parentSymbolTable = ((SymbolTableActorItem) parentItem).getActorSymbolTable();
                    try {
                        parentSymbolTable.get(handlerItem.getKey());
                        addHandlerRedefinitionError(handlerDeclaration);
                        break;
                    } catch(ItemNotFoundException e2) {}

                    visited.add(parent);
                    parent = ((SymbolTableActorItem) parentItem).getParentName();
                } catch (ItemNotFoundException e1) {
                    // Parent Not Found
                }
            }

        } catch(ItemNotFoundException e) {
            // Actor Not Found
        }
    }

    @Override
    public void visit(Program program) {
        preOrder.add(program.toString());

        ArrayList<ActorDeclaration> programActors = program.getActors();
        for(ActorDeclaration actorDec : programActors) {
            actorDec.accept(this);
        }

        Main programMain = program.getMain();
        programMain.accept(this);

        SymbolTable.pop();
    }

    @Override
    public void visit(ActorDeclaration actorDeclaration) {
        preOrder.add(actorDeclaration.toString());

        SymbolTableActorItem symbolTableActorItem = new SymbolTableActorItem(actorDeclaration);

        if(firstPass) {
            try {
                SymbolTable.top.put(symbolTableActorItem);
            } catch(ItemAlreadyExistsException e) {
                addActorRedefinitionError(actorDeclaration);
                while(true) {
                    try {
                        String newName = symbolTableActorItem.getKey() + actorTempCount;
                        symbolTableActorItem.setName(newName);
                        SymbolTable.top.put(symbolTableActorItem);
                    } catch(ItemAlreadyExistsException e1) {
                        actorTempCount++;
                        continue;
                    }
                    break;
                }
            }
        }

        SymbolTable actorSymbolTable = new SymbolTable(SymbolTable.top, actorDeclaration.getName().getName());
        symbolTableActorItem.setActorSymbolTable(actorSymbolTable);
        SymbolTable.push(actorSymbolTable);

        Identifier actorName = actorDeclaration.getName();
        actorName.accept(this);

        Identifier parentName = actorDeclaration.getParentName();
        if(parentName != null) {
            parentName.accept(this); //TODO: check if null is valid
        }

        if(firstPass) {
            int actorQueueSize = actorDeclaration.getQueueSize();
            if(actorQueueSize <= 0) {
                addQueueSizeError(actorDeclaration);
            }
        }

        ArrayList<VarDeclaration> actorKnownActors = actorDeclaration.getKnownActors();
        for(VarDeclaration knownActor : actorKnownActors) {
            SymbolTableKnownActorItem symbolTableKnownActorItem = new SymbolTableKnownActorItem(knownActor);

            if(firstPass) {
                handleVariableItemFirstPass(symbolTableKnownActorItem, knownActor);
            } else if(secondPass) {
                handleVariableItemSecondPass(symbolTableKnownActorItem, knownActor, actorDeclaration.getName().getName());
            }

            knownActor.accept(this);
        }

        ArrayList<VarDeclaration> actorVars = actorDeclaration.getActorVars();
        for(VarDeclaration actorVar : actorVars) {
            SymbolTableActorVariableItem symbolTableActorVariableItem = new SymbolTableActorVariableItem(actorVar);
            if(firstPass) {
                handleVariableItemFirstPass(symbolTableActorVariableItem, actorVar);
            } else if (secondPass) {
                handleVariableItemSecondPass(symbolTableActorVariableItem, actorVar, actorDeclaration.getName().getName());
            }
            actorVar.accept(this);
        }

        InitHandlerDeclaration actorInit = actorDeclaration.getInitHandler();
        if(actorInit != null) {
            actorInit.accept(this);
        }

        ArrayList<MsgHandlerDeclaration> actorMsgHandlers = actorDeclaration.getMsgHandlers(); //TODO: check duplicate
        for(MsgHandlerDeclaration msgHandler : actorMsgHandlers) {
            msgHandler.accept(this);
        }

        SymbolTable.pop();
    }

    @Override
    public void visit(HandlerDeclaration handlerDeclaration) {
        preOrder.add(handlerDeclaration.toString());

        SymbolTableHandlerItem symbolTableHandlerItem = new SymbolTableHandlerItem(handlerDeclaration);

        if(firstPass) {
            handleHandlerItemFirstPass(symbolTableHandlerItem, handlerDeclaration);
        } else if(secondPass) {
            handleHandlerItemSecondPass(symbolTableHandlerItem, handlerDeclaration, SymbolTable.top.getName());
        }

        SymbolTable handlerSymbolTable = new SymbolTable(SymbolTable.top, SymbolTable.top.getName());
        symbolTableHandlerItem.setHandlerSymbolTable(handlerSymbolTable);
        SymbolTable.push(handlerSymbolTable);

        Identifier handlerName = handlerDeclaration.getName();
        handlerName.accept(this);

        ArrayList<VarDeclaration> handlerArgs = handlerDeclaration.getArgs();
        for(VarDeclaration arg : handlerArgs) {
            SymbolTableHandlerArgumentItem symbolTableHandlerArgumentItem = new SymbolTableHandlerArgumentItem(arg);
            if(firstPass) {
                handleVariableItemFirstPass(symbolTableHandlerArgumentItem, arg);
            } else if(secondPass) {
                handleVariableItemSecondPass(symbolTableHandlerArgumentItem, arg, SymbolTable.top.getName());
            }
            arg.accept(this);
        }

        ArrayList<VarDeclaration> handlerLocalVars = handlerDeclaration.getLocalVars();
        for(VarDeclaration localVar : handlerLocalVars) {
            SymbolTableLocalVariableItem symbolTableLocalVariableItem = new SymbolTableLocalVariableItem(localVar);
            if(firstPass) {
                handleVariableItemFirstPass(symbolTableLocalVariableItem, localVar);
            } else if(secondPass) {
                handleVariableItemSecondPass(symbolTableLocalVariableItem, localVar, SymbolTable.top.getName());
            }
            localVar.accept(this);
        }

        ArrayList<Statement> handlerStatements = handlerDeclaration.getBody();
        for(Statement statement : handlerStatements) {
            statement.accept(this);
        }

        SymbolTable.pop();
    }

    @Override
    public void visit(VarDeclaration varDeclaration) {
        preOrder.add(varDeclaration.toString());

        if(firstPass) {
            Type varType = varDeclaration.getType();
            if(varType instanceof ArrayType) {
                if(((ArrayType) varType).getSize() <= 0) {
                    addArraySizeError(varDeclaration);
                }
            }
        }

        Identifier varIdentifier = varDeclaration.getIdentifier();
        varIdentifier.accept(this);
    }

    @Override
    public void visit(Main mainActors) {
        preOrder.add(mainActors.toString());

        ArrayList<ActorInstantiation> actorInstantiations = mainActors.getMainActors();
        for(ActorInstantiation actorInstantiation : actorInstantiations) {
            actorInstantiation.accept(this);
        }
    }

    @Override
    public void visit(ActorInstantiation actorInstantiation) {
        preOrder.add(actorInstantiation.toString());

        Identifier actorName = actorInstantiation.getIdentifier();
        actorName.accept(this);

        ArrayList<Identifier> actorKnownActors = actorInstantiation.getKnownActors();
        for(Identifier knownActor : actorKnownActors) {
            knownActor.accept(this);
        }

        ArrayList<Expression> initArgs = actorInstantiation.getInitArgs();
        for(Expression initArg : initArgs) {
            initArg.accept(this);
        }
    }

    @Override
    public void visit(UnaryExpression unaryExpression) {
        preOrder.add(unaryExpression.toString());

        Expression operator = unaryExpression.getOperand();
        operator.accept(this);
    }

    @Override
    public void visit(BinaryExpression binaryExpression) {
        preOrder.add(binaryExpression.toString());

        Expression left = binaryExpression.getLeft();
        left.accept(this);

        Expression right = binaryExpression.getRight();
        right.accept(this);
    }

    @Override
    public void visit(ArrayCall arrayCall) {
        preOrder.add(arrayCall.toString());

        Expression arrayInstance = arrayCall.getArrayInstance();
        arrayInstance.accept(this);

        Expression index = arrayCall.getIndex();
        index.accept(this);
    }

    @Override
    public void visit(ActorVarAccess actorVarAccess) {
        preOrder.add(actorVarAccess.toString());

        Self self = actorVarAccess.getSelf();
        self.accept(this);

        Identifier variableName = actorVarAccess.getVariable();
        variableName.accept(this);
    }

    @Override
    public void visit(Identifier identifier) {
        preOrder.add(identifier.toString());
    }

    @Override
    public void visit(Self self) {
        preOrder.add(self.toString());
    }

    @Override
    public void visit(Sender sender) {
        preOrder.add(sender.toString());
    }

    @Override
    public void visit(BooleanValue value) {
        preOrder.add(value.toString());
    }

    @Override
    public void visit(IntValue value) {
        preOrder.add(value.toString());
    }

    @Override
    public void visit(StringValue value) {
        preOrder.add(value.toString());
    }

    @Override
    public void visit(Block block) {
        preOrder.add(block.toString());

        ArrayList<Statement> statements = block.getStatements();
        for(Statement statement : statements) {
            statement.accept(this);
        }
    }

    @Override
    public void visit(Conditional conditional) {
        preOrder.add(conditional.toString());

        Expression expression = conditional.getExpression();
        expression.accept(this);

        Statement thenBody = conditional.getThenBody();
        thenBody.accept(this);

        Statement elseBody = conditional.getElseBody();
        if(elseBody != null) {
            elseBody.accept(this);
        }
    }

    @Override
    public void visit(For loop) {
        preOrder.add(loop.toString());

        Assign initialize = loop.getInitialize();
        initialize.accept(this);

        Expression condition = loop.getCondition();
        condition.accept(this);

        Assign update = loop.getUpdate();
        update.accept(this);

        Statement body = loop.getBody();
        body.accept(this);
    }

    @Override
    public void visit(Break breakLoop) {
        preOrder.add(breakLoop.toString());
    }

    @Override
    public void visit(Continue continueLoop) {
        preOrder.add(continueLoop.toString());
    }

    @Override
    public void visit(MsgHandlerCall msgHandlerCall) {
        preOrder.add(msgHandlerCall.toString());

        Expression instance = msgHandlerCall.getInstance();
        instance.accept(this);

        Identifier msgHandlerName = msgHandlerCall.getMsgHandlerName();
        msgHandlerName.accept(this);

        ArrayList<Expression> args = msgHandlerCall.getArgs();
        for(Expression arg : args) {
            arg.accept(this);
        }
    }

    @Override
    public void visit(Print print) {
        preOrder.add(print.toString());

        Expression arg = print.getArg();
        arg.accept(this);
    }

    @Override
    public void visit(Assign assign) {
        preOrder.add(assign.toString());

        Expression lValue = assign.getlValue();
        lValue.accept(this);

        Expression rValue = assign.getrValue();
        rValue.accept(this);
    }
}
