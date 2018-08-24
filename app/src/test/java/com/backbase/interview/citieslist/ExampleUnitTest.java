package com.backbase.interview.citieslist;

import com.google.gson.stream.JsonReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
  @Test public void addition_isCorrect() {
    assertEquals(4, 2 + 2);
  }

  @Test public void test_cities_json_load() throws IOException {
    final File file = new File(ASSET_BASE_PATH + "cities.json");
    InputStream inputStream = new FileInputStream(file);
    final JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));

    //assertTrue(file.exists());
    assertTrue(inputStream.available() > 0);
  }

  public static final String  ASSET_BASE_PATH = "../app/src/main/assets/";



  public String readJsonFile (String filename) throws IOException {
    BufferedReader
        br = new BufferedReader(new InputStreamReader(new FileInputStream(ASSET_BASE_PATH + filename)));
    StringBuilder sb = new StringBuilder();
    String line = br.readLine();
    while (line != null) {
      sb.append(line);
      line = br.readLine();
    }

    return sb.toString();
  }
}