package com.extantape.blueminimap.share;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HouseRulesGuardTest {
    @Test
    void sharingIsOffWithHouseRulesOlderThan02() {
        assertNotNull(ShareService.olderHouseRules("0.1.1"));
        assertNotNull(ShareService.olderHouseRules("0.1.0"));
        assertNotNull(ShareService.olderHouseRules("something odd"), "an unreadable version fails closed");
    }

    @Test
    void sharingIsOnWithHouseRules02OrNewerOrWithout() {
        assertNull(ShareService.olderHouseRules("0.2.0"));
        assertNull(ShareService.olderHouseRules("0.2.1-SNAPSHOT"));
        assertNull(ShareService.olderHouseRules("1.0.0"));
        assertNull(ShareService.olderHouseRules(null));
    }
}
