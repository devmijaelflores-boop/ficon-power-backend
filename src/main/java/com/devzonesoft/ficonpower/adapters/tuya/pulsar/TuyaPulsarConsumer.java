package com.devzonesoft.ficonpower.adapters.tuya.pulsar;

import com.devzonesoft.ficonpower.config.TuyaPulsarProperties;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class TuyaPulsarConsumer implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(TuyaPulsarConsumer.class);
    private static final long[] RETRY_DELAYS_SECONDS = {5, 10, 20, 30, 60};
    private final TuyaPulsarProperties properties;
    private final TuyaPulsarConfigurationValidator validator;
    private final TuyaPulsarConnectionFactory connections;
    private final ScheduledExecutorService reconnectExecutor;

    private volatile boolean lifecycleActive;
    private volatile ConnectionStatus status = ConnectionStatus.DISABLED;
    private volatile Instant lastConnectedAt;
    private volatile Instant lastFailureAt;
    private volatile String lastFailure;
    private volatile int retryAttempt;
    private volatile TuyaPulsarConnection connection;
    private ScheduledFuture<?> reconnectTask;

    @Autowired
    public TuyaPulsarConsumer(
            TuyaPulsarProperties properties,
            TuyaPulsarConfigurationValidator validator,
            TuyaPulsarConnectionFactory connections
    ) {
        this(properties, validator, connections, Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "tuya-pulsar-reconnect");
            thread.setDaemon(true);
            return thread;
        }));
    }

    TuyaPulsarConsumer(
            TuyaPulsarProperties properties,
            TuyaPulsarConfigurationValidator validator,
            TuyaPulsarConnectionFactory connections,
            ScheduledExecutorService reconnectExecutor
    ) {
        this.properties = properties;
        this.validator = validator;
        this.connections = connections;
        this.reconnectExecutor = reconnectExecutor;
    }

    @Override
    public synchronized void start() {
        if (lifecycleActive || !properties.isEnabled()) {
            return;
        }
        validator.validate(properties);
        lifecycleActive = true;
        status = ConnectionStatus.CONNECTING;
        reconnectTask = reconnectExecutor.schedule(this::connect, 0, TimeUnit.SECONDS);
    }

    @Override
    public synchronized void stop() {
        lifecycleActive = false;
        if (reconnectTask != null) {
            reconnectTask.cancel(true);
            reconnectTask = null;
        }
        if (connection != null) {
            connection.close();
            connection = null;
        }
        status = properties.isEnabled()
                ? ConnectionStatus.STOPPED
                : ConnectionStatus.DISABLED;
        reconnectExecutor.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return lifecycleActive;
    }

    @Override
    public boolean isAutoStartup() {
        return properties.isEnabled();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    public ConnectionSnapshot snapshot() {
        TuyaPulsarConnection currentConnection = connection;
        ConnectionStatus currentStatus = status;
        if (currentStatus == ConnectionStatus.CONNECTED
                && (currentConnection == null || !currentConnection.isConnected())) {
            currentStatus = ConnectionStatus.DEGRADED;
        }
        return new ConnectionSnapshot(
                properties.isEnabled(),
                currentStatus,
                retryAttempt,
                lastConnectedAt,
                lastFailureAt);
    }

    private void connect() {
        if (!lifecycleActive) {
            return;
        }
        status = ConnectionStatus.CONNECTING;
        try {
            TuyaPulsarConnection newConnection = connections.connect(properties);
            synchronized (this) {
                if (!lifecycleActive) {
                    newConnection.close();
                    return;
                }
                connection = newConnection;
                retryAttempt = 0;
                lastConnectedAt = Instant.now();
                lastFailure = null;
                status = ConnectionStatus.CONNECTED;
                reconnectTask = null;
            }
            logger.info("Tuya Pulsar consumer connected to topic {}", properties.topic());
        } catch (PulsarClientException | RuntimeException exception) {
            handleConnectionFailure(exception);
        }
    }

    private synchronized void handleConnectionFailure(Exception exception) {
        connection = null;
        lastFailureAt = Instant.now();
        lastFailure = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        if (!lifecycleActive) {
            status = ConnectionStatus.STOPPED;
            return;
        }
        long delay = RETRY_DELAYS_SECONDS[
                Math.min(retryAttempt, RETRY_DELAYS_SECONDS.length - 1)];
        retryAttempt++;
        status = ConnectionStatus.DEGRADED;
        logger.warn(
                "Tuya Pulsar unavailable; backend remains active and will retry in {} seconds: {}",
                delay,
                lastFailure);
        reconnectTask = reconnectExecutor.schedule(this::connect, delay, TimeUnit.SECONDS);
    }

    public enum ConnectionStatus {
        DISABLED,
        CONNECTING,
        CONNECTED,
        DEGRADED,
        STOPPED
    }

    public record ConnectionSnapshot(
            boolean enabled,
            ConnectionStatus status,
            int retryAttempt,
            Instant lastConnectedAt,
            Instant lastFailureAt
    ) {
    }

}
