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

import org.sonar.api.ServerComponent;
import org.sonar.api.issue.Issue;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.server.platform.UserSession;

import java.util.List;

/**
 * All the issue features that are not published to public API
 */
public class JRubyInternalIssues implements ServerComponent {

  private final ServerIssueActions actions;

  public JRubyInternalIssues(ServerIssueActions actions) {
    this.actions = actions;
  }

  public List<Transition> listTransitions(String issueKey) {
    return actions.listTransitions(issueKey, UserSession.get().userId());
  }

  public Issue doTransition(String issueKey, String transitionKey) {
    return actions.doTransition(issueKey, transitionKey, UserSession.get());
  }
}