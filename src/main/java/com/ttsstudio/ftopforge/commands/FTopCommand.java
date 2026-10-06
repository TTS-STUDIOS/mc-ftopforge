package com.ttsstudio.ftopforge.commands;

import com.ttsstudio.ftopforge.core.FactionSnapshot;
import com.ttsstudio.ftopforge.core.TopCache;
import com.ttsstudio.ftopforge.gui.GuiManager;
import com.ttsstudio.ftopforge.i18n.Messages;
import com.ttsstudio.ftopforge.util.MoneyFormat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class FTopCommand implements CommandExecutor {

    public enum Route {
        GUI_PAGE_1, GUI_PAGE_2,
        CHAT_PAGE_1, CHAT_PAGE_2,
        CONSOLE_GUI_NOT_ALLOWED
    }

    private final TopCache cache;
    private final Messages msg;
    private final int perPage;
    private final GuiManager guiManager;
    private final String flag;

    public FTopCommand(TopCache cache, Messages msg, int perPage, GuiManager guiManager, String flag) {
        this.cache = cache;
        this.msg = msg;
        this.perPage = Math.max(1, perPage);
        this.guiManager = guiManager;
        this.flag = flag == null ? "auto" : flag.toLowerCase();
    }

    /** Pure-logic routing decision. Page-1 vs page-2 distinction is for test coverage of the boundary. */
    public static Route decide(String[] args, boolean isPlayer, String flag) {
        boolean wantGui;
        int parsedPage = 1;
        if (args.length > 0 && args[0].equalsIgnoreCase("gui")) {
            if (!isPlayer) return Route.CONSOLE_GUI_NOT_ALLOWED;
            wantGui = true;
            if (args.length > 1) parsedPage = parsePage(args[1]);
        } else if (args.length > 0 && args[0].equalsIgnoreCase("chat")) {
            wantGui = false;
            if (args.length > 1) parsedPage = parsePage(args[1]);
        } else if (args.length > 0) {
            wantGui = false; // numeric page implicit chat (legacy)
            parsedPage = parsePage(args[0]);
        } else {
            if (!isPlayer) {
                wantGui = false;
            } else {
                String f = flag == null ? "auto" : flag.toLowerCase();
                wantGui = !f.equals("false");
            }
        }
        if (wantGui) {
            return parsedPage <= 1 ? Route.GUI_PAGE_1 : Route.GUI_PAGE_2;
        }
        return parsedPage <= 1 ? Route.CHAT_PAGE_1 : Route.CHAT_PAGE_2;
    }

    private static int parsePage(String s) {
        try { return Math.max(1, Integer.parseInt(s)); } catch (NumberFormatException e) { return 1; }
    }

    private static int extractPage(String[] args) {
        if (args.length == 0) return 1;
        if (args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("chat")) {
            return args.length > 1 ? parsePage(args[1]) : 1;
        }
        return parsePage(args[0]);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean isPlayer = sender instanceof Player;
        Route route = decide(args, isPlayer, flag);
        int page = extractPage(args);

        if (route == Route.CONSOLE_GUI_NOT_ALLOWED) {
            sender.sendMessage(msg.get("errors.must-be-player"));
            return true;
        }

        int total = cache.size();
        if (total == 0) {
            sender.sendMessage(msg.get("gui.top.empty"));
            return true;
        }

        if (route == Route.GUI_PAGE_1 || route == Route.GUI_PAGE_2) {
            if (guiManager != null) {
                guiManager.openTop((Player) sender, page);
                return true;
            }
            // GUI requested but module disabled; fall through to chat
        }

        // Chat fallback (legacy Custom 7 behavior)
        int pages = (int) Math.ceil((double) total / perPage);
        if (page > pages) page = pages;
        int from = (page - 1) * perPage;
        int to = Math.min(from + perPage, total);
        List<FactionSnapshot> top = cache.top(total);
        sender.sendMessage(msg.get("command.ftop.header", "page", String.valueOf(page)));
        for (int i = from; i < to; i++) {
            FactionSnapshot s = top.get(i);
            sender.sendMessage(msg.get("command.ftop.line",
                    "rank", String.valueOf(i + 1),
                    "faction_name", s.factionName(),
                    "value", MoneyFormat.shortFmt(s.totalValue())));
        }
        return true;
    }
}
