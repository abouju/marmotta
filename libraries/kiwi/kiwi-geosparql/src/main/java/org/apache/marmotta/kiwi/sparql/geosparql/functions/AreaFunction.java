/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.marmotta.kiwi.sparql.geosparql.functions;

import org.apache.marmotta.kiwi.persistence.KiWiDialect;
import org.apache.marmotta.kiwi.persistence.pgsql.PostgreSQLDialect;
import org.apache.marmotta.kiwi.sparql.builder.ValueType;
import org.apache.marmotta.kiwi.sparql.function.NativeFunction;
import org.apache.marmotta.kiwi.vocabulary.FN_GEOSPARQL;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.algebra.evaluation.ValueExprEvaluationException;
import org.openrdf.query.algebra.evaluation.function.FunctionRegistry;

/**
 * Returns the area in units square of a
 * geometrie. Should be implemented directly in the database, as the in-memory
 * implementation is non-functional. Only support by postgres - POSTGIS
 * <p/>
 * The function can be called either as:
 * <ul>
 *      <li>geof:area(?geometryA) </li>
 * </ul>
 * Its necesary enable postgis in your database with the next command "CREATE
 * EXTENSION postgis;" Note that for performance reasons it might be preferrable
 * to create a geometry index for your database. Please consult your database
 * documentation on how to do this.
 *
 * @author Alain Bouju (alain.bouju @ univ-lr.fr))
 */
public class AreaFunction implements NativeFunction {
// auto-register for SPARQL environment

    static {
        if (!FunctionRegistry.getInstance().has(FN_GEOSPARQL.AREA.toString())) {
            FunctionRegistry.getInstance().add(new AreaFunction());
        }
    }

    @Override
    public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
        throw new UnsupportedOperationException("cannot evaluate in-memory, needs to be supported by the database");
    }

    @Override
    public String getURI() {
        return FN_GEOSPARQL.AREA.toString();
    }

    /**
     * Return true if this function has available native support for the given
     * dialect
     *
     * @param dialect
     * @return
     */
    @Override
    public boolean isSupported(KiWiDialect dialect) {
        return dialect instanceof PostgreSQLDialect;
    }

    /**
     * Return a string representing how this GeoSPARQL function is translated
     * into SQL ( Postgis Function ) in the given dialect
     *
     * @param dialect
     * @param args
     * @return
     */
    @Override
    public String getNative(KiWiDialect dialect, String... args) {
        if (dialect instanceof PostgreSQLDialect) {
        	System.out.println("args.length ="+args.length);
            if (args.length == 1) {
                String geom1 = args[0];
                String SRID_default = "4326";
                /*
                 * The following condition is required to read WKT  inserted directly into args[0] or args[1] and create a geometries with SRID
                 * POINT, MULTIPOINT, LINESTRING ... and MULTIPOLYGON conditions: 
                 *   example: geof:area(?geom1)
                 * st_AsText condition: It is to use the geometry that is the result of another function geosparql.
                 *   example: geof:area(?geom1)
                 */
                return String.format("ST_Area(%s)", args[0]);
                /*if (args[0].contains("POINT") || args[0].contains("MULTIPOINT") || args[0].contains("LINESTRING") || args[0].contains("MULTILINESTRING") || args[0].contains("POLYGON") || args[0].contains("MULTIPOLYGON") || args[0].contains("ST_AsText")) {
                    geom1 = String.format("ST_GeomFromText(%s,%s)", args[0], SRID_default);
                }
                if (args[1].equalsIgnoreCase("'" + FN_GEOSPARQL.meter.toString() + "'") || args[2].equalsIgnoreCase("'" + FN_GEOSPARQL.metre.toString() + "'")) {
                    return String.format("ST_Area( ST_Transform( %s ,26986), ST_Transform( %s ,26986))", geom1);
                }
                if (args[1].equalsIgnoreCase("'" + FN_GEOSPARQL.degree.toString() + "'")) {
                    return String.format("ST_Area(%s)", geom1);
                }
                if (args[1].equalsIgnoreCase("'" + FN_GEOSPARQL.radian.toString() + "'")) {
                    return String.format("RADIANS(RADIANS(ST_Area(%s)))", geom1);
                }*/
            } else {
                if (args.length == 2) {
                	return String.format("ST_Area( ST_Transform( %s ,%s)",args[0], args[1]);
                }
            }
        }
        throw new UnsupportedOperationException("Area function not supported by dialect " + dialect);
    }

    /**
     * Get the return type of the function. This is needed for SQL type casting
     * inside KiWi.
     *
     * @return
     */
    @Override
    public ValueType getReturnType() {
        return ValueType.DOUBLE;
    }

    /**
     * Get the argument type of the function for the arg'th argument (starting
     * to count at 0). This is needed for SQL type casting inside KiWi.
     *
     * @param arg
     * @return
     */
    @Override
    public ValueType getArgumentType(int arg) {
        return ValueType.GEOMETRY;
    }

    /**
     * Return the minimum number of arguments this function requires.
     *
     * @return
     */
    @Override
    public int getMinArgs() {
        return 1;
    }

    /**
     * Return the maximum number of arguments this function can take
     *
     * @return
     */
    @Override
    public int getMaxArgs() {
        return 1;
    }
}
