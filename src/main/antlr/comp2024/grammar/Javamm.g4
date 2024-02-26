grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LSQUARE: '[';
RSQUARE: ']';
COMMA: ',';
DOT: '.';

MUL : '*' ;
ADD : '+' ;
SUB : '-';
DIV : '/';

// NOTE OR?
AND: '&&';
LT: '<';
NOT: '!';


INT : 'int' ;
BOOL: 'boolean';
VOID: 'void';
STRING: 'String';

TRUE : 'true';
FALSE: 'false';

IF : 'if';
ELSE : 'else';
WHILE: 'while';
NEW : 'new';
LENGTH: 'length';


CLASS : 'class' ;
THIS: 'this';
PUBLIC : 'public' ;
RETURN : 'return' ;
EXTEND: 'extends';

IMPORT : 'import';

STATIC: 'static';
MAIN: 'main';

INTEGER : [0-9] ;
ID : [a-zA-Z]+ ;


WS : [ \t\n\r\f]+ -> skip ;

importDecl
    : IMPORT ID ('.' ID)* ';'
    ;

program
    : importDecl* classDecl EOF
    ;


// NOTE - suportar Implements
// Public e private em vars

classDecl
    : CLASS name=ID
      (EXTEND ID)?
      LCURLY
      varDecl*
      methodDecl*
      RCURLY
    ;


varDecl
    : type name=ID SEMI
    ;

type
    : name= INT  ('[]' | '...')?
    | name= BOOL
    | name = ID
    ;

//NOTE alternado?
methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (param (',' param)*)? RPAREN
        LCURLY
        varDecl* stmt*
        RETURN expr SEMI
        RCURLY
    | (PUBLIC {$isPublic=true;})?
        STATIC VOID MAIN
        LPAREN
        STRING  LSQUARE RSQUARE ID
        RPAREN
        LCURLY
        varDecl* stmt*
        RCURLY
    ;

param
    : type name=ID
    ;

//Note expr EQUALS expr SEMI //#AssignStmt // devia ser ID?
// Return? RETURN expr SEMI //#ReturnStmt
stmt
    : LCURLY stmt* RCURLY
    | IF LPAREN expr RPAREN stmt
      ELSE stmt
    | WHILE LPAREN expr RPAREN stmt
    | expr SEMI
    | ID EQUALS expr SEMI //#AssignStmt //
    | ID LSQUARE expr RSQUARE EQUALS  expr SEMI
    | RETURN expr SEMI //#ReturnStmt
    ;

//NOTE o resto dos operadores
expr
    // Binary expressions
    : NOT expr
    | expr op= (MUL | DIV) expr // #BinaryExpr //
    | expr op= (ADD | SUB) expr // #BinaryExpr //
    | expr op= LT expr
    | expr op= AND expr
    // others
    | expr LSQUARE expr RSQUARE
    | expr DOT LENGTH // ID?
    | expr DOT ID LPAREN (expr (COMMA expr)*)? RPAREN
    | NEW INT LSQUARE expr RSQUARE
    | NEW ID LPAREN RPAREN // Dafult?
    | LPAREN expr RPAREN
    | LSQUARE (expr (COMMA expr)*)? RSQUARE
    | TRUE
    | FALSE
    | THIS
    | value=INTEGER // #IntegerLiteral //
    | name=ID // #VarRefExpr //
    ;



