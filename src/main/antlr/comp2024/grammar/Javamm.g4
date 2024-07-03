grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

LCURLY      : '{' ;
RCURLY      : '}' ;
LPAREN      : '(' ;
RPAREN      : ')' ;
LBRACKET    : '[' ;
RBRACKET    : ']' ;

NOT         : '!' ;
MUL         : '*' ;
DIV         : '/' ;
ADD         : '+' ;
SUB         : '-' ;
MINOR       : '<' ;
MAJOR       : '>' ;
MINOREQ     : '<=' ;
MAJOREQ     : '>=' ;
NOTEQ       : '!=' ;
BOOL_EQ     : '==' ;
AND         : '&' ;
OR          : '|' ;
SCAND       : '&&' ;
SCOR        : '||' ;
EQUALS      : '=';

SEMI        : ';' ;
COMMA       : ',' ;
DOT         : '.' ;
TDOT        : '...' ;

WHILE       : 'while' ;
IF          : 'if' ;
ELSE        : 'else' ;

NEW         : 'new' ;
THIS        : 'this' ;

IMPORT      : 'import' ;
CLASS       : 'class' ;
PUBLIC      : 'public' ;
STATIC      : 'static' ;
EXTENDS     : 'extends' ;
BOOLEAN     : 'boolean' ;
TRUE        : 'true' ;
FALSE       : 'false' ;
VOID        : 'void' ;
INT         : 'int' ;
STRING      : 'String' ;
RETURN      : 'return' ;

INTEGER     : '0' | [1-9][0-9]* ;
ID          : [a-zA-Z_$][a-zA-Z0-9_$]* ;

WS          : [ \t\n\r\f]+ -> skip ;

SL_COMMENT  : '//' .*? '\n' -> skip;
ML_COMMENT  : '/*' .*? '*/' -> skip;

program
    : importStmt* cls=classDecl EOF
    ;

importStmt
    : IMPORT name+=ID (DOT name+=ID)* SEMI
    ;

classDecl
    : CLASS name=ID
        (EXTENDS extendedClass=ID)?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (param (COMMA param)*)? RPAREN
        LCURLY varDecl* stmt* RCURLY
    | (PUBLIC {$isPublic=true;})? STATIC VOID name=ID
        LPAREN STRING LBRACKET RBRACKET atribute=ID RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

param
    : type name=ID
    ;

stmt

    : expr SEMI                                     #ExprStmt       //
    | LCURLY varDecl* stmt* RCURLY                  #ScopeStmt      //
    | WHILE LPAREN expr RPAREN (stmt)               #WhileStmt      //
    | IF LPAREN expr RPAREN (stmt) ELSE (stmt)      #IfElseStmt     //
    | RETURN expr SEMI                              #ReturnStmt     //
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : name= INT                                 #Ttype //
    | name= BOOLEAN                             #Ttype //
    | name= STRING                              #Ttype //
    | name= ID                                  #Ttype //
    | name= VOID                                #Ttype //
    | nameType= type TDOT                       #Vararg //
    | nameType= type LBRACKET RBRACKET          #ArrayType //
    ;

expr
    : expr DOT name=ID                                     #LengthExpr         //
    | NEW type LBRACKET expr RBRACKET                       #NewArrayExpr       //
    | NEW name=ID LPAREN RPAREN                             #NewClassExpr       //
    | obj=expr LBRACKET inarr=expr RBRACKET                     #ArrayAccessExpr    //
    | obj=expr DOT name=ID LPAREN (expr (COMMA expr)*)? RPAREN  #MethodCallExpr     //
    | name=ID LPAREN (expr (COMMA expr)*)? RPAREN           #MethodCallExpr     //
    | op = NOT expr                                         #UnaryExpr          //
    | expr op= (MUL | DIV) expr                             #MultiplicativeExpr //
    | expr op= (ADD | SUB) expr                             #AdditiveExpr       //
    | expr op= (MINOR | MAJOR | MINOREQ | MAJOREQ) expr     #RelationalExpr     //
    | expr op= (NOTEQ | BOOL_EQ) expr                       #BoolEqualExpr      //
    | expr op= SCAND expr                                   #ShortCAndExpr     //
    | expr op= AND expr                                     #AndExpr            //
    | expr op= SCOR expr                                    #ShortCOrExpr      //
    | expr op= OR expr                                      #OrExpr             //
    | expr op= EQUALS expr                                  #AssignmentExpr     //
    | LPAREN (expr (COMMA expr)*)? RPAREN                   #ParenExpr          //
    | LBRACKET (expr (COMMA expr)*)? RBRACKET               #ArrayInitExpr      //
    | name=ID                                               #VarRefExpr         //
    | value=INTEGER                                         #IntegerLiteralExpr //
    | value=(TRUE | FALSE)                                  #BooleanLiteralExpr //
    | value=STRING                                          #StringLiteralExpr  //
    | THIS                                                  #ThisLiteralExpr    //
    ;



