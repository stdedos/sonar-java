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
package org.sonar.java.checks;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.cfg.CFG;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph.Block;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S128")
public class SwitchCaseWithoutBreakCheck extends IssuableSubscriptionVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.SWITCH_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    SwitchStatementTree switchStatementTree = (SwitchStatementTree) tree;
    Set<CaseGroupTree> caseGroupTrees = new HashSet<>(switchStatementTree.cases());
    CFG cfg = CFG.buildCFG(Collections.singletonList(tree), true);
    Set<Block> switchSuccessors = cfg.entryBlock().successors();

    Map<Block, CaseGroupTree> cfgBlockToCaseGroupMap = createMapping(switchSuccessors, caseGroupTrees);
    switchSuccessors.stream()
      .filter(cfgBlockToCaseGroupMap.keySet()::contains)
      .flatMap(cfgBlock -> getForbiddenCaseGroupPredecessors(cfgBlock, cfgBlockToCaseGroupMap))
      .map(CaseGroupTree::labels)
      .map(caseGroupLabels -> caseGroupLabels.get(caseGroupLabels.size() - 1))
      .forEach(label -> reportIssue(label, "End this switch case with an unconditional break, return or throw statement."));
  }

  private static Map<Block, CaseGroupTree> createMapping(Set<Block> switchSuccessors, Set<CaseGroupTree> caseGroupTrees) {
    return switchSuccessors.stream()
      .filter(cfgBlock -> cfgBlock.caseGroup() != null && caseGroupTrees.contains(cfgBlock.caseGroup()))
      .collect(
        Collectors.toMap(
          Function.identity(),
          Block::caseGroup));
  }

  private static Stream<CaseGroupTree> getForbiddenCaseGroupPredecessors(Block cfgBlock, Map<Block, CaseGroupTree> cfgBlockToCaseGroupMap) {
    CaseGroupTree caseGroup = cfgBlockToCaseGroupMap.get(cfgBlock);
    return cfgBlock.predecessors().stream()
      .map(predecessor -> getForbiddenCaseGroupPredecessor(predecessor, cfgBlockToCaseGroupMap, new HashSet<>()))
      .filter(Objects::nonNull)
      .filter(predecessor -> !intentionalFallThrough(predecessor, caseGroup))
      .distinct();
  }

  @Nullable
  private static CaseGroupTree getForbiddenCaseGroupPredecessor(Block predecessor, Map<Block, CaseGroupTree> cfgBlockToCaseGroupMap, Set<Block> seen) {
    if (cfgBlockToCaseGroupMap.get(predecessor) != null) {
      return cfgBlockToCaseGroupMap.get(predecessor);
    }

    if (seen.contains(predecessor)) {
      return null;
    }

    seen.add(predecessor);
    return predecessor.predecessors().stream()
      .map(previousPredecessors -> getForbiddenCaseGroupPredecessor(previousPredecessors, cfgBlockToCaseGroupMap, seen))
      .filter(Objects::nonNull)
      .findFirst()
      .orElse(null);
  }

  private static boolean intentionalFallThrough(Tree caseGroup, Tree nextCaseGroup) {
    // Check first token of next case group when comment is last element of case group it is attached to next group.
    FallThroughCommentVisitor visitor = new FallThroughCommentVisitor();
    CaseGroupTree caseGroupTree = (CaseGroupTree) caseGroup;
    List<Tree> treesToScan = Stream.of(caseGroupTree.body(), Collections.singletonList(nextCaseGroup.firstToken()))
      .flatMap(List::stream)
      .collect(Collectors.toList());
    visitor.scan(treesToScan);
    return visitor.hasComment;
  }

  private static class FallThroughCommentVisitor extends SubscriptionVisitor {
    private static final Pattern FALL_THROUGH_PATTERN = Pattern.compile("falls?[\\-\\s]?thro?u[gh]?", Pattern.CASE_INSENSITIVE);
    boolean hasComment = false;

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.TRIVIA);
    }

    @Override
    public void visitTrivia(SyntaxTrivia syntaxTrivia) {
      if (!hasComment && FALL_THROUGH_PATTERN.matcher(syntaxTrivia.comment()).find()) {
        hasComment = true;
      }
    }

    private void scan(List<Tree> trees) {
      for (Tree tree : trees) {
        if (hasComment) {
          return;
        }
        scanTree(tree);
      }
    }
  }
}
