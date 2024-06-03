/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.prettyprint;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.StatementListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.AssignmentExpressionTreeImpl;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.InternalPrefixUnaryExpression;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.pattern.GuardedPatternTreeImpl;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.model.statement.CaseGroupTreeImpl;
import org.sonar.java.model.statement.CaseLabelTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.IfStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.java.model.statement.SwitchExpressionTreeImpl;
import org.sonar.java.model.statement.SwitchStatementTreeImpl;
import org.sonar.java.model.statement.WhileStatementTreeImpl;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VarTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

public final class PrintableNodesCreation {

  private PrintableNodesCreation(){}

  // Tokens may be set to null only if they are not used by the prettyprinter

  // <editor-fold desc="Expressions">

  public static LiteralTree literal(Object o){
    if (o instanceof Boolean){
      return new LiteralTreeImpl(Tree.Kind.BOOLEAN_LITERAL, token(o.toString()));
    }
    throw new UnsupportedOperationException("not yet implemented");
  }

  public static BinaryExpressionTreeImpl binop(ExpressionTree lhs, Tree.Kind operator, ExpressionTree rhs) {
    return new BinaryExpressionTreeImpl(operator, lhs, token(KindsPrinter.printExprKind(operator)), rhs);
  }

  public static UnaryExpressionTree not(ExpressionTree operand){
    return new InternalPrefixUnaryExpression(Tree.Kind.LOGICAL_COMPLEMENT, null, operand);
  }

  public static MethodInvocationTree methodInvocation(@Nullable ExpressionTree receiver, String methodName, ExpressionTree... args){
    var methodSelect = receiver == null ? new IdentifierTreeImpl(token(methodName))
      : new MemberSelectExpressionTreeImpl(receiver, null, new IdentifierTreeImpl(token(methodName)));
    var argsTreeList = ArgumentListTreeImpl.emptyList();
    argsTreeList.addAll(Arrays.asList(args));
    return new MethodInvocationTreeImpl(methodSelect, null, argsTreeList);
  }

  // </editor-fold>

  // <editor-fold desc="Statements">

  public static VariableTree varDecl(ModifiersTree modifiers, TypeTree type, IdentifierTree simpleName){
    return new VariableTreeImpl(modifiers, type, simpleName);
  }

  public static AssignmentExpressionTree assignment(ExpressionTree lhs, ExpressionTree rhs){
    return new AssignmentExpressionTreeImpl(Tree.Kind.ASSIGNMENT, lhs, null, rhs);
  }

  public static IfStatementTree ifStat(ExpressionTree cond, StatementTree thenBranch){
    return ifStat(cond, thenBranch, null);
  }

  public static IfStatementTree ifStat(ExpressionTree cond, StatementTree thenBranch, @Nullable StatementTree elseBranch){
    return new IfStatementTreeImpl(null, null, cond, null, thenBranch, null, elseBranch);
  }

  public static WhileStatementTree whileLoop(ExpressionTree condition, StatementTree body){
    return new WhileStatementTreeImpl(null, null, condition, null, body);
  }

  public static BlockTree block(StatementTree... stats) {
    return new BlockTreeImpl(null, Arrays.stream(stats).toList(), null);
  }

  public static BlockTree forceBlock(StatementTree stat){
    return (stat instanceof BlockTree blockTree) ? blockTree : block(stat);
  }

  public static ReturnStatementTree returnStat(){
    return new ReturnStatementTreeImpl(null, null, null);
  }

  public static ReturnStatementTree returnStat(ExpressionTree expr){
    return new ReturnStatementTreeImpl(null, expr, null);
  }

  public static ExpressionStatementTree exprStat(ExpressionTree expr){
    return new ExpressionStatementTreeImpl(expr, null);
  }

  // </editor-fold>

  // <editor-fold desc="Switches">

  public static SwitchStatementTree switchStat(ExpressionTree scrutinee, List<CaseGroupTreeImpl> cases) {
    return new SwitchStatementTreeImpl(null, null, scrutinee, null, null, cases, null);
  }

  public static SwitchExpressionTree switchExpr(ExpressionTree scrutinee, List<CaseGroupTreeImpl> cases){
    return new SwitchExpressionTreeImpl(null, null, scrutinee, null, null, cases, null);
  }

  public static CaseGroupTreeImpl switchCaseFromLabels(List<CaseLabelTreeImpl> caseLabelTrees, StatementTree body){
    return new CaseGroupTreeImpl(caseLabelTrees, makeStatementsList(body));
  }

  public static CaseGroupTreeImpl switchCase(PatternTree patternTree, List<ExpressionTree> guards, StatementTree body) {
    return switchCaseFromLabels(List.of(new CaseLabelTreeImpl(null, List.of(withGuards(patternTree, guards)), token("->"))), body);
  }

  public static CaseGroupTreeImpl switchCase(List<ExpressionTree> constants, StatementTree body) {
    return switchCaseFromLabels(List.of(new CaseLabelTreeImpl(null, constants, token("->"))), body);
  }

  public static CaseGroupTreeImpl switchDefault(StatementTree body) {
    var label = new CaseLabelTreeImpl(null, List.of(), token("->"));
    return new CaseGroupTreeImpl(List.of(label), makeStatementsList(body));
  }

  private static PatternTree withGuards(PatternTree unguardedPattern, List<ExpressionTree> guards) {
    return guards.isEmpty() ? unguardedPattern
      : new GuardedPatternTreeImpl(unguardedPattern, null, guards.stream().reduce((x, y) -> binop(x, Tree.Kind.CONDITIONAL_AND, y)).get()
    );
  }

  // <editor-fold desc="Types">

  public static TypeTree forceNotVar(TypeTree typeTree){
    if (typeTree instanceof VarTypeTree){
      return new IdentifierTreeImpl(token(typeTree.symbolType().name()));
    } else {
      return typeTree;
    }
  }

  // </editor-fold>

  // </editor-fold>

  // <editor-fold desc="Auxiliary methods">

  private static StatementListTreeImpl makeStatementsList(StatementTree body) {
    var ls = StatementListTreeImpl.emptyList();
    ls.add(body);
    return ls;
  }

  private static InternalSyntaxToken token(String s) {
    return new InternalSyntaxToken(0, 0, s, List.of(), false);
  }

  // </editor-fold>

}
