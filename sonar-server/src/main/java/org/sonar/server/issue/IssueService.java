/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.issue;

import com.google.common.base.Strings;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.server.platform.UserSession;

import javax.annotation.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * @since 3.6
 */
public class IssueService implements ServerComponent {

  private final DefaultIssueFinder finder;
  private final IssueWorkflow workflow;
  private final IssueUpdater issueUpdater;
  private final IssueStorage issueStorage;
  private final ActionPlanService actionPlanService;
  private final RuleFinder ruleFinder;

  public IssueService(DefaultIssueFinder finder,
                      IssueWorkflow workflow,
                      IssueStorage issueStorage,
                      IssueUpdater issueUpdater,
                      ActionPlanService actionPlanService,
                      RuleFinder ruleFinder) {
    this.finder = finder;
    this.workflow = workflow;
    this.issueStorage = issueStorage;
    this.issueUpdater = issueUpdater;
    this.actionPlanService = actionPlanService;
    this.ruleFinder = ruleFinder;
  }

  /**
   * List of available transitions, ordered by key.
   * <p/>
   * Never return null, but return an empty list if the issue does not exist.
   */
  public List<Transition> listTransitions(String issueKey) {
    DefaultIssue issue = loadIssue(issueKey);
    if (issue == null) {
      return Collections.emptyList();
    }
    List<Transition> transitions = workflow.outTransitions(issue);
    Collections.sort(transitions, new Comparator<Transition>() {
      @Override
      public int compare(Transition transition, Transition transition2) {
        return transition.key().compareTo(transition2.key());
      }
    });
    return transitions;
  }

  /**
   * Never return null, but an empty list if the issue does not exist.
   */
  public List<Transition> listTransitions(@Nullable Issue issue) {
    if (issue == null) {
      return Collections.emptyList();
    }
    List<Transition> transitions = workflow.outTransitions(issue);
    Collections.sort(transitions, new Comparator<Transition>() {
      @Override
      public int compare(Transition transition, Transition transition2) {
        return transition.key().compareTo(transition2.key());
      }
    });
    return transitions;
  }

  public Issue doTransition(String issueKey, String transition, UserSession userSession) {
    verifyLoggedIn(userSession);
    DefaultIssue issue = loadIssue(issueKey);
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.login());
    if (workflow.doTransition(issue, transition, context)) {
      issueStorage.save(issue);
    }
    return issue;
  }

  public Issue assign(String issueKey, @Nullable String assigneeLogin, UserSession userSession) {
    verifyLoggedIn(userSession);
    DefaultIssue issue = loadIssue(issueKey);

    // TODO check that assignee exists
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.login());
    if (issueUpdater.assign(issue, assigneeLogin, context)) {
      issueStorage.save(issue);
    }
    return issue;
  }

  public Issue plan(String issueKey, @Nullable String actionPlanKey, UserSession userSession) {
    if (!Strings.isNullOrEmpty(actionPlanKey) && actionPlanService.findByKey(actionPlanKey) == null) {
      throw new IllegalStateException("Unknown action plan: " + actionPlanKey);
    }

    verifyLoggedIn(userSession);
    DefaultIssue issue = loadIssue(issueKey);
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.login());
    if (issueUpdater.plan(issue, actionPlanKey, context)) {
      issueStorage.save(issue);
    }
    return issue;
  }

  public Issue setSeverity(String issueKey, String severity, UserSession userSession) {
    verifyLoggedIn(userSession);
    DefaultIssue issue = loadIssue(issueKey);

    IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.login());
    if (issueUpdater.setManualSeverity(issue, severity, context)) {
      issueStorage.save(issue);
    }
    return issue;
  }

  public DefaultIssue createManualIssue(DefaultIssue issue, UserSession userSession) {
    verifyLoggedIn(userSession);
    if (!"manual".equals(issue.ruleKey().repository())) {
      throw new IllegalArgumentException("Issues can be created only on rules marked as 'manual': " + issue.ruleKey());
    }
    Rule rule = ruleFinder.findByKey(issue.ruleKey());
    if (rule == null) {
      throw new IllegalArgumentException("Unknown rule: " + issue.ruleKey());
    }


    // TODO check existence of component
    // TODO verify authorization

    issueStorage.save(issue);
    return issue;
  }


  public DefaultIssue loadIssue(String issueKey) {
    return finder.findByKey(issueKey, UserRole.USER);
  }

  public List<String> listStatus(){
    return workflow.statusKeys();
  }

  private void verifyLoggedIn(UserSession userSession) {
    if (!userSession.isLoggedIn()) {
      // must be logged
      throw new IllegalStateException("User is not logged in");
    }
  }
}
