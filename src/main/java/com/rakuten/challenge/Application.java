package com.rakuten.challenge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.rakuten.challenge.algorithm.Dijkstra;
import com.rakuten.challenge.model.Graph;
import com.rakuten.challenge.model.Node;

public class Application {
    public static void generateGraph() {
        Map<Integer, Integer> max = new HashMap<>();
        Set<String> used = new HashSet<>();
        Random r = new Random();
        var low = 0;
        var high = 50;
        var lines = 0;
        var notPrinted = true;
        for (int i = 0; i < 50; i++) {
            // every store linked to minimum 1 and max 5
            do {
                notPrinted = true;
                var links = r.nextInt(5) + 1;
                for (int j = 1; j <= links; j++) {
                    var store2 = r.nextInt(i + links) + i;

                    if (!used.contains(i + " " + store2) && store2 <= 50 && store2 != i) {
                        lines++;
                        used.add(i + " " + store2);
                        var weight = r.nextInt(10) + 1.0;
                        System.out.println(i + " " + store2 + " " + weight);
                        notPrinted = false;
                    }
                }
            } while (notPrinted);
        }

        System.out.println("50 " + lines);
        System.out.println("8");
        for (int i = 1; i <= 8; i++) {
            var dvd = r.nextInt(high - low) + low;
            var price = r.nextInt(10) + 5.0;
            System.out.println(dvd + " " + price);
        }
    }

    public static void main(String... arg) throws IOException {
        // generate random big graph
        // generateGraph();
        // System.out.println();

        // String first = arg[0];
        String first = "test.file";
        var lines = Files.readAllLines(Paths.get(first));
        if (lines.isEmpty()) {
            System.out.println("nothing to process bye bye");
        }

        // read first line number of scenarios
        var scenarios = parse(lines.get(0).trim());

        // remove header lines
        lines = lines.subList(2, lines.size());

        var lineNumber = 0;
        // for every scenario need to construct the elements
        for (var scenario = 1; scenario <= scenarios; scenario++) {
            var startTime = System.nanoTime();
            System.out.println("Process number " + scenario);
            String[] line = lines.get(lineNumber).split(" ");
            if (line.length != 2) {
                System.out.println("bad File format");
            }

            // starting node
            Graph graph = new Graph();

            graph.addNode(new Node(0));

            // create stores ==> nodes
            var stores = parse(line[0]);
            stores(graph, stores);

            // create connections
            var roads = parse(line[1]);
            roads(lines, lineNumber, graph, roads);

            // get necessary shops
            if (lines.size() <= lineNumber + roads + 1) {
                System.out.println("Don't leave the house");
                continue;
            }
            var prices = new HashMap<Integer, Double>();
            var dvds = parse(lines.get(lineNumber + roads + 1));
            dvds(lines, lineNumber, graph, roads, prices, dvds);
            if (dvds == 0) {
                System.out.println("Don't leave the house");
                lineNumber = lineNumber + dvds + roads + 2;
                continue;
            }
            // INIT ALL VARS FROM SCENARIO FINISHED

            // nssa hada hada retour
            var initialGraph = Dijkstra.calculateShortestPathFromSource(graph, graph.getNode(0));

            List<Node> bestRoute = new ArrayList<>();
            List<Node> bestReturnRoute = new ArrayList<>();

            // Shortest route and most profitable first way
            List<Integer> leftShops = new ArrayList<>(prices.keySet());
            Integer iterations = 0;
            Double bestOptim = - Double.MAX_VALUE;
            Integer bestIteration = 0;
            Node start = graph.getNode(0);
            while (!leftShops.isEmpty()) {
                Map<Node, Double> checkpointMap = nextNode(graph, initialGraph, leftShops, start);
                Node checkpoint = checkpointMap.keySet().stream().findFirst().get();
                // start to lose ? after i was wining ?
                iterations++;
                if (bestOptim < checkpointMap.values().stream().findFirst().get()) {
                    bestOptim = checkpointMap.values().stream().findFirst().get();
                    bestIteration = iterations;
                }

                start = checkpoint;

                leftShops.remove(checkpoint.getName());
                leftShops.removeAll(checkpoint.getShortestPath().stream().map(Node::getName).collect(Collectors.toList()));
            }

            if (bestOptim < 0) {
                System.out.println("Don't leave the house");
                lineNumber = lineNumber + dvds + roads + 3;
                continue;
            }

            // best go round i found
            start = graph.getNode(0);
            leftShops = new ArrayList<>(prices.keySet());
            for (int i = 0; i < bestIteration; i++) {
                Map<Node, Double> checkpointMap = nextNode(graph, initialGraph, leftShops, start);
                Node checkpoint = checkpointMap.keySet().stream().findFirst().get();

                start = checkpoint;
                bestRoute.addAll(checkpoint.getShortestPath().subList(1, checkpoint.getShortestPath().size()));
                bestRoute.add(checkpoint);

                leftShops.remove(checkpoint.getName());
                leftShops.removeAll(checkpoint.getShortestPath().stream().map(Node::getName).collect(Collectors.toList()));
            }

            if (bestRoute.isEmpty()) {
                System.out.println("Don't leave the house");
                lineNumber = lineNumber + dvds + roads + 3;
                continue;
            }

            // return road maybe i'll go to another road from a crossroad
            Node stopNode = initialGraph.getNode(bestRoute.get(bestRoute.size() - 1).getName());
            List<Node> returnPath = stopNode.getShortestPath();
            Collections.reverse(returnPath);

            List<Node> leftReturnPath = new ArrayList<>(returnPath);
            while (!leftReturnPath.isEmpty()) {
                Node inRouteNode = leftReturnPath.get(0);
                leftReturnPath.remove(inRouteNode);

                bestReturnRoute.add(inRouteNode);
                Node checkpoint = nextReturnNode(graph, initialGraph, leftShops, inRouteNode).keySet().stream().findFirst().get();
                if (inRouteNode.equals(checkpoint)) {
                    continue;
                }

                if (!checkpoint.getShortestPath().isEmpty()) {
                    bestReturnRoute.addAll(checkpoint.getShortestPath().subList(1, checkpoint.getShortestPath().size()));
                }
                bestReturnRoute.add(checkpoint);

                stopNode = initialGraph.getNode(checkpoint.getName());
                leftReturnPath = stopNode.getShortestPath();
                Collections.reverse(leftReturnPath);

                leftShops.remove(checkpoint.getName());
                leftShops.removeAll(checkpoint.getShortestPath().stream().map(Node::getName).collect(Collectors.toList()));
            }

            // Print the results
            printTheResults(graph, bestRoute, bestReturnRoute);

            lineNumber = lineNumber + dvds + roads + 3;
            var endTime = System.nanoTime();
            System.out.println(" ended in " + (endTime - startTime) / 1_000_000_000.0);
        }
    }

