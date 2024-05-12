package com.eon37_dev.kzhomebankstatementsummary.configs;

import com.eon37_dev.kzhomebankstatementsummary.Bot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
@RequiredArgsConstructor
public class BotInitializer {
  private static final Logger logger = LoggerFactory.getLogger(BotInitializer.class);
  private final Bot bot;

  @EventListener({ContextRefreshedEvent.class})
  public void init() {
    try {
      TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
      telegramBotsApi.registerBot(bot);
    } catch (TelegramApiException e) {
      logger.error("Error registering bot", e);
      throw new RuntimeException(e);
    }
  }
}
