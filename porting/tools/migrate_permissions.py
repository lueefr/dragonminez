from pathlib import Path
p=Path('src/main/java/com/dragonminez/server/commands/DMZPermissions.java')
s=p.read_text()
for line in s.splitlines():
 if line.startswith('import net.neoforged.') or line.startswith('import com.dragonminez.platform.event.'):
  s=s.replace(line+'\n','')
s=s.replace('import java.util.List;', 'import java.util.List;\nimport java.util.Map;\nimport java.util.LinkedHashMap;\nimport net.fabricmc.fabric.api.permission.v1.PermissionNode;\nimport net.fabricmc.fabric.api.permission.v1.PermissionContextOwner;\nimport net.minecraft.server.permissions.Permissions;')
s=s.replace('@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME)\n','')
s=s.replace('private static final List<PermissionNode<Boolean>> NODES = new ArrayList<>();', 'public record Definition(PermissionNode<Boolean> node, String description, boolean defaultAllowed) {}\n\tprivate static final Map<PermissionNode<Boolean>, Definition> NODES = new LinkedHashMap<>();')
s=s.replace('(player, uuid, context) -> false', 'false').replace('(player, uuid, context) -> true', 'true')
a=s.index('\tprivate static PermissionNode<Boolean> register(')
b=s.index('\tpublic static boolean check(',a)
s=s[:a]+'''    private static PermissionNode<Boolean> register(String node, String description, boolean defaultAllowed) {
        var permission = PermissionNode.of(Reference.MOD_ID, node);
        if (NODES.putIfAbsent(permission, new Definition(permission, description, defaultAllowed)) != null)
            throw new IllegalArgumentException("Duplicate permission: " + node);
        return permission;
    }

    /** Complete permission catalog, including defaults, for server integrations. */
    public static List<Definition> definitions() { return List.copyOf(NODES.values()); }

    private static boolean granted(ServerPlayer player, PermissionNode<Boolean> node) {
        var definition = NODES.get(node);
        if (definition == null) throw new IllegalArgumentException("Unknown permission: " + node.key());
        return ((PermissionContextOwner) player).checkPermission(node, definition.defaultAllowed());
    }

    public static boolean hasPermission(CommandSourceStack source, PermissionNode<Boolean> node) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return granted(player, node) || granted(player, ADMIN)
                    || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        }
        return true;
    }

'''+s[b:]
p.write_text(s)
