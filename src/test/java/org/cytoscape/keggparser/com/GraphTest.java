package org.cytoscape.keggparser.com;

import com.google.common.base.Strings;
import junit.framework.JUnit4TestAdapter;
import org.cytoscape.keggparser.parsing.Parser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.*;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

@RunWith(Parameterized.class)
public class GraphTest {
    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(GraphTest.class);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> graphSet() {
        ArrayList<Graph[]> testGraphs = new ArrayList<Graph[]>();

        Graph graph;
        Parser parser = new Parser();
        try {
            graph = parser.parse(new File("src/test/testdata/hsa04062.xml"));
            if (graph != null) testGraphs.add(new Graph[]{graph});
            graph = parser.parse(new File("src/test/testdata/hsa00010.xml"));
            if (graph != null) testGraphs.add(new Graph[]{graph});
            graph = parser.parse(new File("src/test/testdata/hsa00020.xml"));
            if (graph != null) testGraphs.add(new Graph[]{graph});
            graph = (parser.parse(new File("src/test/testdata/hsa04662.xml")));
            if (graph != null) testGraphs.add(new Graph[]{graph});
            graph = (parser.parse(new File("src/test/testdata/1234_Citrate cycle (TCA cycle).xml")));
            if (graph != null) testGraphs.add(new Graph[]{graph});

        } catch (Exception e) {
            e.printStackTrace();
        }

        Object[][] objects = new Object[testGraphs.size()][1];

        for (int index = 0; index < testGraphs.size(); index++) {
            objects[index] = testGraphs.get(index);
        }

        return Arrays.asList(objects);
    }

    public GraphTest(Graph graph) {
        this.graph = graph;
    }


    private Graph graph;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @After
    public void tearDown() {
        thrown = ExpectedException.none();
    }


    @Test
    public void testConstructor() {
        Assert.assertNotNull(graph.getNodes());
        Assert.assertNotNull(graph.getRelations());
    }

    @Test
    public void testAddNode() {
        graph.addNode(new KeggNode(1, "", KeggNode.GENE));
        assertTrue(graph.getNodes().containsKey(1));
        thrown.expect(IllegalArgumentException.class);
        graph.addNode(null);
    }

    @Test
    public void testGetNode() {
        for (int id : new int[]{1, 100, 200}) {
            KeggNode keggNode = (new KeggNode(id, "", KeggNode.GENE));
            if (graph.getNodes().containsKey(id)) {
                graph.addNode(keggNode);
                assertEquals(id, graph.getNode(id).getId());
                assertNotSame(keggNode, graph.getNode(id));
            } else {
                graph.addNode(keggNode);
                assertEquals(keggNode, graph.getNode(id));
            }
        }
        assertNull(graph.getNode(0));
    }

    @Test
    public void testAddRelation() {
        KeggRelation keggRelation = new KeggRelation(new KeggNode(1, "", KeggNode.GENE),
                new KeggNode(2, "", KeggNode.GENE), KeggRelation.PPrel);
        graph.addRelation(keggRelation);
        assertTrue(graph.getRelations().contains(keggRelation));
        thrown.expect(IllegalArgumentException.class);
        graph.addRelation(null);
    }

    @Test
    public void testEdgeExists() {
        Graph testGraph = new Graph();
        KeggRelation keggRelation = new KeggRelation(new KeggNode(1, "", KeggNode.GENE),
                new KeggNode(2, "", KeggNode.GENE), KeggRelation.PPrel);
        testGraph.addRelation(keggRelation);
        assertFalse(testGraph.edgeExists(null, keggRelation.getEntry1()));
        assertTrue(testGraph.edgeExists(keggRelation.getEntry1(), keggRelation.getEntry2()));
        assertFalse(testGraph.edgeExists(keggRelation.getEntry2(), keggRelation.getEntry1()));
    }

    @Test
    public void testGetRelation() {
        KeggRelation keggRelation = new KeggRelation(new KeggNode(1, "", KeggNode.GENE),
                new KeggNode(2, "", KeggNode.GENE), KeggRelation.PPrel);
        graph.addRelation(keggRelation);
        assertEquals(keggRelation, graph.getRelation(keggRelation.getEntry1(),
                keggRelation.getEntry2()));
        assertNotSame(keggRelation, graph.getRelation(keggRelation.getEntry2(),
                keggRelation.getEntry1()));
        assertNull(graph.getRelation(null, null));
    }

