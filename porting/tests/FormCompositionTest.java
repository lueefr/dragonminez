import com.dragonminez.api.forms.FormCompositionResolver;
import com.dragonminez.api.forms.FormCompositionResolver.*;
import java.util.*;

public final class FormCompositionTest {
    private static Multipliers m(double v) { return new Multipliers(v,v,v,v,v,v,v,v); }
    private static Definition f(String id, String base, String inherits, double value) {
        return new Definition(id, base, inherits, m(value));
    }
    private static void equal(double expected, double actual) {
        if (Math.abs(expected-actual)>1e-9) throw new AssertionError(expected + " != " + actual);
    }
    public static void main(String[] args) {
        var resolver = new FormCompositionResolver();
        var definitions = List.of(f("g.a",null,null,2), f("g.b",null,null,4),
                f("custom.root","best",null,1.5), f("custom.child",null,"custom.root",1.2));
        resolver.reload(definitions);
        UUID one=UUID.randomUUID(), two=UUID.randomUUID();
        resolver.updatePlayer(one,new PlayerState(1,"custom.child",Set.of("g.a","custom.root","custom.child"),Map.of()));
        resolver.updatePlayer(two,new PlayerState(1,null,Set.of("g.b"),Map.of()));
        Multipliers first=resolver.resolve(one,"custom.child");
        equal(3.6,first.str()); // Descendants cannot become their own dynamic ancestor.
        if (first != resolver.resolve(one,"custom.child")) throw new AssertionError("Expected cached value");
        equal(7.2,resolver.resolve(two,"custom.child").str());
        resolver.updatePlayer(one,new PlayerState(2,null,Set.of("g.a","g.b"),Map.of()));
        equal(7.2,resolver.resolve(one,"custom.child").str());
        resolver.reload(List.of(f("g.a",null,null,3),f("custom.root","best",null,1.5),f("custom.child",null,"custom.root",1.2)));
        resolver.updatePlayer(one,new PlayerState(2,null,Set.of("g.a"),Map.of()));
        equal(5.4,resolver.resolve(one,"custom.child").str());
        try {
            resolver.reload(List.of(f("g.a",null,"g.b",1),f("g.b",null,"g.a",1)));
            throw new AssertionError("Cycle accepted");
        } catch (IllegalArgumentException expected) {
            if (!expected.getMessage().contains("g.a -> g.b -> g.a")) throw expected;
        }
        // Rejected reloads leave the previously validated config intact.
        equal(5.4,resolver.resolve(one,"custom.child").str());
        resolver.reload(List.of(f("g.a","current",null,2),f("g.b",null,null,3)));
        resolver.updatePlayer(one,new PlayerState(3,"g.b",Set.of("g.a","g.b"),Map.of()));
        equal(6,resolver.resolve(one,"g.a").str());
        resolver.updatePlayer(one,new PlayerState(4,"g.a",Set.of("g.a","g.b"),Map.of()));
        equal(2,resolver.resolve(one,"g.a").str());
        System.out.println("Form composition: chain, unlock, config reload, player isolation, cache, self exclusion, static cycles and current reference passed.");
    }
}
