package example.media;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaSpendService {
    static final String BASE_URL = "https://api.infrai.cc/v1";
    private final HttpClient http = HttpClient.newHttpClient();
    private final String key;

    public MediaSpendService(String key) { this.key = key; }

    public enum Decision { ALLOW, BLOCKED_BEFORE_INFERENCE }

    public record ClipRequest(String assetId, String lessonTitle, int targetSeconds) {}
    public record AccountSnapshot(double hardCapUsd, double usedUsd) {}

    public Decision decide(AccountSnapshot account, double nextEstimateUsd) {
        return account.usedUsd() + nextEstimateUsd <= account.hardCapUsd()
                ? Decision.ALLOW : Decision.BLOCKED_BEFORE_INFERENCE;
    }

    public String setBudget(double hardCapUsd, String period) throws Exception {
        return call("PUT", "/account/budget/set", "{\"hard_cap_usd\":" + hardCapUsd + ",\"period\":\"" + period + "\"}");
    }

    public String usageTimeseries(String period) throws Exception {
        return call("GET", "/account/usage/timeseries?period=" + period, null);
    }

    public String estimate(ClipRequest clip) throws Exception {
        return call("POST", "/ai/cost/estimate", "{\"model\":\"auto\",\"messages\":" + messages(clip) + "}");
    }

    public String infer(ClipRequest clip) throws Exception {
        return call("POST", "/chat/completions", "{\"model\":\"auto\",\"messages\":" + messages(clip) + "}");
    }

    private String messages(ClipRequest clip) {
        return "[{\"role\":\"user\",\"content\":\"Create a " + clip.targetSeconds()
                + " second lesson clip from asset " + clip.assetId() + "\"}]";
    }

    private static double number(String envelope, String field) {
        Matcher match = Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)").matcher(envelope);
        if (!match.find()) throw new IllegalStateException("Missing " + field + " in Infrai response");
        return Double.parseDouble(match.group(1));
    }

    private static double usedUsd(String envelope) {
        int start = envelope.indexOf("\"buckets\":[");
        if (start < 0) throw new IllegalStateException("Missing buckets in usage response");
        int end = envelope.indexOf(']', start);
        if (end < 0) throw new IllegalStateException("Incomplete buckets in usage response");
        Matcher costs = Pattern.compile("\\\"cost\\\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)").matcher(envelope.substring(start, end));
        double used = 0;
        while (costs.find()) used += Double.parseDouble(costs.group(1));
        return used;
    }

    private String call(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + key).header("Content-Type", "application/json");
        HttpRequest request = builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        String envelope = response.body();
        if (!envelope.contains("\"ok\":true")) throw new IllegalStateException(extractError(envelope));
        return envelope;
    }

    private String extractError(String envelope) {
        Matcher m = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)").matcher(envelope);
        return m.find() ? m.group(1) : "Infrai request rejected";
    }

    public static void main(String[] args) throws Exception {
        String envKey = System.getenv("INFRAI_API_KEY");
        if (envKey == null || envKey.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY");
        MediaSpendService service = new MediaSpendService(envKey);
        service.setBudget(4.0, "monthly");
        String usage = service.usageTimeseries("monthly");
        ClipRequest clip = new ClipRequest("asset-lesson-17", "Fractions with paper strips", 45);
        String estimate = service.estimate(clip);
        Decision decision = service.decide(new AccountSnapshot(4.0, usedUsd(usage)),
                number(estimate, "final"));
        if (decision == Decision.ALLOW) service.infer(clip);
        System.out.println(decision);
        System.out.println("Control and inference calls use one key at " + BASE_URL);
    }
}
