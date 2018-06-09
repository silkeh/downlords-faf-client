package com.faforever.client.i18n;

import java.util.Locale;
import java.util.Optional;

public interface I18n {

  String get(String key, Object... args);

  String getQuantized(String singularKey, String pluralKey, long arg);

  Locale getUserSpecificLocale();

  Optional<String> getCountryNameLocalized(String isoCode);

  String number(int number);

  String numberWithSign(int number);

  String number(double number);

  String rounded(double number, int digits);
}
