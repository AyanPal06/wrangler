grammar Directives;

@header {
/**
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.cdap.wrangler.parser;
}

// Starting point for parsing a recipe
recipe
    : directive (NEWLINE directive)* (NEWLINE)?
    ;

// A directive statement
directive
    : simplename predicate?
    | predicate
    ;

// A directive with the name and its arguments
simplename
    : IDENTIFIER COLON arguments
    ;

// Predicate
predicate
    : PREDICATE_START ID PREDICATE_END
    | PREDICATE_START ID COLON arguments PREDICATE_END
    ;

// Arguments for a specific directive
arguments
    : arg (COMMA arg)*
    ;

// Different types of arguments
arg
    : ID                            # IdentifierArg
    | column_name                   # ColumnArg
    | column_range                  # ColumnRangeArg
    | decimal                       # DecimalArg
    | NUMBER                        # NumberArg
    | BOOLEAN                       # BooleanArg
    | STRING                        # StringArg
    | byteSizeArg                   # ByteSizeArg
    | timeDurationArg               # TimeDurationArg
    | record_path                   # RecordPathArg
    | property_path                 # PropertyPathArg
    | directive                     # DirectiveArg
    | directive_name                # DirectiveNameArg
    ;

// Byte size arguments (e.g., 10KB, 1.5MB)
byteSizeArg
    : BYTE_SIZE
    ;

// Time duration arguments (e.g., 150ms, 2.5s)
timeDurationArg
    : TIME_DURATION
    ;

// Column name, examples: name, cnl[1] or cp.name
column_name
    : ID col_range? property_path? macro?
    ;

// Column range, examples cnl[1-5], cnl[1,2,3,4]
column_range
    : ID col_range
    ;

// Column access, examples: [1]
col_range
    : LBRACKET (NUMBER | range | ranges) RBRACKET
    ;

// Range with start and end positions, example: 1-5
range
    : NUMBER RANGE NUMBER
    ;

// Ranges with multiple columns, example: 1,2,3,4
ranges
    : NUMBER (COMMA NUMBER)*
    ;

// Path to a property for nested data in objects
property_path
    : DOT property
    ;

property
    : ID DOT property
    | ID (LBRACKET STRING RBRACKET)*
    | ID
    ;

// Path to a value in a record
record_path
    : PATH
    ;

decimal
    : NUM_OR_DOT
    ;

// Directive name
directive_name
    : COLON IDENTIFIER
    ;

macro
    : MACRO
    ;

// Lexer Rules
WS: [ \t\r]+ -> skip;
NEWLINE: [\n]+;

// Special symbols
LBRACKET: '[';
RBRACKET: ']';
COLON: ':';
COMMA: ',';
DOT: '.';
RANGE: '-';

// Tokens with explicit lexical rules
NUMBER: DIGIT+;
NUM_OR_DOT: DIGIT+ | DIGIT+ DOT DIGIT+ | DOT DIGIT+;
BOOLEAN: 'true' | 'false';
PREDICATE_START: '<';
PREDICATE_END: '>';

// New tokens for byte size and time duration
BYTE_SIZE: (DIGIT+ | DIGIT+ DOT DIGIT+) BYTE_UNIT;
TIME_DURATION: (DIGIT+ | DIGIT+ DOT DIGIT+) TIME_UNIT;

// Units for byte size
fragment BYTE_UNIT: ('B'|'b'|'KB'|'Kb'|'kb'|'MB'|'Mb'|'mb'|'GB'|'Gb'|'gb'|'TB'|'Tb'|'tb'|'PB'|'Pb'|'pb');

// Units for time duration
fragment TIME_UNIT: ('ms'|'s'|'m'|'min'|'h'|'d');

STRING: '\'' ( ~('\''|'\\') | ('\\' .) )* '\'' | '"' ( ~('"'|'\\') | ('\\' .) )* '"';
IDENTIFIER: [a-zA-Z_][a-zA-Z0-9_-]*;

ID: [a-zA-Z0-9_-][a-zA-Z0-9_.-]*;
PATH: '$' [a-zA-Z0-9_.[\]]+;
MACRO: '${' (.*?) '}';
fragment DIGIT: [0-9];