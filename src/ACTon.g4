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
        knownActorDefinition*
    RBRACE
;

knownActorDefinition:
    ((type = ID) (name = ID) SEMI)
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
    INITIAL {print("MsgHanderDec:initial");} LPAR (varDeclaration (COMMA varDeclaration)*)? RPAR
    LBRACE
    (varDeclaration SEMI)*
    statements
    RBRACE
;

varDeclaration:
    arrayDeclaration |
    primitiveDeclaration
;

varType:
    STRING |
    INT |
    BOOLEAN
;

arrayDeclaration:
    INT ID LBRACK CONST_INT RBRACK
;

primitiveDeclaration:
    varType (name = ID)
    { print("VarDec:" + $varType.text + "," + $name.text); }
;

msghandlerDefinition:
    MSGHANDLER
    (name = ID) {print("MsgHanderDec:" + $name.text);} LPAR (varDeclaration (COMMA varDeclaration)*)? RPAR
    LBRACE
    (varDeclaration SEMI)*
    statements
    RBRACE
;

statements:
    statement*
;

statement:
    matchedStatement |
    openStatement
;

otherStatement:
    blockStatement |
    expressionStatement SEMI |
    loopStatement |
    printStatement |
    SEMI
;

printStatement:
    PRINT LPAR { print("Built-in:Print"); } expressionStatement RPAR SEMI
;

matchedStatement:
    IF { print("Conditional:if"); } LPAR expressionStatement RPAR matchedStatement ELSE { print("Conditional:else"); } matchedStatement |
    otherStatement
;

openStatement:
    IF { print("Conditional:if"); } LPAR expressionStatement RPAR statement |
    IF { print("Conditional:if"); } LPAR expressionStatement RPAR matchedStatement ELSE { print("Conditional:else"); } openStatement
;

loopStatement:
    FOR { print("Loop: for"); } LPAR assignmentStatement? SEMI expressionStatement? SEMI assignmentStatement? RPAR
//    LBRACE //Bug when deleting lbrace and rbraces
        statements
//    RBRACE
;

blockStatement:
    LBRACE
    statements
    RBRACE
;

expressionStatement:
    normalStatement |
    assignmentStatement
;

normalStatement:
    expressionOr
;

//expressionTernary:
//    expressionStatement Q_MARK expressionStatement COLON expressionStatement
//;

assignmentStatement:
    LPAR assignmentStatement RPAR |
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
    expressionUnary ((name = (STAR | SLASH | MODULO) { print("Operator:" + $name.text); })expressionUnary)*
;

expressionUnary:
    (name = (NOT | MINUS | MINUSMINUS | PLUSPLUS) { print("Operator:" + $name.text); } )* expressionMem
;

expressionMem:
    expressionMethods (name = (MINUSMINUS | PLUSPLUS) { print("Operator:" + $name.text); })*
;

expressionMethods:
    ((name = (SELF | SENDER | ID)) (method = expressionMethodsTemp) {print("MsgHandlerCall:" + $name.text + "," + $method.str); } ) | expressionOther //TODO: fix child name
;

expressionMethodsTemp returns [String str]:
    DOT (name = ID) LPAR (expression (COMMA expression)*)? RPAR
    { $str = $name.text; }
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

actorDeclaration returns [List <String> strs]
    @init
    {
        $strs = new ArrayList<>();
    }
    :
    (type = ID) (name = ID)
    LPAR
    ((known = ID) {$strs.add($known.text);} (COMMA (known_other = ID) {$strs.add($known_other.text);})*)?
    RPAR
    COLON
    {
        String message = "ActorInstantiation:" + $type.text + "," + $name.text;
        for(String str : $strs)
            message += ("," + str);
        print(message);
    }
    LPAR
    (expression (COMMA expression)*)?
    RPAR
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