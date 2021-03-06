/*
 * Sonar C++ Plugin (Community)
 * Copyright (C) 2010-2017 SonarOpenCommunity
 * http://github.com/SonarOpenCommunity/sonar-cxx
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.cxx.sensors.utils;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.cxx.CxxLanguage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;


/**
 * Utility class holding various, well, utilities
 */
public final class CxxUtils {

  public static final String ERROR_RECOVERY_KEY = "errorRecoveryEnabled";
  
  /**
   * Default logger.
   */
  public static final Logger LOG = Loggers.get(CxxUtils.class);

  private CxxUtils() {
    // only static methods
  }

  /**
   * Normalize the given path to pass it to sonar. Return null if normalization
   * has failed.
   * @param filename
   * @return normalized path
   */
  public static String normalizePath(String filename) {
    try {
      return new File(filename).getCanonicalPath();
    } catch (java.io.IOException e) {
      LOG.error("path normalizing of '{}' failed: '{}'", filename, e);
      return null;
    }
  }

  /**
   * @param filename
   * @param baseDir
   * @return returns case sensitive full path
   */
  public static String normalizePathFull(String filename, String baseDir) {
    File targetfile = new java.io.File(filename.trim());
    String filePath;
    if (targetfile.isAbsolute()) {
      filePath = normalizePath(filename);
    } else {
      // RATS, CppCheck and Vera++ provide names like './file.cpp' - add input folder for index check
      filePath = normalizePath(baseDir + File.separator + filename);
    }
    return filePath;
  }

  /**
   * transformFile
   * 
   * @param stylesheetFile  
   * @param input
   * @param output
   */
  public static void transformFile(Source stylesheetFile, File input, File output) throws TransformerException {
    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer transformer = factory.newTransformer(stylesheetFile);
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.transform(new StreamSource(input), new StreamResult(output));
  }


  /**
   * <p>Gets the stack trace from a Throwable as a String.</p>
   * 
   * @param throwable  the <code>Throwable</code> to be examined
   * @return the stack trace as generated by the exception's printStackTrace(PrintWriter) method
   */
  public static String getStackTrace(final Throwable throwable) {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw, true);
    throwable.printStackTrace(pw);
    return sw.getBuffer().toString();
  }
  
  /**
   * validateRecovery
   * 
   * @param ex  
   * @param language
   */
  public static void validateRecovery(Exception ex, CxxLanguage language) {
    if (!language.IsRecoveryEnabled()) {
      LOG.info("Recovery is disabled, failing analysis : '{}'", ex.toString());
      throw new IllegalStateException(ex.getMessage(), ex.getCause());
    }
  }
}
