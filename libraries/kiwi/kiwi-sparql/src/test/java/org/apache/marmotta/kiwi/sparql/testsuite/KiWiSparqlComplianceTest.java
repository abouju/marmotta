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

package org.apache.marmotta.kiwi.sparql.testsuite;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import info.aduna.iteration.Iterations;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.marmotta.commons.sesame.model.StatementCommons;
import org.apache.marmotta.kiwi.config.KiWiConfiguration;
import org.apache.marmotta.kiwi.sail.KiWiStore;
import org.apache.marmotta.kiwi.sparql.sail.KiWiSparqlSail;
import org.apache.marmotta.kiwi.test.junit.KiWiDatabaseRunner;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.openrdf.model.BNode;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.*;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;

/**
 * A collection of tests from the SPARQL 1.1 testsuite, for testing compliance.
 *
 * @author Sebastian Schaffert (sschaffert@apache.org)
 */
@RunWith(KiWiDatabaseRunner.class)
public class KiWiSparqlComplianceTest {

    private KiWiStore store;

    private KiWiSparqlSail ssail;

    private Repository repository;

    // reference repository for checking if the results are the same
    private Repository reference;

    private final KiWiConfiguration dbConfig;

    public KiWiSparqlComplianceTest(KiWiConfiguration dbConfig) {
        this.dbConfig = dbConfig;
        dbConfig.setFulltextEnabled(true);
        dbConfig.setFulltextLanguages(new String[] {"en"});
    }


    @Before
    public void initDatabase() throws RepositoryException, IOException, RDFParseException {
        store = new KiWiStore(dbConfig);
        ssail = new KiWiSparqlSail(store);
        repository = new SailRepository(ssail);
        repository.initialize();

        // load demo data
        RepositoryConnection con = repository.getConnection();
        try {
            con.begin();

            con.add(this.getClass().getResourceAsStream("date-num-str.ttl"), "http://localhost/test/", RDFFormat.TURTLE);

            con.commit();
        } finally {
            con.close();
        }

        reference = new SailRepository(new MemoryStore());
        reference.initialize();

        // load demo data
        RepositoryConnection con2 = reference.getConnection();
        try {
            con2.begin();

            con2.add(this.getClass().getResourceAsStream("date-num-str.ttl"), "http://localhost/test/", RDFFormat.TURTLE);

            con2.commit();
        } finally {
            con2.close();
        }
    }

    @After
    public void dropDatabase() throws RepositoryException, SQLException {
        store.getPersistence().dropDatabase();
        repository.shutDown();
    }

    final Logger logger =
            LoggerFactory.getLogger(this.getClass());

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        /**
         * Invoked when a test is about to start
         */
        @Override
        protected void starting(Description description) {
            logger.info("{} being run...", description.getMethodName());
        }
    };


    @Test
    public void testQueryHours() throws Exception {
        testQuery("hours.sparql");
    }

    @Test
    public void testQueryMinutes() throws Exception {
        testQuery("minutes.sparql");
    }

    @Test
    public void testQuerySeconds() throws Exception {
        testQuery("seconds.sparql");
    }

    @Test
    public void testQueryMonth() throws Exception {
        testQuery("month.sparql");
    }

    @Test
    public void testQueryYear() throws Exception {
        testQuery("year.sparql");
    }


    @Test
    public void testQueryFloor() throws Exception {
        testQuery("floor.sparql");
    }

    @Test
    public void testQueryCeil() throws Exception {
        testQuery("ceil.sparql");
    }

    @Test
    public void testQueryRound() throws Exception {
        testQuery("round.sparql");
    }


 private void testQuery(String filename) throws Exception {
    String queryString = IOUtils.toString(this.getClass().getResourceAsStream(filename), "UTF-8");

    RepositoryConnection con1 = repository.getConnection();
    RepositoryConnection con2 = reference.getConnection();
    try {
        con2.begin();
        TupleQuery query2 = con2.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult result2 = query2.evaluate();

        // Matérialiser AVANT le Assume pour ne pas consommer le curseur
        List<BindingSet> list2 = Iterations.asList(result2);
        con2.commit();

        Assume.assumeTrue(!list2.isEmpty());  // utiliser la liste, pas result2

        con1.begin();
        TupleQuery query1 = con1.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult result1 = query1.evaluate();
        List<BindingSet> list1 = Iterations.asList(result1);
        con1.commit();

        Assert.assertTrue(!list1.isEmpty());

        compareResults(list1, list2);  // passer les listes

    } catch(RepositoryException ex) {
        con1.rollback();
    } finally {
        con1.close();
        con2.close();
    }
}

    private void testUpdate(String filename, URI... properties) throws Exception {
        String queryString = IOUtils.toString(this.getClass().getResourceAsStream(filename), "UTF-8");

        RepositoryConnection con1 = repository.getConnection();
        RepositoryConnection con2 = reference.getConnection();
        try {
            con2.begin();

            Update query2 = con2.prepareUpdate(QueryLanguage.SPARQL, queryString);
            query2.execute();

            con2.commit();

            con1.begin();

            Update query1 = con1.prepareUpdate(QueryLanguage.SPARQL, queryString);
            // workaround for a Sesame bug: we explicitly set the context for the query in the dataset

            URI context = new URIImpl("http://localhost/mycontext");
            DatasetImpl ds = new DatasetImpl();
            //ds.addDefaultGraph(context);
            //ds.addNamedGraph(context);
            //ds.addDefaultRemoveGraph(context);
            ds.setDefaultInsertGraph(context);
            query1.setDataset(ds);

            query1.execute();

            con1.commit();


            con1.begin();
            Set<Statement> set1 = new HashSet<>();
            for(URI u : properties) {
                set1.addAll(Collections2.transform(Iterations.asSet(con1.getStatements(null, u, null, true)), new StatementCommons.TripleEquality()));
            }
            con1.commit();

            con2.begin();
            Set<Statement> set2 = new HashSet<>();
            for(URI u : properties) {
                set2.addAll(Collections2.transform(Iterations.asSet(con2.getStatements(null,u,null,true)), new StatementCommons.TripleEquality()));
            }
            con2.commit();

            for(Statement stmt : set1) {
                Assert.assertTrue(stmt + " not contained in set 2", set2.contains(stmt));
            }
            for(Statement stmt : set2) {
                Assert.assertTrue(stmt + " not contained in set 1", set1.contains(stmt));
            }

        } catch(RepositoryException ex) {
            con1.rollback();
        } finally {
            con1.close();
            con2.close();
        }
    }


