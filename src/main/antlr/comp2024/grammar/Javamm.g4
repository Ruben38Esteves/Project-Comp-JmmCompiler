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
LBRACK : '[' ;
RBRACK : ']' ;
MUL : '*' ;
ADD : '+' ;
DIV : '/' ;
SUB : '-' ;
NOT : '!' ;
TRUE : 'true' ;
FALSE : 'false' ;
AND : '&&' ;
OR : '||' ;
LT : '<' ;
GT : '>' ;
ACCESS : '.' ;

CLASS : 'class' ;
NEW : 'new' ;
INTARRAY : 'int' '[' ']' ;
STRINGARRAY : 'String['']' ;
INTVARARG: 'int' '...' ;
INT : 'int' ;
FLOAT : 'float' ;
CHAR : 'char' ;
BOOLEAN : 'boolean' ;
STRING : 'String' ;
VOID : 'void' ;
PUBLIC : 'public' ;
RETURN : 'return' ;

INTEGER : ('0' | [1-9][0-9]*) ;
ID : [a-zA-Z$_][a-zA-Z0-9$_]* ;
LINE_COMMENT : '//' .*? '\n' -> skip ;
MULTILINE_COMMENT : '/*' .*? '*/' -> skip ;
WS : [ \t\n\r\f]+ -> skip ;

program
    : stmt EOF
    | importStmt* classDecl EOF
    ;

importStmt
    : 'import' value+=ID(ACCESS value+=ID)* SEMI
    ;

classDecl
    : CLASS name=ID ('extends' extendedClass= ID)?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : name= INTVARARG
    | name= STRINGARRAY
    | name= INTARRAY
    | name= INT
    | name= FLOAT
    | name= BOOLEAN
    | name= CHAR
    | name= STRING
    | name= VOID
    | name= ID
    ;

param
    : type name=ID
    ;

methodDecl locals[boolean isPublic=false]
    : 'public'? 'static' type name=ID LPAREN (param(',' param)*)? RPAREN LCURLY varDecl* stmt* RCURLY
    |(PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN (param(',' param)*)? RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;
//(params+=param(',' params+=param)*)?

stmt
    : expr SEMI #Something //S
    | expr EQUALS expr SEMI #AssignStmt //
    | RETURN expr SEMI #ReturnStmt //
    | LCURLY (stmt)* RCURLY #Tobereplaced //
    | ifStmt (elseIfStmt)* elseStmt #IfChainStatement //
    | 'while' LPAREN expr RPAREN stmt #WhileStatement //
    ;

methodCall
    : LPAREN ((expr)(',' expr)*)? RPAREN ;


ifStmt
    : 'if' LPAREN expr RPAREN stmt #IfStatement //
    ;

elseIfStmt
    : 'else' ifStmt #ElseIfStatement //
    ;

elseStmt
    : 'else' stmt #ElseStatement //
    ;


expr
    : LPAREN expr RPAREN #Parenthesis //
    | expr LBRACK expr RBRACK #ArrayAccessExpre //
    | expr op= ACCESS expr #MethodExpr //
    | op= NOT expr #UnaryExpr //
    | NEW name= ID LPAREN RPAREN #ClassInstance //
    | NEW type expr #NewArray //
    | expr op= MUL expr #ArithmeticExpr //
    | expr op= DIV expr #ArithmeticExpr //
    | expr op= ADD expr #ArithmeticExpr //
    | expr op= SUB expr #ArithmeticExpr //
    | expr op= LT expr #BooleanExpr //
    | expr op= GT expr #BooleanExpr //
    | expr op= AND expr #BooleanExpr //
    | expr op= OR expr #BooleanExpr //
    | value=INTEGER #IntegerLiteral //
    | value=(TRUE|FALSE) #BooleanLiteral //
    | name= ID #VarRefExpr //
    | name= ID methodCall #CallMethod //
    | expr methodCall #CallMethod //
    | LBRACK ((expr)(',' expr)*)? RBRACK #ArrayInitialization
    ;





