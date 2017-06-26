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
package org.sonar.cxx.sensors.compiler;

import org.sonar.cxx.sensors.compiler.CxxCompilerSensor;
import org.sonar.cxx.sensors.compiler.CompilerParser;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.Assert;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.cxx.CxxLanguage;

public class MockCxxCompilerSensor extends CxxCompilerSensor {

  private final List<CompilerParser.Warning> warnings;
  public List<CompilerParser.Warning> savedWarnings;

  @Override
  protected CompilerParser getCompilerParser() {

    CompilerParser compileParser = mock(CompilerParser.class);
  
    try {
      doAnswer(new Answer<List<CompilerParser.Warning>>() {

          @Override
          public List<CompilerParser.Warning> answer(InvocationOnMock invocation)
                  throws Throwable {
              Object[] args = invocation.getArguments();
              if (args[4] instanceof List<?>) {
                List<CompilerParser.Warning> list = (List<CompilerParser.Warning>) args[4];
                list.addAll(warnings);
              }
              return null;
          }
        }).when(compileParser).processReport(any(SensorContext.class), any(File.class), any(String.class),  any(String.class), any(List.class));
      } catch (FileNotFoundException e) {
        Assert.fail(e.getMessage());
      }
    
    return compileParser;
  }

  public MockCxxCompilerSensor(CxxLanguage language, FileSystem fs, RulesProfile profile, List<CompilerParser.Warning> warningsToProcess) {
    super(language);

    warnings = warningsToProcess;
    savedWarnings = new LinkedList<>();
  }

  @Override
  public void saveUniqueViolation(SensorContext context, String ruleRepoKey, String file,
      String line, String ruleId, String msg, @Nullable Iterable<HashMap<String, String>> flowLocations) {
    
    CompilerParser.Warning w = new CompilerParser.Warning(file, line, ruleId, msg);
    savedWarnings.add(w);
  }
  
  
}