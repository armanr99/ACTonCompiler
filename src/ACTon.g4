grammar ACTon;

INTEGER:
    [0-9]+
;

STRING:
    '"' ~('\r' | '\n' | '"')* '"'
;

ID:
    [a-zA-Z_][a-zA-Z0-9_]*
;

COMMENT:
    '//'(~[\r\n])* -> skip
;

NEW_LINE:
    '\r'? '\n' -> skip
;

WHITE_SPACE:
    [ \t] -> skip
;