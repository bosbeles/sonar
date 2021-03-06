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
package org.sonar.api.workflow.condition;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.workflow.internal.DefaultReview;
import org.sonar.api.workflow.internal.DefaultWorkflowContext;

import static org.fest.assertions.Assertions.assertThat;

public class HasReviewPropertyConditionTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void doVerify() {
    HasReviewPropertyCondition condition = new HasReviewPropertyCondition("foo");

    DefaultWorkflowContext context = new DefaultWorkflowContext();
    assertThat(condition.doVerify(new DefaultReview(), context)).isFalse();
    assertThat(condition.doVerify(new DefaultReview().setProperty("foo", ""), context)).isFalse();
    assertThat(condition.doVerify(new DefaultReview().setProperty("foo", "bar"), context)).isTrue();
  }

  @Test
  public void getPropertyKey() {
    HasReviewPropertyCondition condition = new HasReviewPropertyCondition("foo");
    assertThat(condition.getPropertyKey()).isEqualTo("foo");
  }

  @Test
  public void failIfNullProperty() {
    thrown.expect(IllegalArgumentException.class);
    new HasReviewPropertyCondition(null);
  }

  @Test
  public void failIfEmptyProperty() {
    thrown.expect(IllegalArgumentException.class);
    new HasReviewPropertyCondition("");
  }
}
