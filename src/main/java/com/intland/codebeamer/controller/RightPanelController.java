package com.intland.codebeamer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intland.codebeamer.ajax.JsonView;
import com.intland.codebeamer.manager.TrackerItemManager;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Controller
public class RightPanelController {

    @Autowired
    private TrackerItemManager trackerItemManager;

    private final ObjectMapper jsonMapper = new ObjectMapper();

    /**
     * 点击右侧 custom-tab 时，POST 到 /trackers/ajax/custom_item.spr?itemId=123
     * 返回当前 Item 的 name、description、status 等字段的 JSON。
     */
    @RequestMapping(
            value  = "/trackers/ajax/custom_item.spr",
            method = RequestMethod.POST,
            params = "itemId"
    )
    public JsonView getCustomItemData(
            HttpServletRequest request,
            @RequestParam("itemId") String itemIdStr
    ) throws Exception {
        // —— 1. 从 Session 中直接取 currentUser ——
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new IllegalStateException("Session 不存在，用户可能未登录");
        }
        UserDto user = (UserDto) session.getAttribute("currentUser");
        if (user == null) {
            throw new IllegalStateException("Session 中未找到 currentUser，请确认已登录");
        }

        // —— 2. 解析 itemId，加载 TrackerItemDto（含权限检查） ——
        int itemId;
        try {
            itemId = Integer.parseInt(itemIdStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("itemId 必须是数字: " + itemIdStr, e);
        }
        TrackerItemDto item = trackerItemManager.findById(user, itemId);
        if (item == null) {
            throw new NoSuchElementException("未找到 ID=" + itemId + " 的 TrackerItem");
        }

        // —— 3. 按需取字段，封装到 Map ——
        Map<String, Object> result = new HashMap<>();
        result.put("id",          item.getId());
        result.put("name",        item.getName());
        result.put("description", item.getDescription());
        result.put("status",      item.getStatus().getName());
        // 如需更多字段，可继续 put()

        // —— 4. 序列化并返回 ——
        String json = jsonMapper.writeValueAsString(result);
        return new JsonView(json);
    }
}
