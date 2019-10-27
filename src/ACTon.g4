grammar ACTon;

@header
{
    import java.util.ArrayList;
    import java.util.List;
}

@members{
    void print(String str){
       System.out.println(str);
    }
}

acton:
    actorDefinition+ main
;

actorDefinition:
    ACTOR (name = ID) (EXTENDS (parentName = ID))? LPAR CONST_INT RPAR
    { print("ActorDec:" + $name.text + ($parentName.text == null ? "" : ("," + $parentName.text))); }
    LBRACE
        actorBody
    RBRACE
;

actorBody:
    knownActors
    actorVars
    initialDefinition?
    msghandlerDefinition*
;

knownActors:
    KNOWNACTORS
    LBRACE
        (knownActorDefinition SEMI)*
    RBRACE
;

knownActorDefinition:
    (type = ID) (name = ID)
    {print("KnownActor:" + $type.text + "," + $name.text);}
;

actorVars:
    ACTORVARS
    LBRACE
        (varDeclaration  SEMI)*
    RBRACE
;

initialDefinition:
    MSGHANDLER
    INITIAL {print("MsgHanderDec:initial");} LPAR msghandlerArguments RPAR
    LBRACE
    (varDeclaration SEMI)*
    statements
    RBRACE
;

varDeclaration:
    arrayDeclaration |
    primitiveDeclaration
;

arrayDeclaration:
    INT ID LBRACK CONST_INT RBRACK
;

primitiveDeclaration:
    varType (name = ID)
    { print("VarDec:" + $varType.text + "," + $name.text); }
;

varType:
    STRING |
    INT |
    BOOLEAN
;

msghandlerDefinition:
    MSGHANDLER
    (name = ID) {print("MsgHanderDec:" + $name.text);} LPAR msghandlerArguments RPAR
    LBRACE
    (varDeclaration SEMI)*
    statements
    RBRACE
;

msghandlerArguments:
    (varDeclaration (COMMA varDeclaration)*)?
;

statements:
    statement*
;

statement:
    statementMatched |
    statementOpen
;

otherStatement:
    statementLoop |
    statementPrint |
    statementBlock |
    statementBreak SEMI |
    statementContinue SEMI |
    statementExpression SEMI |
    statementMsgHandler SEMI |
    SEMI
;

statementMsgHandler:
    (name = (SELF | SENDER | ID)) DOT (method = ID) LPAR msgHandlerStatementArguments RPAR
    {print("MsgHandlerCall:" + $name.text + "," + $method.text); }
;

msgHandlerStatementArguments:
    (expression (COMMA expression)*)?
;

statementPrint:
    PRINT LPAR { print("Built-in:Print"); } statementExpression RPAR SEMI
;

statementBreak:
    BREAK
;

statementContinue:
    CONTINUE
;

statementMatched:
    IF { print("Conditional:if"); } LPAR statementExpression RPAR statementMatched ELSE { print("Conditional:else"); } statementMatched |
    otherStatement
;

statementOpen:
    IF { print("Conditional:if"); } LPAR statementExpression RPAR statement |
    IF { print("Conditional:if"); } LPAR statementExpression RPAR statementMatched ELSE { print("Conditional:else"); } statementOpen
;

statementLoop:
    FOR { print("Loop: for"); } LPAR statementAssignment? SEMI statementExpression? SEMI statementAssignment? RPAR
        statements
;

statementBlock:
    LBRACE
    statements
    RBRACE
;

statementExpression:
    statementNonAssignment |
    statementAssignment
;

statementNonAssignment:
    expressionOr
;

statementAssignment:
    LPAR statementAssignment RPAR |
    ID '=' { print("Operator:="); } expressionOr
;

expression:
    expressionOr
;

expressionOr:
    expressionAnd (OR { print("Operator:||"); } expressionAnd)*
;

expressionAnd:
    expressionEq (AND { print("Operator:&&"); } expressionEq)*
;

