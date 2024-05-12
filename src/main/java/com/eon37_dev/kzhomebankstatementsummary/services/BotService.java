package com.eon37_dev.kzhomebankstatementsummary.services;

import com.eon37_dev.kzhomebankstatementsummary.Bot;
import com.eon37_dev.kzhomebankstatementsummary.model.Summary;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.HashMap;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BotService {
  private static final Pattern TX_PATTERN = Pattern.compile("(\\w+) (-|\\+)(\\d(\\d|\\s)*\\.\\d{2})");
  private static final Logger logger = LoggerFactory.getLogger(BotService.class);

  public void start(long chatId, Bot bot) {
    SendMessage sendMessage = new SendMessage(String.valueOf(chatId),
            """
                      Welcome! This bot calculates summary from the home credit bank's statement.
                      This bot only parses the incomes and outcomes to sum up. It does not collect or store your personal information.
                      Load pdf statement to calculate.
                    """);

    sendMessage(sendMessage, bot);
  }

  public void calculateSummary(Document document, long chatId, Bot bot) {
    java.io.File file = downloadFileFromServer(document, chatId, bot);

    try (PDDocument pdf = Loader.loadPDF(file)) {
      PDFTextStripper stripper = new PDFTextStripper();
      String text = stripper.getText(pdf);
      Matcher m = TX_PATTERN.matcher(text);

      HashMap<String, Summary> currencySummaryMap = new HashMap<>(3);

      while (m.find()) {
        Summary currencySummary = currencySummaryMap.getOrDefault(m.group(1), new Summary());

        switch (m.group(2)) {
          case "+" -> currencySummary.setIncomes(currencySummary.getIncomes() + extractDouble(m.group(3)));
          case "-" -> currencySummary.setOutcomes(currencySummary.getOutcomes() + extractDouble(m.group(3)));
        }

        currencySummaryMap.put(m.group(1), currencySummary);
      }

      StringJoiner sj = new StringJoiner("\n\n");
      currencySummaryMap.forEach((key, value) -> sj.add(key + "\nIncomes: " + value.getIncomes() + "\nOutcomes: " + value.getOutcomes()));

      this.sendMessage(new SendMessage(String.valueOf(chatId), sj.toString()), bot);
    } catch (IOException e) {
      this.sendMessage(new SendMessage(String.valueOf(chatId), "Error reading file"), bot);
    } finally {
      if (!file.delete()) {
        logger.error("File [{}] was not deleted", file.getAbsolutePath());
      }
    }
  }

  private java.io.File downloadFileFromServer(Document document, long chatId, Bot bot) {
    try {
      GetFile getFile = GetFile.builder()
              .fileId(document.getFileId())
              .build();

      File uploadedFile = bot.execute(getFile);

      return bot.downloadFile(new File(
              uploadedFile.getFileId(),
              uploadedFile.getFileUniqueId(),
              uploadedFile.getFileSize(),
              uploadedFile.getFilePath()));
    } catch (TelegramApiException e) {
      this.sendMessage(new SendMessage(String.valueOf(chatId), "Error obtaining file"), bot);
      logger.error("Error obtaining file", e);
      throw new RuntimeException(e);
    }
  }

  private static double extractDouble(String formattedDouble) {
    return Double.parseDouble(formattedDouble.replaceAll("\\s", ""));
  }

  public void sendMessage(SendMessage sendMessage, Bot bot) {
    try {
      bot.execute(sendMessage);
    } catch (TelegramApiException e) {
      logger.error("Error while sending message", e);
      throw new RuntimeException(e);
    }
  }
}
