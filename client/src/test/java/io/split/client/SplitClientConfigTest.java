package io.split.client;

import io.split.client.impressions.ImpressionListener;
import org.junit.Test;

/**
 * Created by adilaijaz on 3/23/17.
 */
public class SplitClientConfigTest {

    @Test(expected = IllegalArgumentException.class)
    public void cannot_set_feature_refresh_rate_to_less_than_30() {
        SplitClientConfig.builder()
                .featuresRefreshRate(29)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannot_set_segment_refresh_rate_to_less_than_30() {
        SplitClientConfig.builder()
                .segmentsRefreshRate(29)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannot_set_impression_refresh_rate_to_less_than_30() {
        SplitClientConfig.builder()
                .impressionsRefreshRate(29)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannot_set_metrics_refresh_rate_to_less_than_30() {
        SplitClientConfig.builder()
                .metricsRefreshRate(29)
                .build();
    }

    @Test
    public void can_set_refresh_rates_to__30() {
        SplitClientConfig.builder()
                .featuresRefreshRate(30)
                .segmentsRefreshRate(30)
                .impressionsRefreshRate(30)
                .metricsRefreshRate(30)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannot_set_zero_capacity_on_impression_listener() throws InterruptedException {
        SplitClientConfig.builder()
                .impressionListener(new ImpressionListener.NoopImpressionListener(), 0)
                .build();

    }
}
