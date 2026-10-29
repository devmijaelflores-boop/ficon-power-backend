package com.devzonesoft.ficonpower.config;

import com.devzonesoft.ficonpower.adapters.notification.MultiNotificationService;
import com.devzonesoft.ficonpower.adapters.notification.NotificationChannel;
import com.devzonesoft.ficonpower.adapters.notification.FcmAccessTokenProvider;
import com.devzonesoft.ficonpower.adapters.notification.FcmPushNotificationAdapter;
import com.devzonesoft.ficonpower.adapters.notification.WhatsAppNotificationAdapter;
import com.devzonesoft.ficonpower.ports.output.MobileDeviceTokenRepository;
import com.devzonesoft.ficonpower.ports.output.NotificationDeliveryRepository;
import com.devzonesoft.ficonpower.ports.output.WhatsAppRecipientRepository;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class NotificationConfig {

    @Bean
    MultiNotificationService notificationService(
            WhatsAppProperties whatsapp,
            FcmProperties fcm,
            MobileDeviceTokenRepository mobileTokens,
            NotificationDeliveryRepository deliveries,
            WhatsAppRecipientRepository whatsappRecipients,
            RestClient.Builder restClientBuilder,
            ZoneId applicationTimeZone
    ) {
        List<NotificationChannel> channels = new ArrayList<>();

        if (whatsapp.isConfigured()) {
            channels.add(new WhatsAppNotificationAdapter(
                    restClientBuilder.clone(),
                    whatsapp.getApiUrl(),
                    whatsapp.getPhone(),
                    whatsapp.getToken(),
                    whatsappRecipients,
                    whatsapp.getTemplateName(),
                    whatsapp.getTemplateLanguage(),
                    whatsapp.getOutageTemplateName(),
                    whatsapp.getOutageTemplateParams(),
                    whatsapp.getRestoredTemplateName(),
                    whatsapp.getRestoredTemplateParams(),
                    applicationTimeZone,
                    deliveries));
        }

        if (fcm.isConfigured()) {
            channels.add(new FcmPushNotificationAdapter(
                    restClientBuilder.clone(),
                    fcm,
                    new FcmAccessTokenProvider(fcm),
                    mobileTokens,
                    deliveries,
                    applicationTimeZone));
        }

        return new MultiNotificationService(channels);
    }
}