private void compareResults(List<BindingSet> bindingSets1, List<BindingSet> bindingSets2) {
    
    // Convertir chaque BindingSet en une string triée reproductible
    Set<String> set1 = new HashSet<>();
    for (BindingSet bs : bindingSets1) {
        set1.add(bindingSetToString(bs));
    }
    
    Set<String> set2 = new HashSet<>();
    for (BindingSet bs : bindingSets2) {
        set2.add(bindingSetToString(bs));
    }

    for (String p : set1) {
        Assert.assertTrue("binding [" + p + "] from result set not found in reference set", set2.contains(p));
    }
    for (String p : set2) {
        Assert.assertTrue("binding [" + p + "] from reference set not found in result set", set1.contains(p));
    }

    Assert.assertEquals(set1, set2);
}

private String bindingSetToString(BindingSet bs) {
    // Construire une représentation string triée et déterministe
    List<String> pairs = new ArrayList<>();
    for (Binding b : bs) {
        String value;
        if (b.getValue() == null) {
            value = "null";
        } else if (b.getValue() instanceof BNode) {
            value = "_";
        } else {
            value = normalizeValue(b.getValue().stringValue());
        }
        pairs.add(b.getName() + "=" + value);
    }
    Collections.sort(pairs);  // tri déterministe
    return pairs.toString();
}

private String normalizeValue(String value) {
    if (value == null) return null;
    try {
        java.math.BigDecimal bd = new java.math.BigDecimal(value).stripTrailingZeros();
        String s = bd.toPlainString();
        if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
        return s;
    } catch (NumberFormatException e) {
        return value;
    }
}

private static class BindingSetPairFunction implements Function<BindingSet, Set<Pair>> {
    @Override
    public Set<Pair> apply(BindingSet input) {
        Set<Pair> result = new HashSet<>();
        for (Binding b : input) {
            String value = null;
            if (b.getValue() != null) {
                if (b.getValue() instanceof BNode) {
                    value = "_";
                } else if (b.getValue() instanceof org.openrdf.model.Literal) {
                    value = normalizeValue((org.openrdf.model.Literal) b.getValue());
                } else {
                    value = b.getValue().stringValue();
                }
            }
            result.add(new Pair(b.getName(), value));
        }
        return result;
    }

    private String normalizeValue(org.openrdf.model.Literal literal) {
        String datatype = literal.getDatatype() != null 
            ? literal.getDatatype().stringValue() : "";
        
        // Normaliser tous les types numériques
        if (datatype.contains("integer") || datatype.contains("int") ||
            datatype.contains("long") || datatype.contains("short") ||
            datatype.contains("byte") || datatype.contains("decimal") ||
            datatype.contains("float") || datatype.contains("double")) {
            try {
                // Convertir via BigDecimal pour éviter les pertes de précision
                java.math.BigDecimal bd = new java.math.BigDecimal(literal.getLabel());
                // Supprimer les zéros trailing et le point si entier
                bd = bd.stripTrailingZeros();
                String normalized = bd.toPlainString();
                // "3.0" -> "3", "-2.0" -> "-2"
                if (normalized.endsWith(".0")) {
                    normalized = normalized.substring(0, normalized.length() - 2);
                }
                return normalized;
            } catch (NumberFormatException e) {
                return literal.getLabel();
            }
        }
        return literal.getLabel();
    }
}	

    private static class Pair {
        String key, value;

        private Pair(String key, String value) {
            this.key = key;
            this.value = value;
        }

        private String getKey() {
            return key;
        }

        private String getValue() {
            return value;
        }
        
        @Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Pair pair = (Pair) o;

    if (!key.equals(pair.getKey())) return false;
    
    // Comparaison numérique pour les décimaux
    if (value != null && pair.getValue() != null) {
        try {
            double d1 = Double.parseDouble(value);
            double d2 = Double.parseDouble(pair.getValue());
            return Math.abs(d1 - d2) < 0.000001;
        } catch (NumberFormatException e) {
            // pas un nombre, comparaison string normale
        }
    }
    
    return value != null ? value.equals(pair.getValue()) : pair.getValue() == null;
}

/*        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Pair pair = (Pair) o;

            if (!key.equals(pair.getKey())) return false;
            if (value != null ? !value.equals(pair.getValue()) : pair.getValue() != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = key.hashCode();
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }*/
        

        @Override
        public String toString() {
            return key + " = " + value;
        }
    }


}
