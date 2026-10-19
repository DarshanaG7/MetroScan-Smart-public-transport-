import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/** MetroScan REST API + static file server. Zero dependencies (JDK 17+). */
public class MetroScanServer {
    record Route(String no, String to, int freqMins, int offset) {}
    record Place(String name, String type, int walkMins) {}
    record Stop(String id, String name, List<Route> routes, List<Place> places) {}

    static final Map<String, Stop> STOPS = new HashMap<>();
    static {
        STOPS.put("MG01", new Stop("MG01", "MG Road Metro",
            List.of(new Route("500D", "Kempegowda Bus Station", 12, 3),
                    new Route("335E", "Whitefield", 15, 7),
                    new Route("G4", "Indiranagar", 20, 1)),
            List.of(new Place("Cubbon Park", "Park", 8),
                    new Place("UB City", "Shopping", 6),
                    new Place("Church Street", "Food", 5))));
        STOPS.put("MJ02", new Stop("MJ02", "Majestic Bus Stand",
            List.of(new Route("401", "Electronic City", 10, 2),
                    new Route("201", "Banashankari", 18, 5)),
            List.of(new Place("Cubbon Park", "Park", 20),
                    new Place("Vidhana Soudha", "Landmark", 18))));
    }

    static String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }

    static String stopJson(Stop s) {
        LocalTime t = LocalTime.now();
        int nowMin = t.getHour() * 60 + t.getMinute();
        String routes = s.routes().stream().map(r -> {
            int next = Math.floorMod(r.offset() - nowMin, r.freqMins());
            return String.format("{\"no\":\"%s\",\"to\":\"%s\",\"arrivals\":[%d,%d,%d]}",
                esc(r.no()), esc(r.to()), next, next + r.freqMins(), next + 2 * r.freqMins());
        }).collect(Collectors.joining(","));
        String places = s.places().stream().map(p ->
            String.format("{\"name\":\"%s\",\"type\":\"%s\",\"walkMins\":%d}",
                esc(p.name()), esc(p.type()), p.walkMins())).collect(Collectors.joining(","));
        return String.format("{\"id\":\"%s\",\"name\":\"%s\",\"updated\":%d,\"routes\":[%s],\"places\":[%s]}",
            s.id(), esc(s.name()), System.currentTimeMillis(), routes, places);
    }

    static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    static void api(HttpExchange ex) throws IOException {
        String id = ex.getRequestURI().getPath().substring("/api/stops/".length());
        Stop s = STOPS.get(id);
        if (s == null) send(ex, 404, "application/json", "{\"error\":\"stop not found\"}".getBytes());
        else send(ex, 200, "application/json; charset=utf-8", stopJson(s).getBytes(StandardCharsets.UTF_8));
    }

    static void files(HttpExchange ex) throws IOException {
        Path root = Path.of("web").toAbsolutePath().normalize();
        String p = ex.getRequestURI().getPath();
        Path f = root.resolve(p.equals("/") ? "index.html" : p.substring(1)).normalize();
        if (!f.startsWith(root) || !Files.isRegularFile(f)) { send(ex, 404, "text/plain", "Not found".getBytes()); return; }
        String n = f.toString();
        String type = n.endsWith(".html") ? "text/html; charset=utf-8" : n.endsWith(".js") ? "application/javascript"
            : n.endsWith(".css") ? "text/css" : n.endsWith(".json") ? "application/json" : "application/octet-stream";
        send(ex, 200, type, Files.readAllBytes(f));
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/stops/", MetroScanServer::api);
        server.createContext("/", MetroScanServer::files);
        server.start();
        System.out.println("MetroScan running on http://localhost:" + port);
    }
}
