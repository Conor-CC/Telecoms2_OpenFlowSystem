import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DjikstraAlgo {

    private List<Vertex> nodes;
    private List<Edge> edges;
    private Set<Vertex> settledNodes;
    private Set<Vertex> unsettledNodes;
    private Map<Vertex, Vertex> predecessors;
    private Map<Vertex, Integer> distance;
    private String dstClient;

    DjikstraAlgo(Graph graph) {
        this.nodes = new ArrayList<Vertex>(graph.getVertices());
        this.edges = new ArrayList<Edge>(graph.getEdges());
    }

    public void executeAlgorithm(Vertex sourceNode, String dstClient) {
        this.dstClient = dstClient;
        settledNodes = new HashSet<Vertex>();
        unsettledNodes = new HashSet<Vertex>();
        distance = new HashMap<Vertex, Integer>();
        predecessors = new HashMap<Vertex, Vertex>();
        distance.put(sourceNode, 0); //Put source node in first and set its distance to zero
        unsettledNodes.add(sourceNode);
        while (unsettledNodes.size() > 0) {
            Vertex node = getMinimum(unsettledNodes);
            settledNodes.add(node);
            unsettledNodes.remove(node);
            findMinimumDistances(node);                 //Gets node with shortest distance
        }
    }

    private void findMinimumDistances(Vertex node) {
        List<Vertex> adjacentNodes = getNeighbours(node);
        for (Vertex target: adjacentNodes) {
            if (target.getId().equals(dstClient)) {
                distance.put(target, getShortestDistance(node) + getDistance(node, target));
                predecessors.put(target, node);
                unsettledNodes.add(target);
            }
            else if (target.getId().contains("R") && getShortestDistance(target) > getShortestDistance(node) + getDistance(node, target)) {
                distance.put(target, getShortestDistance(node) + getDistance(node, target));
                predecessors.put(target, node);
                unsettledNodes.add(target);
            }
        }
    }

    private int getDistance(Vertex vertex, Vertex targetVertex) {
        for (Edge edge : edges) {
            if (edge.getSourceVertex().equals(vertex) && edge.getDestVertex().equals(targetVertex)) {
                return edge.getWeight();
            }
        }
        throw new RuntimeException("Not possible");
    }

    private List<Vertex> getNeighbours(Vertex node) {
        List<Vertex> neighbors = new ArrayList<Vertex>();
        for (Edge edge : edges) {
            if (edge.getSourceVertex().equals(node) && !isSettled(edge.getDestVertex())) {
                neighbors.add(edge.getDestVertex());
            }
        }
        return neighbors;
    }

    private Vertex getMinimum(Set<Vertex> vertices) {
        Vertex min = null;
        for (Vertex vertex : vertices) {
            if (min == null) {
                min = vertex;
            }
            else {
                if (getShortestDistance(vertex) < getShortestDistance(min)) {
                    min = vertex;
                }
            }
        }
        return min;
    }

    private boolean isSettled(Vertex vertex) {
        if (settledNodes.contains(vertex)) {
            return true;
        }
        return false;
    }

    private int getShortestDistance(Vertex dest) {
        Integer d = distance.get(dest);
        if (d == null) {
            return Integer.MAX_VALUE; //Assume distance is HUGE until we find out otherwise
        }
        else {
            return d;
        }
    }

    public ArrayList<Vertex> generatePath(Vertex dest) {
        ArrayList<Vertex> path = new ArrayList<Vertex>();
        Vertex v = dest;

        if (predecessors.get(v) == null) {
            return null;
        }
        path.add(v);
        while (predecessors.get(v) != null) {
            v = predecessors.get(v);
            path.add(v);
        }
        Collections.reverse(path);
        return path;
    }



}


