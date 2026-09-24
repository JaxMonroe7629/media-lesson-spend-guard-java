package example.media;

public class MediaSpendServiceTest {
    public static void main(String[] args) {
        MediaSpendService service = new MediaSpendService("test-key");
        MediaSpendService.AccountSnapshot account = new MediaSpendService.AccountSnapshot(4.00, 3.20);
        check(service.decide(account, 0.60) == MediaSpendService.Decision.ALLOW, "estimate at cap should proceed");
        check(service.decide(account, 0.90) == MediaSpendService.Decision.BLOCKED_BEFORE_INFERENCE, "estimate over cap should stop");
        System.out.println("MediaSpendServiceTest passed");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
