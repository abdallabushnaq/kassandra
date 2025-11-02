/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.util;

import de.bushnaq.abdalla.kassandra.Application;
import de.bushnaq.abdalla.kassandra.report.dao.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class Util {
    public static final  long   ONE_WORKING_DAY_MILLIS = 7500L * 60L * 60L;
    private static final String buildTime;
    public static final  String copyright;
    public static final  Locale locale                 = Locale.US;
    private static final Logger logger                 = LoggerFactory.getLogger(Util.class);
    public static final  String module;
    private static final String moduleName;
    private static final String moduleVersion;

    static {
        moduleName    = MavenProperiesProvider.getProperty(Application.class, "module.name");
        moduleVersion = MavenProperiesProvider.getProperty(Application.class, "module.version");
        buildTime     = MavenProperiesProvider.getProperty(Application.class, "build.time");
        module        = String.format("%s %s built at %s", moduleName, moduleVersion, buildTime);
        copyright     = String.format("%s %s, generated %%s", moduleName, moduleVersion);

    }

    public static Double add(Double value1, Double value2) {
        if (value1 == null && value2 == null) {
            return null;
        } else if (value1 == null) {
            return value2;
        } else if (value2 == null) {
            return value1;
        } else {
            return value1 + value2;
        }
    }

    public static String columnIdtoExcelColumnName(int number) {
        final StringBuilder sb = new StringBuilder();

        int num = number - 1;
        while (num >= 0) {
            int numChar = (num % 26) + 65;
            sb.append((char) numChar);
            num = (num / 26) - 1;
        }
        return sb.reverse().toString();
    }

    public static Set<String> createColumnSet(PreparedStatement preparedStatement) throws SQLException {
        Set<String>       columns = new HashSet<>();
        ResultSetMetaData md      = preparedStatement.getMetaData();
        int               numCol  = md.getColumnCount();
        for (int i = 1; i <= numCol; i++) {
            columns.add(md.getColumnName(i));
        }
        return columns;
    }

    public static String createTestCaseString(Long testCases) {
        return testCases + " tc";
    }

    public static String extractParameters(final String aStringToParse, final Map<String, Integer> aParameterList) {
        int _lastParameterIndex = 1;
        aParameterList.clear();
        final StringBuffer _stringToParse      = new StringBuffer(aStringToParse);
        int                _foundColumnAtIndex = -1;
        do {
            _foundColumnAtIndex = _stringToParse.indexOf(":");
            if (_foundColumnAtIndex > -1) {
                final int _foundSpaceAtIndex = findParameterEnd(_stringToParse, _foundColumnAtIndex + 1);
                if (_foundSpaceAtIndex > -1) {
                    final String _parameterName = _stringToParse.substring(_foundColumnAtIndex + 1, _foundSpaceAtIndex);
                    aParameterList.put(_parameterName, Integer.valueOf(_lastParameterIndex++));
                    _stringToParse.replace(_foundColumnAtIndex, _foundSpaceAtIndex, "?");
                } else {
                    // ---Finished
                    break;
                }
            } else {
                // ---Finished
                break;
            }
        } while (_foundColumnAtIndex != -1);
        return _stringToParse.toString();
    }

    public static void fillpatternRect(Graphics2D graphics2d, int x, int y, int width, int height, BufferedImage img) {
        TexturePaint tp = new TexturePaint(img, new Rectangle(0, 0, 4, 4));
        graphics2d.setPaint(tp);
        graphics2d.fillRect(x, y, width, height);
    }

    public static int findParameterEnd(final StringBuffer aStringToParse, final int aStartIndex) {
        final String[] _token = {",", " ", "\r", ")"};
        int            _index = aStringToParse.length();
        for (int i = 0; i < _token.length; i++) {
            final int _tempIndex = aStringToParse.indexOf(_token[i], aStartIndex);
            if ((_tempIndex != -1) && (_tempIndex < _index)) {
                _index = _tempIndex;
            }
        }
        return _index;
    }

    public static String generateCopyrightString(LocalDateTime now) {
        DateTimeFormatter sdfForCopyright    = DateTimeFormatter.ofPattern("yyyy.MMM.dd HH:mm");
        String            populatedCopyRight = String.format(copyright, now.format(sdfForCopyright)) + " CET";
        return populatedCopyRight;
    }

    public static String[] getCsvList(ResultSet rs, String columnName) throws SQLException {
        String result = rs.getString(columnName);
        if (rs.wasNull()) {
            return null;
        }
        return result.split("\\|", -1);
    }

    public static Range getIntersection(LocalDateTime os, LocalDateTime of, LocalDateTime ps, LocalDateTime pf) {
        //o   |      |
        //i   ------
        //p |      |
        if (!os.isBefore(ps) && !os.isAfter(pf)) {
            return new Range(os, pf);
        }
        //o|  |
        //i ---
        //p |      |
        if (!of.isBefore(ps) && !of.isAfter(pf)) {
            return new Range(ps, of);
        }
        //o|        |
        //i  -------
        //p |      |
        if (!os.isAfter(ps) && !of.isBefore(pf)) {
            return new Range(ps, pf);
        }
        //o  |    |
        //i  ------
        //p |      |
        if (os.isAfter(ps) && of.isBefore(pf)) {
            return new Range(os, of);
        }
        return null;
    }

    public static String longToString(final Long aValue, final boolean aCreateLeadingZero) {
        if (aValue != null) {
            if (!aCreateLeadingZero || (aValue > 9)) {
                return Long.toString(aValue);
            } else {
                return "0" + aValue;
            }
        } else {
            return "";
        }
    }

    public static String putCsvList(String[] list) {
        StringBuilder result = new StringBuilder();
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                if (i != 0) {
                    result.append("\\|");
                }
                result.append(list[i]);
            }
        } else {
            result.append("null");
        }
        return result.toString();
    }

    public static List<String> toList(String[] array) {
        if (array == null) {
            return null;
        } else {
            return Arrays.asList(array);
        }
    }

}
