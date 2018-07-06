package com.rakuten.challenge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.rakuten.challenge.algorithm.Dijkstra;
import com.rakuten.challenge.model.Graph;
import com.rakuten.challenge.model.Node;

public class Application {
    public static void main(String... arg) throws IOException {
        // String first = arg[0];
        String first = "test.file";
        var lines = Files.readAllLines(Paths.get(first));
        if (lines.isEmpty()) {
            System.out.println("nothing to process bye bye");
        }

        // read first line number of scenarios
        var scenarios = Integer.valueOf(lines.get(0).trim());

        // remove header lines
        lines = lines.subList(2, lines.size());

        var lineNumber = 0;
        // for every scenario need to construct the elements
        for (var scenario = 1; scenario <= scenarios; scenario++) {
            String[] line = lines.get(lineNumber).split(" ");
            if (line.length != 2) {
                System.out.println("bad File format");
            }

            // starting node
            Graph graph = new Graph();

            graph.addNode(new Node(0));
            var stores = Integer.valueOf(line[0]);
            // create stores ==> nodes
            for (var store = 1; store <= stores; store++) {
                graph.addNode(new Node(store));
            }

            // create connections
            var roads = Integer.valueOf(line[1]);
            for (var road = 1; road <= roads; road++) {
                // create nodes with store number and create
                String[] roadLine = lines.get(lineNumber + road).split(" ");
                var start = Integer.valueOf(roadLine[0]);
                var end = Integer.valueOf(roadLine[1]);
                var weight = Double.valueOf(roadLine[2]);

                graph.getNode(start).addDestination(graph.getNode(end), weight);
                graph.getNode(end).addDestination(graph.getNode(start), weight);
            }

            // get necessary shops
            var prices = new HashMap<Integer, Double>();
            var dvds = Integer.valueOf(lines.get(lineNumber + roads + 1));
            for (var dvd = 1; dvd <= dvds; dvd++) {
                String[] optimizations = lines.get(lineNumber + roads + dvd + 1).split(" ");
                var shopNumber = Integer.valueOf(optimizations[0]);
                var optimization = Double.valueOf(optimizations[1]);

                graph.getNode(shopNumber).setPrize(optimization);
                prices.put(shopNumber, optimization);
            }

            // INIT ALL VARS FROM SCENARIO FINISHED

            // nssa hada hada retour
            var initialGraph = Dijkstra.calculateShortestPathFromSource(graph, graph.getNode(0));

            List<Node> bestRoute = new ArrayList<>();
            Double totalDist = 0d;

            List<Integer> leftShops = new ArrayList<>(prices.keySet());
            Node start = graph.getNode(0);
            while (!leftShops.isEmpty()) {
                Node checkpoint = nextNode(graph, initialGraph, leftShops, start);
                if (start == checkpoint) {
                    break;
                }

                start = checkpoint;
                totalDist += checkpoint.getDistance();
                bestRoute.addAll(checkpoint.getShortestPath().subList(1, checkpoint.getShortestPath().size()));
                bestRoute.add(checkpoint);

                leftShops.remove(checkpoint.getName());
                leftShops.removeAll(checkpoint.getShortestPath().stream().map(Node::getName).collect(Collectors.toList()));
            }

            System.out.println("=== === === === ===");
            System.out.println("best optim I think is : ");
            System.out.print("Node : " + 0);
            bestRoute.stream().map(node -> " -> Node : " + node.getName()).forEach(System.out::print);
            Node stopNode = initialGraph.getNode(bestRoute.get(bestRoute.size() - 1).getName());
            List<Node> returnPath = stopNode.getShortestPath();
            Collections.reverse(returnPath);
            returnPath.stream().map(node -> " -> Node : " + node.getName()).forEach(System.out::print);
            System.out.println();
            double sum = bestRoute.stream().distinct().mapToDouble(Node::getPrize).sum();
            System.out.println("total gain is " + (sum - totalDist - stopNode.getDistance()));

            lineNumber = lineNumber + dvds + roads + 3;
        }
    }

    private static Node nextNode(Graph graph, Graph initialGraph, List<Integer> leftShops, Node firstOptimisation) {
        Graph copy2 = graph.copy();
        Node start2 = copy2.getNode(firstOptimisation.getName());

        var calculatedGraph2 = Dijkstra.calculateShortestPathFromSource(copy2, start2);

        // calculation of profit
        Node firstOptimisation2 = firstOptimisation;
        var firstOpt2 = 0d;
        for (int i = 0, integersSize = leftShops.size(); i < integersSize; i++) {
            Integer shop = leftShops.get(i);
            Double retour = initialGraph.getNode(shop).getDistance();
            Double dist = calculatedGraph2.getNode(shop).getDistance();

            var opt = calculatedGraph2.getNode(shop).getPrize();

            for (var node : calculatedGraph2.getNode(shop).getShortestPath()) {
                if (leftShops.contains(node.getName())) {
                    opt += calculatedGraph2.getNode(node.getName()).getPrize();
                }
            }
            if (opt - retour - dist > firstOpt2) {
                firstOpt2 = opt - retour - dist;
                firstOptimisation2 = calculatedGraph2.getNode(shop);
            }

            // TODO : if a shop is on the same road needs to check for cross roads
            // System.out.println("shop: " + shop + " dist: " + dist + " opt: " + opt);
        }
        return firstOptimisation2;
    }
}
