package mindustry.plugin;

import java.awt.*;
import java.io.File;
import java.util.*;

import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.util.*;
import arc.util.Timer;
import com.google.gson.Gson;
import mindustry.core.NetClient;
//import mindustry.entities.Effects;
//import mindustry.entities.traits.Entity;
//import mindustry.entities.type.BaseUnit;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.net.Administration;
import mindustry.net.Administration.Config;
import mindustry.plugin.requests.GetMap;
import mindustry.world.Tile;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.json.JSONObject;
import org.json.JSONTokener;

import arc.Core;
import arc.Events;
import mindustry.Vars;
//import mindustry.entities.type.Player;
import mindustry.game.EventType;
import mindustry.gen.Call;
import redis.clients.jedis.JedisPool;

import static mindustry.Vars.*;
import static mindustry.plugin.Utils.*;

public class ioMain extends Plugin {
//    public static final File prefsFile = new File("prefs.properties");
//    public static Net net = new Net();
//    public static Prefs prefs = new Prefs(prefsFile);
//    public GetMap map = new GetMap();

    public static JedisPool pool;
    static Gson gson = new Gson();

    private final long CDT = 300L;
    private ObjectMap<Long, String> cooldowns = new ObjectMap<>(); //uuid
    public static DiscordApi api = null;
    public static String prefix = ".";
    public static String serverName = "<untitled>";

//    public static HashMap<String, PersistentPlayerData> playerDataGroup = new HashMap<>(); // uuid(), data


    public ObjectMap<String, Role> discRoles = new ObjectMap<>();
    public ObjectMap<String, TextChannel> discChannels = new ObjectMap<>();

    private final String fileNotFoundErrorMessage = "File not found: config\\mods\\settings.json";
    private JSONObject alldata;
    public static JSONObject data; //token, channel_id, role_id
    public static String apiKey = "";

    protected Interval timer = new Interval(1);