expressionEq:
    expressionCmp ((name = (EQUAL | NOT_EQUAL) { print("Operator:" + $name.text); })expressionCmp)*
;

expressionCmp:
    expressionAdd ((name = (LT | GT) { print("Operator:" + $name.text); })expressionAdd)*
;

expressionAdd:
    expressionMult ((name = (PLUS | MINUS) { print("Operator:" + $name.text); })expressionMult)*
;

expressionMult:
    expressionPre ((name = (STAR | SLASH | MODULO) { print("Operator:" + $name.text); })expressionPre)*
;

expressionPre:
    (name = (NOT | MINUS | MINUSMINUS | PLUSPLUS) { print("Operator:" + $name.text); } )* expressionPost
;

expressionPost:
    expressionOther (name = (MINUSMINUS | PLUSPLUS) { print("Operator:" + $name.text); })*
;

expressionOther:
    CONST_INT |
    CONST_STRING |
    TRUE |
    FALSE |
    ID |
    ID LBRACK expression RBRACK |
    LPAR expression RPAR
;

main:
    MAIN
    LBRACE
        actorDeclarations
    RBRACE
;

actorDeclarations:
    (actorDeclaration SEMI)*
;

actorDeclaration
    @init
    {
        List <String> knowns;
        knowns = new ArrayList<>();
    }
    :
    (type = ID) (name = ID)
    LPAR
    ((known = ID) {knowns.add($known.text);} (COMMA (known_other = ID) {knowns.add($known_other.text);})*)?
    RPAR
    COLON
    {
        String message = "ActorInstantiation:" + $type.text + "," + $name.text;
        for(String known : knowns)
            message += ("," + known);
        print(message);
    }
    LPAR
    actorDeclarationArguments
    RPAR
;

actorDeclarationArguments:
    (expression (COMMA expression)*)?
;

//Reserved keywords
MSGHANDLER:
    'msghandler'
;

INITIAL:
    'initial'
;

EXTENDS:
    'extends'
;

ACTORVARS:
    'actorvars'
;

KNOWNACTORS:
    'knownactors'
;

ACTOR:
    'actor'
;

PRINT:
    'print'
;

FOR:
    'for'
;

ELSE:
    'else'
;

IF:
    'if'
;

SENDER:
    'sender'
;

SELF:
    'self'
;

MAIN:
    'main'
;

STRING:
    'string'
;

BOOLEAN:
    'boolean'
;

INT:
    'int'
;

TRUE:
    'true'
;

FALSE:
    'false'
;

CONTINUE:
    'continue'
;

BREAK:
    'break'
;

//Seperators
LBRACE:
    '{'
;

RBRACE:
    '}'
;

SEMI:
    ';'
;

DOT:
    '.'
;

//Operators
LPAR:
    '('
;

RPAR:
    ')'
;

LBRACK:
    '['
;

RBRACK:
    ']'
;

PLUSPLUS:
    '++'
;

MINUSMINUS:
    '--'
;

NOT:
    '!'
;

MINUS:
    '-'
;

MODULO:
    '%'
;

SLASH:
    '/'
;

STAR:
    '*'
;

PLUS:
    '+'
;

LT:
    '<'
;

GT:
    '>'
;

EQUAL:
    '=='
;

NOT_EQUAL:
    '!='
;

AND:
    '&&'
;

OR:
    '||'
;

Q_MARK:
    '?'
;

COLON:
    ':'
;

ASSIGN:
    '='
;

COMMA:
    ','
;

//Literals
CONST_INT:
    [0-9]+
;

CONST_STRING:
    '"' ~('\r' | '\n' | '"')* '"'
;

ID:
    [a-zA-Z_][a-zA-Z0-9_]*
;

//Whitespaces
COMMENT:
    '//'(~[\r\n])* -> skip
;

NEW_LINE:
    '\r'? '\n' -> skip
;

WHITE_SPACE:
    [ \t] -> skip
;