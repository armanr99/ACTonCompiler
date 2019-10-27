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
    actorDefinition+ main EOF
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
    INT (name = ID) LBRACK CONST_INT RBRACK
    { print("int[]" + $name); }
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
    (statementExpression (COMMA statementExpression)*)?
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
    FOR { print("Loop: for"); } LPAR expressionAssignment? SEMI statementExpression? SEMI expressionAssignment? RPAR
        statements
;

statementBlock:
    LBRACE
    statements
    RBRACE
;

statementExpression:
    expressionNonAssignment |
    expressionAssignment
;

expressionNonAssignment:
    expressionOr expressionNonAssignmentTemp
;

expressionNonAssignmentTemp:
    (Q_MARK { print("Operator:?:"); } statementExpression COLON statementExpression)*
;

expressionAssignment:
    LPAR expressionAssignment RPAR |
    ID '=' { print("Operator:="); } expressionNonAssignment
;

expressionOr:
    { print("Operator:||"); } expressionAnd OR expressionOr |
    expressionAnd
;

expressionAnd:
    { print("Operator:&&"); } expressionEq AND expressionAnd |
    expressionEq
;

expressionEq:
    { print("Operator:=="); } expressionCmp EQUAL expressionEq |
    { print("Operator:!="); } expressionCmp NOT_EQUAL expressionEq |
    expressionCmp
;

expressionCmp:
    { print("Operator:<"); } expressionAdd LT expressionCmp |
    { print("Operator:>"); } expressionAdd GT expressionCmp |
    expressionAdd
;

expressionAdd:
    { print("Operator:+"); } expressionMult PLUS expressionAdd |
    { print("Operator:-"); } expressionMult MINUS expressionAdd |
    expressionMult
;

expressionMult:
    { print("Operator:*"); } expressionPre STAR expressionMult |
    { print("Operator:/"); } expressionPre SLASH expressionMult |
    { print("Operator:%"); } expressionPre MODULO expressionMult |
    expressionPre
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
    ID LBRACK statementExpression RBRACK |
    LPAR statementExpression RPAR
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
    (statementExpression (COMMA statementExpression)*)?
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