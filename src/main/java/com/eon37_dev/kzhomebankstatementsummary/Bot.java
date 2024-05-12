package com.eon37_dev.kzhomebankstatementsummary;

import com.eon37_dev.kzhomebankstatementsummary.services.BotService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Bot extends TelegramLongPollingBot {
  private static final Logger logger = LoggerFactory.getLogger(Bot.class);
  private final String botUsername;
  private final BotService botService;

  public Bot(@Value("${telegram.bot.token}") String botToken,
             @Value("${telegram.bot.username}") String botUsername,
             BotService botService) {
    super(botToken);
    this.botUsername = botUsername;
    this.botService = botService;
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage() && update.getMessage().hasText()) {
      String message = update.getMessage().getText();
      long chatId = update.getMessage().getChatId();

      if (message.startsWith("/start")) {
        botService.start(chatId, this);
      } else {
        botService.sendMessage(new SendMessage(String.valueOf(chatId), "Unknown command"), this);
      }
    }

    if (update.hasMessage()
            && update.getMessage().hasDocument()
            && "application/pdf".equals(update.getMessage().getDocument().getMimeType())) {
      long chatId = update.getMessage().getChatId();

      botService.calculateSummary(update.getMessage().getDocument(), chatId, this);
    }
  }

  @Override
  public String getBotUsername() {
    return botUsername;
  }
}