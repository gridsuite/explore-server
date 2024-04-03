package org.gridsuite.explore.server.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Service
public class ScheduledCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledCleaner.class);

    private ExploreService exploreService;

    public ScheduledCleaner(ExploreService exploreService) {
        this.exploreService = exploreService;
    }

    @Scheduled(cron = "${cleaning-stash-cron}", zone = "UTC")
    public void deleteStashedExpired() {
        ZonedDateTime startZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC);
        LOGGER.info("Cleaning cases cron starting execution at {}", startZonedDateTime);
        exploreService.deleteStashedElements(30); // delete all stashed elements older than 30 days
        ZonedDateTime endZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC);
        LOGGER.info("Cleaning cases cron finished execution at {}", endZonedDateTime);
    }
}