    //register event handlers and create variables in the constructor
    public ioMain() {
        Utils.init();

        try {
            String pureJson = Core.settings.getDataDirectory().child("mods/settings.json").readString();
            data = alldata = new JSONObject(new JSONTokener(pureJson));
        } catch (Exception e) {
            Log.err("Couldn't read settings.json file.");
        }
        try {
            api = new DiscordApiBuilder().setToken(alldata.getString("token")).login().join();
            Log.info("logged in as: " + api.getYourself());
        }catch (Exception e){
            Log.err("Couldn't log into discord.");
        }
        BotThread bt = new BotThread(api, Thread.currentThread(), alldata);
        bt.setDaemon(false);
        bt.start();

        TextChannel tc = getTextChannel("881300954845179914");
        if (tc != null) {
            Events.on(EventType.PlayerChatEvent.class, event -> {
                if (event.message.charAt(0) != '/') {
                    tc.sendMessage("**" + Strings.stripColors(event.player.name) + "**: " + event.message);
                }
            });
        }

        // database
        try {
//            pool = new JedisPool(new JedisPoolConfig(), "localhost");
//            pool = new JedisPool();
            Log.info("jedis database loaded");
        } catch (Exception e){
            e.printStackTrace();
            Core.app.exit();
        }

        // setup prefix
        if (data.has("prefix")) {
            prefix = String.valueOf(data.getString("prefix").charAt(0));
        } else {
            Log.warn("Prefix not found, using default '.' prefix.");
        }

        // setup name
        if (data.has("server_name")) {
            serverName = String.valueOf(data.getString("server_name"));
        } else {
            Log.warn("No server name setting detected!");
        }

        if(data.has("api_key")){
            apiKey = data.getString("api_key");
            Log.info("api_key set successfully");
        }

        // display on screen messages
        float duration = 10f;
        int start = 450;
        int increment = 30;

        Timer.schedule(() -> {
            int currentInc = 0;
            for(String msg : onScreenMessages){
                Call.infoPopup(msg, duration, 20, 50, 20, start + currentInc, 0);
                currentInc = currentInc + increment;
            }
        }, 0, 10);

        // update every tick


        // player joined
//        Events.on(EventType.PlayerJoin.class, event -> {
//            Player player = event.player;
//            if(bannedNames.contains(player.name)) {
//                player.con.kick("[scarlet]Download the game from legitimate sources to join.\n[accent]https://anuke.itch.io/mindustry");
//                return;
//            }
//
//            PlayerData pd = getData(player.uuid());
//
//            if (!playerDataGroup.containsKey(player.uuid())) {
//                PersistentPlayerData data = new PersistentPlayerData();
//                playerDataGroup.put(player.uuid(), data);
//            }
//
//            if(pd != null) {
//                try {
//                    if (pd.discordLink == null) {
//                        pd.reprocess();
//                        setData(player.uuid(), pd);
//                    }
//                } catch (Exception ignored){
//                    pd.reprocess();
//                    setData(player.uuid(), pd);
//                }
//                if(pd.banned || pd.bannedUntil > Instant.now().getEpochSecond()){
//                    player.con.kick("[scarlet]You are banned.[accent] Reason:\n" + pd.banReason);
//                }
//                int rank = pd.rank;
//                switch (rank) { // apply new tag
//                    case 0:
//                        break;
//                    case 1:
//                        Call.sendMessage("[#91f063]Active player [] " + player.name + " joined the server!");
//                        player.name = "[white][][accent][] " + player.name;
//                        break;
//                    case 2:
//                        Call.sendMessage("[#dfd06e]Veteran player[] " + player.name + " joined the server!");
//                        player.name = "[white][][accent][] " + player.name;
//                        break;
//                    case 3:
//                        Call.sendMessage("[#bf7134]Contributor [] " + player.name + " joined the server!");
//                        player.name = "[white][][accent][] " + player.name;
//                        break;
//                    case 4:
//                        Call.sendMessage("[orange]<[][white]io [#9c59ce]Moderator[][orange]>[] " + player.name + " joined the server!");
//                        player.name = "[white][][accent][] " + player.name;
//                        break;
//                    case 5:
//                        Call.sendMessage("[orange]<[][white]io [#00f8fd]Admin[][orange]>[] " + player.name + " joined the server!");
//                        player.name = "[white][][accent][] " + player.name;
//                        break;
//                }
//            } else { // not in database
//                setData(player.uuid(), new PlayerData(0));
//                Call.onInfoMessage(player.con, formatMessage(player, welcomeMessage));
//            }
//
//            CompletableFuture.runAsync(() -> {
//                if(verification) {
//                    if (pd != null && !pd.verified) {
//                        Log.info("Unverified player joined: " + player.name);
//                        String url = "http://api.vpnblocker.net/v2/json/" + player.con.address + "/" + apiKey;
//                        String pjson = ClientBuilder.newClient().target(url).request().accept(MediaType.APPLICATION_JSON).get(String.class);
//
//                        JSONObject json = new JSONObject(new JSONTokener(pjson));
//                        if (json.has("host-ip")) {
//                            if (json.getBoolean("host-ip")) { // verification failed
//                                Log.info("IP verification failed for: " + player.name);
//                                Call.onInfoMessage(player.con, verificationMessage);
//                            } else {
//                                Log.info("IP verification success for: " + player.name);
//                                pd.verified = true;
//                                setData(player.uuid(), pd);
//                            }
//                        } else { // site doesn't work for some reason  ?
//                            pd.verified = true;
//                            setData(player.uuid(), pd);
//                        }
//                    }
//                }
//            });
//        });

        // player built building
//        Events.on(EventType.BlockBuildEndEvent.class, event -> {
//            if (event.player == null) return;
//            if (event.breaking) return;
//            PlayerData pd = getData(event.player.uuid());
//            PersistentPlayerData td = (playerDataGroup.getOrDefault(event.player.uuid(), null));
//            if (pd == null || td == null) return;
//            if (event.tile.entity != null) {
//                if (!activeRequirements.bannedBlocks.contains(event.tile.block())) {
//                    td.bbIncrementor++;
//                }
//            }
//        });

        Events.on(EventType.ServerLoadEvent.class, event -> {
            // action filter
            Vars.netServer.admins.addActionFilter(action -> {
                Player player = action.player;
                if (player == null) return true;

                if (player.admin) return true;

                return action.type != Administration.ActionType.rotate;
            });
        });

//        Events.on(EventType.GameOverEvent.class, event -> {
//            for (Player p : playerGroup.all()) {
//                PlayerData pd = getData(p.uuid());
//                if (pd != null) {
//                    pd.gamesPlayed++;
//                    setData(p.uuid(), pd);
//                    Call.onInfoToast(p.con, "[accent]+1 games played", 10);
//                }
//            }
//        });

//        Events.on(EventType.WorldLoadEvent.class, event -> {
//            Timer.schedule(MapRules::run, 5); // idk
//        });

        // TODO: remove this when MapRules is back in use
//        Events.on(EventType.ServerLoadEvent.class, event -> {
//            // action filter
//            Vars.netServer.admins.addActionFilter(action -> {
//                Player player = action.player;
//                if (player == null) return true;
//
//                // disable checks for admins
//                if (player.isAdmin) return true;
//
//                PersistentPlayerData tdata = (playerDataGroup.getOrDefault(action.player.uuid(), null));
//                if (tdata == null) { // should never happen
//                    player.sendMessage("[scarlet]You may not build right now due to a server error, please tell an administrator");
//                    return false;
//                }
//
//                switch (action.type) {
//                    case rotate: {
//                        boolean hit = tdata.rotateRatelimit.get();
//                        if (hit) {
//                            player.sendMessage("[scarlet]Rotate ratelimit exceeded, please rotate slower");
//                            return false;
//                        }
//                        break;
//                    }
//
//                    case configure: {
//                        boolean hit = tdata.configureRatelimit.get();
//                        if (hit) {
//                            player.sendMessage("[scarlet]Configure ratelimit exceeded, please configure slower");
//                            return false;
//                        }
//                        break;
//                    }
//                }
//
//                return true;
//            });
//        });
    }

    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler){

    }

    //cooldown between votes
    int voteCooldown = 120 * 1;

    public static boolean checkChatRatelimit(String message, Player player) {
        // copied almost exactly from mindustry core, will probably need updating
        // will also update the user's global chat ratelimits
        long resetTime = Config.messageRateLimit.num() * 1000;
        if(Config.antiSpam.bool() && !player.isLocal() && !player.admin){
            //prevent people from spamming messages quickly
            if(resetTime > 0 && Time.timeSinceMillis(player.getInfo().lastMessageTime) < resetTime){
                //supress message
                player.sendMessage("[scarlet]You may only send messages every " + Config.messageRateLimit.num() + " seconds.");
                player.getInfo().messageInfractions ++;
                //kick player for spamming and prevent connection if they've done this several times
                if(player.getInfo().messageInfractions >= Config.messageSpamKick.num() && Config.messageSpamKick.num() != 0){
                    player.con.kick("You have been kicked for spamming.", 1000 * 60 * 2);
                }
                return false;
            }else{
                player.getInfo().messageInfractions = 0;
            }

            //prevent players from sending the same message twice in the span of 50 seconds
            if(message.equals(player.getInfo().lastSentMessage) && Time.timeSinceMillis(player.getInfo().lastMessageTime) < 1000 * 50){
                player.sendMessage("[scarlet]You may not send the same message twice.");
                return false;
            }

            player.getInfo().lastSentMessage = message;
            player.getInfo().lastMessageTime = Time.millis();
        }
        return true;
    }

    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){
        if (api != null) {
            handler.removeCommand("t");
            handler.<Player>register("t", "<message...>", "Send a message only to your teammates.", (args, player) -> {
                String message = args[0];
                if (!checkChatRatelimit(message, player)) return;
                Groups.player.each(p -> p.team() == player.team(), o -> o.sendMessage(message, player, "[#" + player.team().color.toString() + "]<T>" + NetClient.colorizeName(player.id, player.name)));
            });

//            handler.<Player>register("d", "<text...>", "Sends a message to moderators. Use when no moderators are online and there's a griefer.", (args, player) -> {
//                if (!data.has("warnings_chat_channel_id")) {
//                    player.sendMessage("[scarlet]This command is disabled.");
//                } else {
//                    String message = args[0];
//                    if (!checkChatRatelimit(message, player)) return;
//                    TextChannel tc = getTextChannel(data.getString("warnings_chat_channel_id"));
//                    if (tc == null) {
//                        player.sendMessage("[scarlet]This command is disabled.");
//                        return;
//                    }
//                    tc.sendMessage(escapeCharacters(player.name) + " *@mindustry* : `" + message + "`");
//                    player.sendMessage("[scarlet]Successfully sent message to moderators.");
//                }
//            });

            handler.<Player>register("players", "Display all players and their ids", (args, player) -> {
                StringBuilder builder = new StringBuilder();
                builder.append("[orange]List of players: \n");
                for (Player p : Groups.player) {
                    if(p.admin) {
                        builder.append("[accent]");
                    } else{
                        builder.append("[lightgray]");
                    }
                    builder.append(p.name).append("[accent] : ").append(p.id).append("\n");
                }
                player.sendMessage(builder.toString());
            });

            handler.<Player>register("w", "<player> <text...>", "Whisper text to another player.", (args, player) -> {
                //find player by name
//            Player other = Groups.player.find(p -> p.name.equalsIgnoreCase(args[0]));

                Player other = Utils.findPlayer(args[0]);

                //give error message with scarlet-colored text if player isn't found
                if(other == null){
                    player.sendMessage("[scarlet]No player by that name found!");
                    return;
                }

                //send the other player a message, using [lightgray] for gray text color and [] to reset color
                other.sendMessage("[orange][[[gray]whisper from [#ffd37f]" + Strings.stripColors(player.name) + "[orange]]: [gray]" + args[1]);
                player.sendMessage("[orange][[[gray]whisper to [#ffd37f]" + Strings.stripColors(other.name) + "[orange]]: [gray]" + args[1]);
            });

            TextChannel tc_c = getTextChannel("881300595875643452");
            handler.<Player>register("gr", "[player] [reason...]", "Report a griefer by id (use '/gr' to get a list of ids)", (args, player) -> {
                //https://github.com/Anuken/Mindustry/blob/master/core/src/io/anuke/mindustry/core/NetServer.java#L300-L351
                for (Long key : cooldowns.keys()) {
                    if (key + CDT < System.currentTimeMillis() / 1000L) {
                        cooldowns.remove(key);
                        continue;
                    } else if (player.uuid() == cooldowns.get(key)) {
                        player.sendMessage("[scarlet]This command is on a 5 minute cooldown!");
                        return;
                    }
                }

                if (args.length == 0) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("[orange]List or reportable players: \n");
                    for (Player p : Groups.player) {
                        if (p.admin() || p.con == null) continue;

                        builder.append("[lightgray] ").append(p.name).append("[accent] (#").append(p.id).append(")\n");
                    }
                    player.sendMessage(builder.toString());
                } else {
                    Player found = null;
                    if (args[0].length() > 1 && args[0].startsWith("#") && Strings.canParseInt(args[0].substring(1))) {
                        int id = Strings.parseInt(args[0].substring(1));
                        for (Player p : Groups.player) {
                            if (p.id == id) {
                                found = p;
                                break;
                            }
                        }
                    } else {
                        for (Player p : Groups.player) {
                            if (p.name.equalsIgnoreCase(args[0])) {
                                found = p;
                                break;
                            }
                        }
                    }
                    if (found != null) {
                        if (found.admin()) {
                            player.sendMessage("[scarlet]Did you really expect to be able to report an admin?");
                        } else if (found.team() != player.team()) {
                            player.sendMessage("[scarlet]Only players on your team can be reported.");
                        } else {
                            //send message
                            if (args.length > 1) {
                                Role ro = discRoles.get("861523420076179457");
//                                Role role = .getRoleById(661155250123702302L);
                                new MessageBuilder()
                                        .setEmbed(new EmbedBuilder()
                                                .setTitle("Potential griefer online")
                                                .setDescription("<@&861523420076179457>")
                                                .addField("name", escapeColorCodes(found.name))
                                                .addField("reason", args[1])
                                                .setColor(Color.RED)
                                                .setFooter("Reported by " + player.name))
                                        .send(tc_c);
                            } else {
                                Role ro = discRoles.get("861523420076179457");
                                new MessageBuilder()
                                        .setEmbed(new EmbedBuilder()
                                                .setTitle("Potential griefer online")
                                                .setDescription("<@&861523420076179457>")
                                                .addField("name", escapeColorCodes(found.name))
                                                .setColor(Color.RED)
                                                .setFooter("Reported by " + player.name))
                                        .send(tc_c);
                            }
                            Call.sendMessage(found.name + "[sky] is reported to discord.");
                            cooldowns.put(System.currentTimeMillis() / 1000L, player.uuid());
                        }
                    } else {
                        player.sendMessage("[scarlet]No player[orange] '" + args[0] + "'[scarlet] found.");
                    }
                }
            });

//            handler.<Player>register("draugpet", "[active+] Spawn a draug mining drone for your team (disabled on pvp)", (args, player) -> {
//                if(!state.rules.pvp || player.isAdmin) {
//                    PlayerData pd = getData(player.uuid());
//                    if (pd != null && pd.rank >= 1) {
//                        PersistentPlayerData tdata = playerDataGroup.get(player.uuid());
//                        if (tdata == null) return;
//                        if (tdata.draugPets.size < pd.rank || player.isAdmin) {
//                            BaseUnit baseUnit = UnitTypes.draug.create(player.getTeam());
//                            baseUnit.set(player.getX(), player.getY());
//                            baseUnit.add();
//                            tdata.draugPets.add(baseUnit);
//                            Call.sendMessage(player.name + "[#b177fc] spawned in a draug pet! " + tdata.draugPets.size + "/" + pd.rank + " spawned.");
//                        } else {
//                            player.sendMessage("[#b177fc]You already have " + pd.rank + " draug pets active!");
//                        }
//                    } else {
//                        player.sendMessage(noPermissionMessage);
//                    }
//                } else {
//                    player.sendMessage("[scarlet]This command is disabled on pvp.");
//                }
//            });

//            handler.<Player>register("lichpet", "[contributor+] Spawn yourself a lich defense pet (max. 1 per game, lasts 2 minutes, disabled on pvp)", (args, player) -> {
//                if(!state.rules.pvp || player.isAdmin) {
//                    PlayerData pd = getData(player.uuid());
//                    if (pd != null && pd.rank >= 3) {
//                        PersistentPlayerData tdata = playerDataGroup.get(player.uuid());
//                        if (tdata == null) return;
//                        if (!tdata.spawnedLichPet || player.isAdmin) {
//                            tdata.spawnedLichPet = true;
//                            BaseUnit baseUnit = UnitTypes.lich.create(player.getTeam());
//                            baseUnit.set(player.getClosestCore().x, player.getClosestCore().y);
//                            baseUnit.health = 200f;
//                            baseUnit.add();
//                            Call.sendMessage(player.name + "[#ff0000] spawned in a lich defense pet! (lasts 2 minutes)");
//                            Timer.schedule(baseUnit::kill, 120);
//                        } else {
//                            player.sendMessage("[#42a1f5]You already spawned a lich defense pet in this game!");
//                        }
//                    } else {
//                        player.sendMessage(noPermissionMessage);
//                    }
//                } else {
//                    player.sendMessage("[scarlet]This command is disabled on pvp.");
//                }
//            });

//            handler.<Player>register("powergen", "[contributor+] Spawn yourself a power generator.", (args, player) -> {
//                if(!state.rules.pvp || player.isAdmin) {
//                    PlayerData pd = getData(player.uuid());
//                    if (pd != null && pd.rank >= 3) {
//                        PersistentPlayerData tdata = playerDataGroup.get(player.uuid());
//                        if (tdata == null) return;
//                        if (!tdata.spawnedPowerGen || player.isAdmin) {
//                            float x = player.getX();
//                            float y = player.getY();
//
//                            Tile targetTile = world.tileWorld(x, y);
//
//                            if (targetTile == null || !Build.validPlace(player.getTeam(), targetTile.x, targetTile.y, Blocks.rtgGenerator, 0)) {
//                                Call.onInfoToast(player.con, "[scarlet]Cannot place a power generator here.",5f);
//                                return;
//                            }
//
//                            tdata.spawnedPowerGen = true;
//                            targetTile.setNet(Blocks.rtgGenerator, player.getTeam(), 0);
//                            Call.onLabel("[accent]" + escapeCharacters(escapeColorCodes(player.name)) + "'s[] generator", 60f, targetTile.worldx(), targetTile.worldy());
//                            Call.onEffectReliable(Fx.explosion, targetTile.worldx(), targetTile.worldy(), 0, Pal.accent);
//                            Call.onEffectReliable(Fx.placeBlock, targetTile.worldx(), targetTile.worldy(), 0, Pal.accent);
//                            Call.sendMessage(player.name + "[#ff82d1] spawned in a power generator!");
//
//                            // ok seriously why is this necessary
//                            new Object() {
//                                private Task task;
//                                {
//                                    task = Timer.schedule(() -> {
//                                        if (targetTile.block() == Blocks.rtgGenerator) {
//                                            Call.transferItemTo(Items.thorium, 1, targetTile.drawx(), targetTile.drawy(), targetTile);
//                                        } else {
//                                            player.sendMessage("[scarlet]Your power generator was destroyed!");
//                                            task.cancel();
//                                        }
//                                    }, 0, 6);
//                                }
//                            };
//                        } else {
//                            player.sendMessage("[#ff82d1]You already spawned a power generator in this game!");
//                        }
//                    } else {
//                        player.sendMessage(noPermissionMessage);
//                    }
//                } else {
//                    player.sendMessage("[scarlet]This command is disabled on pvp.");
//                }
//            });

//            handler.<Player>register("spawn", "[active+]Skip the core spawning stage and spawn instantly.", (args, player) -> {
//                if(!state.rules.pvp || player.isAdmin) {
//                    PlayerData pd = getData(player.uuid());
//                    if (pd != null && pd.rank >= 1) {
//                        player.onRespawn(player.getClosestCore().tile);
//                        player.sendMessage("[accent]Spawned!");
//                    } else {
//                        player.sendMessage(noPermissionMessage);
//                    }
//                } else {
//                    player.sendMessage("[scarlet]This command is disabled on pvp.");
//                }
//            });

//            handler.<Player>register("stats", "<player>", "Display stats of the specified player.", (args, player) -> {
//                if(args[0].length() > 0) {
//                    Player p = findPlayer(args[0]);
//                    if(p != null){
//                        PlayerData pd = getData(p.uuid());
//                        if (pd != null) {
//                            Call.onInfoMessage(player.con, formatMessage(p, statMessage));
//                        }
//                    } else {
//                        player.sendMessage("[scarlet]Error: Player not found or offline");
//                    }
//                } else {
//                    Call.onInfoMessage(player.con, formatMessage(player, statMessage));
//                }
//            });

//            handler.<Player>register("info", "Display your stats.", (args, player) -> { // self info
//                PlayerData pd = getData(player.uuid());
//                if (pd != null) {
//                    Call.onInfoMessage(player.con, formatMessage(player, statMessage));
//                }
//            });

//            handler.<Player>register("rules", "Server rules. Please read carefully.", (args, player) -> { // self info
//                PlayerData pd = getData(player.uuid());
//                if (pd != null) {
//                    Call.onInfoMessage(player.con, formatMessage(player, ruleMessage));
//                }
//            });

//            handler.<Player>register("event", "Join an ongoing event (if there is one)", (args, player) -> { // self info
//                if(eventIp.length() > 0){
//                    Call.onConnect(player.con, eventIp, eventPort);
//                } else{
//                    player.sendMessage("[accent]There is no ongoing event at this time.");
//                }
//            });

            handler.<Player>register("maps","[page]", "Display all maps in the playlist.", (args, player) -> { // self info
                if(args.length > 0 && !Strings.canParseInt(args[0])){
                    player.sendMessage("[scarlet]'page' must be a number.");
                    return;
                }
                int commandsPerPage = 6;
                int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
                int pages = Mathf.ceil((float)Vars.maps.customMaps().size / commandsPerPage);

                page --;

                if(page >= pages || page < 0){
                    player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[scarlet].");
                    return;
                }

                StringBuilder result = new StringBuilder();
                result.append(Strings.format("[orange]-- Maps Page[lightgray] {0}[gray]/[lightgray]{1}[orange] --\n\n", (page+1), pages));

                for(int i = commandsPerPage * page; i < Math.min(commandsPerPage * (page + 1), Vars.maps.customMaps().size); i++){
                    mindustry.maps.Map map = Vars.maps.customMaps().get(i);
                    result.append("[white] - [accent]").append(escapeColorCodes(map.name())).append("\n");
                }
                player.sendMessage(result.toString());
            });

            Timekeeper vtime = new Timekeeper(voteCooldown);

            VoteSession[] currentlyKicking = {null};

            handler.<Player>register("nominate","[map...]", "[veteran+] Vote to change to a specific map.", (args, player) -> {
                if(!state.rules.pvp || player.admin) {
//                    PlayerData pd = getData(player.uuid());
//                    if (pd != null && pd.rank >= 2) {

                        mindustry.maps.Map found = getMapBySelector(args[0]);

                        if(found != null){
                            if(!vtime.get()){
                                player.sendMessage("[scarlet]You must wait " + voteCooldown/20 + " minutes between nominations.");
                                return;
                            }

                            VoteSession session = new VoteSession(currentlyKicking, found);

                            session.vote(player, 1);
                            vtime.reset();
                            currentlyKicking[0] = session;
                        }else{
                            player.sendMessage("[scarlet]No map[orange]'" + args[0] + "'[scarlet] found.");
                        }
//                    } else {
//                        player.sendMessage(noPermissionMessage);
//                    }
                } else {
                    player.sendMessage("[scarlet]This command is disabled on pvp.");
                }
            });

            handler.<Player>register("rtv", "Vote to change the map.", (args, player) -> { // self info
                if(currentlyKicking[0] == null){
                    player.sendMessage("[scarlet]No map is being voted on.");
                }else{
                    //hosts can vote all they want
                    if(player.uuid() != null && (currentlyKicking[0].voted.contains(player.uuid()) || currentlyKicking[0].voted.contains(netServer.admins.getInfo(player.uuid()).lastIP))){
                        player.sendMessage("[scarlet]You've already voted. Sit down.");
                        return;
                    }

                    currentlyKicking[0].vote(player, 1);
                }
            });

            handler.<Player>register("label", "<duration> <text...>", "[admin only] Create an in-world label at the current position.", (args, player) -> {
                if(args[0].length() <= 0 || args[1].length() <= 0) player.sendMessage("[scarlet]Invalid arguments provided.");
                if (player.admin) {
                    float x = player.getX();
                    float y = player.getY();

                    Tile targetTile = world.tileWorld(x, y);
                    Call.label(args[1], Float.parseFloat(args[0]), targetTile.worldx(), targetTile.worldy());
                } else {
                    player.sendMessage(noPermissionMessage);
                }
            });

        }

    }

    public static TextChannel getTextChannel(String id){
        Optional<Channel> dc = api.getChannelById(id);
        if (dc.isEmpty()) {
            Log.err("[ERR!] discordplugin: channel not found! " + id);
            return null;
        }
        Optional<TextChannel> dtc = dc.get().asTextChannel();
        if (dtc.isEmpty()){
            Log.err("[ERR!] discordplugin: textchannel not found! " + id);
            return null;
        }
        return dtc.get();
    }

}