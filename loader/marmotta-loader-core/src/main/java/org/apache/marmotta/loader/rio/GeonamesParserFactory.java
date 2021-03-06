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

package org.apache.marmotta.loader.rio;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserFactory;

/**
 * Add file description here!
 *
 * @author Sebastian Schaffert (sschaffert@apache.org)
 */
public class GeonamesParserFactory implements RDFParserFactory {

    /**
     * Returns the RDF format for this factory.
     */
    @Override
    public RDFFormat getRDFFormat() {
        return GeonamesFormat.FORMAT;
    }

    /**
     * Returns a RDFParser instance.
     */
    @Override
    public RDFParser getParser() {
        return new GeonamesParser();
    }
}
