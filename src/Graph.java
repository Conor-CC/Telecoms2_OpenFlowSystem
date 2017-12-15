import java.util.List;

public class Graph {

    private List<Vertex> vertices;
    private List<Edge> edges;

    Graph (List<Vertex> vertices, List<Edge> edges) {
        this.vertices = vertices;
        this.edges = edges;
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public List<Edge> getEdges() {
        return edges;
    }
}