    @Test
    public void testGetNodeRelations() {
        for (KeggNode keggNode : graph.getNodes().values()) {
            for (KeggRelation keggRelation : graph.getNodeRelations(keggNode)) {
                assertTrue(keggRelation.getEntry1().equals(keggNode) ||
                        keggRelation.getEntry2().equals(keggNode));
            }
        }
    }

    @Test
    public void testProcessGroups() {
        try {
            graph.processGroups();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        for (Map.Entry<Integer, KeggNode> nodeEntry : graph.getNodes().entrySet()) {
            KeggNode node = nodeEntry.getValue();

            if (node.getGroupId() > 0) {
                //For each groupNode;
                int groupId = node.getGroupId();
                int numOfExternalNodes;
                int numOfTerminalNodes = 0;
                TreeSet<KeggNode> groupNodes = new TreeSet<KeggNode>();
                TreeSet<KeggNode> externalNodes = new TreeSet<KeggNode>();
                //Combine groupNodes
                groupNodes.add(node);
                for (KeggNode gNode : graph.getNodes().values()) {
                    if (gNode.getGroupId() == groupId)
                        if (!gNode.equals(node))
                            groupNodes.add(gNode);
                }
                //Check number of external and internal relations for nodes in the group
                for (KeggNode gNode : groupNodes) {

                    int numOfExternalRelations = 0;
                    int numOfInGroupRelations = 0;
                    for (KeggRelation relation : graph.getNodeRelations(gNode)) {
                        KeggNode otherNode;
                        if (!relation.getEntry1().equals(gNode))
                            otherNode = relation.getEntry1();
                        else otherNode = relation.getEntry2();
                        if (otherNode.getGroupId() != groupId) {//Other node is not from the group
                            externalNodes.add(gNode);
                            assert externalNodes.size() <= 2;
                            numOfExternalRelations++;
                        } else numOfInGroupRelations++;
                    }
                    if (numOfExternalRelations > 0)
                        Assert.assertTrue(numOfInGroupRelations == 1);
                    else {
                        if (numOfInGroupRelations == 1) {//This should be terminal node
                            assert numOfTerminalNodes < 2;
                            numOfTerminalNodes++;
                        } else assert numOfInGroupRelations == 2;
                    }
                }


            }
        }
    }


    @Test
    public void testProcessCompounds() {
        graph.processCompounds();


        //Check that the relations with comment "Compound processed" are between GENE and COMPOUND types
        for (KeggRelation relation : graph.getRelations()) {
            if (!Strings.isNullOrEmpty(relation.getComment()))
                if (relation.getComment().contains(KeggRelation.COMPOUND_PROCESSED)) {
                    String type1 = relation.getEntry1().getType();
                    String type2 = relation.getEntry2().getType();

                    Assert.assertTrue("fail message: " + graph.getName() + ": " + relation.toString(),
                            (type1.equals(KeggNode.COMPOUND) &&
                                    (type2.equals(KeggNode.MAP) || type2.equals(KeggNode.GENE))) ||
                                    (type2.equals(KeggNode.COMPOUND) &&
                                            (type1.equals(KeggNode.MAP) || type1.equals(KeggNode.GENE))));

                }

        }
    }

    @Test
    public void testCorrectEdgeDirections(){
        graph.correctEdgeDirections();
        for (KeggRelation relation : graph.getRelations()){
            if (!Strings.isNullOrEmpty(relation.getComment()))
                if (relation.getComment().contains(KeggRelation.DIRECTION_REVERSED)){
                    KeggNode node1 = relation.getEntry1();
                    KeggNode node2 = relation.getEntry2();
                    assert node1.getId() > node2.getId();

                    if (node1.getX() - node2.getX() != 0)
                        assert node1.getX() < node2.getX();
                    else
                        assert node1.getY() < node2.getY();
                }
        }
    }

    @Test
    public void testToString(){
        assertNotNull(graph.toString());
    }

    @Test
    public void testGetSize(){
        assertNotNull(graph.getSize());
        junit.framework.Assert.assertEquals(graph.getRelations().size(), graph.getSize());
    }

    @Test
    public void testGetOrder(){
        assertNotNull(graph.getOrder());
        junit.framework.Assert.assertEquals(graph.getNodes().size(), graph.getOrder());
    }


}