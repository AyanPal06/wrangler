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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.parser;

import io.cdap.wrangler.api.LazyNumber;
import io.cdap.wrangler.api.RecipeSymbol;
import io.cdap.wrangler.api.SourceInfo;
import io.cdap.wrangler.api.Triplet;
import io.cdap.wrangler.api.parser.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

/**
 * This class <code>RecipeVisitor</code> implements the visitor pattern
 * used during traversal of the AST tree.
 */
public final class RecipeVisitor extends DirectivesBaseVisitor<RecipeSymbol.Builder> {
  private RecipeSymbol.Builder builder = new RecipeSymbol.Builder();

  public RecipeSymbol getCompiledUnit() {
    return builder.build();
  }

  @Override
  public RecipeSymbol.Builder visitDirective(DirectivesParser.DirectiveContext ctx) {
    builder.createTokenGroup(getOriginalSource(ctx));
    return super.visitDirective(ctx);
  }

  @Override
  public RecipeSymbol.Builder visitIdentifier(DirectivesParser.IdentifierContext ctx) {
    builder.addToken(new Identifier(ctx.Identifier().getText()));
    return super.visitIdentifier(ctx);
  }

  @Override
  public RecipeSymbol.Builder visitPropertyList(DirectivesParser.PropertyListContext ctx) {
    Map<String, Token> props = new HashMap<>();
    for (DirectivesParser.PropertyContext property : ctx.property()) {
      String identifier = property.Identifier().getText();
      Token token;
      if (property.number() != null) {
        token = new Numeric(new LazyNumber(property.number().getText()));
      } else if (property.bool() != null) {
        token = new Bool(Boolean.parseBoolean(property.bool().getText()));
      } else {
        String text = property.text().getText();
        token = new Text(text.substring(1, text.length() - 1));
      }
      props.put(identifier, token);
    }
    builder.addToken(new Properties(props));
    return builder;
  }

  @Override
  public RecipeSymbol.Builder visitPragmaLoadDirective(DirectivesParser.PragmaLoadDirectiveContext ctx) {
    for (TerminalNode identifier : ctx.identifierList().Identifier()) {
      builder.addLoadableDirective(identifier.getText());
    }
    return builder;
  }

  @Override
  public RecipeSymbol.Builder visitPragmaVersion(DirectivesParser.PragmaVersionContext ctx) {
    builder.addVersion(ctx.Number().getText());
    return builder;
  }

  @Override
  public RecipeSymbol.Builder visitNumberRanges(DirectivesParser.NumberRangesContext ctx) {
    List<Triplet<Numeric, Numeric, String>> output = new ArrayList<>();
    for (DirectivesParser.NumberRangeContext range : ctx.numberRange()) {
      List<TerminalNode> numbers = range.Number();
      String text = range.value().getText();
      if (text.startsWith("'") && text.endsWith("'")) {
        text = text.substring(1, text.length() - 1);
      }
      output.add(new Triplet<>(
        new Numeric(new LazyNumber(numbers.get(0).getText())),
        new Numeric(new LazyNumber(numbers.get(1).getText())),
        text
      ));
    }
    builder.addToken(new Ranges(output));
    return builder;
  }

  @Override
  public RecipeSymbol.Builder visitEcommand(DirectivesParser.EcommandContext ctx) {
    builder.addToken(new DirectiveName(ctx.Identifier().getText()));
    return builder;
  }

  @Override
  public RecipeSymbol.Builder visitColumn(DirectivesParser.ColumnContext ctx) {
    builder.addToken(new ColumnName(ctx.Column().getText().substring(1)));
    return builder;
  }

  @Override
  public RecipeSymbol.Builder visitText(DirectivesParser.TextContext ctx) {
    String value = ctx.String().getText();
    builder.addToken(new Text(value.substring(1, value.length() - 1)));
    return builder;
  }

  @Override
  public RecipeSymbol.Builder visitNumber(DirectivesParser.NumberContext ctx) {
    builder.addToken(new Numeric(new LazyNumber(ctx.Number().getText())));
    return builder;
  }

  @Override
  public RecipeSymbol.Builder visitBool(DirectivesParser.BoolContext ctx) {
    builder.addToken(new Bool(Boolean.parseBoolean(ctx.Bool().getText())));
    return builder;
  }

  @Override
  public RecipeSymbol.Builder visitCondition(DirectivesParser.ConditionContext ctx) {
    StringBuilder sb = new StringBuilder();
    for (int i = 1; i < ctx.getChildCount() - 1; ++i) {
      sb.append(ctx.getChild(i).getText()).append(" ");
    }
    builder.addToken(new Expression(sb.toString().trim()));
    return builder;
  }

  @Override
  public RecipeSymbol.Builder visitCommand(DirectivesParser.CommandContext ctx) {
    builder.addToken(new DirectiveName(ctx.Identifier().getText()));
    return builder;
  }

  @Override
  public RecipeSymbol.Builder visitColList(DirectivesParser.ColListContext ctx) {
    List<String> names = new ArrayList<>();
    for (TerminalNode column : ctx.Column()) {
      names.add(column.getText().substring(1));
    }
    builder.addToken(new ColumnNameList(names));
    return builder;
  }

  @Override
  public RecipeSymbol.Builder visitNumberList(DirectivesParser.NumberListContext ctx) {
    List<LazyNumber> numerics = new ArrayList<>();
    for (TerminalNode number : ctx.Number()) {
      numerics.add(new LazyNumber(number.getText()));
    }
    builder.addToken(new NumericList(numerics));
    return builder;
  }

  @Override
  public RecipeSymbol.Builder visitBoolList(DirectivesParser.BoolListContext ctx) {
    List<Boolean> booleans = new ArrayList<>();
    for (TerminalNode bool : ctx.Bool()) {
      booleans.add(Boolean.parseBoolean(bool.getText()));
    }
    builder.addToken(new BoolList(booleans));
    return builder;
  }

  @Override
  public RecipeSymbol.Builder visitStringList(DirectivesParser.StringListContext ctx) {
    List<String> strs = new ArrayList<>();
    for (TerminalNode string : ctx.String()) {
      String text = string.getText();
      strs.add(text.substring(1, text.length() - 1));
    }
    builder.addToken(new TextList(strs));
    return builder;
  }

  /**
   * Handles BYTE_SIZE token parsing.
   */
  @Override
  public RecipeSymbol.Builder visitByteSizeArg(DirectivesParser.ByteSizeArgContext ctx) {
    builder.addToken(new ByteSize(ctx.getText()));
    return builder;
  }

  /**
   * Handles TIME_DURATION token parsing.
   */
  @Override
  public RecipeSymbol.Builder visitTimeDurationArg(DirectivesParser.TimeDurationArgContext ctx) {
    builder.addToken(new TimeDuration(ctx.getText()));
    return builder;
  }

  private SourceInfo getOriginalSource(ParserRuleContext ctx) {
    int a = ctx.getStart().getStartIndex();
    int b = ctx.getStop().getStopIndex();
    Interval interval = new Interval(a, b);
    String text = ctx.start.getInputStream().getText(interval);
    int lineno = ctx.getStart().getLine();
    int column = ctx.getStart().getCharPositionInLine();
    return new SourceInfo(lineno, column, text);
  }
}