    private static void printTheResults(Graph graph, List<Node> bestRoute, List<Node> bestReturnRoute) {
        // System.out.println("=== === === === ===");
        // System.out.println("Best optimisation I think is : ");
        List<Node> totalRoute = new ArrayList<>();
        totalRoute.add(graph.getNode(0));
        totalRoute.addAll(bestRoute);
        totalRoute.addAll(bestReturnRoute);

        totalRoute.stream().map(node -> " -> Node : " + node.getName()).forEach(System.out::print);

        System.out.println();
        double sum = totalRoute.stream().distinct().mapToDouble(Node::getPrize).sum();
        double dist = IntStream.range(0, totalRoute.size() - 1)
                .mapToDouble(i -> totalRoute.get(i).getAdjacentNodes().get(totalRoute.get(i + 1)))
                .sum();

        if (sum - dist > 0) {
            String value = String.format("%.2f", sum - dist);
            System.out.println("David can save $" + value);
        } else {
            System.out.println("Don't leave the house");
        }
    }

    private static Map<Node, Double> nextNode(Graph graph, Graph initialGraph, List<Integer> leftShops, Node firstOptimisation) {
        Graph copy = graph.copy();
        Node start = copy.getNode(firstOptimisation.getName());

        var calculatedGraph = Dijkstra.calculateShortestPathFromSource(copy, start);

        // calculation of profit
        Node resultNode = firstOptimisation;
        var bestOptimisation = - Double.MAX_VALUE;
        for (int i = 0, integersSize = leftShops.size(); i < integersSize; i++) {
            Integer shop = leftShops.get(i);
            Double retour = initialGraph.getNode(shop).getDistance();
            Double dist = calculatedGraph.getNode(shop).getDistance();

            var opt = calculatedGraph.getNode(shop).getPrize();

            for (var node : calculatedGraph.getNode(shop).getShortestPath()) {
                if (leftShops.contains(node.getName())) {
                    opt += calculatedGraph.getNode(node.getName()).getPrize();
                }
            }
            var bestDist = Math.min(retour + dist, dist * 2);
            if (opt - bestDist > bestOptimisation) {
                bestOptimisation = opt - retour - dist;
                resultNode = calculatedGraph.getNode(shop);
            }

            // System.out.println("shop: " + shop + " dist: " + dist + " opt: " + opt);
        }
        Map<Node, Double> result = new HashMap<>();
        result.put(resultNode, bestOptimisation);
        return result;
    }

