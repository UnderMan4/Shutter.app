package pl.lodz.p.it.ssbd2022.ssbd02.mok.service;

import pl.lodz.p.it.ssbd2022.ssbd02.entity.TokenType;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.VerificationToken;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.BaseApplicationException;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.facade.TokenFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.util.ConfigLoader;
import pl.lodz.p.it.ssbd2022.ssbd02.util.EmailService;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerService;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Serwis wysyłający powiadomienia o konieczności potwierdzenia rejestracji w połowie okresu pozwalającego na
 * dokonanie tejże operacji
 */
@Startup
@Singleton
public class AutoReminderService {

    @Inject
    private EmailService emailService;

    @Inject
    private TokenFacade tokenFacade;

    @Inject
    private ConfigLoader configLoader;

    @Resource
    TimerService timerService;

    @PostConstruct
    public void init() {
        long interval = (long) configLoader.getRegistrationConfirmationTokenLifetime() * 60 * 60 * 1000;
        timerService.createTimer(
                0,
                interval,
                "Co każdą ustaloną przez zmienną interval liczbę godzin"
        );
    }

    @Timeout
    public void remindRegistrationConfirmation() {
        List<VerificationToken> toRemindTokens = null;
        try {
            toRemindTokens = tokenFacade.findExpiredAfterOfType(
                    TokenType.REGISTRATION_CONFIRMATION,
                    LocalDateTime.now().plusHours(configLoader.getRegistrationConfirmationTokenLifetime())
            );
        } catch (BaseApplicationException ex) {
            return;
        }

        for (VerificationToken token : toRemindTokens) {
            emailService.sendRegistrationConfirmationReminder(
                    token.getTargetUser().getEmail(),
                    token.getTargetUser().getLocale(),
                    token
            );
        }
    }
}
