import com.dragonminez.compat.network.NetworkDirection;
import com.dragonminez.compat.network.NetworkEvent;
import com.dragonminez.compat.network.simple.SimpleChannel;
import com.dragonminez.platform.network.ProtocolHandshake;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class NetworkMigrationTest {
    public static void main(String[] args) {
        var tasks = new ArrayList<Runnable>();
        var count = new AtomicInteger();
        var context = NetworkEvent.wrap(null, tasks::add, 123456L);
        context.get().enqueueWork(count::incrementAndGet);
        if (count.get() != 0 || tasks.size() != 1) throw new AssertionError("Executor bypassed");
        tasks.removeFirst().run();
        if (count.get() != 1 || context.get().receivedNanos() != 123456L) throw new AssertionError("Arrival timestamp lost");
        context.get().setPacketHandled(true);
        if (!context.get().isPacketHandled()) throw new AssertionError("Handled state lost");
        var type = ProtocolHandshake.type(Identifier.parse("test:network"));
        var codec = ProtocolHandshake.codec(type);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            codec.encode(buffer, new ProtocolHandshake.Version(type, "2.2+1-fingerprint"));
            var decoded = codec.decode(buffer);
            if (!decoded.type().equals(type) || !decoded.value().equals("2.2+1-fingerprint")) throw new AssertionError("Protocol codec changed data");
        } finally { buffer.release(); }
        var channel = new SimpleChannel(Identifier.parse("test:validation"), () -> "1");
        try {
            channel.messageBuilder(String.class, 1, NetworkDirection.PLAY_TO_SERVER).add();
            throw new AssertionError("Incomplete packet accepted");
        } catch (IllegalStateException expected) { }
        channel.messageBuilder(String.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, out) -> out.writeUtf(message)).decoder(FriendlyByteBuf::readUtf)
                .consumerMainThread((message, ctx) -> {}).add();
        try {
            channel.messageBuilder(String.class, 2, NetworkDirection.PLAY_TO_SERVER)
                    .encoder((message, out) -> out.writeUtf(message)).decoder(FriendlyByteBuf::readUtf)
                    .consumerMainThread((message, ctx) -> {}).add();
            throw new AssertionError("Duplicate packet class accepted");
        } catch (IllegalArgumentException expected) { }
        System.out.println("Network migration: executor scheduling, decoder arrival timestamp, handled state, protocol codec and registration validation passed.");
    }
}
