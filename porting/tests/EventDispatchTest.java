import com.dragonminez.platform.event.*;
import java.util.*;

public final class EventDispatchTest {
    private static class Base extends Event implements ICancellableEvent {}
    private static final class Derived extends Base {}
    private static final class Handler {
        final List<String> calls=new ArrayList<>();
        @SubscribeEvent(priority=EventPriority.HIGH)
        public void cancel(Base event) { calls.add("high");event.setCanceled(true); }
        @SubscribeEvent
        public void skipped(Derived event) { calls.add("unexpected"); }
        @SubscribeEvent(priority=EventPriority.LOW,receiveCanceled=true)
        public void observe(Base event) { calls.add("low"); }
    }
    public static void main(String[] args) {
        var bus=new EventBus();var handler=new Handler();
        bus.register(handler);bus.register(handler);
        Derived event=new Derived();
        if (bus.post(event)!=event || !event.isCanceled()) throw new AssertionError();
        if (!handler.calls.equals(List.of("high","low"))) throw new AssertionError(handler.calls.toString());
        var failures=new EventBus();
        failures.addListener(Base.class,EventPriority.NORMAL,false,value -> { throw new IllegalStateException("expected"); });
        try { failures.post(new Derived());throw new AssertionError("Listener failure swallowed"); }
        catch (IllegalStateException expected) { if (!expected.getMessage().equals("expected")) throw expected; }
        System.out.println("Event dispatch: priorities, inheritance, cancellation, canceled observers, duplicate registration and propagated failures passed.");
    }
}
