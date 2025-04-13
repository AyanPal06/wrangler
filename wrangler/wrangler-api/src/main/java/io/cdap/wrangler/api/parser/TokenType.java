/*
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

package io.cdap.wrangler.api.parser;

/**
 * Enum defining different types of tokens that are recognized by the parser.
 */
public enum TokenType {
  BOOLEAN,        // Boolean token. Example: true, false.
  INTEGER,        // Integer token. Example: 1, 2, 10.
  DECIMAL,        // Double, float token. Example: 1.0, 10.01, 20.32
  STRING,         // String token. Example: 'This is a string', "This is also a string".
  COLUMN_NAME,    // Identifier used as column name. Example: body.
  COLUMN_RANGE,   // Range of columns. Example: body[1-5], body[1,2,3], body[1,3-4].
  DIRECTIVE_NAME, // Directive name token. Example :parse-as-csv
  COMMAND,        // Command complete token. Example: parse-as-csv body ',' true true.
  IDENTIFIER,     // Identifier token. Example: parse-as-csv
  RECORD_PATH,    // Path of record or element of record. Example: $
  PROPERTY_NAME,  // Name of the property in the record. Example: body.id, body.message
  BYTE_SIZE,      // Byte size token with unit. Example: 10KB, 1.5MB
  TIME_DURATION  // Time duration token with unit. Example: 150ms, 2.5s
}