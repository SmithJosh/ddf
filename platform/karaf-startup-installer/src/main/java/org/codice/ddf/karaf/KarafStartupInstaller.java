/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.karaf;

import java.util.EnumSet;
import java.util.concurrent.Executors;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.FeaturesService.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KarafStartupInstaller {
  private static final Logger LOGGER = LoggerFactory.getLogger(KarafStartupInstaller.class);

  private static final EnumSet<Option> INSTALL_OPTIONS = EnumSet.of(Option.NoAutoRefreshBundles);
  private static final String PROFILE = "profile-standard";
  private static final String ADMIN_POST_INSTALL_MODULES = "admin-post-install-modules";
  private static final String ADMIN_MODULES_INSTALLER = "admin-modules-installer";

  private FeaturesService featuresService;
  private Bundle bundle;

  public KarafStartupInstaller(FeaturesService featuresService, Bundle bundle) {
    this.featuresService = featuresService;
    this.bundle = bundle;
  }

  public void init() {
    Executors.newSingleThreadExecutor().submit(this::provision);
  }

  public void provision() {
    if (featuresService == null) {
      LOGGER.error("Karaf features service is unavailable. Skipping installation.");
      return;
    }

    if (installFeature(PROFILE)
        && installFeature(ADMIN_POST_INSTALL_MODULES)
        && uninstallFeature(ADMIN_MODULES_INSTALLER)) {
      try {
        bundle.uninstall();
      } catch (BundleException e) {
        LOGGER.error(
            "Could not uninstall karaf-startup-installer. Auto-provision will run again on system restart.");
      }
    } else {
      LOGGER.error("Auto-provision failed. Restart the system to try again.");
    }
  }

  private boolean installFeature(String feature) {
    try {
      if (!featuresService.isInstalled(featuresService.getFeature(feature))) {
        featuresService.installFeature(feature, INSTALL_OPTIONS);
      }
      return true;
    } catch (Exception e) {
      LOGGER.error("Failed to install feature" + feature, e);
      return false;
    }
  }

  private boolean uninstallFeature(String feature) {
    try {
      if (featuresService.isInstalled(featuresService.getFeature(feature))) {
        featuresService.uninstallFeature(feature, INSTALL_OPTIONS);
      }
      return true;
    } catch (Exception e) {
      LOGGER.error("Failed to uninstall feature" + feature, e);
      return false;
    }
  }
}