    private static Map<Node, Double> nextReturnNode(Graph graph, Graph initialGraph, List<Integer> leftShops, Node firstOptimisation) {
        Graph copy = graph.copy();
        Node start = copy.getNode(firstOptimisation.getName());

        var calculatedGraph = Dijkstra.calculateShortestPathFromSource(copy, start);

        // calculation of profit
        Node resultNode = firstOptimisation;
        var bestOptimisation = 0d;
        for (int i = 0, integersSize = leftShops.size(); i < integersSize; i++) {
            Integer shop = leftShops.get(i);
            Double retour = initialGraph.getNode(shop).getDistance();
            Double dist = calculatedGraph.getNode(shop).getDistance();

            var opt = calculatedGraph.getNode(shop).getPrize();

            for (var node : calculatedGraph.getNode(shop).getShortestPath()) {
                if (leftShops.contains(node.getName())) {
                    opt += calculatedGraph.getNode(node.getName()).getPrize();
                }
            }
            var bestDist = Math.min(retour + dist, dist * 2);
            if (opt - bestDist > bestOptimisation) {
                bestOptimisation = opt - retour - dist;
                resultNode = calculatedGraph.getNode(shop);
            }

            // System.out.println("shop: " + shop + " dist: " + dist + " opt: " + opt);
        }
        Map<Node, Double> result = new HashMap<>();
        result.put(resultNode, bestOptimisation);
        return result;
    }

    // PARSING METHODS NOT IMPORTANT...
    private static void stores(Graph graph, Integer stores) {
        for (var store = 1; store <= stores; store++) {
            graph.addNode(new Node(store));
        }
    }

    // PARSING METHODS NOT IMPORTANT...
    private static void roads(List<String> lines, int lineNumber, Graph graph, Integer roads) {
        for (var road = 1; road <= roads; road++) {
            // create nodes with store number and create
            String[] roadLine = lines.get(lineNumber + road).split(" ");
            var start = parse(roadLine[0]);
            var end = parse(roadLine[1]);
            var weight = parseD(roadLine[2]);

            graph.getNode(start).addDestination(graph.getNode(end), weight);
            graph.getNode(end).addDestination(graph.getNode(start), weight);
        }
    }

    // PARSING METHODS NOT IMPORTANT...
    private static void dvds(List<String> lines, int lineNumber, Graph graph,
                             Integer roads, HashMap<Integer, Double> prices, Integer dvds) {
        for (var dvd = 1; dvd <= dvds; dvd++) {
            String[] optimizations = lines.get(lineNumber + roads + dvd + 1).split(" ");
            var shopNumber = parse(optimizations[0]);
            var optimization = parseD(optimizations[1]);

            graph.getNode(shopNumber).setPrize(graph.getNode(shopNumber).getPrize() + optimization);
            prices.put(shopNumber, prices.getOrDefault(shopNumber, 0d) + optimization);
        }
    }

    // PARSING METHODS NOT IMPORTANT...
    private static Integer parse(String val) {
        try {
            return Integer.valueOf(val);
        } catch (Exception e) {
            return 0;
        }
    }

    // PARSING METHODS NOT IMPORTANT...
    private static Double parseD(String val) {
        try {
            return Double.valueOf(val);
        } catch (Exception e) {
            return 0d;
        }
    }
}
