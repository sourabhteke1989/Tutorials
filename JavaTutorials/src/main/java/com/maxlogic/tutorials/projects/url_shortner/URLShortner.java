package com.maxlogic.tutorials.projects.url_shortner;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class URLShortner {

  private String domainURL;
  private static volatile long counter = 1;
  private static Map<Long, String> db = new ConcurrentHashMap<>();
  private static final String BASE_62_ALPHABET =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final int BASE_62 = BASE_62_ALPHABET.length();

  URLShortner(String domainURL) {
    this.domainURL = domainURL;
  }

  public static void main(String[] args) throws NotValidShortURL {
    URLShortner urlShortner = new URLShortner("http://urlshortner.in/");
    String originalLongURL = "https://spring.io/guides/gs/maven/";
    String shortURL = urlShortner.shortURL(originalLongURL);
    String longURL = urlShortner.longURL(shortURL);
    System.out.println(longURL + " -> " + shortURL);
  }

  private static long getNextCounter() {
    return counter++;
  }

  public String shortURL(String longURL) {
    long counter = getNextCounter();
    String base62Str = base62Encode(counter);
    db.put(counter, longURL);
    return formShortURL(base62Str);
  }

  public String longURL(String shortURL) throws NotValidShortURL {
    String base62Str = getTokenFromShortURL(shortURL);
    long counter = base62Decode(base62Str);
    return db.get(counter);
  }

  private String base62Encode(long counter) {
    StringBuilder sb = new StringBuilder();
    while (counter > 0) {
      sb.append(BASE_62_ALPHABET.charAt((int) counter % BASE_62));
      counter = counter / BASE_62;
    }
    int shortL = 7 - sb.length();
    for (int i = 0; i < shortL; i++) {
      sb.append("0");
    }
    return sb.reverse().toString();
  }

  private long base62Decode(String base62Str) {
    char[] base62Chars = base62Str.toCharArray();
    boolean startingZerosFinished = false;
    long counter = 0;
    for (int i = 0; i < base62Chars.length; i++) {
      if (base62Chars[i] == '0' && !startingZerosFinished) {
        continue;
      } else {
        startingZerosFinished = true;
        counter = (counter * BASE_62) + BASE_62_ALPHABET.indexOf(base62Chars[i]);
      }
    }
    return counter;
  }

  private String getTokenFromShortURL(String shortURL) throws NotValidShortURL {
    int idx = shortURL.indexOf(domainURL);
    if (idx != 0) {
      throw new NotValidShortURL();
    }
    return shortURL.substring(domainURL.length());
  }

  private String formShortURL(String token) {
    return domainURL + token;
  }

  static class NotValidShortURL extends Exception {
    NotValidShortURL() {
      super("Not valid short URL");
    }
  }

}
