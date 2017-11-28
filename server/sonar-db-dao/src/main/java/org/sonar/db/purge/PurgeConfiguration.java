/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.db.purge;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.core.config.PurgeConstants;

import static java.util.Arrays.asList;

public class PurgeConfiguration {

  private final IdUuidPair rootProjectIdUuid;
  private final Collection<String> qualifiersWithoutHistoricalData;
  private final int maxAgeInDaysOfClosedIssues;
  private final Optional<Integer> maxAgeInDaysOfInactiveShortLivingBranches;
  private final System2 system2;
  private final Collection<String> disabledComponentUuids;

  public PurgeConfiguration(IdUuidPair rootProjectId, Collection<String> qualifiersWithoutHistoricalData, int maxAgeInDaysOfClosedIssues,
    Optional<Integer> maxAgeInDaysOfInactiveShortLivingBranches, System2 system2, Collection<String> disabledComponentUuids) {
    this.rootProjectIdUuid = rootProjectId;
    this.qualifiersWithoutHistoricalData = qualifiersWithoutHistoricalData;
    this.maxAgeInDaysOfClosedIssues = maxAgeInDaysOfClosedIssues;
    this.system2 = system2;
    this.disabledComponentUuids = disabledComponentUuids;
    this.maxAgeInDaysOfInactiveShortLivingBranches = maxAgeInDaysOfInactiveShortLivingBranches;
  }

  public static PurgeConfiguration newDefaultPurgeConfiguration(Configuration config, IdUuidPair idUuidPair, Collection<String> disabledComponentUuids) {
    Collection<String> qualifiersWithoutHistoricalData = asList(Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE);
    if (config.getBoolean(PurgeConstants.PROPERTY_CLEAN_DIRECTORY).orElse(false)) {
      qualifiersWithoutHistoricalData = asList(Qualifiers.DIRECTORY, Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE);
    }
    return new PurgeConfiguration(idUuidPair, qualifiersWithoutHistoricalData, config.getInt(PurgeConstants.DAYS_BEFORE_DELETING_CLOSED_ISSUES).get(),
      config.getInt(PurgeConstants.DAYS_BEFORE_DELETING_INACTIVE_SHORT_LIVING_BRANCHES), System2.INSTANCE, disabledComponentUuids);
  }

  public IdUuidPair rootProjectIdUuid() {
    return rootProjectIdUuid;
  }

  public Collection<String> getQualifiersWithoutHistoricalData() {
    return qualifiersWithoutHistoricalData;
  }

  public Collection<String> getDisabledComponentUuids() {
    return disabledComponentUuids;
  }

  @CheckForNull
  public Date maxLiveDateOfClosedIssues() {
    return maxLiveDateOfClosedIssues(new Date(system2.now()));
  }

  public Optional<Date> maxLiveDateOfInactiveShortLivingBranches() {
    return maxAgeInDaysOfInactiveShortLivingBranches.map(age -> DateUtils.addDays(new Date(system2.now()), -age));
  }

  @VisibleForTesting
  @CheckForNull
  Date maxLiveDateOfClosedIssues(Date now) {
    if (maxAgeInDaysOfClosedIssues > 0) {
      return DateUtils.addDays(now, -maxAgeInDaysOfClosedIssues);
    }

    // delete all closed issues
    return null;
  }
}
