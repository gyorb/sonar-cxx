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
package org.sonar.cxx.sensors.clangsa;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.cxx.CxxLanguage;
import org.sonar.cxx.sensors.utils.CxxReportSensor;

import org.xml.sax.SAXException;

import com.dd.plist.PropertyListParser;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSArray;
import com.dd.plist.NSObject;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.NSNumber;


/**
 * Sensor for Clang Static Analyzer.
 *
 */
public class CxxClangSASensor extends CxxReportSensor {
  public static final Logger LOG = Loggers.get(CxxClangSASensor.class);
  public static final String REPORT_PATH_KEY = "clangsa.reportPath";
  public static String KEY = "ClangSA";

  /**
   * {@inheritDoc}
   */
  public CxxClangSASensor(CxxLanguage language) {
    super(language);
  }

  @Override
  protected String reportPathKey() {
    return REPORT_PATH_KEY;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(this.language.getKey()).name(language.getName() + " ClangSASensor");
  }

  List< HashMap<String, String> > getFlowLocations(final SensorContext context, NSObject[] reportPath, NSObject[] sourceFiles){

    // TODO: reverse the list for better understanding!!!

    List<HashMap<String, String>> flowLocations =new ArrayList<HashMap<String, String>>();  

    for(NSObject p:reportPath){
      //LOG.error(p.toString());
      NSDictionary nd = (NSDictionary)p;
      //LOG.error(nd.toString());
      //LOG.error(nd.toASCIIPropertyList());

      String kind = ((NSString)nd.get("kind")).getContent();
      // We are only interested in the events.
      // control types are skipped for now.
      if (kind.equals("event")){
        LOG.error("EVENT EVENT");
        String message = ((NSString)nd.get("message")).getContent();
        NSDictionary loc = (NSDictionary)nd.get("location");

        Integer line = ((NSNumber)loc.get("line")).intValue();
        Integer col = ((NSNumber)loc.get("col")).intValue();
        NSNumber fileIndex = (NSNumber)loc.get("file");
        NSObject filePath = sourceFiles[fileIndex.intValue()];
        String normalPath = ((NSString)filePath).getContent();

        LOG.debug("ADDING FLOW");
        LOG.debug("-----------------");
        LOG.debug(message);
        LOG.debug(line.toString());
        LOG.debug(col.toString());
        LOG.debug(normalPath);
        //NSArray rngs = (NSArray)nd.get("ranges");
        //LOG.debug(rngs.toASCIIPropertyList());
        //LOG.debug(String.valueOf(rngs.count()));

        // There is at least on range for events.
        // NSObject range = rngs.objectAtIndex(0);
        // NSObject startPos = ((NSArray)range).objectAtIndex(0);
        // NSObject endPos = ((NSArray)range).objectAtIndex(1);

        //LOG.debug(String.valueOf(startPos.get("line")));
        //LOG.debug(String.valueOf(startPos.get("col")));
        //LOG.debug(startPos.get("file"));
        
        //LOG.debug(String.valueOf(endPos.get("line")));
        //LOG.debug(String.valueOf(endPos.get("col")));
        //LOG.debug(endPos.get("file"));

        LOG.debug("-----------------");

        HashMap<String, String> flowSection = new HashMap<String, String>();
        flowSection.put("message", message);
        flowSection.put("line", line.toString());
        flowSection.put("file", normalPath);

        flowLocations.add(flowSection);
      }

    }

    return flowLocations;
  }
  

  @Override
  protected void processReport(final SensorContext context, File report)
    throws javax.xml.stream.XMLStreamException {

    LOG.debug("Processing clangsa report '{}''", report.getName());

    try {
      File f = new File(report.getPath());

      NSDictionary rootDict = (NSDictionary)PropertyListParser.parse(f);

      // Array of file paths where an issue was detected.
      NSObject[] sourceFiles = ((NSArray)rootDict.objectForKey("files")).getArray();

      NSObject[] diagnostics = ((NSArray)rootDict.objectForKey("diagnostics")).getArray();

      for(NSObject diagnostic:diagnostics){
        NSDictionary diag = (NSDictionary)diagnostic;

        NSString desc = (NSString)diag.get("description");
        String description = desc.getContent();

        String checkerName = ((NSString)diag.get("check_name")).getContent();

        NSDictionary location = (NSDictionary)diag.get("location");

        Integer line = ((NSNumber)location.get("line")).intValue();

        NSNumber fileIndex = (NSNumber)location.get("file");

        NSObject filePath = sourceFiles[fileIndex.intValue()];

        NSObject[] reportPath = ((NSArray)diag.objectForKey("path")).getArray();

        List<HashMap<String, String>> flowLocations = getFlowLocations(context, reportPath, sourceFiles);
        
        LOG.debug("Collected flow info");
        for (HashMap<String, String> l: flowLocations){
          LOG.debug(l.get("line"));
          LOG.debug(l.get("file"));
          LOG.debug(l.get("message"));
        }

        if (flowLocations.size() > 1){
          LOG.debug("LONG LONG FLOW");
          saveUniqueViolation(context,
              CxxClangSARuleRepository.KEY,
              ((NSString)filePath).getContent(),
              line.toString(),
              checkerName,
              description,
              flowLocations);
        } else {
          saveUniqueViolation(context,
              CxxClangSARuleRepository.KEY,
              ((NSString)filePath).getContent(),
              line.toString(),
              checkerName,
              description,
              null);

        }

      }
    } catch (final java.io.IOException
                  | java.text.ParseException
                  | javax.xml.parsers.ParserConfigurationException
                  | org.xml.sax.SAXException
                  | com.dd.plist.PropertyListFormatException e){

      LOG.error("Failed to parse clangsa report: {}", e);

    }
  }

  @Override
  protected String getSensorKey() {
    return KEY;
  }
}
