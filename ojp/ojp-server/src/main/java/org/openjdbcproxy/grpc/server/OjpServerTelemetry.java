package org.openjdbcproxy.grpc.server;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * OJP Server Telemetry Configuration for OpenTelemetry with Prometheus Exporter.
 * This class provides methods to create a GrpcTelemetry instance with Prometheus metrics.
 */
public class OjpServerTelemetry {
	private static final Logger logger = LoggerFactory.getLogger(OjpServerTelemetry.class);
    private PrometheusHttpServer prometheusServer;

    /**
     * Creates GrpcTelemetry with specified Prometheus port.
     */
    public GrpcTelemetry createGrpcTelemetry(int prometheusPort) {
        return createGrpcTelemetry(prometheusPort, List.of(IpWhitelistValidator.ALLOW_ALL_IPS));
    }

    /**
	 * Creates GrpcTelemetry with specified Prometheus port and IP whitelist.
	 */
	public GrpcTelemetry createGrpcTelemetry(int prometheusPort, List<String> allowedIps) {
		logger.info("Initializing OpenTelemetry with Prometheus on port {} with IP whitelist: {}", 
					prometheusPort, allowedIps);

		// Validate IP whitelist
		if (!IpWhitelistValidator.validateWhitelistRules(allowedIps)) {
			logger.warn("Invalid IP whitelist rules detected, falling back to allow all");
			allowedIps = List.of(IpWhitelistValidator.ALLOW_ALL_IPS);
		}

		this.prometheusServer = PrometheusHttpServer.builder()
				.setPort(prometheusPort)
				.build();


		OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
				.setMeterProvider(
						SdkMeterProvider.builder()
								.registerMetricReader(prometheusServer)
								.build())
				.build();

		return GrpcTelemetry.create(openTelemetry);
	}

    public void close() {
        prometheusServer.close();
    }

}
