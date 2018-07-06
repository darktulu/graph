package com.rakuten.challenge.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Graph {
    private Map<Integer, Node> nodes = new HashMap<>();

    public Graph() {
    }

    public void addNode(Node nodeA) {
        nodes.put(nodeA.getName(), nodeA);
    }

    public Node getNode(Integer nodeA) {
        return nodes.get(nodeA);
    }

    public Set<Node> getNodes() {
        return new HashSet<>(nodes.values());
    }

    public Graph copy() {
        Graph graph = new Graph();
        // init all nodes
        for (Integer key : nodes.keySet()) {
            Node node = new Node(key, nodes.get(key).getUsed());
            node.setPrize(nodes.get(key).getPrize());
            graph.addNode(node);
        }

        // init roads
        for (Integer key : nodes.keySet()) {
            Map<Node, Double> adjacent = new HashMap<>();
            Node node = nodes.get(key);
            Map<Node, Double> adjacentNodes = node.getAdjacentNodes();
            for (Node adja : adjacentNodes.keySet()) {
                adjacent.put(graph.getNode(adja.getName()), adjacentNodes.get(adja));
            }
            graph.getNode(key).setAdjacentNodes(adjacent);
        }
        return graph;
    }
}
