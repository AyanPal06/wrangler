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

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.wrangler.api.Executor;
import io.cdap.wrangler.api.annotations.Usage;
import io.cdap.wrangler.steps.AggregateStatsDirective; // ✅ Added import
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * Registry of directive usages managed through this class.
 */
public final class UsageRegistry implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(UsageRegistry.class);

  /**
   * A {@link UsageEntry} defines the information about the directives that are available.
   */
  public class UsageEntry {
    private final String directive;
    private final String usage;
    private final String description;

    public UsageEntry(String directive, String usage, String description) {
      this.directive = directive;
      this.usage = usage;
      this.description = description;
    }

    public String getDirective() {
      return directive;
    }

    public String getUsage() {
      return usage;
    }

    public String getDescription() {
      return description;
    }
  }

  private final Map<String, UsageEntry> usages = new HashMap<>();
  private final List<UsageEntry> usageList = new ArrayList<>();

  public UsageRegistry() {
    addDefaultDirectives();
  }

  public void addUsage(Class<? extends Executor> classz) {
    Name name = classz.getAnnotation(Name.class);
    Description description = classz.getAnnotation(Description.class);
    Usage usage = classz.getAnnotation(Usage.class);
    if (usage == null || name == null || description == null) {
      return;
    }
    usages.put(name.value(), new UsageEntry(name.value(), usage.value(), description.value()));
    usageList.add(new UsageEntry(name.value(), usage.value(), description.value()));
  }

  public String getUsage(String directive) {
    if (usages.containsKey(directive)) {
      return usages.get(directive).getUsage();
    }
    return null;
  }

  public String getDescription(String directive) {
    if (usages.containsKey(directive)) {
      return usages.get(directive).getDescription();
    }
    return null;
  }

  public List<UsageEntry> getAll() {
    return usageList;
  }

  private void addUsage(String directive, String usage, String description) {
    UsageEntry d = new UsageEntry(directive, usage, description);
    usages.put(directive, d);
    usageList.add(d);
  }

  /**
   * Loads all the default system directives that are available with the system.
   */
  private void addDefaultDirectives() {
    // Iterate through registry of steps to collect the directive and usage.
    Reflections reflections = new Reflections("io.cdap.wrangler");
    Set<Class<? extends Executor>> steps = reflections.getSubTypesOf(Executor.class);
    for (Class<? extends Executor> step : steps) {
      addUsage(step);
    }

    // Explicitly register special or custom directives
    addUsage("set format", "set format csv <delimiter> <skip empty lines>",
             "[DEPRECATED] Parses the predefined column as CSV. Use 'parse-as-csv' instead.");
    addUsage("format-unix-timestamp", "format-unix-timestamp <column> <format>",
             "Formats a UNIX timestamp using the specified format");
    addUsage("filter-row-if-not-matched", "filter-row-if-not-matched <column> <regex>",
             "Filters rows if the regex does not match");
    addUsage("filter-row-if-false", "filter-row-if-false <condition>",
             "Filters rows if the condition evaluates to false");

    // ✅ Explicitly register the new directive
    addUsage(AggregateStatsDirective.class);
  }
}
