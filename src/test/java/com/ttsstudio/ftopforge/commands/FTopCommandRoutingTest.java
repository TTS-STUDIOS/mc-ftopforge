package com.ttsstudio.ftopforge.commands;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ttsstudio.ftopforge.commands.FTopCommand.Route.*;

class FTopCommandRoutingTest {

    @Test
    void player_noArgs_flagAuto_returnsGui() {
        assertEquals(GUI_PAGE_1, FTopCommand.decide(new String[]{}, true, "auto"));
    }

    @Test
    void player_noArgs_flagTrue_returnsGui() {
        assertEquals(GUI_PAGE_1, FTopCommand.decide(new String[]{}, true, "true"));
    }

    @Test
    void player_noArgs_flagFalse_returnsChat() {
        assertEquals(CHAT_PAGE_1, FTopCommand.decide(new String[]{}, true, "false"));
    }

    @Test
    void console_noArgs_anyFlag_returnsChat() {
        assertEquals(CHAT_PAGE_1, FTopCommand.decide(new String[]{}, false, "auto"));
        assertEquals(CHAT_PAGE_1, FTopCommand.decide(new String[]{}, false, "true"));
    }

    @Test
    void player_forceGui_returnsGui() {
        assertEquals(GUI_PAGE_1, FTopCommand.decide(new String[]{"gui"}, true, "false"));
    }

    @Test
    void player_forceChat_returnsChat() {
        assertEquals(CHAT_PAGE_1, FTopCommand.decide(new String[]{"chat"}, true, "true"));
    }

    @Test
    void console_forceGui_returnsConsoleError() {
        assertEquals(CONSOLE_GUI_NOT_ALLOWED, FTopCommand.decide(new String[]{"gui"}, false, "auto"));
    }

    @Test
    void numeric_page_returnsChat() {
        assertEquals(CHAT_PAGE_2, FTopCommand.decide(new String[]{"2"}, true, "auto"));
    }

    @Test
    void gui_with_page_number_returnsGuiPage() {
        assertEquals(GUI_PAGE_2, FTopCommand.decide(new String[]{"gui", "2"}, true, "false"));
    }

    @Test
    void chat_with_page_number_returnsChatPage() {
        assertEquals(CHAT_PAGE_2, FTopCommand.decide(new String[]{"chat", "2"}, true, "true"));
    }
}
